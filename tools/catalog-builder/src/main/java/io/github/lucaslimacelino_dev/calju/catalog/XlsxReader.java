package io.github.lucaslimacelino_dev.calju.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Lê uma aba de um arquivo .xlsx sem bibliotecas externas. Um .xlsx é um ZIP com arquivos XML:
 * workbook.xml lista as abas, sharedStrings.xml guarda os textos e cada aba fica em worksheets/.
 */
public final class XlsxReader {

    private static final Pattern CELL_REFERENCE = Pattern.compile("([A-Z]+)(\\d+)");

    private XlsxReader() {
    }

    /** Retorna as linhas da aba como número da linha → (coluna → texto da célula). */
    public static Map<Integer, Map<String, String>> readSheet(Path file, String sheetName) throws IOException {
        try (ZipFile zip = new ZipFile(file.toFile())) {
            List<String> sharedStrings = readSharedStrings(zip);
            String sheetPath = findSheetPath(zip, sheetName);
            return readCells(parse(zip, sheetPath), sharedStrings);
        }
    }

    private static List<String> readSharedStrings(ZipFile zip) throws IOException {
        List<String> strings = new ArrayList<>();
        if (zip.getEntry("xl/sharedStrings.xml") == null) {
            return strings;
        }
        NodeList items = parse(zip, "xl/sharedStrings.xml").getElementsByTagName("si");
        for (int i = 0; i < items.getLength(); i++) {
            strings.add(concatenatedText((Element) items.item(i)));
        }
        return strings;
    }

    private static String findSheetPath(ZipFile zip, String sheetName) throws IOException {
        String relationId = null;
        NodeList sheets = parse(zip, "xl/workbook.xml").getElementsByTagName("sheet");
        for (int i = 0; i < sheets.getLength(); i++) {
            Element sheet = (Element) sheets.item(i);
            if (sheetName.equals(sheet.getAttribute("name"))) {
                relationId = sheet.getAttribute("r:id");
            }
        }
        if (relationId == null) {
            throw new IOException("Aba não encontrada: " + sheetName);
        }
        NodeList relations = parse(zip, "xl/_rels/workbook.xml.rels").getElementsByTagName("Relationship");
        for (int i = 0; i < relations.getLength(); i++) {
            Element relation = (Element) relations.item(i);
            if (relationId.equals(relation.getAttribute("Id"))) {
                String target = relation.getAttribute("Target");
                return target.startsWith("/") ? target.substring(1) : "xl/" + target;
            }
        }
        throw new IOException("Arquivo da aba não encontrado: " + sheetName);
    }

    private static Map<Integer, Map<String, String>> readCells(Document sheet, List<String> sharedStrings) {
        Map<Integer, Map<String, String>> rows = new TreeMap<>();
        NodeList cells = sheet.getElementsByTagName("c");
        for (int i = 0; i < cells.getLength(); i++) {
            Element cell = (Element) cells.item(i);
            Matcher reference = CELL_REFERENCE.matcher(cell.getAttribute("r"));
            if (!reference.matches()) {
                continue;
            }
            String value = cellValue(cell, sharedStrings);
            if (value != null) {
                rows.computeIfAbsent(Integer.parseInt(reference.group(2)), row -> new TreeMap<>())
                        .put(reference.group(1), value);
            }
        }
        return rows;
    }

    private static String cellValue(Element cell, List<String> sharedStrings) {
        String type = cell.getAttribute("t");
        if ("inlineStr".equals(type)) {
            return concatenatedText(cell);
        }
        NodeList values = cell.getElementsByTagName("v");
        if (values.getLength() == 0) {
            return null;
        }
        String raw = values.item(0).getTextContent();
        return "s".equals(type) ? sharedStrings.get(Integer.parseInt(raw)) : raw;
    }

    private static String concatenatedText(Element element) {
        StringBuilder text = new StringBuilder();
        NodeList parts = element.getElementsByTagName("t");
        for (int i = 0; i < parts.getLength(); i++) {
            text.append(parts.item(i).getTextContent());
        }
        return text.toString();
    }

    private static Document parse(ZipFile zip, String entryName) throws IOException {
        ZipEntry entry = zip.getEntry(entryName);
        if (entry == null) {
            throw new IOException("Arquivo ausente no .xlsx: " + entryName);
        }
        try (InputStream input = zip.getInputStream(entry)) {
            return secureBuilder().parse(input);
        } catch (SAXException e) {
            throw new IOException("XML inválido em " + entryName + ": " + e.getMessage(), e);
        }
    }

    /** Bloqueia DOCTYPE e entidades externas (proteção contra XXE). */
    private static DocumentBuilder secureBuilder() throws IOException {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new IOException("Não foi possível configurar o leitor de XML", e);
        }
    }
}
