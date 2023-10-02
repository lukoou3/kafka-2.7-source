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

// THIS CODE IS AUTOMATICALLY GENERATED.  DO NOT EDIT.

package org.apache.kafka.common.message;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import org.apache.kafka.common.errors.UnsupportedVersionException;
import org.apache.kafka.common.protocol.ApiMessage;
import org.apache.kafka.common.protocol.Message;
import org.apache.kafka.common.protocol.MessageUtil;
import org.apache.kafka.common.protocol.ObjectSerializationCache;
import org.apache.kafka.common.protocol.Readable;
import org.apache.kafka.common.protocol.Writable;
import org.apache.kafka.common.protocol.types.ArrayOf;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.protocol.types.RawTaggedField;
import org.apache.kafka.common.protocol.types.RawTaggedFieldWriter;
import org.apache.kafka.common.protocol.types.Schema;
import org.apache.kafka.common.protocol.types.Struct;
import org.apache.kafka.common.protocol.types.Type;
import org.apache.kafka.common.utils.ByteUtils;


public class DescribeClientQuotasResponseData implements ApiMessage {
    int throttleTimeMs;
    short errorCode;
    String errorMessage;
    List<EntryData> entries;
    private List<RawTaggedField> _unknownTaggedFields;
    
    public static final Schema SCHEMA_0 =
        new Schema(
            new Field("throttle_time_ms", Type.INT32, "The duration in milliseconds for which the request was throttled due to a quota violation, or zero if the request did not violate any quota."),
            new Field("error_code", Type.INT16, "The error code, or `0` if the quota description succeeded."),
            new Field("error_message", Type.NULLABLE_STRING, "The error message, or `null` if the quota description succeeded."),
            new Field("entries", ArrayOf.nullable(EntryData.SCHEMA_0), "A result entry.")
        );
    
    public static final Schema[] SCHEMAS = new Schema[] {
        SCHEMA_0
    };
    
    public static final short LOWEST_SUPPORTED_VERSION = 0;
    public static final short HIGHEST_SUPPORTED_VERSION = 0;
    
    public DescribeClientQuotasResponseData(Readable _readable, short _version) {
        read(_readable, _version);
    }
    
    public DescribeClientQuotasResponseData(Struct _struct, short _version) {
        fromStruct(_struct, _version);
    }
    
    public DescribeClientQuotasResponseData() {
        this.throttleTimeMs = 0;
        this.errorCode = (short) 0;
        this.errorMessage = "";
        this.entries = new ArrayList<EntryData>(0);
    }
    
    @Override
    public short apiKey() {
        return 48;
    }
    
    @Override
    public short lowestSupportedVersion() {
        return 0;
    }
    
    @Override
    public short highestSupportedVersion() {
        return 0;
    }
    
    @Override
    public void read(Readable _readable, short _version) {
        this.throttleTimeMs = _readable.readInt();
        this.errorCode = _readable.readShort();
        {
            int length;
            length = _readable.readShort();
            if (length < 0) {
                this.errorMessage = null;
            } else if (length > 0x7fff) {
                throw new RuntimeException("string field errorMessage had invalid length " + length);
            } else {
                this.errorMessage = _readable.readString(length);
            }
        }
        {
            int arrayLength;
            arrayLength = _readable.readInt();
            if (arrayLength < 0) {
                this.entries = null;
            } else {
                ArrayList<EntryData> newCollection = new ArrayList<EntryData>(arrayLength);
                for (int i = 0; i < arrayLength; i++) {
                    newCollection.add(new EntryData(_readable, _version));
                }
                this.entries = newCollection;
            }
        }
        this._unknownTaggedFields = null;
    }
    
