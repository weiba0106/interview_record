package com.interviewrecord.common.util;

import com.interviewrecord.common.error.NotFoundException;

/**
 * Public identifiers are opaque strings; malformed or unparseable ids are
 * treated the same as unknown resources.
 */
public final class ResourceIds {
    private ResourceIds() {}

    public static Long parse(String id) {
        if (id == null || id.isBlank()) {
            throw new NotFoundException();
        }
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException exception) {
            throw new NotFoundException();
        }
    }
}
