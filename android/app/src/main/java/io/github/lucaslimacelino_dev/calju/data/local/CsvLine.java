package io.github.lucaslimacelino_dev.calju.data.local;

import java.util.ArrayList;
import java.util.List;

/** Divide uma linha de CSV em campos (aspas protegem vírgulas; "" vira "). */
public final class CsvLine {

    private CsvLine() {
    }

    public static List<String> parse(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (quoted) {
                if (ch == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
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
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(ch);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Aspas não fechadas: " + line);
        }
        fields.add(field.toString());
        return fields;
    }
}