    @Override
    public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
        int _numTaggedFields = 0;
        _writable.writeInt(throttleTimeMs);
        _writable.writeShort(errorCode);
        if (errorMessage == null) {
            _writable.writeShort((short) -1);
        } else {
            byte[] _stringBytes = _cache.getSerializedValue(errorMessage);
            _writable.writeShort((short) _stringBytes.length);
            _writable.writeByteArray(_stringBytes);
        }
        if (entries == null) {
            _writable.writeInt(-1);
        } else {
            _writable.writeInt(entries.size());
            for (EntryData entriesElement : entries) {
                entriesElement.write(_writable, _cache, _version);
            }
        }
        RawTaggedFieldWriter _rawWriter = RawTaggedFieldWriter.forFields(_unknownTaggedFields);
        _numTaggedFields += _rawWriter.numFields();
        if (_numTaggedFields > 0) {
            throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void fromStruct(Struct struct, short _version) {
        this._unknownTaggedFields = null;
        this.throttleTimeMs = struct.getInt("throttle_time_ms");
        this.errorCode = struct.getShort("error_code");
        this.errorMessage = struct.getString("error_message");
        {
            Object[] _nestedObjects = struct.getArray("entries");
            if (_nestedObjects == null) {
                this.entries = null;
            } else {
                this.entries = new ArrayList<EntryData>(_nestedObjects.length);
                for (Object nestedObject : _nestedObjects) {
                    this.entries.add(new EntryData((Struct) nestedObject, _version));
                }
            }
        }
    }
    
    @Override
    public Struct toStruct(short _version) {
        TreeMap<Integer, Object> _taggedFields = null;
        Struct struct = new Struct(SCHEMAS[_version]);
        struct.set("throttle_time_ms", this.throttleTimeMs);
        struct.set("error_code", this.errorCode);
        struct.set("error_message", this.errorMessage);
        {
            if (entries == null) {
                struct.set("entries", null);
            } else {
                Struct[] _nestedObjects = new Struct[entries.size()];
                int i = 0;
                for (EntryData element : this.entries) {
                    _nestedObjects[i++] = element.toStruct(_version);
                }
                struct.set("entries", (Object[]) _nestedObjects);
            }
        }
        return struct;
    }
    
    @Override
    public int size(ObjectSerializationCache _cache, short _version) {
        int _size = 0, _numTaggedFields = 0;
        _size += 4;
        _size += 2;
        if (errorMessage == null) {
            _size += 2;
        } else {
            byte[] _stringBytes = errorMessage.getBytes(StandardCharsets.UTF_8);
            if (_stringBytes.length > 0x7fff) {
                throw new RuntimeException("'errorMessage' field is too long to be serialized");
            }
            _cache.cacheSerializedValue(errorMessage, _stringBytes);
            _size += _stringBytes.length + 2;
        }
        if (entries == null) {
            _size += 4;
        } else {
            int _arraySize = 0;
            _arraySize += 4;
            for (EntryData entriesElement : entries) {
                _arraySize += entriesElement.size(_cache, _version);
            }
            _size += _arraySize;
        }
        if (_unknownTaggedFields != null) {
            _numTaggedFields += _unknownTaggedFields.size();
            for (RawTaggedField _field : _unknownTaggedFields) {
                _size += ByteUtils.sizeOfUnsignedVarint(_field.tag());
                _size += ByteUtils.sizeOfUnsignedVarint(_field.size());
                _size += _field.size();
            }
        }
        if (_numTaggedFields > 0) {
            throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
        }
        return _size;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DescribeClientQuotasResponseData)) return false;
        DescribeClientQuotasResponseData other = (DescribeClientQuotasResponseData) obj;
        if (throttleTimeMs != other.throttleTimeMs) return false;
        if (errorCode != other.errorCode) return false;
        if (this.errorMessage == null) {
            if (other.errorMessage != null) return false;
        } else {
            if (!this.errorMessage.equals(other.errorMessage)) return false;
        }
        if (this.entries == null) {
            if (other.entries != null) return false;
        } else {
            if (!this.entries.equals(other.entries)) return false;
        }
        return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
    }
    
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 31 * hashCode + throttleTimeMs;
        hashCode = 31 * hashCode + errorCode;
        hashCode = 31 * hashCode + (errorMessage == null ? 0 : errorMessage.hashCode());
        hashCode = 31 * hashCode + (entries == null ? 0 : entries.hashCode());
        return hashCode;
    }
    
    @Override
    public DescribeClientQuotasResponseData duplicate() {
        DescribeClientQuotasResponseData _duplicate = new DescribeClientQuotasResponseData();
        _duplicate.throttleTimeMs = throttleTimeMs;
        _duplicate.errorCode = errorCode;
        if (errorMessage == null) {
            _duplicate.errorMessage = null;
        } else {
            _duplicate.errorMessage = errorMessage;
        }
        if (entries == null) {
            _duplicate.entries = null;
        } else {
            ArrayList<EntryData> newEntries = new ArrayList<EntryData>(entries.size());
            for (EntryData _element : entries) {
                newEntries.add(_element.duplicate());
            }
            _duplicate.entries = newEntries;
        }
        return _duplicate;
    }
    
    @Override
    public String toString() {
        return "DescribeClientQuotasResponseData("
            + "throttleTimeMs=" + throttleTimeMs
            + ", errorCode=" + errorCode
            + ", errorMessage=" + ((errorMessage == null) ? "null" : "'" + errorMessage.toString() + "'")
            + ", entries=" + ((entries == null) ? "null" : MessageUtil.deepToString(entries.iterator()))
            + ")";
    }
    
    public int throttleTimeMs() {
        return this.throttleTimeMs;
    }
    
    public short errorCode() {
        return this.errorCode;
    }
    
    public String errorMessage() {
        return this.errorMessage;
    }
    
    public List<EntryData> entries() {
        return this.entries;
    }
    
    @Override
    public List<RawTaggedField> unknownTaggedFields() {
        if (_unknownTaggedFields == null) {
            _unknownTaggedFields = new ArrayList<>(0);
        }
        return _unknownTaggedFields;
    }
    
    public DescribeClientQuotasResponseData setThrottleTimeMs(int v) {
        this.throttleTimeMs = v;
        return this;
    }
    
    public DescribeClientQuotasResponseData setErrorCode(short v) {
        this.errorCode = v;
        return this;
    }
    
    public DescribeClientQuotasResponseData setErrorMessage(String v) {
        this.errorMessage = v;
        return this;
    }
    
    public DescribeClientQuotasResponseData setEntries(List<EntryData> v) {
        this.entries = v;
        return this;
    }
    
    public static class EntryData implements Message {
        List<EntityData> entity;
        List<ValueData> values;
        private List<RawTaggedField> _unknownTaggedFields;
        
        public static final Schema SCHEMA_0 =
            new Schema(
                new Field("entity", new ArrayOf(EntityData.SCHEMA_0), "The quota entity description."),
                new Field("values", new ArrayOf(ValueData.SCHEMA_0), "The quota values for the entity.")
            );
        
        public static final Schema[] SCHEMAS = new Schema[] {
            SCHEMA_0
        };
        
        public static final short LOWEST_SUPPORTED_VERSION = 0;
        public static final short HIGHEST_SUPPORTED_VERSION = 0;
        
        public EntryData(Readable _readable, short _version) {
            read(_readable, _version);
        }
        
        public EntryData(Struct _struct, short _version) {
            fromStruct(_struct, _version);
        }
        
        public EntryData() {
            this.entity = new ArrayList<EntityData>(0);
            this.values = new ArrayList<ValueData>(0);
        }
        
        
        @Override
        public short lowestSupportedVersion() {
            return 0;
        }
        
        @Override
        public short highestSupportedVersion() {
            return 0;
        }
        
        @Override
        public void read(Readable _readable, short _version) {
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't read version " + _version + " of EntryData");
            }
            {
                int arrayLength;
                arrayLength = _readable.readInt();
                if (arrayLength < 0) {
                    throw new RuntimeException("non-nullable field entity was serialized as null");
                } else {
                    ArrayList<EntityData> newCollection = new ArrayList<EntityData>(arrayLength);
                    for (int i = 0; i < arrayLength; i++) {
                        newCollection.add(new EntityData(_readable, _version));
                    }
                    this.entity = newCollection;
                }
            }
            {
                int arrayLength;
                arrayLength = _readable.readInt();
                if (arrayLength < 0) {
                    throw new RuntimeException("non-nullable field values was serialized as null");
                } else {
                    ArrayList<ValueData> newCollection = new ArrayList<ValueData>(arrayLength);
                    for (int i = 0; i < arrayLength; i++) {
                        newCollection.add(new ValueData(_readable, _version));
                    }
                    this.values = newCollection;
                }
            }
            this._unknownTaggedFields = null;
        }
        
        @Override
        public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
            int _numTaggedFields = 0;
            _writable.writeInt(entity.size());
            for (EntityData entityElement : entity) {
                entityElement.write(_writable, _cache, _version);
            }
            _writable.writeInt(values.size());
            for (ValueData valuesElement : values) {
                valuesElement.write(_writable, _cache, _version);
            }
            RawTaggedFieldWriter _rawWriter = RawTaggedFieldWriter.forFields(_unknownTaggedFields);
            _numTaggedFields += _rawWriter.numFields();
            if (_numTaggedFields > 0) {
                throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
            }
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void fromStruct(Struct struct, short _version) {
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't read version " + _version + " of EntryData");
            }
            this._unknownTaggedFields = null;
            {
                Object[] _nestedObjects = struct.getArray("entity");
                this.entity = new ArrayList<EntityData>(_nestedObjects.length);
                for (Object nestedObject : _nestedObjects) {
                    this.entity.add(new EntityData((Struct) nestedObject, _version));
                }
            }
            {
                Object[] _nestedObjects = struct.getArray("values");
                this.values = new ArrayList<ValueData>(_nestedObjects.length);
                for (Object nestedObject : _nestedObjects) {
                    this.values.add(new ValueData((Struct) nestedObject, _version));
                }
            }
        }
        
        @Override
        public Struct toStruct(short _version) {
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't write version " + _version + " of EntryData");
            }
            TreeMap<Integer, Object> _taggedFields = null;
            Struct struct = new Struct(SCHEMAS[_version]);
            {
                Struct[] _nestedObjects = new Struct[entity.size()];
                int i = 0;
                for (EntityData element : this.entity) {
                    _nestedObjects[i++] = element.toStruct(_version);
                }
                struct.set("entity", (Object[]) _nestedObjects);
            }
            {
                Struct[] _nestedObjects = new Struct[values.size()];
                int i = 0;
                for (ValueData element : this.values) {
                    _nestedObjects[i++] = element.toStruct(_version);
                }
                struct.set("values", (Object[]) _nestedObjects);
            }
            return struct;
        }
        
        @Override
        public int size(ObjectSerializationCache _cache, short _version) {
            int _size = 0, _numTaggedFields = 0;
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't size version " + _version + " of EntryData");
            }
            {
                int _arraySize = 0;
                _arraySize += 4;
                for (EntityData entityElement : entity) {
                    _arraySize += entityElement.size(_cache, _version);
                }
                _size += _arraySize;
            }
            {
                int _arraySize = 0;
                _arraySize += 4;
                for (ValueData valuesElement : values) {
                    _arraySize += valuesElement.size(_cache, _version);
                }
                _size += _arraySize;
            }
            if (_unknownTaggedFields != null) {
                _numTaggedFields += _unknownTaggedFields.size();
                for (RawTaggedField _field : _unknownTaggedFields) {
                    _size += ByteUtils.sizeOfUnsignedVarint(_field.tag());
                    _size += ByteUtils.sizeOfUnsignedVarint(_field.size());
                    _size += _field.size();
                }
            }
            if (_numTaggedFields > 0) {
                throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
            }
            return _size;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EntryData)) return false;
            EntryData other = (EntryData) obj;
            if (this.entity == null) {
                if (other.entity != null) return false;
            } else {
                if (!this.entity.equals(other.entity)) return false;
            }
            if (this.values == null) {
                if (other.values != null) return false;
            } else {
                if (!this.values.equals(other.values)) return false;
            }
            return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
        }
        
        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode = 31 * hashCode + (entity == null ? 0 : entity.hashCode());
            hashCode = 31 * hashCode + (values == null ? 0 : values.hashCode());
            return hashCode;
        }
        
        @Override
        public EntryData duplicate() {
            EntryData _duplicate = new EntryData();
            ArrayList<EntityData> newEntity = new ArrayList<EntityData>(entity.size());
            for (EntityData _element : entity) {
                newEntity.add(_element.duplicate());
            }
            _duplicate.entity = newEntity;
            ArrayList<ValueData> newValues = new ArrayList<ValueData>(values.size());
            for (ValueData _element : values) {
                newValues.add(_element.duplicate());
            }
            _duplicate.values = newValues;
            return _duplicate;
        }
        
        @Override
        public String toString() {
            return "EntryData("
                + "entity=" + MessageUtil.deepToString(entity.iterator())
                + ", values=" + MessageUtil.deepToString(values.iterator())
                + ")";
        }
        
        public List<EntityData> entity() {
            return this.entity;
        }
        
        public List<ValueData> values() {
            return this.values;
        }
        
        @Override
        public List<RawTaggedField> unknownTaggedFields() {
            if (_unknownTaggedFields == null) {
                _unknownTaggedFields = new ArrayList<>(0);
            }
            return _unknownTaggedFields;
        }
        
        public EntryData setEntity(List<EntityData> v) {
            this.entity = v;
            return this;
        }
        
        public EntryData setValues(List<ValueData> v) {
            this.values = v;
            return this;
        }
    }
    
    public static class EntityData implements Message {
        String entityType;
        String entityName;
        private List<RawTaggedField> _unknownTaggedFields;
        
        public static final Schema SCHEMA_0 =
            new Schema(
                new Field("entity_type", Type.STRING, "The entity type."),
                new Field("entity_name", Type.NULLABLE_STRING, "The entity name, or null if the default.")
            );
        
        public static final Schema[] SCHEMAS = new Schema[] {
            SCHEMA_0
        };
        
        public static final short LOWEST_SUPPORTED_VERSION = 0;
        public static final short HIGHEST_SUPPORTED_VERSION = 0;
        
        public EntityData(Readable _readable, short _version) {
            read(_readable, _version);
        }
        
        public EntityData(Struct _struct, short _version) {
            fromStruct(_struct, _version);
        }
        
        public EntityData() {
            this.entityType = "";
            this.entityName = "";
        }
        
        
        @Override
        public short lowestSupportedVersion() {
            return 0;
        }
        
        @Override
        public short highestSupportedVersion() {
            return 0;
        }
        
        @Override
        public void read(Readable _readable, short _version) {
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't read version " + _version + " of EntityData");
            }
            {
                int length;
                length = _readable.readShort();
                if (length < 0) {
                    throw new RuntimeException("non-nullable field entityType was serialized as null");
                } else if (length > 0x7fff) {
                    throw new RuntimeException("string field entityType had invalid length " + length);
                } else {
                    this.entityType = _readable.readString(length);
                }
            }
            {
                int length;
                length = _readable.readShort();
                if (length < 0) {
                    this.entityName = null;
                } else if (length > 0x7fff) {
                    throw new RuntimeException("string field entityName had invalid length " + length);
                } else {
                    this.entityName = _readable.readString(length);
                }
            }
            this._unknownTaggedFields = null;
        }
        
        @Override
        public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
            int _numTaggedFields = 0;
            {
                byte[] _stringBytes = _cache.getSerializedValue(entityType);
                _writable.writeShort((short) _stringBytes.length);
                _writable.writeByteArray(_stringBytes);
            }
            if (entityName == null) {
                _writable.writeShort((short) -1);
            } else {
                byte[] _stringBytes = _cache.getSerializedValue(entityName);
                _writable.writeShort((short) _stringBytes.length);
                _writable.writeByteArray(_stringBytes);
            }
            RawTaggedFieldWriter _rawWriter = RawTaggedFieldWriter.forFields(_unknownTaggedFields);
            _numTaggedFields += _rawWriter.numFields();
            if (_numTaggedFields > 0) {
                throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
            }
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void fromStruct(Struct struct, short _version) {
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't read version " + _version + " of EntityData");
            }
            this._unknownTaggedFields = null;
            this.entityType = struct.getString("entity_type");
            this.entityName = struct.getString("entity_name");
        }
        
        @Override
        public Struct toStruct(short _version) {
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't write version " + _version + " of EntityData");
            }
            TreeMap<Integer, Object> _taggedFields = null;
            Struct struct = new Struct(SCHEMAS[_version]);
            struct.set("entity_type", this.entityType);
            struct.set("entity_name", this.entityName);
            return struct;
        }
        
        @Override
        public int size(ObjectSerializationCache _cache, short _version) {
            int _size = 0, _numTaggedFields = 0;
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't size version " + _version + " of EntityData");
            }
            {
                byte[] _stringBytes = entityType.getBytes(StandardCharsets.UTF_8);
                if (_stringBytes.length > 0x7fff) {
                    throw new RuntimeException("'entityType' field is too long to be serialized");
                }
                _cache.cacheSerializedValue(entityType, _stringBytes);
                _size += _stringBytes.length + 2;
            }
            if (entityName == null) {
                _size += 2;
            } else {
                byte[] _stringBytes = entityName.getBytes(StandardCharsets.UTF_8);
                if (_stringBytes.length > 0x7fff) {
                    throw new RuntimeException("'entityName' field is too long to be serialized");
                }
                _cache.cacheSerializedValue(entityName, _stringBytes);
                _size += _stringBytes.length + 2;
            }
            if (_unknownTaggedFields != null) {
                _numTaggedFields += _unknownTaggedFields.size();
                for (RawTaggedField _field : _unknownTaggedFields) {
                    _size += ByteUtils.sizeOfUnsignedVarint(_field.tag());
                    _size += ByteUtils.sizeOfUnsignedVarint(_field.size());
                    _size += _field.size();
                }
            }
            if (_numTaggedFields > 0) {
                throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
            }
            return _size;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EntityData)) return false;
            EntityData other = (EntityData) obj;
            if (this.entityType == null) {
                if (other.entityType != null) return false;
            } else {
                if (!this.entityType.equals(other.entityType)) return false;
            }
            if (this.entityName == null) {
                if (other.entityName != null) return false;
            } else {
                if (!this.entityName.equals(other.entityName)) return false;
            }
            return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
        }
        
        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode = 31 * hashCode + (entityType == null ? 0 : entityType.hashCode());
            hashCode = 31 * hashCode + (entityName == null ? 0 : entityName.hashCode());
            return hashCode;
        }
        
        @Override
        public EntityData duplicate() {
            EntityData _duplicate = new EntityData();
            _duplicate.entityType = entityType;
            if (entityName == null) {
                _duplicate.entityName = null;
            } else {
                _duplicate.entityName = entityName;
            }
            return _duplicate;
        }
        
        @Override
        public String toString() {
            return "EntityData("
                + "entityType=" + ((entityType == null) ? "null" : "'" + entityType.toString() + "'")
                + ", entityName=" + ((entityName == null) ? "null" : "'" + entityName.toString() + "'")
                + ")";
        }
        
        public String entityType() {
            return this.entityType;
        }
        
        public String entityName() {
            return this.entityName;
        }
        
        @Override
        public List<RawTaggedField> unknownTaggedFields() {
            if (_unknownTaggedFields == null) {
                _unknownTaggedFields = new ArrayList<>(0);
            }
            return _unknownTaggedFields;
        }
        
        public EntityData setEntityType(String v) {
            this.entityType = v;
            return this;
        }
        
        public EntityData setEntityName(String v) {
            this.entityName = v;
            return this;
        }
    }
    
    public static class ValueData implements Message {
        String key;
        double value;
        private List<RawTaggedField> _unknownTaggedFields;
        
        public static final Schema SCHEMA_0 =
            new Schema(
                new Field("key", Type.STRING, "The quota configuration key."),
                new Field("value", Type.FLOAT64, "The quota configuration value.")
            );
        
        public static final Schema[] SCHEMAS = new Schema[] {
            SCHEMA_0
        };
        
        public static final short LOWEST_SUPPORTED_VERSION = 0;
        public static final short HIGHEST_SUPPORTED_VERSION = 0;
        
        public ValueData(Readable _readable, short _version) {
            read(_readable, _version);
        }
        
        public ValueData(Struct _struct, short _version) {
            fromStruct(_struct, _version);
        }
        
        public ValueData() {
            this.key = "";
            this.value = 0.0;
        }
        
        
        @Override
        public short lowestSupportedVersion() {
            return 0;
        }
        
        @Override
        public short highestSupportedVersion() {
            return 0;
        }
        
        @Override
        public void read(Readable _readable, short _version) {
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't read version " + _version + " of ValueData");
            }
            {
                int length;
                length = _readable.readShort();
                if (length < 0) {
                    throw new RuntimeException("non-nullable field key was serialized as null");
                } else if (length > 0x7fff) {
                    throw new RuntimeException("string field key had invalid length " + length);
                } else {
                    this.key = _readable.readString(length);
                }
            }
            this.value = _readable.readDouble();
            this._unknownTaggedFields = null;
        }
        
        @Override
        public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
            int _numTaggedFields = 0;
            {
                byte[] _stringBytes = _cache.getSerializedValue(key);
                _writable.writeShort((short) _stringBytes.length);
                _writable.writeByteArray(_stringBytes);
            }
            _writable.writeDouble(value);
            RawTaggedFieldWriter _rawWriter = RawTaggedFieldWriter.forFields(_unknownTaggedFields);
            _numTaggedFields += _rawWriter.numFields();
            if (_numTaggedFields > 0) {
                throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
            }
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public void fromStruct(Struct struct, short _version) {
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't read version " + _version + " of ValueData");
            }
            this._unknownTaggedFields = null;
            this.key = struct.getString("key");
            this.value = struct.getDouble("value");
        }
        
        @Override
        public Struct toStruct(short _version) {
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't write version " + _version + " of ValueData");
            }
            TreeMap<Integer, Object> _taggedFields = null;
            Struct struct = new Struct(SCHEMAS[_version]);
            struct.set("key", this.key);
            struct.set("value", this.value);
            return struct;
        }
        
        @Override
        public int size(ObjectSerializationCache _cache, short _version) {
            int _size = 0, _numTaggedFields = 0;
            if (_version > 0) {
                throw new UnsupportedVersionException("Can't size version " + _version + " of ValueData");
            }
            {
                byte[] _stringBytes = key.getBytes(StandardCharsets.UTF_8);
                if (_stringBytes.length > 0x7fff) {
                    throw new RuntimeException("'key' field is too long to be serialized");
                }
                _cache.cacheSerializedValue(key, _stringBytes);
                _size += _stringBytes.length + 2;
            }
            _size += 8;
            if (_unknownTaggedFields != null) {
                _numTaggedFields += _unknownTaggedFields.size();
                for (RawTaggedField _field : _unknownTaggedFields) {
                    _size += ByteUtils.sizeOfUnsignedVarint(_field.tag());
                    _size += ByteUtils.sizeOfUnsignedVarint(_field.size());
                    _size += _field.size();
                }
            }
            if (_numTaggedFields > 0) {
                throw new UnsupportedVersionException("Tagged fields were set, but version " + _version + " of this message does not support them.");
            }
            return _size;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ValueData)) return false;
            ValueData other = (ValueData) obj;
            if (this.key == null) {
                if (other.key != null) return false;
            } else {
                if (!this.key.equals(other.key)) return false;
            }
            if (value != other.value) return false;
            return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
        }
        
        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode = 31 * hashCode + (key == null ? 0 : key.hashCode());
            hashCode = 31 * hashCode + Double.hashCode(value);
            return hashCode;
        }
        
        @Override
        public ValueData duplicate() {
            ValueData _duplicate = new ValueData();
            _duplicate.key = key;
            _duplicate.value = value;
            return _duplicate;
        }
        
        @Override
        public String toString() {
            return "ValueData("
                + "key=" + ((key == null) ? "null" : "'" + key.toString() + "'")
                + ", value=" + value
                + ")";
        }
        
        public String key() {
            return this.key;
        }
        
        public double value() {
            return this.value;
        }
        
        @Override
        public List<RawTaggedField> unknownTaggedFields() {
            if (_unknownTaggedFields == null) {
                _unknownTaggedFields = new ArrayList<>(0);
            }
            return _unknownTaggedFields;
        }
        
        public ValueData setKey(String v) {
            this.key = v;
            return this;
        }
        
        public ValueData setValue(double v) {
            this.value = v;
            return this;
        }
    }
}
