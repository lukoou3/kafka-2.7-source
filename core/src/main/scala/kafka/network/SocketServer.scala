/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.network

import java.io.IOException
import java.net._
import java.nio.ByteBuffer
import java.nio.channels._
import java.nio.channels.{Selector => NSelector}
import java.util
import java.util.Optional
import java.util.concurrent._
import java.util.concurrent.atomic._

import kafka.cluster.{BrokerEndPoint, EndPoint}
import kafka.metrics.KafkaMetricsGroup
import kafka.network.RequestChannel.{CloseConnectionResponse, EndThrottlingResponse, NoOpResponse, SendResponse, StartThrottlingResponse}
import kafka.network.Processor._
import kafka.network.SocketServer._
import kafka.security.CredentialProvider
import kafka.server.{BrokerReconfigurable, KafkaConfig}
import kafka.utils._
import kafka.utils.Implicits._
import org.apache.kafka.common.config.ConfigException
import org.apache.kafka.common.errors.InvalidRequestException
import org.apache.kafka.common.{Endpoint, KafkaException, MetricName, Reconfigurable}
import org.apache.kafka.common.memory.{MemoryPool, SimpleMemoryPool}
import org.apache.kafka.common.metrics._
import org.apache.kafka.common.metrics.stats.{Avg, CumulativeSum, Meter, Rate}
import org.apache.kafka.common.network.ClientInformation
import org.apache.kafka.common.network.KafkaChannel.ChannelMuteEvent
import org.apache.kafka.common.network.{ChannelBuilder, ChannelBuilders, KafkaChannel, ListenerName, ListenerReconfigurable, Selectable, Send, Selector => KSelector}
import org.apache.kafka.common.protocol.ApiKeys
import org.apache.kafka.common.requests.ApiVersionsRequest
import org.apache.kafka.common.requests.{RequestContext, RequestHeader}
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.utils.{KafkaThread, LogContext, Time}
import org.slf4j.event.Level

import scala.collection._
import scala.jdk.CollectionConverters._
import scala.collection.mutable.{ArrayBuffer, Buffer}
import scala.util.control.ControlThrowable

/**
 * 总体来说SocketServer仅仅负责以下三个方面：
 *    1）建立Socket，保持和客户端的通信；
 *    2）转发客户端的Request；
 *    3）返回Response给客户端。
 *    最后通过RequestChannel与其他模块解耦。
 *
 * 下面就是这个类的解释, 感觉还是能看懂的
 * Handles new connections, requests and responses to and from broker.
 * Kafka supports two types of request planes :
 *  - data-plane :
 *    - Handles requests from clients and other brokers in the cluster.
 *    - The threading model is
 *      1 Acceptor thread per listener, that handles new connections.
 *      It is possible to configure multiple data-planes by specifying multiple "," separated endpoints for "listeners" in KafkaConfig.
 *      Acceptor has N Processor threads that each have their own selector and read requests from sockets
 *      M Handler threads that handle requests and produce responses back to the processor threads for writing.
 *  - control-plane :
 *    - Handles requests from controller. This is optional and can be configured by specifying "control.plane.listener.name".
 *      If not configured, the controller requests are handled by the data-plane.
 *    - The threading model is
 *      1 Acceptor thread that handles new connections
 *      Acceptor has 1 Processor thread that has its own selector and read requests from the socket.
 *      1 Handler thread that handles requests and produce responses back to the processor thread for writing.
 */
