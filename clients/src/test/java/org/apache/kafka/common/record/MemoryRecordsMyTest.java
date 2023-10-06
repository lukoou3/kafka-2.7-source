package org.apache.kafka.common.record;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

public class MemoryRecordsMyTest {
    long pid;
    short epoch;
    int firstSequence;
    long logAppendTime = System.currentTimeMillis();
    int partitionLeaderEpoch = 998;

    @Test
    public void testIteratorNoCompression() {
        CompressionType compression = CompressionType.NONE;
        byte magic = RecordBatch.CURRENT_MAGIC_VALUE; // 2
        long firstOffset = 0;

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        MemoryRecordsBuilder builder = new MemoryRecordsBuilder(buffer, magic, compression,
                TimestampType.CREATE_TIME, firstOffset, logAppendTime, pid, epoch, firstSequence, false, false,
                partitionLeaderEpoch, buffer.limit());

        SimpleRecord[] records = new SimpleRecord[] {
                new SimpleRecord(1L, "a".getBytes(), "1".getBytes()),
                new SimpleRecord(2L, "b".getBytes(), "2".getBytes()),
                new SimpleRecord(3L, "c".getBytes(), "3".getBytes()),
                new SimpleRecord(4L, null, "4".getBytes()),
                new SimpleRecord(5L, "d".getBytes(), null),
                new SimpleRecord(6L, (byte[]) null, null)
        };

        for (SimpleRecord record : records)
            builder.append(record);

        MemoryRecords memoryRecords = builder.build();
        int total = 0;
        for (RecordBatch batch : memoryRecords.batches()) {
            System.out.println(String.format("magic:%d, compression:%s, total:%d", magic, compression, total));
            assertTrue(batch.isValid());
            assertEquals(compression, batch.compressionType());
            assertEquals(firstOffset + total, batch.baseOffset());

            if (magic >= RecordBatch.MAGIC_VALUE_V2) {
                assertEquals(pid, batch.producerId());
                assertEquals(epoch, batch.producerEpoch());
                assertEquals(firstSequence + total, batch.baseSequence());
                assertEquals(partitionLeaderEpoch, batch.partitionLeaderEpoch());
                assertEquals(records.length, batch.countOrNull().intValue());
                assertEquals(TimestampType.CREATE_TIME, batch.timestampType());
                assertEquals(records[records.length - 1].timestamp(), batch.maxTimestamp());
            }

            int recordCount = 0;
            for (Record record : batch) {
                assertTrue(record.isValid());
                assertTrue(record.hasMagic(batch.magic()));
                assertFalse(record.isCompressed());
                assertEquals(firstOffset + total, record.offset());
                assertEquals(records[total].key(), record.key());
                assertEquals(records[total].value(), record.value());

                if (magic >= RecordBatch.MAGIC_VALUE_V2)
                    assertEquals(firstSequence + total, record.sequence());

                assertFalse(record.hasTimestampType(TimestampType.LOG_APPEND_TIME));
                if (magic == RecordBatch.MAGIC_VALUE_V0) {
                    assertEquals(RecordBatch.NO_TIMESTAMP, record.timestamp());
                    assertFalse(record.hasTimestampType(TimestampType.CREATE_TIME));
                    assertTrue(record.hasTimestampType(TimestampType.NO_TIMESTAMP_TYPE));
                } else {
                    assertEquals(records[total].timestamp(), record.timestamp());
                    assertFalse(record.hasTimestampType(TimestampType.NO_TIMESTAMP_TYPE));
                    if (magic < RecordBatch.MAGIC_VALUE_V2)
                        assertTrue(record.hasTimestampType(TimestampType.CREATE_TIME));
                    else
                        assertFalse(record.hasTimestampType(TimestampType.CREATE_TIME));
                }

                total++;
                recordCount++;
            }

            assertEquals(batch.baseOffset() + recordCount - 1, batch.lastOffset());
        }
    }

