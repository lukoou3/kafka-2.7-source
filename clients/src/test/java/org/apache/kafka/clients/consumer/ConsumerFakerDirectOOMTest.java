package org.apache.kafka.clients.consumer;

import sun.nio.ch.IOUtil;
import sun.nio.ch.Util;
import sun.security.action.GetPropertyAction;

import java.nio.ByteBuffer;

/**
 * 正常情况下很难内存溢出
 *   Util.getTemporaryDirectBuffer(size); 申请buffer
 *   Util.releaseTemporaryDirectBuffer(bb); 归还buffer
 *   Util.getTemporaryDirectBuffer(size);第二次申请buffer比较大时会释放队列首个buffer
 *   除非同一个线程申请多个buffer没归还，否则除非申请buffer的越来越大，多个线程才有可能内存溢出
 * 为了避免可以设置-Djdk.nio.maxCachedBufferSize=1048576
 */
public class ConsumerFakerDirectOOMTest {

    public static void main(String[] args) throws Exception{
        int size = 100000;
        for (int i = 0; i < 10; i++) {
            size = size + i * 1000;
            ByteBuffer bb = Util.getTemporaryDirectBuffer(size);
            Util.releaseTemporaryDirectBuffer(bb);
        }

        //int TEMP_BUF_POOL_SIZE = IOUtil.IOV_MAX;
        String maxCachedBufferSize = GetPropertyAction.privilegedGetProperty("jdk.nio.maxCachedBufferSize");
        System.out.println(maxCachedBufferSize);
        Util.getTemporaryDirectBuffer(1000);
        System.out.println(maxCachedBufferSize);
    }

}
