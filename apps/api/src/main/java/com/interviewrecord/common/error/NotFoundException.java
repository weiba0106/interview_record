package com.interviewrecord.common.error;

/**
 * Thrown when a resource does not exist or belongs to another user. Both cases
 * return the same response so callers cannot probe resource ownership.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException() {
        super("RESOURCE_NOT_FOUND");
    }
}
