package com.laulem.vectopath.knowledge.api.shared.util;

import jakarta.validation.Valid;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }

    public static <T> List<T> emptyIfNull(final @Valid List<T> groupPermissions) {
        return groupPermissions == null ? Collections.emptyList() : groupPermissions;
    }
}
