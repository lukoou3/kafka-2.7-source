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

package org.apache.kafka.streams.internals.generated;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;
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
import org.apache.kafka.common.utils.Bytes;


public class SubscriptionInfoData implements ApiMessage {
    int version;
    int latestSupportedVersion;
    UUID processId;
    List<TaskId> prevTasks;
    List<TaskId> standbyTasks;
    byte[] userEndPoint;
    List<TaskOffsetSum> taskOffsetSums;
    byte uniqueField;
    private List<RawTaggedField> _unknownTaggedFields;
    
    public static final Schema SCHEMA_1 =
        new Schema(
            new Field("version", Type.INT32, ""),
            new Field("process_id", Type.UUID, ""),
            new Field("prev_tasks", new ArrayOf(TaskId.SCHEMA_1), ""),
            new Field("standby_tasks", new ArrayOf(TaskId.SCHEMA_1), "")
        );
    
    public static final Schema SCHEMA_2 =
        new Schema(
            new Field("version", Type.INT32, ""),
            new Field("process_id", Type.UUID, ""),
            new Field("prev_tasks", new ArrayOf(TaskId.SCHEMA_1), ""),
            new Field("standby_tasks", new ArrayOf(TaskId.SCHEMA_1), ""),
            new Field("user_end_point", Type.BYTES, "")
        );
    
    public static final Schema SCHEMA_3 =
        new Schema(
            new Field("version", Type.INT32, ""),
            new Field("latest_supported_version", Type.INT32, ""),
            new Field("process_id", Type.UUID, ""),
            new Field("prev_tasks", new ArrayOf(TaskId.SCHEMA_1), ""),
            new Field("standby_tasks", new ArrayOf(TaskId.SCHEMA_1), ""),
            new Field("user_end_point", Type.BYTES, "")
        );
    
    public static final Schema SCHEMA_4 = SCHEMA_3;
    
    public static final Schema SCHEMA_5 = SCHEMA_4;
    
    public static final Schema SCHEMA_6 = SCHEMA_5;
    
    public static final Schema SCHEMA_7 =
        new Schema(
            new Field("version", Type.INT32, ""),
            new Field("latest_supported_version", Type.INT32, ""),
            new Field("process_id", Type.UUID, ""),
            new Field("user_end_point", Type.BYTES, ""),
            new Field("task_offset_sums", new ArrayOf(TaskOffsetSum.SCHEMA_7), "")
        );
    
    public static final Schema SCHEMA_8 =
        new Schema(
            new Field("version", Type.INT32, ""),
            new Field("latest_supported_version", Type.INT32, ""),
            new Field("process_id", Type.UUID, ""),
            new Field("user_end_point", Type.BYTES, ""),
            new Field("task_offset_sums", new ArrayOf(TaskOffsetSum.SCHEMA_7), ""),
            new Field("unique_field", Type.INT8, "")
        );
    
    public static final Schema[] SCHEMAS = new Schema[] {
        null,
        SCHEMA_1,
        SCHEMA_2,
        SCHEMA_3,
        SCHEMA_4,
        SCHEMA_5,
        SCHEMA_6,
        SCHEMA_7,
        SCHEMA_8
    };
    
    public static final short LOWEST_SUPPORTED_VERSION = 1;
    public static final short HIGHEST_SUPPORTED_VERSION = 8;
    
    public SubscriptionInfoData(Readable _readable, short _version) {
        read(_readable, _version);
    }
    
    public SubscriptionInfoData(Struct _struct, short _version) {
        fromStruct(_struct, _version);
    }
    
    public SubscriptionInfoData() {
        this.version = 0;
        this.latestSupportedVersion = -1;
        this.processId = MessageUtil.ZERO_UUID;
        this.prevTasks = new ArrayList<TaskId>(0);
        this.standbyTasks = new ArrayList<TaskId>(0);
        this.userEndPoint = Bytes.EMPTY;
        this.taskOffsetSums = new ArrayList<TaskOffsetSum>(0);
        this.uniqueField = (byte) 0;
    }
    
    @Override
    public short apiKey() {
        return -1;
    }
    
    @Override
    public short lowestSupportedVersion() {
        return 1;
    }
    
    @Override
    public short highestSupportedVersion() {
        return 8;
    }
    
