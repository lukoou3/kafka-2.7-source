/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.kafka.common.network;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 异步多通道网络I/O接口
 * An interface for asynchronous, multi-channel network I/O
 */
public interface Selectable {

    /**
     * See {@link #connect(String, InetSocketAddress, int, int) connect()}
     */
    int USE_DEFAULT_BUFFER_SIZE = -1;

    /**
     * 开始建立到由给定地址标识的给定地址的套接字连接
     * Begin establishing a socket connection to the given address identified by the given address
     * @param id The id for this connection
     * @param address The address to connect to
     * @param sendBufferSize The send buffer for the socket
     * @param receiveBufferSize The receive buffer for the socket
     * @throws IOException If we cannot begin connecting
     */
    void connect(String id, InetSocketAddress address, int sendBufferSize, int receiveBufferSize) throws IOException;

    /**
     * 唤醒这个selector如果它阻塞在I/O
     * Wakeup this selector if it is blocked on I/O
     */
    void wakeup();

    /**
     * Close this selector
     */
    void close();

    /**
     * Close the connection identified by the given id
     */
    void close(String id);

    /**
     * 把这个请求send放入Queue队列，在之后的poll方法调用时会发送这个send
     * Queue the given request for sending in the subsequent {@link #poll(long) poll()} calls
     * @param send The request to send
     */
    void send(Send send);

    /**
     * 做io操作。读、写、连接建立等。
     * Do I/O. Reads, writes, connection establishment, etc.
     * @param timeout The amount of time to block if there is nothing to do
     * @throws IOException
     */
    void poll(long timeout) throws IOException;

    /**
     * 完成的sends
     * 客户端轮训这个处理
     * The list of sends that completed on the last {@link #poll(long) poll()} call.
     */
    List<Send> completedSends();

    /**
     * 完成的网络接收NetworkReceive
     * 客户端轮训这个处理
     * The collection of receives that completed on the last {@link #poll(long) poll()} call.
     */
    Collection<NetworkReceive> completedReceives();

    /**
     * The connections that finished disconnecting on the last {@link #poll(long) poll()}
     * call. Channel state indicates the local channel state at the time of disconnection.
     */
    Map<String, ChannelState> disconnected();

    /**
     * The list of connections that completed their connection on the last {@link #poll(long) poll()}
     * call.
     */
    List<String> connected();

    /**
     * 停止从给定连接读操作。mute: /mjuːt/ 沉默
     * Disable reads from the given connection
     * @param id The id for the connection
     */
    void mute(String id);

    /**
     * 重新启用从给定连接读操作
     *
     *
     *
     *
     *
     *
     *
     *
     *
     * Re-enable reads from the given connection
     * @param id The id for the connection
     */
    void unmute(String id);

    /**
     * Disable reads from all connections
     */
    void muteAll();

    /**
     * Re-enable reads from all connections
     */
    void unmuteAll();

    /**
     * returns true  if a channel is ready
     * @param id The id for the connection
     */
    boolean isChannelReady(String id);
}
