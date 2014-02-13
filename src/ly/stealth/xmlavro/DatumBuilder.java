package ly.stealth.xmlavro;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class DatumBuilder {
    private Datum.Type type;
    private Element el;

    private static Element parseElement(File file) {
        try (InputStream stream = new FileInputStream(file)) {
            return parseElement(new InputSource(stream));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Element parseElement(InputSource source) {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);

            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document doc = builder.parse(source);
            return doc.getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public DatumBuilder(Datum.Type type, File file) { this(type, parseElement(file)); }
    public DatumBuilder(Datum.Type type, String xml) { this(type, new StringReader(xml)); }
    public DatumBuilder(Datum.Type type, Reader reader) { this(type, parseElement(new InputSource(reader))); }
    public DatumBuilder(Datum.Type type, InputStream stream) { this(type, parseElement(new InputSource(stream))); }

    private DatumBuilder(Datum.Type type, Element el) {
        this.type = type;
        this.el = el;
    }

    public <D extends Datum> D createDatum() {
        return createDatum(type, el);
    }

    private <D extends Datum> D createDatum(Datum.Type type, Element el) {
        @SuppressWarnings("unchecked")
        D datum = (D) (type.isPrimitive() ? createValue((Value.Type) type, el.getTextContent()) : createRecord((Record.Type) type, el));
        return datum;
    }

    private Value createValue(Value.Type type, String text) {
        return new Value(type, parseValue(type, text));
    }

    private Record createRecord(Record.Type type, Element el) {
        Record record = new Record(type);

        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element child = (Element) node;
            QName qName = new QName(child.getTagName(), child.getNamespaceURI());
            Record.Field field = type.getField(qName);

            if (field != null) {
                Datum datum = createDatum(field.getType(), child);
                record.setDatum(field, datum);
            } else {
                if (!type.supportsAnyElement())
                    throw new IllegalStateException("Type doesn't support any element");

                Record.Field anyField = type.getAnyElementField();
                if (record.getDatum(anyField) == null)
                    record.setDatum(anyField, new Map(new Map.Type(Value.Type.STRING)));

                Map map = record.getDatum(anyField);
                QName key = new QName(child.getTagName(), child.getNamespaceURI());
                map.setValue("" + key, new Value(Value.Type.STRING, getContentAsText(child)));
            }
        }

        NamedNodeMap attrMap = el.getAttributes();
        for (int i = 0; i < attrMap.getLength(); i++) {
            Attr attr = (Attr) attrMap.item(i);

            List<String> ignoredNamespaces = Arrays.asList("http://www.w3.org/2000/xmlns/", "http://www.w3.org/2001/XMLSchema-instance");
            if (ignoredNamespaces.contains(attr.getNamespaceURI())) continue;

            QName qName = new QName(attr.getName(), attr.getNamespaceURI());
            Record.Field field = type.getField(qName, true);

            Value value = createValue((Value.Type) field.getType(), attr.getValue());
            record.setDatum(field, value);
        }

        return record;
    }

    private  Object parseValue(Value.Type type, String text) {
        if (type == Value.Type.BOOLEAN)
            return "true".equals(text) || "1".equals(text);

        if (type == Value.Type.INT)
            return Integer.parseInt(text);

        if (type == Value.Type.LONG)
            return Long.parseLong(text);

        if (type == Value.Type.FLOAT)
            return Float.parseFloat(text);

        if (type == Value.Type.DOUBLE)
            return Double.parseDouble(text);

        if (type == Value.Type.STRING)
            return text;

        throw new UnsupportedOperationException("Unsupported type " + type);
    }

    private String getContentAsText(Element el) {
        if (el.getTextContent().length() == 0) return "";

        StringWriter writer = new StringWriter();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(el), new StreamResult(writer));
        } catch (TransformerException impossible) {
            throw new RuntimeException(impossible);
        }

        String result = "" + writer.getBuffer();

        //trim element's start tag
        int startTag = result.indexOf(el.getNodeName());
        startTag = result.indexOf('>', startTag);
        result = result.substring(startTag + 1);

        //trim element's end tag
        int endTag = result.lastIndexOf("</");
        result = result.substring(0, endTag);

        return result;
    }
}