class SocketServer(val config: KafkaConfig,
                   val metrics: Metrics,
                   val time: Time,
                   val credentialProvider: CredentialProvider)
  extends Logging with KafkaMetricsGroup with BrokerReconfigurable {

  private val maxQueuedRequests = config.queuedMaxRequests

  private val logContext = new LogContext(s"[SocketServer brokerId=${config.brokerId}] ")
  this.logIdent = logContext.logPrefix

  private val memoryPoolSensor = metrics.sensor("MemoryPoolUtilization")
  private val memoryPoolDepletedPercentMetricName = metrics.metricName("MemoryPoolAvgDepletedPercent", MetricsGroup)
  private val memoryPoolDepletedTimeMetricName = metrics.metricName("MemoryPoolDepletedTimeTotal", MetricsGroup)
  memoryPoolSensor.add(new Meter(TimeUnit.MILLISECONDS, memoryPoolDepletedPercentMetricName, memoryPoolDepletedTimeMetricName))
  // queued.max.request.bytes, 默认-1
  private val memoryPool = if (config.queuedMaxBytes > 0) new SimpleMemoryPool(config.queuedMaxBytes, config.socketRequestMaxBytes, false, memoryPoolSensor) else MemoryPool.NONE
  // data-plane
  private val dataPlaneProcessors = new ConcurrentHashMap[Int, Processor]()
  private[network] val dataPlaneAcceptors = new ConcurrentHashMap[EndPoint, Acceptor]()
  val dataPlaneRequestChannel = new RequestChannel(maxQueuedRequests, DataPlaneMetricPrefix, time)
  // control-plane
  private var controlPlaneProcessorOpt : Option[Processor] = None
  private[network] var controlPlaneAcceptorOpt : Option[Acceptor] = None
  val controlPlaneRequestChannelOpt: Option[RequestChannel] = config.controlPlaneListenerName.map(_ =>
    new RequestChannel(20, ControlPlaneMetricPrefix, time))

  private var nextProcessorId = 0
  private var connectionQuotas: ConnectionQuotas = _
  private var startedProcessingRequests = false
  private var stoppedProcessingRequests = false

  /**
   * Starts the socket server and creates all the Acceptors and the Processors. The Acceptors
   * start listening at this stage so that the bound port is known when this method completes
   * even when ephemeral ports are used. Acceptors and Processors are started if `startProcessingRequests`
   * is true. If not, acceptors and processors are only started when [[kafka.network.SocketServer#startProcessingRequests()]]
   * is invoked. Delayed starting of acceptors and processors is used to delay processing client
   * connections until server is fully initialized, e.g. to ensure that all credentials have been
   * loaded before authentications are performed. Incoming connections on this server are processed
   * when processors start up and invoke [[org.apache.kafka.common.network.Selector#poll]].
   *
   * @param startProcessingRequests Flag indicating whether `Processor`s must be started.
   */
  def startup(startProcessingRequests: Boolean = true): Unit = {
    this.synchronized {
      connectionQuotas = new ConnectionQuotas(config, time, metrics)
      // ControlPlane的Acceptor
      createControlPlaneAcceptorAndProcessor(config.controlPlaneListener)
      /**
       * 创建一个Acceptor多个Processors
       *
       * 创建Processor线程, N=num.network.threads, 默认为3
       * server.properties:
       *    # The number of threads that the server uses for receiving requests from the network and sending responses to the network
       *    num.network.threads=3
       *
       * dataPlaneListeners: If none of those are defined, we default to PLAINTEXT://:9092
       */
      createDataPlaneAcceptorsAndProcessors(config.numNetworkThreads, config.dataPlaneListeners)
      if (startProcessingRequests) {
        this.startProcessingRequests()
      }
    }

    newGauge(s"${DataPlaneMetricPrefix}NetworkProcessorAvgIdlePercent", () => SocketServer.this.synchronized {
      val ioWaitRatioMetricNames = dataPlaneProcessors.values.asScala.iterator.map { p =>
        metrics.metricName("io-wait-ratio", MetricsGroup, p.metricTags)
      }
      ioWaitRatioMetricNames.map { metricName =>
        Option(metrics.metric(metricName)).fold(0.0)(m => Math.min(m.metricValue.asInstanceOf[Double], 1.0))
      }.sum / dataPlaneProcessors.size
    })
    newGauge(s"${ControlPlaneMetricPrefix}NetworkProcessorAvgIdlePercent", () => SocketServer.this.synchronized {
      val ioWaitRatioMetricName = controlPlaneProcessorOpt.map { p =>
        metrics.metricName("io-wait-ratio", "socket-server-metrics", p.metricTags)
      }
      ioWaitRatioMetricName.map { metricName =>
        Option(metrics.metric(metricName)).fold(0.0)(m => Math.min(m.metricValue.asInstanceOf[Double], 1.0))
      }.getOrElse(Double.NaN)
    })
    newGauge("MemoryPoolAvailable", () => memoryPool.availableMemory)
    newGauge("MemoryPoolUsed", () => memoryPool.size() - memoryPool.availableMemory)
    newGauge(s"${DataPlaneMetricPrefix}ExpiredConnectionsKilledCount", () => SocketServer.this.synchronized {
      val expiredConnectionsKilledCountMetricNames = dataPlaneProcessors.values.asScala.iterator.map { p =>
        metrics.metricName("expired-connections-killed-count", "socket-server-metrics", p.metricTags)
      }
      expiredConnectionsKilledCountMetricNames.map { metricName =>
        Option(metrics.metric(metricName)).fold(0.0)(m => m.metricValue.asInstanceOf[Double])
      }.sum
    })
    newGauge(s"${ControlPlaneMetricPrefix}ExpiredConnectionsKilledCount", () => SocketServer.this.synchronized {
      val expiredConnectionsKilledCountMetricNames = controlPlaneProcessorOpt.map { p =>
        metrics.metricName("expired-connections-killed-count", "socket-server-metrics", p.metricTags)
      }
      expiredConnectionsKilledCountMetricNames.map { metricName =>
        Option(metrics.metric(metricName)).fold(0.0)(m => m.metricValue.asInstanceOf[Double])
      }.getOrElse(0.0)
    })
  }

  /**
   * Start processing requests and new connections. This method is used for delayed starting of
   * all the acceptors and processors if [[kafka.network.SocketServer#startup]] was invoked with
   * `startProcessingRequests=false`.
   *
   * Before starting processors for each endpoint, we ensure that authorizer has all the metadata
   * to authorize requests on that endpoint by waiting on the provided future. We start inter-broker
   * listener before other listeners. This allows authorization metadata for other listeners to be
   * stored in Kafka topics in this cluster.
   *
   * @param authorizerFutures Future per [[EndPoint]] used to wait before starting the processor
   *                          corresponding to the [[EndPoint]]
   */
  def startProcessingRequests(authorizerFutures: Map[Endpoint, CompletableFuture[Void]] = Map.empty): Unit = {
    info("Starting socket server acceptors and processors")
    this.synchronized {
      if (!startedProcessingRequests) {
        startControlPlaneProcessorAndAcceptor(authorizerFutures)
        startDataPlaneProcessorsAndAcceptors(authorizerFutures)
        startedProcessingRequests = true
      } else {
        info("Socket server acceptors and processors already started")
      }
    }
    info("Started socket server acceptors and processors")
  }

  /**
   * Starts processors of the provided acceptor and the acceptor itself.
   *
   * Before starting them, we ensure that authorizer has all the metadata to authorize
   * requests on that endpoint by waiting on the provided future.
   */
  private def startAcceptorAndProcessors(threadPrefix: String,
                                         endpoint: EndPoint,
                                         acceptor: Acceptor,
                                         authorizerFutures: Map[Endpoint, CompletableFuture[Void]] = Map.empty): Unit = {
    debug(s"Wait for authorizer to complete start up on listener ${endpoint.listenerName}")
    waitForAuthorizerFuture(acceptor, authorizerFutures)
    debug(s"Start processors on listener ${endpoint.listenerName}")
    acceptor.startProcessors(threadPrefix)
    debug(s"Start acceptor thread on listener ${endpoint.listenerName}")
    if (!acceptor.isStarted()) {
      KafkaThread.nonDaemon(
        s"${threadPrefix}-kafka-socket-acceptor-${endpoint.listenerName}-${endpoint.securityProtocol}-${endpoint.port}",
        acceptor
      ).start()
      acceptor.awaitStartup()
    }
    info(s"Started $threadPrefix acceptor and processor(s) for endpoint : ${endpoint.listenerName}")
  }

  /**
   * Starts processors of all the data-plane acceptors and all the acceptors of this server.
   *
   * We start inter-broker listener before other listeners. This allows authorization metadata for
   * other listeners to be stored in Kafka topics in this cluster.
   */
  private def startDataPlaneProcessorsAndAcceptors(authorizerFutures: Map[Endpoint, CompletableFuture[Void]]): Unit = {
    val interBrokerListener = dataPlaneAcceptors.asScala.keySet
      .find(_.listenerName == config.interBrokerListenerName)
      .getOrElse(throw new IllegalStateException(s"Inter-broker listener ${config.interBrokerListenerName} not found, endpoints=${dataPlaneAcceptors.keySet}"))
    val orderedAcceptors = List(dataPlaneAcceptors.get(interBrokerListener)) ++
      dataPlaneAcceptors.asScala.filter { case (k, _) => k != interBrokerListener }.values
    orderedAcceptors.foreach { acceptor =>
      val endpoint = acceptor.endPoint
      startAcceptorAndProcessors(DataPlaneThreadPrefix, endpoint, acceptor, authorizerFutures)
    }
  }

  /**
   * Start the processor of control-plane acceptor and the acceptor of this server.
   */
  private def startControlPlaneProcessorAndAcceptor(authorizerFutures: Map[Endpoint, CompletableFuture[Void]]): Unit = {
    controlPlaneAcceptorOpt.foreach { controlPlaneAcceptor =>
      val endpoint = config.controlPlaneListener.get
      startAcceptorAndProcessors(ControlPlaneThreadPrefix, endpoint, controlPlaneAcceptor, authorizerFutures)
    }
  }

  private def endpoints = config.listeners.map(l => l.listenerName -> l).toMap

  private def createDataPlaneAcceptorsAndProcessors(dataProcessorsPerListener: Int,
                                                    endpoints: Seq[EndPoint]): Unit = {
    endpoints.foreach { endpoint =>
      connectionQuotas.addListener(config, endpoint.listenerName)
      val dataPlaneAcceptor = createAcceptor(endpoint, DataPlaneMetricPrefix)
      addDataPlaneProcessors(dataPlaneAcceptor, endpoint, dataProcessorsPerListener)
      dataPlaneAcceptors.put(endpoint, dataPlaneAcceptor)
      info(s"Created data-plane acceptor and processors for endpoint : ${endpoint.listenerName}")
    }
  }

  private def createControlPlaneAcceptorAndProcessor(endpointOpt: Option[EndPoint]): Unit = {
    endpointOpt.foreach { endpoint =>
      connectionQuotas.addListener(config, endpoint.listenerName)
      val controlPlaneAcceptor = createAcceptor(endpoint, ControlPlaneMetricPrefix)
      val controlPlaneProcessor = newProcessor(nextProcessorId, controlPlaneRequestChannelOpt.get, connectionQuotas, endpoint.listenerName, endpoint.securityProtocol, memoryPool)
      controlPlaneAcceptorOpt = Some(controlPlaneAcceptor)
      controlPlaneProcessorOpt = Some(controlPlaneProcessor)
      val listenerProcessors = new ArrayBuffer[Processor]()
      listenerProcessors += controlPlaneProcessor
      controlPlaneRequestChannelOpt.foreach(_.addProcessor(controlPlaneProcessor))
      nextProcessorId += 1
      controlPlaneAcceptor.addProcessors(listenerProcessors, ControlPlaneThreadPrefix)
      info(s"Created control-plane acceptor and processor for endpoint : ${endpoint.listenerName}")
    }
  }

  private def createAcceptor(endPoint: EndPoint, metricPrefix: String) : Acceptor = {
    val sendBufferSize = config.socketSendBufferBytes
    val recvBufferSize = config.socketReceiveBufferBytes
    val brokerId = config.brokerId
    new Acceptor(endPoint, sendBufferSize, recvBufferSize, brokerId, connectionQuotas, metricPrefix)
  }

  private def addDataPlaneProcessors(acceptor: Acceptor, endpoint: EndPoint, newProcessorsPerListener: Int): Unit = {
    val listenerName = endpoint.listenerName
    val securityProtocol = endpoint.securityProtocol
    val listenerProcessors = new ArrayBuffer[Processor]()
    // 创建3个Processor
    for (_ <- 0 until newProcessorsPerListener) {
      val processor = newProcessor(nextProcessorId, dataPlaneRequestChannel, connectionQuotas, listenerName, securityProtocol, memoryPool)
      listenerProcessors += processor
      dataPlaneRequestChannel.addProcessor(processor)
      nextProcessorId += 1
    }
    listenerProcessors.foreach(p => dataPlaneProcessors.put(p.id, p))
    // 3个Processor绑定到acceptor
    acceptor.addProcessors(listenerProcessors, DataPlaneThreadPrefix)
  }

  /**
   * Stop processing requests and new connections.
   */
  def stopProcessingRequests(): Unit = {
    info("Stopping socket server request processors")
    this.synchronized {
      dataPlaneAcceptors.asScala.values.foreach(_.initiateShutdown())
      dataPlaneAcceptors.asScala.values.foreach(_.awaitShutdown())
      controlPlaneAcceptorOpt.foreach(_.initiateShutdown())
      controlPlaneAcceptorOpt.foreach(_.awaitShutdown())
      dataPlaneRequestChannel.clear()
      controlPlaneRequestChannelOpt.foreach(_.clear())
      stoppedProcessingRequests = true
    }
    info("Stopped socket server request processors")
  }

  def resizeThreadPool(oldNumNetworkThreads: Int, newNumNetworkThreads: Int): Unit = synchronized {
    info(s"Resizing network thread pool size for each data-plane listener from $oldNumNetworkThreads to $newNumNetworkThreads")
    if (newNumNetworkThreads > oldNumNetworkThreads) {
      dataPlaneAcceptors.forEach { (endpoint, acceptor) =>
        addDataPlaneProcessors(acceptor, endpoint, newNumNetworkThreads - oldNumNetworkThreads)
      }
    } else if (newNumNetworkThreads < oldNumNetworkThreads)
      dataPlaneAcceptors.asScala.values.foreach(_.removeProcessors(oldNumNetworkThreads - newNumNetworkThreads, dataPlaneRequestChannel))
  }

  /**
   * Shutdown the socket server. If still processing requests, shutdown
   * acceptors and processors first.
   */
  def shutdown(): Unit = {
    info("Shutting down socket server")
    this.synchronized {
      if (!stoppedProcessingRequests)
        stopProcessingRequests()
      dataPlaneRequestChannel.shutdown()
      controlPlaneRequestChannelOpt.foreach(_.shutdown())
      connectionQuotas.close()
    }
    info("Shutdown completed")
  }

  def boundPort(listenerName: ListenerName): Int = {
    try {
      val acceptor = dataPlaneAcceptors.get(endpoints(listenerName))
      if (acceptor != null) {
        acceptor.serverChannel.socket.getLocalPort
      } else {
        controlPlaneAcceptorOpt.map (_.serverChannel.socket().getLocalPort).getOrElse(throw new KafkaException("Could not find listenerName : " + listenerName + " in data-plane or control-plane"))
      }
    } catch {
      case e: Exception =>
        throw new KafkaException("Tried to check server's port before server was started or checked for port of non-existing protocol", e)
    }
  }

  def addListeners(listenersAdded: Seq[EndPoint]): Unit = synchronized {
    info(s"Adding data-plane listeners for endpoints $listenersAdded")
    createDataPlaneAcceptorsAndProcessors(config.numNetworkThreads, listenersAdded)
    listenersAdded.foreach { endpoint =>
      val acceptor = dataPlaneAcceptors.get(endpoint)
      startAcceptorAndProcessors(DataPlaneThreadPrefix, endpoint, acceptor)
    }
  }

  def removeListeners(listenersRemoved: Seq[EndPoint]): Unit = synchronized {
    info(s"Removing data-plane listeners for endpoints $listenersRemoved")
    listenersRemoved.foreach { endpoint =>
      connectionQuotas.removeListener(config, endpoint.listenerName)
      dataPlaneAcceptors.asScala.remove(endpoint).foreach { acceptor =>
        acceptor.initiateShutdown()
        acceptor.awaitShutdown()
      }
    }
  }

  override def reconfigurableConfigs: Set[String] = SocketServer.ReconfigurableConfigs

  override def validateReconfiguration(newConfig: KafkaConfig): Unit = {

  }

  override def reconfigure(oldConfig: KafkaConfig, newConfig: KafkaConfig): Unit = {
    val maxConnectionsPerIp = newConfig.maxConnectionsPerIp
    if (maxConnectionsPerIp != oldConfig.maxConnectionsPerIp) {
      info(s"Updating maxConnectionsPerIp: $maxConnectionsPerIp")
      connectionQuotas.updateMaxConnectionsPerIp(maxConnectionsPerIp)
    }
    val maxConnectionsPerIpOverrides = newConfig.maxConnectionsPerIpOverrides
    if (maxConnectionsPerIpOverrides != oldConfig.maxConnectionsPerIpOverrides) {
      info(s"Updating maxConnectionsPerIpOverrides: ${maxConnectionsPerIpOverrides.map { case (k, v) => s"$k=$v" }.mkString(",")}")
      connectionQuotas.updateMaxConnectionsPerIpOverride(maxConnectionsPerIpOverrides)
    }
    val maxConnections = newConfig.maxConnections
    if (maxConnections != oldConfig.maxConnections) {
      info(s"Updating broker-wide maxConnections: $maxConnections")
      connectionQuotas.updateBrokerMaxConnections(maxConnections)
    }
    val maxConnectionRate = newConfig.maxConnectionCreationRate
    if (maxConnectionRate != oldConfig.maxConnectionCreationRate) {
      info(s"Updating broker-wide maxConnectionCreationRate: $maxConnectionRate")
      connectionQuotas.updateBrokerMaxConnectionRate(maxConnectionRate)
    }
  }

  private def waitForAuthorizerFuture(acceptor: Acceptor,
                                      authorizerFutures: Map[Endpoint, CompletableFuture[Void]]): Unit = {
    //we can't rely on authorizerFutures.get() due to ephemeral ports. Get the future using listener name
    authorizerFutures.forKeyValue { (endpoint, future) =>
      if (endpoint.listenerName == Optional.of(acceptor.endPoint.listenerName.value))
        future.join()
    }
  }

  // `protected` for test usage
  protected[network] def newProcessor(id: Int, requestChannel: RequestChannel, connectionQuotas: ConnectionQuotas, listenerName: ListenerName,
                                      securityProtocol: SecurityProtocol, memoryPool: MemoryPool): Processor = {
    new Processor(id,
      time,
      config.socketRequestMaxBytes,
      requestChannel,
      connectionQuotas,
      config.connectionsMaxIdleMs, // connections.max.idle.ms, 默认10分钟：10 * 60 * 1000L
      config.failedAuthenticationDelayMs,
      listenerName,
      securityProtocol,
      config,
      metrics,
      credentialProvider,
      memoryPool,
      logContext,
    )
  }

  // For test usage
  private[network] def connectionCount(address: InetAddress): Int =
    Option(connectionQuotas).fold(0)(_.get(address))

  // For test usage
  private[network] def dataPlaneProcessor(index: Int): Processor = dataPlaneProcessors.get(index)

}

