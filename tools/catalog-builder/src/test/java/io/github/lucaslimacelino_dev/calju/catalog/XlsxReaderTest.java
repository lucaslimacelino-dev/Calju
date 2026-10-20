package io.github.lucaslimacelino_dev.calju.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class XlsxReaderTest {

    private static final String WORKBOOK = """
            <workbook xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
              <sheets><sheet name="Outra" sheetId="1" r:id="rId1"/><sheet name="Dados" sheetId="2" r:id="rId2"/></sheets>
            </workbook>""";
    private static final String RELATIONS = """
            <Relationships>
              <Relationship Id="rId1" Target="worksheets/sheet1.xml"/>
              <Relationship Id="rId2" Target="worksheets/sheet2.xml"/>
            </Relationships>""";
    private static final String SHARED_STRINGS = """
            <sst><si><t>Banana</t></si><si><r><t>Arroz, </t></r><r><t>cozido</t></r></si></sst>""";
    private static final String SHEET = """
            <worksheet><sheetData>
              <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1"><v>1.5</v></c></row>
              <row r="3"><c r="AC3" t="s"><v>1</v></c><c r="D3" t="inlineStr"><is><t>Tr</t></is></c><c r="E3"/></row>
            </sheetData></worksheet>""";

    private static Path xlsx(Path dir, String sheetContent) throws IOException {
        Path file = dir.resolve("teste.xlsx");
        try (OutputStream out = Files.newOutputStream(file); ZipOutputStream zip = new ZipOutputStream(out)) {
            add(zip, "xl/workbook.xml", WORKBOOK);
            add(zip, "xl/_rels/workbook.xml.rels", RELATIONS);
            add(zip, "xl/sharedStrings.xml", SHARED_STRINGS);
            add(zip, "xl/worksheets/sheet1.xml", "<worksheet><sheetData/></worksheet>");
            add(zip, "xl/worksheets/sheet2.xml", sheetContent);
        }
        return file;
    }

    private static void add(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    @Test
    void leTextosCompartilhadosNumerosETextoEmbutido(@TempDir Path dir) throws IOException {
        Map<Integer, Map<String, String>> rows = XlsxReader.readSheet(xlsx(dir, SHEET), "Dados");

        assertEquals("Banana", rows.get(1).get("A"));
        assertEquals("1.5", rows.get(1).get("B"));
        assertEquals("Arroz, cozido", rows.get(3).get("AC"));
        assertEquals("Tr", rows.get(3).get("D"));
        assertEquals(null, rows.get(3).get("E"));
    }

    @Test
    void abaInexistenteGeraErro(@TempDir Path dir) throws IOException {
        Path file = xlsx(dir, SHEET);
        assertThrows(IOException.class, () -> XlsxReader.readSheet(file, "Não existe"));
    }

    @Test
    void recusaXmlComDoctype(@TempDir Path dir) throws IOException {
        String malicious = "<!DOCTYPE x [<!ENTITY e SYSTEM \"file:///c:/windows/win.ini\">]>"
                + "<worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>&e;</t></is></c></row>"
                + "</sheetData></worksheet>";
        Path file = xlsx(dir, malicious);

        assertThrows(IOException.class, () -> XlsxReader.readSheet(file, "Dados"));
    }
}
