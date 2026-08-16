package com.interviewrecord.common.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.interviewrecord.export.domain.ExportFile;
import com.interviewrecord.interviews.domain.InterviewRound;
import com.interviewrecord.tracking.domain.Position;
import java.sql.Types;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.type.BasicType;
import org.junit.jupiter.api.Test;

/**
 * 回归保护：V9/V10 的 LONGTEXT/LONGBLOB 列必须与 Hibernate 对实体字段解析出的
 * JDBC 类型一致（LONGVARCHAR/LONGVARBINARY），否则启动时 MySQL schema 校验会报
 * "wrong column type"。该测试不连接数据库，只构建元数据并断言 JDBC 类型码。
 */
class EntityColumnTypeTest {

    private int jdbcTypeCode(Metadata metadata, Class<?> entity, String property) {
        return ((BasicType<?>) metadata.getEntityBinding(entity.getName())
                .getProperty(property).getType()).getJdbcType().getJdbcTypeCode();
    }

    @Test
    void richTextAndPayloadColumnsResolveToLongTypes() {
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .build();
        try {
            Metadata metadata = new MetadataSources(registry)
                    .addAnnotatedClass(ExportFile.class)
                    .addAnnotatedClass(Position.class)
                    .addAnnotatedClass(InterviewRound.class)
                    .buildMetadata();

            assertThat(jdbcTypeCode(metadata, ExportFile.class, "payload"))
                    .isEqualTo(Types.LONGVARBINARY);
            assertThat(jdbcTypeCode(metadata, Position.class, "description"))
                    .isEqualTo(Types.LONGVARCHAR);
            assertThat(jdbcTypeCode(metadata, InterviewRound.class, "processNotes"))
                    .isEqualTo(Types.LONGVARCHAR);
            assertThat(jdbcTypeCode(metadata, InterviewRound.class, "reviewSummary"))
                    .isEqualTo(Types.LONGVARCHAR);
        } finally {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