object SocketServer {
  val MetricsGroup = "socket-server-metrics"
  val DataPlaneThreadPrefix = "data-plane"
  val ControlPlaneThreadPrefix = "control-plane"
  val DataPlaneMetricPrefix = ""
  val ControlPlaneMetricPrefix = "ControlPlane"

  val ReconfigurableConfigs = Set(
    KafkaConfig.MaxConnectionsPerIpProp,
    KafkaConfig.MaxConnectionsPerIpOverridesProp,
    KafkaConfig.MaxConnectionsProp,
    KafkaConfig.MaxConnectionCreationRateProp)

  val ListenerReconfigurableConfigs = Set(KafkaConfig.MaxConnectionsProp, KafkaConfig.MaxConnectionCreationRateProp)
}

/**
 * A base class with some helper variables and methods
 */
private[kafka] abstract class AbstractServerThread(connectionQuotas: ConnectionQuotas) extends Runnable with Logging {

  private val startupLatch = new CountDownLatch(1)

  // `shutdown()` is invoked before `startupComplete` and `shutdownComplete` if an exception is thrown in the constructor
  // (e.g. if the address is already in use). We want `shutdown` to proceed in such cases, so we first assign an open
  // latch and then replace it in `startupComplete()`.
  @volatile private var shutdownLatch = new CountDownLatch(0)

  private val alive = new AtomicBoolean(true)

  def wakeup(): Unit

  /**
   * Initiates a graceful shutdown by signaling to stop
   */
  def initiateShutdown(): Unit = {
    if (alive.getAndSet(false))
      wakeup()
  }

  /**
   * Wait for the thread to completely shutdown
   */
  def awaitShutdown(): Unit = shutdownLatch.await

  /**
   * Returns true if the thread is completely started
   */
  def isStarted(): Boolean = startupLatch.getCount == 0

  /**
   * Wait for the thread to completely start up
   */
  def awaitStartup(): Unit = startupLatch.await

  /**
   * Record that the thread startup is complete
   */
  protected def startupComplete(): Unit = {
    // Replace the open latch with a closed one
    shutdownLatch = new CountDownLatch(1)
    startupLatch.countDown()
  }

  /**
   * Record that the thread shutdown is complete
   */
  protected def shutdownComplete(): Unit = shutdownLatch.countDown()

  /**
   * Is the server still running?
   */
  protected def isRunning: Boolean = alive.get

  /**
   * Close `channel` and decrement the connection count.
   */
  def close(listenerName: ListenerName, channel: SocketChannel): Unit = {
    if (channel != null) {
      debug(s"Closing connection from ${channel.socket.getRemoteSocketAddress()}")
      connectionQuotas.dec(listenerName, channel.socket.getInetAddress)
      CoreUtils.swallow(channel.socket().close(), this, Level.ERROR)
      CoreUtils.swallow(channel.close(), this, Level.ERROR)
    }
  }
}