    @Override
    public void read(Readable _readable, short _version) {
        this.version = _readable.readInt();
        if (_version >= 3) {
            this.latestSupportedVersion = _readable.readInt();
        } else {
            this.latestSupportedVersion = -1;
        }
        this.processId = _readable.readUUID();
        if (_version <= 6) {
            int arrayLength;
            arrayLength = _readable.readInt();
            if (arrayLength < 0) {
                throw new RuntimeException("non-nullable field prevTasks was serialized as null");
            } else {
                ArrayList<TaskId> newCollection = new ArrayList<TaskId>(arrayLength);
                for (int i = 0; i < arrayLength; i++) {
                    newCollection.add(new TaskId(_readable, _version));
                }
                this.prevTasks = newCollection;
            }
        } else {
            this.prevTasks = new ArrayList<TaskId>(0);
        }
        if (_version <= 6) {
            int arrayLength;
            arrayLength = _readable.readInt();
            if (arrayLength < 0) {
                throw new RuntimeException("non-nullable field standbyTasks was serialized as null");
            } else {
                ArrayList<TaskId> newCollection = new ArrayList<TaskId>(arrayLength);
                for (int i = 0; i < arrayLength; i++) {
                    newCollection.add(new TaskId(_readable, _version));
                }
                this.standbyTasks = newCollection;
            }
        } else {
            this.standbyTasks = new ArrayList<TaskId>(0);
        }
        if (_version >= 2) {
            int length;
            length = _readable.readInt();
            if (length < 0) {
                throw new RuntimeException("non-nullable field userEndPoint was serialized as null");
            } else {
                byte[] newBytes = new byte[length];
                _readable.readArray(newBytes);
                this.userEndPoint = newBytes;
            }
        } else {
            this.userEndPoint = Bytes.EMPTY;
        }
        if (_version >= 7) {
            int arrayLength;
            arrayLength = _readable.readInt();
            if (arrayLength < 0) {
                throw new RuntimeException("non-nullable field taskOffsetSums was serialized as null");
            } else {
                ArrayList<TaskOffsetSum> newCollection = new ArrayList<TaskOffsetSum>(arrayLength);
                for (int i = 0; i < arrayLength; i++) {
                    newCollection.add(new TaskOffsetSum(_readable, _version));
                }
                this.taskOffsetSums = newCollection;
            }
        } else {
            this.taskOffsetSums = new ArrayList<TaskOffsetSum>(0);
        }
        if (_version >= 8) {
            this.uniqueField = _readable.readByte();
        } else {
            this.uniqueField = (byte) 0;
        }
        this._unknownTaggedFields = null;
    }
    
