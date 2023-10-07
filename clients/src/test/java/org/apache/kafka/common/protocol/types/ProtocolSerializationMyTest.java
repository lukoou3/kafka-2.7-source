package org.apache.kafka.common.protocol.types;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class ProtocolSerializationMyTest {
    @Test
    public void testReadIgnoringExtraDataAtTheEnd() {
        Schema oldSchema = new Schema(new Field("field1", Type.NULLABLE_STRING), new Field("field2", Type.NULLABLE_STRING));
        Schema newSchema = new Schema(new Field("field1", Type.NULLABLE_STRING));
        String value = "foo bar baz";
        Struct oldFormat = new Struct(oldSchema).set("field1", value).set("field2", "fine to ignore");
        ByteBuffer buffer = ByteBuffer.allocate(oldSchema.sizeOf(oldFormat));
        oldFormat.writeTo(buffer);
        buffer.flip();
        Struct newFormat = newSchema.read(buffer);
        assertEquals(value, newFormat.get("field1"));
    }

}
