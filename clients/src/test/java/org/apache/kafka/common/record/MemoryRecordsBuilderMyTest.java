package org.apache.kafka.common.record;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemoryRecordsBuilderMyTest {

    /**
     * 这个就是生产消息写入RecordBatch的过程
     * header部分是原始内容，占61B；records内容可以压缩或者不压缩
     */
    @Test
    public void testAppendNoCompression() {
        final int bufferOffset = 0;
        final CompressionType compressionType = CompressionType.NONE;

        // RecordBatch buffer大小
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.position(bufferOffset); // bufferOffset是多少

        // CURRENT_MAGIC_VALUE = MAGIC_VALUE_V2; 当前版本是版本2
        MemoryRecordsBuilder builder = new MemoryRecordsBuilder(buffer, RecordBatch.CURRENT_MAGIC_VALUE, compressionType,
                TimestampType.CREATE_TIME, 0L, 0L, RecordBatch.NO_PRODUCER_ID, RecordBatch.NO_PRODUCER_EPOCH, RecordBatch.NO_SEQUENCE,
                false, false, RecordBatch.NO_PARTITION_LEADER_EPOCH, buffer.capacity());

        int previousEstimate = 0;
        int recordCount = 10;
        for (int i = 0; i < recordCount; i++) {
            // 模拟生产消息
            builder.append(new SimpleRecord(i, ("value_" + i).getBytes()));
            int currentEstimate = builder.estimatedSizeInBytes();
            assertTrue(currentEstimate > previousEstimate);
            previousEstimate = currentEstimate;
        }

        int bytesWrittenBeforeClose = builder.estimatedSizeInBytes();
        MemoryRecords records = builder.build();
        assertEquals(records.sizeInBytes(), builder.estimatedSizeInBytes());
        if (compressionType == CompressionType.NONE)
            assertEquals(records.sizeInBytes(), bytesWrittenBeforeClose);

        int batchHeaderSizeInBytes = AbstractRecords.recordBatchHeaderSizeInBytes(RecordBatch.CURRENT_MAGIC_VALUE, compressionType);
        assertEquals(batchHeaderSizeInBytes, 61);
        int sizeInBytes = records.sizeInBytes();
        System.out.println("total" + sizeInBytes);
        System.out.println("records" + (sizeInBytes - batchHeaderSizeInBytes));
        System.out.println("per record" + (sizeInBytes - batchHeaderSizeInBytes) / recordCount);
    }

    @Test
    public void testAppendWithCompression() {
        final int bufferOffset = 0;
        final CompressionType compressionType = CompressionType.LZ4;

        // RecordBatch buffer大小
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        buffer.position(bufferOffset); // bufferOffset是多少

        // CURRENT_MAGIC_VALUE = MAGIC_VALUE_V2; 当前版本是版本2
        MemoryRecordsBuilder builder = new MemoryRecordsBuilder(buffer, RecordBatch.CURRENT_MAGIC_VALUE, compressionType,
                TimestampType.CREATE_TIME, 0L, 0L, RecordBatch.NO_PRODUCER_ID, RecordBatch.NO_PRODUCER_EPOCH, RecordBatch.NO_SEQUENCE,
                false, false, RecordBatch.NO_PARTITION_LEADER_EPOCH, buffer.capacity());

        int previousEstimate = 0;
        int recordCount = 10;
        for (int i = 0; i < recordCount; i++) {
            // 模拟生产消息
            builder.append(new SimpleRecord(i, ("value_" + i).getBytes()));
            int currentEstimate = builder.estimatedSizeInBytes();
            assertTrue(currentEstimate > previousEstimate);
            previousEstimate = currentEstimate;
        }

        int bytesWrittenBeforeClose = builder.estimatedSizeInBytes();
        MemoryRecords records = builder.build();
        assertEquals(records.sizeInBytes(), builder.estimatedSizeInBytes());
        if (compressionType == CompressionType.NONE)
            assertEquals(records.sizeInBytes(), bytesWrittenBeforeClose);

        int batchHeaderSizeInBytes = AbstractRecords.recordBatchHeaderSizeInBytes(RecordBatch.CURRENT_MAGIC_VALUE, compressionType);
        assertEquals(batchHeaderSizeInBytes, 61);
        int sizeInBytes = records.sizeInBytes();
        System.out.println("total" + sizeInBytes);
        System.out.println("records" + (sizeInBytes - batchHeaderSizeInBytes));
        System.out.println("per record" + (sizeInBytes - batchHeaderSizeInBytes) / recordCount);
    }

}
