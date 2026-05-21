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
import com.viaversion.viaversion.api.data.Mappings;
import com.viaversion.viaversion.api.protocol.Protocol;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class CustomRegistryStorage implements StorableObject {

    public static final String BLOCK_ENTITY_TYPE = "minecraft:block_entity_type";
    public static final String BLOCK_STATE = "minecraft:block_state";

    private final Map<StageRegistryKey, RegistryOverlay> overlays = new HashMap<>();

    public void clear() {
        overlays.clear();
    }

    public void put(final @Nullable Class<? extends Protocol> stage, final String registryKey, final int sourceId, final int targetId, final @Nullable String identifier) {
        overlays.computeIfAbsent(new StageRegistryKey(stageKey(stage), registryKey), key -> new RegistryOverlay()).put(sourceId, targetId, identifier);
    }

    public void putSourceRange(final @Nullable Class<? extends Protocol> stage, final String registryKey, final int minSourceId, final int maxSourceId) {
        overlays.computeIfAbsent(new StageRegistryKey(stageKey(stage), registryKey), key -> new RegistryOverlay()).putSourceRange(minSourceId, maxSourceId);
    }

    public static int mappedBlockStateId(final UserConnection connection, final Protocol stage, final MappingData mappingData, final int sourceId) {
        return mappedBlockStateId(connection, stage != null ? stage.getClass() : null, mappingData, sourceId);
    }

    public static int mappedBlockStateId(final UserConnection connection, final @Nullable Class<? extends Protocol> stage, final MappingData mappingData, final int sourceId) {
        final CustomRegistryStorage storage = connection.get(CustomRegistryStorage.class);
        final int overlayId = storage != null ? storage.mappedId(stage, BLOCK_STATE, sourceId) : -1;
        if (overlayId == -1 && storage != null && storage.inSourceRange(BLOCK_STATE, sourceId)) {
            throw new IllegalStateException("Unknown custom source id " + sourceId + " for registry " + BLOCK_STATE + " at stage " + stageKey(stage) + " while rewriting block state raw id");
        }
        return overlayId != -1 ? overlayId : mappingData.getNewBlockStateId(sourceId);
    }

    public static int mappedBlockEntityTypeId(final UserConnection connection, final Protocol stage, final @Nullable Mappings mappings, final int sourceId) {
        return mappedBlockEntityTypeId(connection, stage != null ? stage.getClass() : null, mappings, sourceId);
    }

    public static int mappedBlockEntityTypeId(final UserConnection connection, final @Nullable Class<? extends Protocol> stage, final @Nullable Mappings mappings, final int sourceId) {
        return mappedBlockEntityTypeId(connection, stage, mappings != null ? (IntUnaryOperator) mappings::getNewId : null, sourceId);
    }

    public static int mappedBlockEntityTypeId(final UserConnection connection, final Protocol stage, final @Nullable IntUnaryOperator fallbackMapping, final int sourceId) {
        return mappedBlockEntityTypeId(connection, stage != null ? stage.getClass() : null, fallbackMapping, sourceId);
    }

    public static int mappedBlockEntityTypeId(final UserConnection connection, final @Nullable Class<? extends Protocol> stage, final @Nullable IntUnaryOperator fallbackMapping, final int sourceId) {
        final CustomRegistryStorage storage = connection.get(CustomRegistryStorage.class);
        final int overlayId = storage != null ? storage.mappedId(stage, BLOCK_ENTITY_TYPE, sourceId) : -1;
        if (overlayId == -1 && storage != null && storage.inSourceRange(BLOCK_ENTITY_TYPE, sourceId)) {
            throw new IllegalStateException("Unknown custom source id " + sourceId + " for registry " + BLOCK_ENTITY_TYPE + " at stage " + stageKey(stage) + " while rewriting block entity raw type id");
        }
        return overlayId != -1 ? overlayId : fallbackMapping != null ? fallbackMapping.applyAsInt(sourceId) : sourceId;
    }

    public static @Nullable String mappedBlockEntityTypeIdentifier(final UserConnection connection, final Protocol stage, final int sourceId) {
        return mappedBlockEntityTypeIdentifier(connection, stage != null ? stage.getClass() : null, sourceId);
    }

    public static @Nullable String mappedBlockEntityTypeIdentifier(final UserConnection connection, final @Nullable Class<? extends Protocol> stage, final int sourceId) {
        final CustomRegistryStorage storage = connection.get(CustomRegistryStorage.class);
        final String identifier = storage != null ? storage.mappedIdentifier(stage, BLOCK_ENTITY_TYPE, sourceId) : null;
        if (identifier == null && storage != null && storage.inSourceRange(BLOCK_ENTITY_TYPE, sourceId)) {
            throw new IllegalStateException("Unknown custom source id " + sourceId + " for registry " + BLOCK_ENTITY_TYPE + " at stage " + stageKey(stage) + " while resolving block entity identifier");
        }
        return identifier;
    }

    public int mappedId(final @Nullable Class<? extends Protocol> stage, final String registryKey, final int sourceId) {
        final RegistryOverlay overlay = overlays.get(new StageRegistryKey(stageKey(stage), registryKey));
        return overlay != null ? overlay.mappedId(sourceId) : -1;
    }

    public @Nullable String mappedIdentifier(final @Nullable Class<? extends Protocol> stage, final String registryKey, final int sourceId) {
        final RegistryOverlay overlay = overlays.get(new StageRegistryKey(stageKey(stage), registryKey));
        return overlay != null ? overlay.mappedIdentifier(sourceId) : null;
    }

    public boolean hasRegistry(final String registryKey) {
        for (Map.Entry<StageRegistryKey, RegistryOverlay> entry : overlays.entrySet()) {
            if (entry.getKey().registryKey.equals(registryKey) && !entry.getValue().mappings.isEmpty()) return true;
        }
        return false;
    }

    public int size(final String registryKey) {
        int size = 0;
        for (Map.Entry<StageRegistryKey, RegistryOverlay> entry : overlays.entrySet()) {
            if (entry.getKey().registryKey.equals(registryKey)) size += entry.getValue().mappings.size();
        }
        return size;
    }

    public int maxTargetId(final String registryKey) {
        int max = -1;
        for (Map.Entry<StageRegistryKey, RegistryOverlay> entry : overlays.entrySet()) {
            if (entry.getKey().registryKey.equals(registryKey)) max = Math.max(max, entry.getValue().maxTargetId);
        }
        return max;
    }

    public int maxTargetId(final @Nullable Class<? extends Protocol> stage, final String registryKey) {
        final RegistryOverlay overlay = overlays.get(new StageRegistryKey(stageKey(stage), registryKey));
        return overlay != null ? overlay.maxTargetId : -1;
    }

    public int maxSourceId(final String registryKey) {
        int max = -1;
        for (Map.Entry<StageRegistryKey, RegistryOverlay> entry : overlays.entrySet()) {
            if (entry.getKey().registryKey.equals(registryKey)) max = Math.max(max, entry.getValue().maxSourceId);
        }
        return max;
    }

    public int maxSourceId(final @Nullable Class<? extends Protocol> stage, final String registryKey) {
        final RegistryOverlay overlay = overlays.get(new StageRegistryKey(stageKey(stage), registryKey));
        return overlay != null ? Math.max(overlay.maxAllocatedSourceId, overlay.maxSourceId) : -1;
    }

    public boolean inSourceRange(final String registryKey, final int sourceId) {
        for (Map.Entry<StageRegistryKey, RegistryOverlay> entry : overlays.entrySet()) {
            if (entry.getKey().registryKey.equals(registryKey) && entry.getValue().inSourceRange(sourceId)) return true;
        }
        return false;
    }

    private static String stageKey(final @Nullable Class<? extends Protocol> stage) {
        return stage != null ? stage.getName() : "";
    }

    private static final class RegistryOverlay {
        private final Map<Integer, Entry> mappings = new HashMap<>();
        private int minSourceId = Integer.MAX_VALUE;
        private int maxSourceId = -1;
        private int minAllocatedSourceId = Integer.MAX_VALUE;
        private int maxAllocatedSourceId = -1;
        private int maxTargetId = -1;

        private void putSourceRange(final int minSourceId, final int maxSourceId) {
            if (minSourceId < 0 || maxSourceId < minSourceId) {
                throw new IllegalArgumentException("Invalid custom source id range " + minSourceId + ".." + maxSourceId);
            }
            if (minSourceId < minAllocatedSourceId) {
                minAllocatedSourceId = minSourceId;
            }
            if (maxSourceId > maxAllocatedSourceId) {
                maxAllocatedSourceId = maxSourceId;
            }
        }

        private void put(final int sourceId, final int targetId, final @Nullable String identifier) {
            mappings.put(sourceId, new Entry(targetId, identifier));
            if (sourceId > maxSourceId) {
                maxSourceId = sourceId;
            }
            if (sourceId < minSourceId) {
                minSourceId = sourceId;
            }
            if (targetId > maxTargetId) {
                maxTargetId = targetId;
            }
        }

        private int mappedId(final int sourceId) {
            final Entry entry = mappings.get(sourceId);
            return entry != null ? entry.targetId : -1;
        }

        private @Nullable String mappedIdentifier(final int sourceId) {
            final Entry entry = mappings.get(sourceId);
            return entry != null ? entry.identifier : null;
        }

        private boolean inSourceRange(final int sourceId) {
            return sourceId >= minAllocatedSourceId && sourceId <= maxAllocatedSourceId || sourceId >= minSourceId && sourceId <= maxSourceId;
        }
    }

    private record Entry(int targetId, @Nullable String identifier) {
    }

    private record StageRegistryKey(String stageKey, String registryKey) {
    }
}
