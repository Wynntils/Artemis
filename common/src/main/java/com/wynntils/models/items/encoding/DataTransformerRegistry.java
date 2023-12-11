/*
 * Copyright © Wynntils 2023.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.models.items.encoding;

import com.wynntils.models.items.encoding.data.EndData;
import com.wynntils.models.items.encoding.data.IdentificationData;
import com.wynntils.models.items.encoding.data.NameData;
import com.wynntils.models.items.encoding.data.PowderData;
import com.wynntils.models.items.encoding.data.RerollData;
import com.wynntils.models.items.encoding.data.ShinyData;
import com.wynntils.models.items.encoding.data.StartData;
import com.wynntils.models.items.encoding.data.TypeData;
import com.wynntils.models.items.encoding.impl.block.EndDataTransformer;
import com.wynntils.models.items.encoding.impl.block.IdentificationDataTransformer;
import com.wynntils.models.items.encoding.impl.block.NameDataTransformer;
import com.wynntils.models.items.encoding.impl.block.PowderDataTransformer;
import com.wynntils.models.items.encoding.impl.block.RerollDataTransformer;
import com.wynntils.models.items.encoding.impl.block.ShinyDataTransformer;
import com.wynntils.models.items.encoding.impl.block.StartDataTransformer;
import com.wynntils.models.items.encoding.impl.block.TypeDataTransformer;
import com.wynntils.models.items.encoding.type.DataTransformer;
import com.wynntils.models.items.encoding.type.ItemData;
import com.wynntils.models.items.encoding.type.ItemTransformingVersion;
import com.wynntils.utils.EncodedByteBuffer;
import com.wynntils.utils.type.ErrorOr;
import com.wynntils.utils.type.UnsignedByte;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for registering and storing all data transformers.
 * Data transformers are used for transforming between {@link ItemData} and {@link UnsignedByte} arrays.
 */
public final class DataTransformerRegistry {
    private final DataTransformerMap dataTransformers = new DataTransformerMap();

    public DataTransformerRegistry() {
        registerAllTransformers();
    }

    public ErrorOr<EncodedByteBuffer> encodeData(ItemTransformingVersion version, List<ItemData> data) {
        // FIXME: Explore using some kind of "ByteBuilder" instead of List<UnsignedByte>
        List<UnsignedByte> bytes = new ArrayList<>();

        for (ItemData itemData : data) {
            ErrorOr<UnsignedByte[]> errorOrEncodedData = encodeData(version, itemData);
            if (errorOrEncodedData.hasError()) {
                return ErrorOr.error(errorOrEncodedData.getError());
            }

            bytes.addAll(Arrays.asList(errorOrEncodedData.getValue()));
        }

        return ErrorOr.of(EncodedByteBuffer.fromBytes(bytes.toArray(new UnsignedByte[0])));
    }

    public ErrorOr<List<ItemData>> decodeData(EncodedByteBuffer encodedItem) {
        // FIXME: Read version from start byte
        return decodeData(ItemTransformingVersion.VERSION_1, encodedItem);
    }

    private ErrorOr<UnsignedByte[]> encodeData(ItemTransformingVersion version, ItemData data) {
        DataTransformer<ItemData> dataTransformer = (DataTransformer<ItemData>) dataTransformers.get(data.getClass());
        if (dataTransformer == null) {
            return ErrorOr.error(
                    "No data transformer found for " + data.getClass().getSimpleName());
        }

        return dataTransformer.encode(version, data);
    }

    private ErrorOr<List<ItemData>> decodeData(ItemTransformingVersion version, EncodedByteBuffer encodedItem) {
        return null;
    }

    private <T extends ItemData> void registerDataTransformer(
            Class<T> dataClass, byte id, DataTransformer<T> dataTransformer) {
        dataTransformers.put(dataClass, id, dataTransformer);
    }

    private void registerAllTransformers() {
        registerDataTransformer(StartData.class, StartDataTransformer.ID, new StartDataTransformer());

        // Order is irrelevant here, keep it ordered as the ids are
        registerDataTransformer(TypeData.class, TypeDataTransformer.ID, new TypeDataTransformer());
        registerDataTransformer(NameData.class, NameDataTransformer.ID, new NameDataTransformer());
        registerDataTransformer(
                IdentificationData.class, IdentificationDataTransformer.ID, new IdentificationDataTransformer());
        registerDataTransformer(PowderData.class, PowderDataTransformer.ID, new PowderDataTransformer());
        registerDataTransformer(RerollData.class, RerollDataTransformer.ID, new RerollDataTransformer());
        registerDataTransformer(ShinyData.class, ShinyDataTransformer.ID, new ShinyDataTransformer());

        registerDataTransformer(EndData.class, EndDataTransformer.ID, new EndDataTransformer());
    }

    private static final class DataTransformerMap {
        private final Map<Class<? extends ItemData>, DataTransformer<? extends ItemData>> dataTransformers =
                new HashMap<>();

        private final Map<Byte, DataTransformer<? extends ItemData>> idToTransformerMap = new HashMap<>();

        public void put(
                Class<? extends ItemData> dataClass, byte id, DataTransformer<? extends ItemData> dataTransformer) {
            if (dataTransformers.put(dataClass, dataTransformer) != null) {
                throw new IllegalStateException("Duplicate data class: " + dataClass.getSimpleName());
            }
            if (idToTransformerMap.put(id, dataTransformer) != null) {
                throw new IllegalStateException("Duplicate id: " + id);
            }
        }

        public <T extends ItemData> DataTransformer<T> get(Class<T> dataClass) {
            return (DataTransformer<T>) dataTransformers.get(dataClass);
        }
    }
}