/**
 * Thread that accepts and configures new connections. There is one of these per endpoint.
 */
private[kafka] class Acceptor(val endPoint: EndPoint,
                              val sendBufferSize: Int,
                              val recvBufferSize: Int,
                              brokerId: Int,
                              connectionQuotas: ConnectionQuotas,
                              metricPrefix: String) extends AbstractServerThread(connectionQuotas) with KafkaMetricsGroup {

  private val nioSelector = NSelector.open()
  val serverChannel = openServerSocket(endPoint.host, endPoint.port)
  private val processors = new ArrayBuffer[Processor]() // 默认3个Processor线程
  private val processorsStarted = new AtomicBoolean
  private val blockedPercentMeter = newMeter(s"${metricPrefix}AcceptorBlockedPercent",
    "blocked time", TimeUnit.NANOSECONDS, Map(ListenerMetricTag -> endPoint.listenerName.value))

  private[network] def addProcessors(newProcessors: Buffer[Processor], processorThreadPrefix: String): Unit = synchronized {
    processors ++= newProcessors
    if (processorsStarted.get)
      startProcessors(newProcessors, processorThreadPrefix)
  }

  private[network] def startProcessors(processorThreadPrefix: String): Unit = synchronized {
    if (!processorsStarted.getAndSet(true)) {
      startProcessors(processors, processorThreadPrefix)
    }
  }

  private def startProcessors(processors: Seq[Processor], processorThreadPrefix: String): Unit = synchronized {
    processors.foreach { processor =>
      KafkaThread.nonDaemon(
        s"${processorThreadPrefix}-kafka-network-thread-$brokerId-${endPoint.listenerName}-${endPoint.securityProtocol}-${processor.id}",
        processor
      ).start()
    }
  }

  private[network] def removeProcessors(removeCount: Int, requestChannel: RequestChannel): Unit = synchronized {
    // Shutdown `removeCount` processors. Remove them from the processor list first so that no more
    // connections are assigned. Shutdown the removed processors, closing the selector and its connections.
    // The processors are then removed from `requestChannel` and any pending responses to these processors are dropped.
    val toRemove = processors.takeRight(removeCount)
    processors.remove(processors.size - removeCount, removeCount)
    toRemove.foreach(_.initiateShutdown())
    toRemove.foreach(_.awaitShutdown())
    toRemove.foreach(processor => requestChannel.removeProcessor(processor.id))
  }

  override def initiateShutdown(): Unit = {
    super.initiateShutdown()
    synchronized {
      processors.foreach(_.initiateShutdown())
    }
  }

  override def awaitShutdown(): Unit = {
    super.awaitShutdown()
    synchronized {
      processors.foreach(_.awaitShutdown())
    }
  }

  /**
   * Accept loop that checks for new connection attempts
   *
   * 主要步骤:
   *    1）开启Socket服务。
   *    2）注册Accept事件。
   *    3）监听此ServerChannel上的ACCEPT事件，当其发生时，将其以轮询（Round Robin）的方式把对应的SocketChannel转交给Processor处理线程。
   *
   * 注意: OP_ACCEPT为NIO中的事件，当此事件发生时，表示服务器监听到了客户连接，服务器可以接收这个连接了。
   */
  def run(): Unit = {
    // 注册Accept事件
    serverChannel.register(nioSelector, SelectionKey.OP_ACCEPT)
    startupComplete()
    try {
      var currentProcessorIndex = 0
      // 监听Accept事件
      while (isRunning) {
        try {
          val ready = nioSelector.select(500)
          if (ready > 0) {
            val keys = nioSelector.selectedKeys()
            val iter = keys.iterator()
            while (iter.hasNext && isRunning) {
              try {
                val key = iter.next
                iter.remove()

                // 有新的连接
                if (key.isAcceptable) {
                  accept(key).foreach { socketChannel =>
                    // Assign the channel to the next processor (using round-robin) to which the
                    // channel can be added without blocking. If newConnections queue is full on
                    // all processors, block until the last one is able to accept a connection.
                    var retriesLeft = synchronized(processors.length)
                    var processor: Processor = null
                    do {
                      retriesLeft -= 1
                      processor = synchronized {
                        // 轮训方式分配processor
                        // adjust the index (if necessary) and retrieve the processor atomically for
                        // correct behaviour in case the number of processors is reduced dynamically
                        currentProcessorIndex = currentProcessorIndex % processors.length
                        processors(currentProcessorIndex)
                      }
                      currentProcessorIndex += 1
                    }
                    // 把此socketChannel交给processor处理
                    while (!assignNewConnection(socketChannel, processor, retriesLeft == 0))
                  }
                } else
                  throw new IllegalStateException("Unrecognized key state for acceptor thread.")
              } catch {
                case e: Throwable => error("Error while accepting connection", e)
              }
            }
          }
        }
        catch {
          // We catch all the throwables to prevent the acceptor thread from exiting on exceptions due
          // to a select operation on a specific channel or a bad request. We don't want
          // the broker to stop responding to requests from other clients in these scenarios.
          case e: ControlThrowable => throw e
          case e: Throwable => error("Error occurred", e)
        }
      }
    } finally {
      debug("Closing server socket and selector.")
      CoreUtils.swallow(serverChannel.close(), this, Level.ERROR)
      CoreUtils.swallow(nioSelector.close(), this, Level.ERROR)
      shutdownComplete()
    }
  }

  /**
  * Create a server socket to listen for connections on.
  */
  private def openServerSocket(host: String, port: Int): ServerSocketChannel = {
    val socketAddress =
      if (host == null || host.trim.isEmpty)
        new InetSocketAddress(port)
      else
        new InetSocketAddress(host, port)
    val serverChannel = ServerSocketChannel.open()
    serverChannel.configureBlocking(false)
    if (recvBufferSize != Selectable.USE_DEFAULT_BUFFER_SIZE)
      serverChannel.socket().setReceiveBufferSize(recvBufferSize)

    try {
      serverChannel.socket.bind(socketAddress)
      info(s"Awaiting socket connections on ${socketAddress.getHostString}:${serverChannel.socket.getLocalPort}.")
    } catch {
      case e: SocketException =>
        throw new KafkaException(s"Socket server failed to bind to ${socketAddress.getHostString}:$port: ${e.getMessage}.", e)
    }
    serverChannel
  }

  /**
   * Accept a new connection
   */
  private def accept(key: SelectionKey): Option[SocketChannel] = {
    val serverSocketChannel = key.channel().asInstanceOf[ServerSocketChannel]
    val socketChannel = serverSocketChannel.accept()
    try {
      connectionQuotas.inc(endPoint.listenerName, socketChannel.socket.getInetAddress, blockedPercentMeter)
      socketChannel.configureBlocking(false)
      socketChannel.socket().setTcpNoDelay(true)
      socketChannel.socket().setKeepAlive(true)
      if (sendBufferSize != Selectable.USE_DEFAULT_BUFFER_SIZE)
        socketChannel.socket().setSendBufferSize(sendBufferSize)
      Some(socketChannel)
    } catch {
      case e: TooManyConnectionsException =>
        info(s"Rejected connection from ${e.ip}, address already has the configured maximum of ${e.count} connections.")
        close(endPoint.listenerName, socketChannel)
        None
    }
  }

  private def assignNewConnection(socketChannel: SocketChannel, processor: Processor, mayBlock: Boolean): Boolean = {
    if (processor.accept(socketChannel, mayBlock, blockedPercentMeter)) {
      debug(s"Accepted connection from ${socketChannel.socket.getRemoteSocketAddress} on" +
        s" ${socketChannel.socket.getLocalSocketAddress} and assigned it to processor ${processor.id}," +
        s" sendBufferSize [actual|requested]: [${socketChannel.socket.getSendBufferSize}|$sendBufferSize]" +
        s" recvBufferSize [actual|requested]: [${socketChannel.socket.getReceiveBufferSize}|$recvBufferSize]")
      true
    } else
      false
  }

  /**
   * Wakeup the thread for selection.
   */
  @Override
  def wakeup(): Unit = nioSelector.wakeup()

}

private[kafka] object Processor {
  val IdlePercentMetricName = "IdlePercent"
  val NetworkProcessorMetricTag = "networkProcessor"
  val ListenerMetricTag = "listener"

  val ConnectionQueueSize = 20
}