    @Test
    public void testIteratorWithCompression() {
        CompressionType compression = CompressionType.LZ4;
        byte magic = RecordBatch.CURRENT_MAGIC_VALUE; // 2
        long firstOffset = 0;

        ByteBuffer buffer = ByteBuffer.allocate(1024);

        MemoryRecordsBuilder builder = new MemoryRecordsBuilder(buffer, magic, compression,
                TimestampType.CREATE_TIME, firstOffset, logAppendTime, pid, epoch, firstSequence, false, false,
                partitionLeaderEpoch, buffer.limit());

        SimpleRecord[] records = new SimpleRecord[] {
                new SimpleRecord(1L, "a".getBytes(), "1".getBytes()),
                new SimpleRecord(2L, "b".getBytes(), "2".getBytes()),
                new SimpleRecord(3L, "c".getBytes(), "3".getBytes()),
                new SimpleRecord(4L, null, "4".getBytes()),
                new SimpleRecord(5L, "d".getBytes(), null),
                new SimpleRecord(6L, (byte[]) null, null)
        };

        for (SimpleRecord record : records)
            builder.append(record);

        MemoryRecords memoryRecords = builder.build();
        int total = 0;
        for (RecordBatch batch : memoryRecords.batches()) {
            System.out.println(String.format("magic:%d, compression:%s, total:%d", magic, compression, total));
            assertTrue(batch.isValid());
            assertEquals(compression, batch.compressionType());
            assertEquals(firstOffset + total, batch.baseOffset());

            // header是没压缩的
            if (magic >= RecordBatch.MAGIC_VALUE_V2) {
                assertEquals(pid, batch.producerId());
                assertEquals(epoch, batch.producerEpoch());
                assertEquals(firstSequence + total, batch.baseSequence());
                assertEquals(partitionLeaderEpoch, batch.partitionLeaderEpoch());
                assertEquals(records.length, batch.countOrNull().intValue());
                assertEquals(TimestampType.CREATE_TIME, batch.timestampType());
                assertEquals(records[records.length - 1].timestamp(), batch.maxTimestamp());
            }

            int recordCount = 0;
            // 压缩的records，看看kafka是怎么处理迭代返回record的
            // DefaultRecordBatch.compressedIterator方法 解压缩迭代器
            // 解压缩需要解压出整个压缩的byte[], 从这里看出RecordBatch也不易过大吧，默认是16k
            for (Record record : batch) {
                assertTrue(record.isValid());
                assertTrue(record.hasMagic(batch.magic()));
                assertFalse(record.isCompressed());
                assertEquals(firstOffset + total, record.offset());
                assertEquals(records[total].key(), record.key());
                assertEquals(records[total].value(), record.value());

                if (magic >= RecordBatch.MAGIC_VALUE_V2)
                    assertEquals(firstSequence + total, record.sequence());

                assertFalse(record.hasTimestampType(TimestampType.LOG_APPEND_TIME));
                if (magic == RecordBatch.MAGIC_VALUE_V0) {
                    assertEquals(RecordBatch.NO_TIMESTAMP, record.timestamp());
                    assertFalse(record.hasTimestampType(TimestampType.CREATE_TIME));
                    assertTrue(record.hasTimestampType(TimestampType.NO_TIMESTAMP_TYPE));
                } else {
                    assertEquals(records[total].timestamp(), record.timestamp());
                    assertFalse(record.hasTimestampType(TimestampType.NO_TIMESTAMP_TYPE));
                    if (magic < RecordBatch.MAGIC_VALUE_V2)
                        assertTrue(record.hasTimestampType(TimestampType.CREATE_TIME));
                    else
                        assertFalse(record.hasTimestampType(TimestampType.CREATE_TIME));
                }

                total++;
                recordCount++;
            }

            assertEquals(batch.baseOffset() + recordCount - 1, batch.lastOffset());
        }
    }
}
