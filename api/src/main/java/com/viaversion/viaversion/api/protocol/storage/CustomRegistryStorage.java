/*
 * This file is part of ViaVersion - https://github.com/ViaVersion/ViaVersion
 * Copyright (C) 2016-2026 ViaVersion and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.viaversion.viaversion.api.protocol.storage;

import com.viaversion.viaversion.api.connection.StorableObject;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.data.MappingData;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CustomRegistryStorage implements StorableObject {

    public static final String BLOCK_ENTITY_TYPE = "minecraft:block_entity_type";
    public static final String BLOCK_STATE = "minecraft:block_state";

    private final Map<String, RegistryOverlay> overlays = new HashMap<>();

    public void put(final String registryKey, final int sourceId, final int targetId, final @Nullable String identifier) {
        overlays.computeIfAbsent(registryKey, key -> new RegistryOverlay()).put(sourceId, targetId, identifier);
    }

    public static int mappedBlockStateId(final UserConnection connection, final MappingData mappingData, final int sourceId) {
        final CustomRegistryStorage storage = connection.get(CustomRegistryStorage.class);
        final int overlayId = storage != null ? storage.mappedId(BLOCK_STATE, sourceId) : -1;
        return overlayId != -1 ? overlayId : mappingData.getNewBlockStateId(sourceId);
    }

    public int mappedId(final String registryKey, final int sourceId) {
        final RegistryOverlay overlay = overlays.get(registryKey);
        return overlay != null ? overlay.mappedId(sourceId) : -1;
    }

    public boolean hasRegistry(final String registryKey) {
        final RegistryOverlay overlay = overlays.get(registryKey);
        return overlay != null && !overlay.mappings.isEmpty();
    }

    public int size(final String registryKey) {
        final RegistryOverlay overlay = overlays.get(registryKey);
        return overlay != null ? overlay.mappings.size() : 0;
    }

    public int maxTargetId(final String registryKey) {
        final RegistryOverlay overlay = overlays.get(registryKey);
        return overlay != null ? overlay.maxTargetId : -1;
    }

    public int maxSourceId(final String registryKey) {
        final RegistryOverlay overlay = overlays.get(registryKey);
        return overlay != null ? overlay.maxSourceId : -1;
    }

    private static final class RegistryOverlay {
        private final Map<Integer, Entry> mappings = new HashMap<>();
        private int maxSourceId = -1;
        private int maxTargetId = -1;

        private void put(final int sourceId, final int targetId, final @Nullable String identifier) {
            mappings.put(sourceId, new Entry(targetId, identifier));
            if (sourceId > maxSourceId) {
                maxSourceId = sourceId;
            }
            if (targetId > maxTargetId) {
                maxTargetId = targetId;
            }
        }

        private int mappedId(final int sourceId) {
            final Entry entry = mappings.get(sourceId);
            return entry != null ? entry.targetId : -1;
        }
    }

    private record Entry(int targetId, @Nullable String identifier) {
    }
}