/**
 * 这个类和RequestChannel就只管转发和返回Request和Response, 具体处理Request的是 [[kafka.server.KafkaRequestHandler]]
 *
 * newConnections保存了由Acceptor线程转移过来的SocketChannel对象，主要步骤如下：
 *    1）当有新的SocketChannel对象进来的时候，注册其上的OP_READ事件以便接收客户端的请求。
 *    2）从Processor中的响应队列获取对应客户端请求的响应，然后产生OP_WRITE事件。
 *    3）监听selector上的事件。如果是读事件，说明有新的request到来，需要转移给RequestChannel的请求队列；
 *      如果是写事件，说明之前的request已经处理完毕，需要从Processor的响应队列获取响应并发送回客户端；
 *      如果是关闭事件，说明客户端已经关闭了该Socket连接，此时服务端也应该释放相应资源。
 *
 * Processor和RequestChannel本质上就是为了解耦SocketServer和KafkaApis两个模块，内部包含Request的阻塞队列和Response的阻塞队列
 * Processor和RequestChannel包含了1个Request的阻塞队列和numProcessors个Response的阻塞队列，其中numProcessors=num.network.threads，默认为3个。
 * Processor线程通过监听OP_READ事件将Request转移到RequestChannel内部的Request阻塞队列，
 * KafkaRequestHandlerPool内部的KafkaRequestHandler线程从RequestChannel内部的Request阻塞队列取出Request进行处理，
 * 然后将对应的Response放回至Response阻塞队列，并触发Processor线程监听的OP_WRITE事件，
 * 最后由Processor线程将Response发送至客户端。
 *
 * memoryPool: MemoryPool, // queued.max.request.bytes, 默认-1.
 * 可见解析socket流不限制pool，而且MemoryPool也不缓存复用buffer。生产者发送消息会复用批次buffer，可能那个实现比较简单和适合吧。
 *
 * Thread that processes all requests from a single connection. There are N of these running in parallel
 * each of which has its own selector
 */
