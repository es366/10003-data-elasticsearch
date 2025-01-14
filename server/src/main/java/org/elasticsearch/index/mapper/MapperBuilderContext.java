/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.index.mapper;

import org.elasticsearch.common.Strings;
import org.elasticsearch.index.mapper.MapperService.MergeReason;

/**
 * Holds context for building Mapper objects from their Builders
 */
public class MapperBuilderContext {

    /**
     * The root context, to be used when building a tree of mappers
     */
    public static MapperBuilderContext root(boolean isSourceSynthetic, boolean isDataStream) {
        return root(isSourceSynthetic, isDataStream, MergeReason.MAPPING_UPDATE);
    }

    public static MapperBuilderContext root(boolean isSourceSynthetic, boolean isDataStream, MergeReason mergeReason) {
        return new MapperBuilderContext(null, isSourceSynthetic, isDataStream, false, mergeReason);
    }

    private final String path;
    private final boolean isSourceSynthetic;
    private final boolean isDataStream;
    private final boolean parentObjectContainsDimensions;
    private final MergeReason mergeReason;

    MapperBuilderContext(String path) {
        this(path, false, false, false, MergeReason.MAPPING_UPDATE);
    }

    MapperBuilderContext(
        String path,
        boolean isSourceSynthetic,
        boolean isDataStream,
        boolean parentObjectContainsDimensions,
        MergeReason mergeReason
    ) {
        this.path = path;
        this.isSourceSynthetic = isSourceSynthetic;
        this.isDataStream = isDataStream;
        this.parentObjectContainsDimensions = parentObjectContainsDimensions;
        this.mergeReason = mergeReason;
    }

    /**
     * Creates a new MapperBuilderContext that is a child of this context
     * @param name the name of the child context
     * @return a new MapperBuilderContext with this context as its parent
     */
    public MapperBuilderContext createChildContext(String name) {
        return new MapperBuilderContext(buildFullName(name), isSourceSynthetic, isDataStream, parentObjectContainsDimensions, mergeReason);
    }

    /**
     * Builds the full name of the field, taking into account parent objects
     */
    public String buildFullName(String name) {
        if (Strings.isEmpty(path)) {
            return name;
        }
        return path + "." + name;
    }

    /**
     * Is the {@code _source} field being reconstructed on the fly?
     */
    public boolean isSourceSynthetic() {
        return isSourceSynthetic;
    }

    /**
     * Are these mappings being built for a data stream index?
     */
    public boolean isDataStream() {
        return isDataStream;
    }

    /**
     * Are these field mappings being built dimensions?
     */
    public boolean parentObjectContainsDimensions() {
        return parentObjectContainsDimensions;
    }

    /**
     * The merge reason to use when merging mappers while building the mapper.
     * See also {@link ObjectMapper.Builder#buildMappers(MapperBuilderContext)}.
     */
    public MergeReason getMergeReason() {
        return mergeReason;
    }
}
