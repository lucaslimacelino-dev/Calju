package io.github.lucaslimacelino_dev.calju.catalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Lê CSV em UTF-8 com cabeçalho (RFC 4180: aspas protegem vírgulas e aspas duplicadas). */
public final class CsvReader {

    private CsvReader() {
    }

    public static List<Map<String, String>> read(Path file) throws IOException {
        List<List<String>> records = parse(Files.readString(file, StandardCharsets.UTF_8));
        if (records.isEmpty()) {
            return List.of();
        }
        List<String> header = records.get(0);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> record = records.get(i);
            if (record.size() != header.size()) {
                throw new IOException("Linha " + (i + 1) + " de " + file + " tem " + record.size()
                        + " colunas; esperado " + header.size());
            }
            Map<String, String> row = new LinkedHashMap<>();
            for (int c = 0; c < header.size(); c++) {
                row.put(header.get(c), record.get(c));
            }
            rows.add(row);
        }
        return rows;
    }

    static List<List<String>> parse(String content) {
        List<List<String>> records = new ArrayList<>();
        List<String> record = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < content.length() && content.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (ch == '"') {
                    quoted = false;
                } else {
                    field.append(ch);
                }
            } else if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                record.add(field.toString());
                field.setLength(0);
            } else if (ch == '\n') {
                record.add(field.toString());
                field.setLength(0);
                records.add(record);
                record = new ArrayList<>();
            } else if (ch != '\r') {
                field.append(ch);
            }
        }
        if (field.length() > 0 || !record.isEmpty()) {
            record.add(field.toString());
            records.add(record);
        }
        return records;
    }
}