private[kafka] class Processor(val id: Int,
                               time: Time,
                               maxRequestSize: Int,
                               requestChannel: RequestChannel, // kafka自己的类, 里面有个属性: requestQueue
                               connectionQuotas: ConnectionQuotas,
                               connectionsMaxIdleMs: Long,
                               failedAuthenticationDelayMs: Int,
                               listenerName: ListenerName,
                               securityProtocol: SecurityProtocol,
                               config: KafkaConfig,
                               metrics: Metrics,
                               credentialProvider: CredentialProvider,
                               memoryPool: MemoryPool, // queued.max.request.bytes, 默认-1
                               logContext: LogContext,
                               connectionQueueSize: Int = ConnectionQueueSize) extends AbstractServerThread(connectionQuotas) with KafkaMetricsGroup {

  private object ConnectionId {
    def fromString(s: String): Option[ConnectionId] = s.split("-") match {
      case Array(local, remote, index) => BrokerEndPoint.parseHostPort(local).flatMap { case (localHost, localPort) =>
        BrokerEndPoint.parseHostPort(remote).map { case (remoteHost, remotePort) =>
          ConnectionId(localHost, localPort, remoteHost, remotePort, Integer.parseInt(index))
        }
      }
      case _ => None
    }
  }

  private[network] case class ConnectionId(localHost: String, localPort: Int, remoteHost: String, remotePort: Int, index: Int) {
    override def toString: String = s"$localHost:$localPort-$remoteHost:$remotePort-$index"
  }

  private val newConnections = new ArrayBlockingQueue[SocketChannel](connectionQueueSize)
  private val inflightResponses = mutable.Map[String, RequestChannel.Response]()
  // requestQueue在RequestChannel中, responseQueue在Processor中
  private val responseQueue = new LinkedBlockingDeque[RequestChannel.Response]()

  private[kafka] val metricTags = mutable.LinkedHashMap(
    ListenerMetricTag -> listenerName.value,
    NetworkProcessorMetricTag -> id.toString
  ).asJava

  newGauge(IdlePercentMetricName, () => {
    Option(metrics.metric(metrics.metricName("io-wait-ratio", MetricsGroup, metricTags))).fold(0.0)(m =>
      Math.min(m.metricValue.asInstanceOf[Double], 1.0))
    },
    // for compatibility, only add a networkProcessor tag to the Yammer Metrics alias (the equivalent Selector metric
    // also includes the listener name)
    Map(NetworkProcessorMetricTag -> id.toString)
  )

  val expiredConnectionsKilledCount = new CumulativeSum()
  private val expiredConnectionsKilledCountMetricName = metrics.metricName("expired-connections-killed-count", "socket-server-metrics", metricTags)
  metrics.addMetric(expiredConnectionsKilledCountMetricName, expiredConnectionsKilledCount)

  private val selector = createSelector(
    ChannelBuilders.serverChannelBuilder(listenerName,
      listenerName == config.interBrokerListenerName,
      securityProtocol,
      config,
      credentialProvider.credentialCache,
      credentialProvider.tokenCache,
      time,
      logContext))
  // Visible to override for testing
  protected[network] def createSelector(channelBuilder: ChannelBuilder): KSelector = {
    channelBuilder match {
      case reconfigurable: Reconfigurable => config.addReconfigurable(reconfigurable)
      case _ =>
    }
    new KSelector(
      maxRequestSize,
      connectionsMaxIdleMs,
      failedAuthenticationDelayMs,
      metrics,
      time,
      "socket-server",
      metricTags,
      false,
      true,
      channelBuilder,
      memoryPool,
      logContext)
  }

  // Connection ids have the format `localAddr:localPort-remoteAddr:remotePort-index`. The index is a
  // non-negative incrementing value that ensures that even if remotePort is reused after a connection is
  // closed, connection ids are not reused while requests from the closed connection are being processed.
  private var nextConnectionIndex = 0

  override def run(): Unit = {
    startupComplete()
    try {
      while (isRunning) {
        try {
          // 针对新的连接，注册其上的OP_READ事件
          // setup any new connections that have been queued up
          configureNewConnections()
          // 从RequestChannel获取响应产生OP_WRITE事件
          // register any new responses for writing
          processNewResponses()
          poll()
          processCompletedReceives()
          processCompletedSends()
          processDisconnected()
          closeExcessConnections()
        } catch {
          // We catch all the throwables here to prevent the processor thread from exiting. We do this because
          // letting a processor exit might cause a bigger impact on the broker. This behavior might need to be
          // reviewed if we see an exception that needs the entire broker to stop. Usually the exceptions thrown would
          // be either associated with a specific socket channel or a bad request. These exceptions are caught and
          // processed by the individual methods above which close the failing channel and continue processing other
          // channels. So this catch block should only ever see ControlThrowables.
          case e: Throwable => processException("Processor got uncaught exception.", e)
        }
      }
    } finally {
      debug(s"Closing selector - processor $id")
      CoreUtils.swallow(closeAll(), this, Level.ERROR)
      shutdownComplete()
    }
  }

  private[network] def processException(errorMessage: String, throwable: Throwable): Unit = {
    throwable match {
      case e: ControlThrowable => throw e
      case e => error(errorMessage, e)
    }
  }

  private def processChannelException(channelId: String, errorMessage: String, throwable: Throwable): Unit = {
    if (openOrClosingChannel(channelId).isDefined) {
      error(s"Closing socket for $channelId because of error", throwable)
      close(channelId)
    }
    processException(errorMessage, throwable)
  }

  private def processNewResponses(): Unit = {
    var currentResponse: RequestChannel.Response = null
    // 处理响应
    while ({currentResponse = dequeueResponse(); currentResponse != null}) {
      val channelId = currentResponse.request.context.connectionId
      try {
        currentResponse match {
          case response: NoOpResponse =>
            // There is no response to send to the client, we need to read more pipelined requests
            // that are sitting in the server's socket buffer
            updateRequestMetrics(response)
            trace(s"Socket server received empty response to send, registering for read: $response")
            // Try unmuting the channel. If there was no quota violation and the channel has not been throttled,
            // it will be unmuted immediately. If the channel has been throttled, it will be unmuted only if the
            // throttling delay has already passed by now.
            handleChannelMuteEvent(channelId, ChannelMuteEvent.RESPONSE_SENT)
            tryUnmuteChannel(channelId)

          case response: SendResponse =>
            // 发送响应
            sendResponse(response, response.responseSend)
          case response: CloseConnectionResponse =>
            updateRequestMetrics(response)
            trace("Closing socket connection actively according to the response code.")
            close(channelId)
          case _: StartThrottlingResponse =>
            handleChannelMuteEvent(channelId, ChannelMuteEvent.THROTTLE_STARTED)
          case _: EndThrottlingResponse =>
            // Try unmuting the channel. The channel will be unmuted only if the response has already been sent out to
            // the client.
            handleChannelMuteEvent(channelId, ChannelMuteEvent.THROTTLE_ENDED)
            tryUnmuteChannel(channelId)
          case _ =>
            throw new IllegalArgumentException(s"Unknown response type: ${currentResponse.getClass}")
        }
      } catch {
        case e: Throwable =>
          processChannelException(channelId, s"Exception while processing response for $channelId", e)
      }
    }
  }

  // `protected` for test usage
  protected[network] def sendResponse(response: RequestChannel.Response, responseSend: Send): Unit = {
    val connectionId = response.request.context.connectionId
    trace(s"Socket server received response to send to $connectionId, registering for write and sending data: $response")
    // `channel` can be None if the connection was closed remotely or if selector closed it for being idle for too long
    if (channel(connectionId).isEmpty) {
      warn(s"Attempting to send response via channel for which there is no open connection, connection id $connectionId")
      response.request.updateRequestMetrics(0L, response)
    }
    // Invoke send for closingChannel as well so that the send is failed and the channel closed properly and
    // removed from the Selector after discarding any pending staged receives.
    // `openOrClosingChannel` can be None if the selector closed the connection because it was idle for too long
    if (openOrClosingChannel(connectionId).isDefined) {
      // 发送响应
      selector.send(responseSend)
      inflightResponses += (connectionId -> response)
    }
  }

  private def poll(): Unit = {
    val pollTimeout = if (newConnections.isEmpty) 300 else 0
    try selector.poll(pollTimeout)
    catch {
      case e @ (_: IllegalStateException | _: IOException) =>
        // The exception is not re-thrown and any completed sends/receives/connections/disconnections
        // from this poll will be processed.
        error(s"Processor $id poll failed", e)
    }
  }

  protected def parseRequestHeader(buffer: ByteBuffer): RequestHeader = {
    val header = RequestHeader.parse(buffer)
    if (!header.apiKey.isEnabled) {
      throw new InvalidRequestException("Received request for disabled api key " + header.apiKey)
    }
    header
  }

  private def processCompletedReceives(): Unit = {
    selector.completedReceives.forEach { receive =>
      try {
        openOrClosingChannel(receive.source) match {
          case Some(channel) =>
            val header = parseRequestHeader(receive.payload)
            if (header.apiKey == ApiKeys.SASL_HANDSHAKE && channel.maybeBeginServerReauthentication(receive,
              () => time.nanoseconds()))
              trace(s"Begin re-authentication: $channel")
            else {
              val nowNanos = time.nanoseconds()
              if (channel.serverAuthenticationSessionExpired(nowNanos)) {
                // be sure to decrease connection count and drop any in-flight responses
                debug(s"Disconnecting expired channel: $channel : $header")
                close(channel.id)
                expiredConnectionsKilledCount.record(null, 1, 0)
              } else {
                val connectionId = receive.source
                val context = new RequestContext(header, connectionId, channel.socketAddress,
                  channel.principal, listenerName, securityProtocol,
                  channel.channelMetadataRegistry.clientInformation)
                // 构造Request, receive.payload就直接是ByteBuffer
                // apis.handle(request) 后会 释放归还请求ByteBuffer，调用[[kafka.network.RequestChannel.Request.releaseBuffer]]
                val req = new RequestChannel.Request(processor = id, context = context,
                  startTimeNanos = nowNanos, memoryPool, receive.payload, requestChannel.metrics)
                // KIP-511: ApiVersionsRequest is intercepted here to catch the client software name
                // and version. It is done here to avoid wiring things up to the api layer.
                if (header.apiKey == ApiKeys.API_VERSIONS) {
                  val apiVersionsRequest = req.body[ApiVersionsRequest]
                  if (apiVersionsRequest.isValid) {
                    channel.channelMetadataRegistry.registerClientInformation(new ClientInformation(
                      apiVersionsRequest.data.clientSoftwareName,
                      apiVersionsRequest.data.clientSoftwareVersion))
                  }
                }
                // 转发请求到requestChannel
                requestChannel.sendRequest(req)
                selector.mute(connectionId)
                handleChannelMuteEvent(connectionId, ChannelMuteEvent.REQUEST_RECEIVED)
              }
            }
          case None =>
            // This should never happen since completed receives are processed immediately after `poll()`
            throw new IllegalStateException(s"Channel ${receive.source} removed from selector before processing completed receive")
        }
      } catch {
        // note that even though we got an exception, we can assume that receive.source is valid.
        // Issues with constructing a valid receive object were handled earlier
        case e: Throwable =>
          processChannelException(receive.source, s"Exception while processing request from ${receive.source}", e)
      }
    }
    selector.clearCompletedReceives()
  }

  private def processCompletedSends(): Unit = {
    selector.completedSends.forEach { send =>
      try {
        val response = inflightResponses.remove(send.destination).getOrElse {
          throw new IllegalStateException(s"Send for ${send.destination} completed, but not in `inflightResponses`")
        }
        updateRequestMetrics(response)

        // Invoke send completion callback
        response.onComplete.foreach(onComplete => onComplete(send))

        // Try unmuting the channel. If there was no quota violation and the channel has not been throttled,
        // it will be unmuted immediately. If the channel has been throttled, it will unmuted only if the throttling
        // delay has already passed by now.
        handleChannelMuteEvent(send.destination, ChannelMuteEvent.RESPONSE_SENT)
        tryUnmuteChannel(send.destination)
      } catch {
        case e: Throwable => processChannelException(send.destination,
          s"Exception while processing completed send to ${send.destination}", e)
      }
    }
    selector.clearCompletedSends()
  }

  private def updateRequestMetrics(response: RequestChannel.Response): Unit = {
    val request = response.request
    val networkThreadTimeNanos = openOrClosingChannel(request.context.connectionId).fold(0L)(_.getAndResetNetworkThreadTimeNanos())
    request.updateRequestMetrics(networkThreadTimeNanos, response)
  }

  private def processDisconnected(): Unit = {
    selector.disconnected.keySet.forEach { connectionId =>
      try {
        val remoteHost = ConnectionId.fromString(connectionId).getOrElse {
          throw new IllegalStateException(s"connectionId has unexpected format: $connectionId")
        }.remoteHost
        inflightResponses.remove(connectionId).foreach(updateRequestMetrics)
        // the channel has been closed by the selector but the quotas still need to be updated
        connectionQuotas.dec(listenerName, InetAddress.getByName(remoteHost))
      } catch {
        case e: Throwable => processException(s"Exception while processing disconnection of $connectionId", e)
      }
    }
  }

  private def closeExcessConnections(): Unit = {
    if (connectionQuotas.maxConnectionsExceeded(listenerName)) {
      val channel = selector.lowestPriorityChannel()
      if (channel != null)
        close(channel.id)
    }
  }

  /**
   * Close the connection identified by `connectionId` and decrement the connection count.
   * The channel will be immediately removed from the selector's `channels` or `closingChannels`
   * and no further disconnect notifications will be sent for this channel by the selector.
   * If responses are pending for the channel, they are dropped and metrics is updated.
   * If the channel has already been removed from selector, no action is taken.
   */
  private def close(connectionId: String): Unit = {
    openOrClosingChannel(connectionId).foreach { channel =>
      debug(s"Closing selector connection $connectionId")
      val address = channel.socketAddress
      if (address != null)
        connectionQuotas.dec(listenerName, address)
      selector.close(connectionId)

      inflightResponses.remove(connectionId).foreach(response => updateRequestMetrics(response))
    }
  }

  /**
   * 将新连接排队以进行读取, newConnections中放的是新的连接
   * Queue up a new connection for reading
   */
  def accept(socketChannel: SocketChannel,
             mayBlock: Boolean,
             acceptorIdlePercentMeter: com.yammer.metrics.core.Meter): Boolean = {
    val accepted = {
      if (newConnections.offer(socketChannel))
        true
      else if (mayBlock) {
        val startNs = time.nanoseconds
        newConnections.put(socketChannel)
        acceptorIdlePercentMeter.mark(time.nanoseconds() - startNs)
        true
      } else
        false
    }
    if (accepted)
      wakeup()
    accepted
  }

  /**
   * Register any new connections that have been queued up. The number of connections processed
   * in each iteration is limited to ensure that traffic and connection close notifications of
   * existing channels are handled promptly.
   */
  private def configureNewConnections(): Unit = {
    var connectionsProcessed = 0
    while (connectionsProcessed < connectionQueueSize && !newConnections.isEmpty) {
      val channel = newConnections.poll()
      try {
        debug(s"Processor $id listening to new connection from ${channel.socket.getRemoteSocketAddress}")
        selector.register(connectionId(channel.socket), channel)
        connectionsProcessed += 1
      } catch {
        // We explicitly catch all exceptions and close the socket to avoid a socket leak.
        case e: Throwable =>
          val remoteAddress = channel.socket.getRemoteSocketAddress
          // need to close the channel here to avoid a socket leak.
          close(listenerName, channel)
          processException(s"Processor $id closed connection from $remoteAddress", e)
      }
    }
  }

  /**
   * Close the selector and all open connections
   */
  private def closeAll(): Unit = {
    while (!newConnections.isEmpty) {
      newConnections.poll().close()
    }
    selector.channels.forEach { channel =>
      close(channel.id)
    }
    selector.close()
    removeMetric(IdlePercentMetricName, Map(NetworkProcessorMetricTag -> id.toString))
  }

  // 'protected` to allow override for testing
  protected[network] def connectionId(socket: Socket): String = {
    val localHost = socket.getLocalAddress.getHostAddress
    val localPort = socket.getLocalPort
    val remoteHost = socket.getInetAddress.getHostAddress
    val remotePort = socket.getPort
    val connId = ConnectionId(localHost, localPort, remoteHost, remotePort, nextConnectionIndex).toString
    nextConnectionIndex = if (nextConnectionIndex == Int.MaxValue) 0 else nextConnectionIndex + 1
    connId
  }

  private[network] def enqueueResponse(response: RequestChannel.Response): Unit = {
    responseQueue.put(response)
    wakeup()
  }

  private def dequeueResponse(): RequestChannel.Response = {
    val response = responseQueue.poll()
    if (response != null)
      response.request.responseDequeueTimeNanos = Time.SYSTEM.nanoseconds
    response
  }

  private[network] def responseQueueSize = responseQueue.size

  // Only for testing
  private[network] def inflightResponseCount: Int = inflightResponses.size

  // Visible for testing
  // Only methods that are safe to call on a disconnected channel should be invoked on 'openOrClosingChannel'.
  private[network] def openOrClosingChannel(connectionId: String): Option[KafkaChannel] =
    Option(selector.channel(connectionId)).orElse(Option(selector.closingChannel(connectionId)))

  // Indicate the specified channel that the specified channel mute-related event has happened so that it can change its
  // mute state.
  private def handleChannelMuteEvent(connectionId: String, event: ChannelMuteEvent): Unit = {
    openOrClosingChannel(connectionId).foreach(c => c.handleChannelMuteEvent(event))
  }

  private def tryUnmuteChannel(connectionId: String) = {
    openOrClosingChannel(connectionId).foreach(c => selector.unmute(c.id))
  }

  /* For test usage */
  private[network] def channel(connectionId: String): Option[KafkaChannel] =
    Option(selector.channel(connectionId))

  /**
   * Wakeup the thread for selection.
   */
  override def wakeup() = selector.wakeup()

  override def initiateShutdown(): Unit = {
    super.initiateShutdown()
    removeMetric("IdlePercent", Map("networkProcessor" -> id.toString))
    metrics.removeMetric(expiredConnectionsKilledCountMetricName)
  }
}

