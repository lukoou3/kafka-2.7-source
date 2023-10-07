package org.apache.kafka.common.utils;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class ByteUtilsMyTest {

    @Test
    public void testUnsignedVarint() {
        ByteBuffer buf = ByteBuffer.allocate(5);
        for (int i = 0; i <= Integer.MAX_VALUE && i >= 0; i*=2) {
            ByteUtils.writeUnsignedVarint(i, buf);
            buf.flip();
            assertEquals(i, ByteUtils.readUnsignedVarint(buf));
            buf.clear();
            if(i == 0){
                i = 1;
            }
        }

        for (int i = 0; i <= Integer.MAX_VALUE && i >= 0; i+=16) {
            ByteUtils.writeUnsignedVarint(i, buf);
            buf.flip();
            assertEquals(i, ByteUtils.readUnsignedVarint(buf));
            buf.clear();
        }

        int preSize = -1;
        for (int i = 0; i <= Integer.MAX_VALUE && i >= 0; i+=16) {
            ByteUtils.writeUnsignedVarint(i, buf);
            int size = ByteUtils.sizeOfUnsignedVarint(i);
            assert buf.limit() == size;
            assert size >= preSize;
            if(size > preSize){
                System.out.println(String.format("%d:%d", i-16, preSize));
                System.out.println(String.format("%d:%d", i, size));
            }
            buf.clear();
            preSize = size;
        }
    }

    @Test
    public void testVarint() {
        ByteBuffer buf = ByteBuffer.allocate(5);
        for (int i = 0; i > -10 ; i-=1) {
            int value = (i << 1) ^ (i >> 31);
            System.out.println(String.format("%d:%d", i, value));
        }
        for (int i = 1; i < 1000000; i*=2) {
            int value = (i << 1) ^ (i >> 31);
            System.out.println(String.format("%d:%d", i, value));
        }

        System.out.println("################");

        int preSize = -1;
        for (int i = 0; i <= Integer.MAX_VALUE && i >= 0; i+=8) {
            ByteUtils.writeVarint(i, buf);
            int size = ByteUtils.sizeOfVarint(i);
            assert buf.limit() == size;
            assert size >= preSize;
            if(size > preSize){
                System.out.println(String.format("%d:%d", i-8, preSize));
                System.out.println(String.format("%d:%d", i, size));
            }
            buf.clear();
            preSize = size;
        }
    }

}
