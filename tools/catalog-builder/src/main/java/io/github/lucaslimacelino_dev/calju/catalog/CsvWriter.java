package io.github.lucaslimacelino_dev.calju.catalog;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Grava CSV em UTF-8 com quebra de linha LF, aspas apenas quando necessário (RFC 4180). */
public final class CsvWriter {

    private CsvWriter() {
    }

    public static void write(Path file, List<String> header, List<List<String>> rows) throws IOException {
        try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writeLine(out, header);
            for (List<String> row : rows) {
                writeLine(out, row);
            }
        }
    }

    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static void writeLine(Writer out, List<String> values) throws IOException {
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.write(',');
            }
            out.write(escape(values.get(i)));
        }
        out.write('\n');
    }
}