class ConnectionQuotas(config: KafkaConfig, time: Time, metrics: Metrics) extends Logging with AutoCloseable {

  @volatile private var defaultMaxConnectionsPerIp: Int = config.maxConnectionsPerIp
  @volatile private var maxConnectionsPerIpOverrides = config.maxConnectionsPerIpOverrides.map { case (host, count) => (InetAddress.getByName(host), count) }
  @volatile private var brokerMaxConnections = config.maxConnections
  private val counts = mutable.Map[InetAddress, Int]()

  // Listener counts and configs are synchronized on `counts`
  private val listenerCounts = mutable.Map[ListenerName, Int]()
  private[network] val maxConnectionsPerListener = mutable.Map[ListenerName, ListenerConnectionQuota]()
  @volatile private var totalCount = 0

  // sensor that tracks broker-wide connection creation rate and limit (quota)
  private val brokerConnectionRateSensor = createConnectionRateQuotaSensor(config.maxConnectionCreationRate)
  private val maxThrottleTimeMs = TimeUnit.SECONDS.toMillis(config.quotaWindowSizeSeconds.toLong)

  def inc(listenerName: ListenerName, address: InetAddress, acceptorBlockedPercentMeter: com.yammer.metrics.core.Meter): Unit = {
    counts.synchronized {
      waitForConnectionSlot(listenerName, acceptorBlockedPercentMeter)

      val count = counts.getOrElseUpdate(address, 0)
      counts.put(address, count + 1)
      totalCount += 1
      if (listenerCounts.contains(listenerName)) {
        listenerCounts.put(listenerName, listenerCounts(listenerName) + 1)
      }
      val max = maxConnectionsPerIpOverrides.getOrElse(address, defaultMaxConnectionsPerIp)
      if (count >= max)
        throw new TooManyConnectionsException(address, max)
    }
  }

  private[network] def updateMaxConnectionsPerIp(maxConnectionsPerIp: Int): Unit = {
    defaultMaxConnectionsPerIp = maxConnectionsPerIp
  }

  private[network] def updateMaxConnectionsPerIpOverride(overrideQuotas: Map[String, Int]): Unit = {
    maxConnectionsPerIpOverrides = overrideQuotas.map { case (host, count) => (InetAddress.getByName(host), count) }
  }

  private[network] def updateBrokerMaxConnections(maxConnections: Int): Unit = {
    counts.synchronized {
      brokerMaxConnections = maxConnections
      counts.notifyAll()
    }
  }

  private[network] def updateBrokerMaxConnectionRate(maxConnectionRate: Int): Unit = {
    // if there is a connection waiting on the rate throttle delay, we will let it wait the original delay even if
    // the rate limit increases, because it is just one connection per listener and the code is simpler that way
    updateConnectionRateQuota(maxConnectionRate)
  }

  private[network] def addListener(config: KafkaConfig, listenerName: ListenerName): Unit = {
    counts.synchronized {
      if (!maxConnectionsPerListener.contains(listenerName)) {
        val newListenerQuota = new ListenerConnectionQuota(counts, listenerName)
        maxConnectionsPerListener.put(listenerName, newListenerQuota)
        listenerCounts.put(listenerName, 0)
        config.addReconfigurable(newListenerQuota)
        newListenerQuota.configure(config.valuesWithPrefixOverride(listenerName.configPrefix))
      }
      counts.notifyAll()
    }
  }

  private[network] def removeListener(config: KafkaConfig, listenerName: ListenerName): Unit = {
    counts.synchronized {
      maxConnectionsPerListener.remove(listenerName).foreach { listenerQuota =>
        listenerCounts.remove(listenerName)
        // once listener is removed from maxConnectionsPerListener, no metrics will be recorded into listener's sensor
        // so it is safe to remove sensor here
        listenerQuota.close()
        counts.notifyAll() // wake up any waiting acceptors to close cleanly
        config.removeReconfigurable(listenerQuota)
      }
    }
  }

  def dec(listenerName: ListenerName, address: InetAddress): Unit = {
    counts.synchronized {
      val count = counts.getOrElse(address,
        throw new IllegalArgumentException(s"Attempted to decrease connection count for address with no connections, address: $address"))
      if (count == 1)
        counts.remove(address)
      else
        counts.put(address, count - 1)

      if (totalCount <= 0)
        error(s"Attempted to decrease total connection count for broker with no connections")
      totalCount -= 1

      if (maxConnectionsPerListener.contains(listenerName)) {
        val listenerCount = listenerCounts(listenerName)
        if (listenerCount == 0)
          error(s"Attempted to decrease connection count for listener $listenerName with no connections")
        else
          listenerCounts.put(listenerName, listenerCount - 1)
      }
      counts.notifyAll() // wake up any acceptors waiting to process a new connection since listener connection limit was reached
    }
  }

  def get(address: InetAddress): Int = counts.synchronized {
    counts.getOrElse(address, 0)
  }

  private def waitForConnectionSlot(listenerName: ListenerName,
                                    acceptorBlockedPercentMeter: com.yammer.metrics.core.Meter): Unit = {
    counts.synchronized {
      val startThrottleTimeMs = time.milliseconds
      val throttleTimeMs = math.max(recordConnectionAndGetThrottleTimeMs(listenerName, startThrottleTimeMs), 0)

      if (throttleTimeMs > 0 || !connectionSlotAvailable(listenerName)) {
        val startNs = time.nanoseconds
        val endThrottleTimeMs = startThrottleTimeMs + throttleTimeMs
        var remainingThrottleTimeMs = throttleTimeMs
        do {
          counts.wait(remainingThrottleTimeMs)
          remainingThrottleTimeMs = math.max(endThrottleTimeMs - time.milliseconds, 0)
        } while (remainingThrottleTimeMs > 0 || !connectionSlotAvailable(listenerName))
        acceptorBlockedPercentMeter.mark(time.nanoseconds - startNs)
      }
    }
  }