    @Override
    public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
        int _numTaggedFields = 0;
        _writable.writeInt(version);
        if (_version >= 3) {
            _writable.writeInt(latestSupportedVersion);
        } else {
            if (this.latestSupportedVersion != -1) {
                throw new UnsupportedVersionException("Attempted to write a non-default latestSupportedVersion at version " + _version);
            }
        }
        _writable.writeUUID(processId);
        if (_version <= 6) {
            _writable.writeInt(prevTasks.size());
            for (TaskId prevTasksElement : prevTasks) {
                prevTasksElement.write(_writable, _cache, _version);
            }
        } else {
            if (!this.prevTasks.isEmpty()) {
                throw new UnsupportedVersionException("Attempted to write a non-default prevTasks at version " + _version);
            }
        }
        if (_version <= 6) {
            _writable.writeInt(standbyTasks.size());
            for (TaskId standbyTasksElement : standbyTasks) {
                standbyTasksElement.write(_writable, _cache, _version);
            }
        } else {
            if (!this.standbyTasks.isEmpty()) {
                throw new UnsupportedVersionException("Attempted to write a non-default standbyTasks at version " + _version);
            }
        }
        if (_version >= 2) {
            _writable.writeInt(userEndPoint.length);
            _writable.writeByteArray(userEndPoint);
        } else {
            if (this.userEndPoint.length != 0) {
                throw new UnsupportedVersionException("Attempted to write a non-default userEndPoint at version " + _version);
            }
        }
        if (_version >= 7) {
            _writable.writeInt(taskOffsetSums.size());
            for (TaskOffsetSum taskOffsetSumsElement : taskOffsetSums) {
                taskOffsetSumsElement.write(_writable, _cache, _version);
            }
        } else {
            if (!this.taskOffsetSums.isEmpty()) {
                throw new UnsupportedVersionException("Attempted to write a non-default taskOffsetSums at version " + _version);
            }
        }
        if (_version >= 8) {
            _writable.writeByte(uniqueField);
        } else {
            if (this.uniqueField != (byte) 0) {
                throw new UnsupportedVersionException("Attempted to write a non-default uniqueField at version " + _version);
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
        this.version = struct.getInt("version");
        if (_version >= 3) {
            this.latestSupportedVersion = struct.getInt("latest_supported_version");
        } else {
            this.latestSupportedVersion = -1;
        }
        this.processId = struct.getUUID("process_id");
        if (_version <= 6) {
            Object[] _nestedObjects = struct.getArray("prev_tasks");
            this.prevTasks = new ArrayList<TaskId>(_nestedObjects.length);
            for (Object nestedObject : _nestedObjects) {
                this.prevTasks.add(new TaskId((Struct) nestedObject, _version));
            }
        } else {
            this.prevTasks = new ArrayList<TaskId>(0);
        }
        if (_version <= 6) {
            Object[] _nestedObjects = struct.getArray("standby_tasks");
            this.standbyTasks = new ArrayList<TaskId>(_nestedObjects.length);
            for (Object nestedObject : _nestedObjects) {
                this.standbyTasks.add(new TaskId((Struct) nestedObject, _version));
            }
        } else {
            this.standbyTasks = new ArrayList<TaskId>(0);
        }
        if (_version >= 2) {
            this.userEndPoint = struct.getByteArray("user_end_point");
        } else {
            this.userEndPoint = Bytes.EMPTY;
        }
        if (_version >= 7) {
            Object[] _nestedObjects = struct.getArray("task_offset_sums");
            this.taskOffsetSums = new ArrayList<TaskOffsetSum>(_nestedObjects.length);
            for (Object nestedObject : _nestedObjects) {
                this.taskOffsetSums.add(new TaskOffsetSum((Struct) nestedObject, _version));
            }
        } else {
            this.taskOffsetSums = new ArrayList<TaskOffsetSum>(0);
        }
        if (_version >= 8) {
            this.uniqueField = struct.getByte("unique_field");
        } else {
            this.uniqueField = (byte) 0;
        }
    }
    
    @Override
    public Struct toStruct(short _version) {
        TreeMap<Integer, Object> _taggedFields = null;
        Struct struct = new Struct(SCHEMAS[_version]);
        struct.set("version", this.version);
        if (_version >= 3) {
            struct.set("latest_supported_version", this.latestSupportedVersion);
        } else {
            if (this.latestSupportedVersion != -1) {
                throw new UnsupportedVersionException("Attempted to write a non-default latestSupportedVersion at version " + _version);
            }
        }
        struct.set("process_id", this.processId);
        if (_version <= 6) {
            Struct[] _nestedObjects = new Struct[prevTasks.size()];
            int i = 0;
            for (TaskId element : this.prevTasks) {
                _nestedObjects[i++] = element.toStruct(_version);
            }
            struct.set("prev_tasks", (Object[]) _nestedObjects);
        } else {
            if (!this.prevTasks.isEmpty()) {
                throw new UnsupportedVersionException("Attempted to write a non-default prevTasks at version " + _version);
            }
        }
        if (_version <= 6) {
            Struct[] _nestedObjects = new Struct[standbyTasks.size()];
            int i = 0;
            for (TaskId element : this.standbyTasks) {
                _nestedObjects[i++] = element.toStruct(_version);
            }
            struct.set("standby_tasks", (Object[]) _nestedObjects);
        } else {
            if (!this.standbyTasks.isEmpty()) {
                throw new UnsupportedVersionException("Attempted to write a non-default standbyTasks at version " + _version);
            }
        }
        if (_version >= 2) {
            struct.setByteArray("user_end_point", this.userEndPoint);
        } else {
            if (this.userEndPoint.length != 0) {
                throw new UnsupportedVersionException("Attempted to write a non-default userEndPoint at version " + _version);
            }
        }
        if (_version >= 7) {
            Struct[] _nestedObjects = new Struct[taskOffsetSums.size()];
            int i = 0;
            for (TaskOffsetSum element : this.taskOffsetSums) {
                _nestedObjects[i++] = element.toStruct(_version);
            }
            struct.set("task_offset_sums", (Object[]) _nestedObjects);
        } else {
            if (!this.taskOffsetSums.isEmpty()) {
                throw new UnsupportedVersionException("Attempted to write a non-default taskOffsetSums at version " + _version);
            }
        }
        if (_version >= 8) {
            struct.set("unique_field", this.uniqueField);
        } else {
            if (this.uniqueField != (byte) 0) {
                throw new UnsupportedVersionException("Attempted to write a non-default uniqueField at version " + _version);
            }
        }
        return struct;
    }
    
    @Override
    public int size(ObjectSerializationCache _cache, short _version) {
        int _size = 0, _numTaggedFields = 0;
        _size += 4;
        if (_version >= 3) {
            _size += 4;
        }
        _size += 16;
        if (_version <= 6) {
            {
                int _arraySize = 0;
                _arraySize += 4;
                for (TaskId prevTasksElement : prevTasks) {
                    _arraySize += prevTasksElement.size(_cache, _version);
                }
                _size += _arraySize;
            }
        }
        if (_version <= 6) {
            {
                int _arraySize = 0;
                _arraySize += 4;
                for (TaskId standbyTasksElement : standbyTasks) {
                    _arraySize += standbyTasksElement.size(_cache, _version);
                }
                _size += _arraySize;
            }
        }
        if (_version >= 2) {
            {
                int _bytesSize = userEndPoint.length;
                _bytesSize += 4;
                _size += _bytesSize;
            }
        }
        if (_version >= 7) {
            {
                int _arraySize = 0;
                _arraySize += 4;
                for (TaskOffsetSum taskOffsetSumsElement : taskOffsetSums) {
                    _arraySize += taskOffsetSumsElement.size(_cache, _version);
                }
                _size += _arraySize;
            }
        }
        if (_version >= 8) {
            _size += 1;
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
        if (!(obj instanceof SubscriptionInfoData)) return false;
        SubscriptionInfoData other = (SubscriptionInfoData) obj;
        if (version != other.version) return false;
        if (latestSupportedVersion != other.latestSupportedVersion) return false;
        if (!this.processId.equals(other.processId)) return false;
        if (this.prevTasks == null) {
            if (other.prevTasks != null) return false;
        } else {
            if (!this.prevTasks.equals(other.prevTasks)) return false;
        }
        if (this.standbyTasks == null) {
            if (other.standbyTasks != null) return false;
        } else {
            if (!this.standbyTasks.equals(other.standbyTasks)) return false;
        }
        if (!Arrays.equals(this.userEndPoint, other.userEndPoint)) return false;
        if (this.taskOffsetSums == null) {
            if (other.taskOffsetSums != null) return false;
        } else {
            if (!this.taskOffsetSums.equals(other.taskOffsetSums)) return false;
        }
        if (uniqueField != other.uniqueField) return false;
        return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
    }
    
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 31 * hashCode + version;
        hashCode = 31 * hashCode + latestSupportedVersion;
        hashCode = 31 * hashCode + processId.hashCode();
        hashCode = 31 * hashCode + (prevTasks == null ? 0 : prevTasks.hashCode());
        hashCode = 31 * hashCode + (standbyTasks == null ? 0 : standbyTasks.hashCode());
        hashCode = 31 * hashCode + Arrays.hashCode(userEndPoint);
        hashCode = 31 * hashCode + (taskOffsetSums == null ? 0 : taskOffsetSums.hashCode());
        hashCode = 31 * hashCode + uniqueField;
        return hashCode;
    }
    
    @Override
    public SubscriptionInfoData duplicate() {
        SubscriptionInfoData _duplicate = new SubscriptionInfoData();
        _duplicate.version = version;
        _duplicate.latestSupportedVersion = latestSupportedVersion;
        _duplicate.processId = processId;
        ArrayList<TaskId> newPrevTasks = new ArrayList<TaskId>(prevTasks.size());
        for (TaskId _element : prevTasks) {
            newPrevTasks.add(_element.duplicate());
        }
        _duplicate.prevTasks = newPrevTasks;
        ArrayList<TaskId> newStandbyTasks = new ArrayList<TaskId>(standbyTasks.size());
        for (TaskId _element : standbyTasks) {
            newStandbyTasks.add(_element.duplicate());
        }
        _duplicate.standbyTasks = newStandbyTasks;
        _duplicate.userEndPoint = MessageUtil.duplicate(userEndPoint);
        ArrayList<TaskOffsetSum> newTaskOffsetSums = new ArrayList<TaskOffsetSum>(taskOffsetSums.size());
        for (TaskOffsetSum _element : taskOffsetSums) {
            newTaskOffsetSums.add(_element.duplicate());
        }
        _duplicate.taskOffsetSums = newTaskOffsetSums;
        _duplicate.uniqueField = uniqueField;
        return _duplicate;
    }
    
    @Override
    public String toString() {
        return "SubscriptionInfoData("
            + "version=" + version
            + ", latestSupportedVersion=" + latestSupportedVersion
            + ", prevTasks=" + MessageUtil.deepToString(prevTasks.iterator())
            + ", standbyTasks=" + MessageUtil.deepToString(standbyTasks.iterator())
            + ", userEndPoint=" + Arrays.toString(userEndPoint)
            + ", taskOffsetSums=" + MessageUtil.deepToString(taskOffsetSums.iterator())
            + ", uniqueField=" + uniqueField
            + ")";
    }
    
    public int version() {
        return this.version;
    }
    
    public int latestSupportedVersion() {
        return this.latestSupportedVersion;
    }
    
    public UUID processId() {
        return this.processId;
    }
    
    public List<TaskId> prevTasks() {
        return this.prevTasks;
    }
    
    public List<TaskId> standbyTasks() {
        return this.standbyTasks;
    }
    
    public byte[] userEndPoint() {
        return this.userEndPoint;
    }
    
    public List<TaskOffsetSum> taskOffsetSums() {
        return this.taskOffsetSums;
    }
    
    public byte uniqueField() {
        return this.uniqueField;
    }
    
    @Override
    public List<RawTaggedField> unknownTaggedFields() {
        if (_unknownTaggedFields == null) {
            _unknownTaggedFields = new ArrayList<>(0);
        }
        return _unknownTaggedFields;
    }
    
    public SubscriptionInfoData setVersion(int v) {
        this.version = v;
        return this;
    }
    
    public SubscriptionInfoData setLatestSupportedVersion(int v) {
        this.latestSupportedVersion = v;
        return this;
    }
    
    public SubscriptionInfoData setProcessId(UUID v) {
        this.processId = v;
        return this;
    }
    
    public SubscriptionInfoData setPrevTasks(List<TaskId> v) {
        this.prevTasks = v;
        return this;
    }
    
    public SubscriptionInfoData setStandbyTasks(List<TaskId> v) {
        this.standbyTasks = v;
        return this;
    }
    
    public SubscriptionInfoData setUserEndPoint(byte[] v) {
        this.userEndPoint = v;
        return this;
    }
    
    public SubscriptionInfoData setTaskOffsetSums(List<TaskOffsetSum> v) {
        this.taskOffsetSums = v;
        return this;
    }
    
    public SubscriptionInfoData setUniqueField(byte v) {
        this.uniqueField = v;
        return this;
    }
    
    public static class PartitionToOffsetSum implements Message {
        int partition;
        long offsetSum;
        private List<RawTaggedField> _unknownTaggedFields;
        
        public static final Schema SCHEMA_7 =
            new Schema(
                new Field("partition", Type.INT32, ""),
                new Field("offset_sum", Type.INT64, "")
            );
        
        public static final Schema SCHEMA_8 = SCHEMA_7;
        
        public static final Schema[] SCHEMAS = new Schema[] {
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            SCHEMA_7,
            SCHEMA_8
        };
        
        public static final short LOWEST_SUPPORTED_VERSION = 7;
        public static final short HIGHEST_SUPPORTED_VERSION = 8;
        
        public PartitionToOffsetSum(Readable _readable, short _version) {
            read(_readable, _version);
        }
        
        public PartitionToOffsetSum(Struct _struct, short _version) {
            fromStruct(_struct, _version);
        }
        
        public PartitionToOffsetSum() {
            this.partition = 0;
            this.offsetSum = 0L;
        }
        
        
        @Override
        public short lowestSupportedVersion() {
            return 7;
        }
        
        @Override
        public short highestSupportedVersion() {
            return 32767;
        }
        
        @Override
        public void read(Readable _readable, short _version) {
            this.partition = _readable.readInt();
            this.offsetSum = _readable.readLong();
            this._unknownTaggedFields = null;
        }
        
        @Override
        public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
            int _numTaggedFields = 0;
            _writable.writeInt(partition);
            _writable.writeLong(offsetSum);
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
            this.partition = struct.getInt("partition");
            this.offsetSum = struct.getLong("offset_sum");
        }
        
        @Override
        public Struct toStruct(short _version) {
            TreeMap<Integer, Object> _taggedFields = null;
            Struct struct = new Struct(SCHEMAS[_version]);
            struct.set("partition", this.partition);
            struct.set("offset_sum", this.offsetSum);
            return struct;
        }
        
        @Override
        public int size(ObjectSerializationCache _cache, short _version) {
            int _size = 0, _numTaggedFields = 0;
            _size += 4;
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
            if (!(obj instanceof PartitionToOffsetSum)) return false;
            PartitionToOffsetSum other = (PartitionToOffsetSum) obj;
            if (partition != other.partition) return false;
            if (offsetSum != other.offsetSum) return false;
            return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
        }
        
        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode = 31 * hashCode + partition;
            hashCode = 31 * hashCode + ((int) (offsetSum >> 32) ^ (int) offsetSum);
            return hashCode;
        }
        
