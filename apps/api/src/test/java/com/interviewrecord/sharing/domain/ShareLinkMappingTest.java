package com.interviewrecord.sharing.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.persistence.Column;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

class ShareLinkMappingTest {
    @Test
    void tokenHashMappingMatchesFixedLengthBinaryMigration() throws Exception {
        Field field = ShareLink.class.getDeclaredField("tokenHash");
        Column column = field.getAnnotation(Column.class);

        assertEquals("BINARY(32)", column.columnDefinition());
    }
}