  // This is invoked in every poll iteration and we close one LRU connection in an iteration
  // if necessary
  def maxConnectionsExceeded(listenerName: ListenerName): Boolean = {
    totalCount > brokerMaxConnections && !protectedListener(listenerName)
  }

  private def connectionSlotAvailable(listenerName: ListenerName): Boolean = {
    if (listenerCounts(listenerName) >= maxListenerConnections(listenerName))
      false
    else if (protectedListener(listenerName))
      true
    else
      totalCount < brokerMaxConnections
  }

  private def protectedListener(listenerName: ListenerName): Boolean =
    config.interBrokerListenerName == listenerName && config.listeners.size > 1

  private def maxListenerConnections(listenerName: ListenerName): Int =
    maxConnectionsPerListener.get(listenerName).map(_.maxConnections).getOrElse(Int.MaxValue)

  /**
   * Calculates the delay needed to bring the observed connection creation rate to listener-level limit or to broker-wide
   * limit, whichever the longest. The delay is capped to the quota window size defined by QuotaWindowSizeSecondsProp
   *
   * @param listenerName listener for which calculate the delay
   * @param timeMs current time in milliseconds
   * @return delay in milliseconds
   */
  private def recordConnectionAndGetThrottleTimeMs(listenerName: ListenerName, timeMs: Long): Long = {
    def recordAndGetListenerThrottleTime(minThrottleTimeMs: Int): Int = {
      maxConnectionsPerListener
      .get(listenerName)
      .map { listenerQuota =>
        val listenerThrottleTimeMs = recordAndGetThrottleTimeMs(listenerQuota.connectionRateSensor, timeMs)
        val throttleTimeMs = math.max(minThrottleTimeMs, listenerThrottleTimeMs)
        // record throttle time due to hitting connection rate quota
        if (throttleTimeMs > 0) {
          listenerQuota.connectionRateThrottleSensor.record(throttleTimeMs.toDouble, timeMs)
        }
        throttleTimeMs
      }
      .getOrElse(0)
    }

    if (protectedListener(listenerName)) {
      recordAndGetListenerThrottleTime(0)
    } else {
      val brokerThrottleTimeMs = recordAndGetThrottleTimeMs(brokerConnectionRateSensor, timeMs)
      recordAndGetListenerThrottleTime(brokerThrottleTimeMs)
    }
  }

  /**
   * Records a new connection into a given connection acceptance rate sensor 'sensor' and returns throttle time
   * in milliseconds if quota got violated
   * @param sensor sensor to record connection
   * @param timeMs current time in milliseconds
   * @return throttle time in milliseconds if quota got violated, otherwise 0
   */
  private def recordAndGetThrottleTimeMs(sensor: Sensor, timeMs: Long): Int = {
    try {
      sensor.record(1.0, timeMs)
      0
    } catch {
      case e: QuotaViolationException =>
        val throttleTimeMs = QuotaUtils.boundedThrottleTime(e, maxThrottleTimeMs, timeMs).toInt
        debug(s"Quota violated for sensor (${sensor.name}). Delay time: $throttleTimeMs ms")
        throttleTimeMs
    }
  }

  /**
   * Creates sensor for tracking the connection creation rate and corresponding connection rate quota for a given
   * listener or broker-wide, if listener is not provided.
   * @param quotaLimit connection creation rate quota
   * @param listenerOpt listener name if sensor is for a listener
   */
  private def createConnectionRateQuotaSensor(quotaLimit: Int, listenerOpt: Option[String] = None): Sensor = {
    val sensorName = listenerOpt.map(listener => s"ConnectionAcceptRate-$listener").getOrElse("ConnectionAcceptRate")
    val sensor = metrics.sensor(sensorName, rateQuotaMetricConfig(quotaLimit))
    sensor.add(connectionRateMetricName(listenerOpt), new Rate, null)
    info(s"Created $sensorName sensor, quotaLimit=$quotaLimit")
    sensor
  }

  /**
   * Updates quota configuration for a given listener or broker-wide (if 'listenerOpt' is None)
   */
  private def updateConnectionRateQuota(quotaLimit: Int, listenerOpt: Option[String] = None): Unit = {
    val metric = metrics.metric(connectionRateMetricName(listenerOpt))
    metric.config(rateQuotaMetricConfig(quotaLimit))
    info(s"Updated ${listenerOpt.getOrElse("broker-wide")} max connection creation rate to $quotaLimit")
  }

  private def connectionRateMetricName(listenerOpt: Option[String]): MetricName = {
    val tags = listenerOpt.map(listener => Map("listener" -> listener)).getOrElse(Map())
    val namePrefix = listenerOpt.map(_ => "").getOrElse("broker-")
    metrics.metricName(
      s"${namePrefix}connection-accept-rate",
      MetricsGroup,
      s"Tracking rate of accepting new connections (per second)",
      tags.asJava)
  }

  private def rateQuotaMetricConfig(quotaLimit: Int): MetricConfig = {
    new MetricConfig()
      .timeWindow(config.quotaWindowSizeSeconds.toLong, TimeUnit.SECONDS)
      .samples(config.numQuotaSamples)
      .quota(new Quota(quotaLimit, true))
  }

  def close(): Unit = {
    metrics.removeSensor("ConnectionAcceptRate")
    maxConnectionsPerListener.values.foreach(_.close())
  }

  class ListenerConnectionQuota(lock: Object, listener: ListenerName) extends ListenerReconfigurable with AutoCloseable {
    @volatile private var _maxConnections = Int.MaxValue
    private[network] val connectionRateSensor = createConnectionRateQuotaSensor(Int.MaxValue, Some(listener.value))
    private[network] val connectionRateThrottleSensor = createConnectionRateThrottleSensor()

    def maxConnections: Int = _maxConnections

    override def listenerName(): ListenerName = listener

    override def configure(configs: util.Map[String, _]): Unit = {
      _maxConnections = maxConnections(configs)
      updateConnectionRateQuota(maxConnectionCreationRate(configs), Some(listener.value))
    }

    override def reconfigurableConfigs(): util.Set[String] = {
      SocketServer.ListenerReconfigurableConfigs.asJava
    }

    override def validateReconfiguration(configs: util.Map[String, _]): Unit = {
      val value = maxConnections(configs)
      if (value <= 0)
        throw new ConfigException(s"Invalid ${KafkaConfig.MaxConnectionsProp} $value")

      val rate = maxConnectionCreationRate(configs)
      if (rate <= 0)
        throw new ConfigException(s"Invalid ${KafkaConfig.MaxConnectionCreationRateProp} $rate")
    }

    override def reconfigure(configs: util.Map[String, _]): Unit = {
      lock.synchronized {
        _maxConnections = maxConnections(configs)
        updateConnectionRateQuota(maxConnectionCreationRate(configs), Some(listener.value))
        lock.notifyAll()
      }
    }

    def close(): Unit = {
      metrics.removeSensor(connectionRateSensor.name)
      metrics.removeSensor(connectionRateThrottleSensor.name)
    }

    private def maxConnections(configs: util.Map[String, _]): Int = {
      Option(configs.get(KafkaConfig.MaxConnectionsProp)).map(_.toString.toInt).getOrElse(Int.MaxValue)
    }

    private def maxConnectionCreationRate(configs: util.Map[String, _]): Int = {
      Option(configs.get(KafkaConfig.MaxConnectionCreationRateProp)).map(_.toString.toInt).getOrElse(Int.MaxValue)
    }

    /**
     * Creates sensor for tracking the average throttle time on this listener due to hitting connection rate quota.
     * The average is out of all throttle times > 0, which is consistent with the bandwidth and request quota throttle
     * time metrics.
     */
    private def createConnectionRateThrottleSensor(): Sensor = {
      val sensor = metrics.sensor(s"ConnectionRateThrottleTime-${listener.value}")
      val metricName = metrics.metricName("connection-accept-throttle-time",
        MetricsGroup,
        "Tracking average throttle-time, out of non-zero throttle times, per listener",
        Map(ListenerMetricTag -> listener.value).asJava)
      sensor.add(metricName, new Avg)
      sensor
    }
  }
}

class TooManyConnectionsException(val ip: InetAddress, val count: Int) extends KafkaException(s"Too many connections from $ip (maximum = $count)")