        @Override
        public PartitionToOffsetSum duplicate() {
            PartitionToOffsetSum _duplicate = new PartitionToOffsetSum();
            _duplicate.partition = partition;
            _duplicate.offsetSum = offsetSum;
            return _duplicate;
        }
        
        @Override
        public String toString() {
            return "PartitionToOffsetSum("
                + "partition=" + partition
                + ", offsetSum=" + offsetSum
                + ")";
        }
        
        public int partition() {
            return this.partition;
        }
        
        public long offsetSum() {
            return this.offsetSum;
        }
        
        @Override
        public List<RawTaggedField> unknownTaggedFields() {
            if (_unknownTaggedFields == null) {
                _unknownTaggedFields = new ArrayList<>(0);
            }
            return _unknownTaggedFields;
        }
        
        public PartitionToOffsetSum setPartition(int v) {
            this.partition = v;
            return this;
        }
        
        public PartitionToOffsetSum setOffsetSum(long v) {
            this.offsetSum = v;
            return this;
        }
    }
    
    public static class TaskId implements Message {
        int topicGroupId;
        int partition;
        private List<RawTaggedField> _unknownTaggedFields;
        
        public static final Schema SCHEMA_1 =
            new Schema(
                new Field("topic_group_id", Type.INT32, ""),
                new Field("partition", Type.INT32, "")
            );
        
        public static final Schema SCHEMA_2 = SCHEMA_1;
        
        public static final Schema SCHEMA_3 = SCHEMA_2;
        
        public static final Schema SCHEMA_4 = SCHEMA_3;
        
        public static final Schema SCHEMA_5 = SCHEMA_4;
        
        public static final Schema SCHEMA_6 = SCHEMA_5;
        
        public static final Schema SCHEMA_7 = SCHEMA_6;
        
        public static final Schema SCHEMA_8 = SCHEMA_7;
        
        public static final Schema[] SCHEMAS = new Schema[] {
            null,
            SCHEMA_1,
            SCHEMA_2,
            SCHEMA_3,
            SCHEMA_4,
            SCHEMA_5,
            SCHEMA_6,
            SCHEMA_7,
            SCHEMA_8
        };
        
        public static final short LOWEST_SUPPORTED_VERSION = 1;
        public static final short HIGHEST_SUPPORTED_VERSION = 8;
        
        public TaskId(Readable _readable, short _version) {
            read(_readable, _version);
        }
        
        public TaskId(Struct _struct, short _version) {
            fromStruct(_struct, _version);
        }
        
        public TaskId() {
            this.topicGroupId = 0;
            this.partition = 0;
        }
        
        
        @Override
        public short lowestSupportedVersion() {
            return 1;
        }
        
        @Override
        public short highestSupportedVersion() {
            return 32767;
        }
        
        @Override
        public void read(Readable _readable, short _version) {
            this.topicGroupId = _readable.readInt();
            this.partition = _readable.readInt();
            this._unknownTaggedFields = null;
        }
        
        @Override
        public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
            int _numTaggedFields = 0;
            _writable.writeInt(topicGroupId);
            _writable.writeInt(partition);
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
            this.topicGroupId = struct.getInt("topic_group_id");
            this.partition = struct.getInt("partition");
        }
        
        @Override
        public Struct toStruct(short _version) {
            TreeMap<Integer, Object> _taggedFields = null;
            Struct struct = new Struct(SCHEMAS[_version]);
            struct.set("topic_group_id", this.topicGroupId);
            struct.set("partition", this.partition);
            return struct;
        }
        
        @Override
        public int size(ObjectSerializationCache _cache, short _version) {
            int _size = 0, _numTaggedFields = 0;
            _size += 4;
            _size += 4;
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
            if (!(obj instanceof TaskId)) return false;
            TaskId other = (TaskId) obj;
            if (topicGroupId != other.topicGroupId) return false;
            if (partition != other.partition) return false;
            return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
        }
        
        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode = 31 * hashCode + topicGroupId;
            hashCode = 31 * hashCode + partition;
            return hashCode;
        }
        
        @Override
        public TaskId duplicate() {
            TaskId _duplicate = new TaskId();
            _duplicate.topicGroupId = topicGroupId;
            _duplicate.partition = partition;
            return _duplicate;
        }
        
        @Override
        public String toString() {
            return "TaskId("
                + "topicGroupId=" + topicGroupId
                + ", partition=" + partition
                + ")";
        }
        
        public int topicGroupId() {
            return this.topicGroupId;
        }
        
        public int partition() {
            return this.partition;
        }
        
        @Override
        public List<RawTaggedField> unknownTaggedFields() {
            if (_unknownTaggedFields == null) {
                _unknownTaggedFields = new ArrayList<>(0);
            }
            return _unknownTaggedFields;
        }
        
        public TaskId setTopicGroupId(int v) {
            this.topicGroupId = v;
            return this;
        }
        
        public TaskId setPartition(int v) {
            this.partition = v;
            return this;
        }
    }
    
    public static class TaskOffsetSum implements Message {
        int topicGroupId;
        List<PartitionToOffsetSum> partitionToOffsetSum;
        private List<RawTaggedField> _unknownTaggedFields;
        
        public static final Schema SCHEMA_7 =
            new Schema(
                new Field("topic_group_id", Type.INT32, ""),
                new Field("partition_to_offset_sum", new ArrayOf(PartitionToOffsetSum.SCHEMA_7), "")
            );
        
        public static final Schema SCHEMA_8 = SCHEMA_7;
        
        public static final Schema[] SCHEMAS = new Schema[] {
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            SCHEMA_7,
            SCHEMA_8
        };
        
        public static final short LOWEST_SUPPORTED_VERSION = 7;
        public static final short HIGHEST_SUPPORTED_VERSION = 8;
        
        public TaskOffsetSum(Readable _readable, short _version) {
            read(_readable, _version);
        }
        
        public TaskOffsetSum(Struct _struct, short _version) {
            fromStruct(_struct, _version);
        }
        
        public TaskOffsetSum() {
            this.topicGroupId = 0;
            this.partitionToOffsetSum = new ArrayList<PartitionToOffsetSum>(0);
        }
        
        
        @Override
        public short lowestSupportedVersion() {
            return 7;
        }
        
        @Override
        public short highestSupportedVersion() {
            return 32767;
        }
        
        @Override
        public void read(Readable _readable, short _version) {
            this.topicGroupId = _readable.readInt();
            {
                int arrayLength;
                arrayLength = _readable.readInt();
                if (arrayLength < 0) {
                    throw new RuntimeException("non-nullable field partitionToOffsetSum was serialized as null");
                } else {
                    ArrayList<PartitionToOffsetSum> newCollection = new ArrayList<PartitionToOffsetSum>(arrayLength);
                    for (int i = 0; i < arrayLength; i++) {
                        newCollection.add(new PartitionToOffsetSum(_readable, _version));
                    }
                    this.partitionToOffsetSum = newCollection;
                }
            }
            this._unknownTaggedFields = null;
        }
        
        @Override
        public void write(Writable _writable, ObjectSerializationCache _cache, short _version) {
            int _numTaggedFields = 0;
            _writable.writeInt(topicGroupId);
            _writable.writeInt(partitionToOffsetSum.size());
            for (PartitionToOffsetSum partitionToOffsetSumElement : partitionToOffsetSum) {
                partitionToOffsetSumElement.write(_writable, _cache, _version);
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
            this.topicGroupId = struct.getInt("topic_group_id");
            {
                Object[] _nestedObjects = struct.getArray("partition_to_offset_sum");
                this.partitionToOffsetSum = new ArrayList<PartitionToOffsetSum>(_nestedObjects.length);
                for (Object nestedObject : _nestedObjects) {
                    this.partitionToOffsetSum.add(new PartitionToOffsetSum((Struct) nestedObject, _version));
                }
            }
        }
        
        @Override
        public Struct toStruct(short _version) {
            TreeMap<Integer, Object> _taggedFields = null;
            Struct struct = new Struct(SCHEMAS[_version]);
            struct.set("topic_group_id", this.topicGroupId);
            {
                Struct[] _nestedObjects = new Struct[partitionToOffsetSum.size()];
                int i = 0;
                for (PartitionToOffsetSum element : this.partitionToOffsetSum) {
                    _nestedObjects[i++] = element.toStruct(_version);
                }
                struct.set("partition_to_offset_sum", (Object[]) _nestedObjects);
            }
            return struct;
        }
        
        @Override
        public int size(ObjectSerializationCache _cache, short _version) {
            int _size = 0, _numTaggedFields = 0;
            _size += 4;
            {
                int _arraySize = 0;
                _arraySize += 4;
                for (PartitionToOffsetSum partitionToOffsetSumElement : partitionToOffsetSum) {
                    _arraySize += partitionToOffsetSumElement.size(_cache, _version);
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
            if (!(obj instanceof TaskOffsetSum)) return false;
            TaskOffsetSum other = (TaskOffsetSum) obj;
            if (topicGroupId != other.topicGroupId) return false;
            if (this.partitionToOffsetSum == null) {
                if (other.partitionToOffsetSum != null) return false;
            } else {
                if (!this.partitionToOffsetSum.equals(other.partitionToOffsetSum)) return false;
            }
            return MessageUtil.compareRawTaggedFields(_unknownTaggedFields, other._unknownTaggedFields);
        }
        
        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode = 31 * hashCode + topicGroupId;
            hashCode = 31 * hashCode + (partitionToOffsetSum == null ? 0 : partitionToOffsetSum.hashCode());
            return hashCode;
        }
        
        @Override
        public TaskOffsetSum duplicate() {
            TaskOffsetSum _duplicate = new TaskOffsetSum();
            _duplicate.topicGroupId = topicGroupId;
            ArrayList<PartitionToOffsetSum> newPartitionToOffsetSum = new ArrayList<PartitionToOffsetSum>(partitionToOffsetSum.size());
            for (PartitionToOffsetSum _element : partitionToOffsetSum) {
                newPartitionToOffsetSum.add(_element.duplicate());
            }
            _duplicate.partitionToOffsetSum = newPartitionToOffsetSum;
            return _duplicate;
        }
        
        @Override
        public String toString() {
            return "TaskOffsetSum("
                + "topicGroupId=" + topicGroupId
                + ", partitionToOffsetSum=" + MessageUtil.deepToString(partitionToOffsetSum.iterator())
                + ")";
        }
        
        public int topicGroupId() {
            return this.topicGroupId;
        }
        
        public List<PartitionToOffsetSum> partitionToOffsetSum() {
            return this.partitionToOffsetSum;
        }
        
        @Override
        public List<RawTaggedField> unknownTaggedFields() {
            if (_unknownTaggedFields == null) {
                _unknownTaggedFields = new ArrayList<>(0);
            }
            return _unknownTaggedFields;
        }
        
        public TaskOffsetSum setTopicGroupId(int v) {
            this.topicGroupId = v;
            return this;
        }
        
        public TaskOffsetSum setPartitionToOffsetSum(List<PartitionToOffsetSum> v) {
            this.partitionToOffsetSum = v;
            return this;
        }
    }
}
