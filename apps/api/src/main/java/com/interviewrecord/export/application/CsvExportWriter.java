package com.interviewrecord.export.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** CSV（UTF-8 BOM）与 ZIP 打包的纯函数工具，供导出服务使用。 */
final class CsvExportWriter {
    private CsvExportWriter() {}

    /** 单个 CSV 的字节内容：以 UTF-8 BOM 开头，保证中文在常见表格软件中正常显示。 */
    static byte[] csv(List<String> header, List<? extends List<?>> rows) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.writeBytes(new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
        try (Writer writer = new OutputStreamWriter(buffer, StandardCharsets.UTF_8)) {
            writeRow(writer, header);
            for (List<?> row : rows) writeRow(writer, row);
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to write CSV export", exception);
        }
        return buffer.toByteArray();
    }

    static byte[] zip(Map<String, byte[]> entries) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to write ZIP export", exception);
        }
        return buffer.toByteArray();
    }

    private static void writeRow(Writer writer, List<?> cells) throws IOException {
        StringBuilder line = new StringBuilder();
        for (int index = 0; index < cells.size(); index++) {
            if (index > 0) line.append(',');
            line.append(cell(cells.get(index)));
        }
        writer.write(line.toString());
        writer.write("\r\n");
    }

    private static String cell(Object value) {
        String text = value == null ? "" : value.toString();
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0 || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    /** 导出时间统一 ISO 8601；空值留空单元格。 */
    static String text(Object value) {
        if (value == null) return "";
        if (value instanceof Instant instant) return instant.toString();
        return value.toString();
    }
}
