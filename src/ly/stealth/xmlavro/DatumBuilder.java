package ly.stealth.xmlavro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
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
import java.util.*;

public class DatumBuilder {
    public static <T> T createDatum(Schema schema, File file) { return new DatumBuilder(schema, file).createDatum(); }
    public static <T> T createDatum(Schema schema, String xml) { return new DatumBuilder(schema, xml).createDatum(); }
    public static <T> T createDatum(Schema schema, Reader reader) { return new DatumBuilder(schema, reader).createDatum(); }
    public static <T> T createDatum(Schema schema, InputStream stream) { return new DatumBuilder(schema, stream).createDatum(); }

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

    private static final List<Schema.Type> PRIMITIVES;
    static {
        PRIMITIVES = Collections.unmodifiableList(Arrays.asList(
                Schema.Type.STRING, Schema.Type.INT, Schema.Type.LONG, Schema.Type.FLOAT,
                Schema.Type.DOUBLE, Schema.Type.BOOLEAN, Schema.Type.NULL
        ));
    }

    public DatumBuilder(Schema schema, File file) { this(schema, parseElement(file)); }
    public DatumBuilder(Schema schema, String xml) { this(schema, new StringReader(xml)); }
    public DatumBuilder(Schema schema, Reader reader) { this(schema, parseElement(new InputSource(reader))); }
    public DatumBuilder(Schema schema, InputStream stream) { this(schema, parseElement(new InputSource(stream))); }

    private Schema schema;
    private Element el;

    private DatumBuilder(Schema schema, Element el) {
        this.schema = schema;
        this.el = el;
    }

    @SuppressWarnings("unchecked")
    public <T> T createDatum() {
        return (T) createDatum(schema, el);
    }

    private Object createDatum(Schema schema, Element el) {
        @SuppressWarnings("unchecked")
        Object datum = PRIMITIVES.contains(schema.getType()) ?
                createValue(schema.getType(), el.getTextContent()) :
                createRecord(schema, el);

        return datum;
    }

    private Object createValue(Schema.Type type, String text) {
        return parseValue(type, text);
    }

    private GenericData.Record createRecord(Schema schema, Element el) {
        GenericData.Record record = new GenericData.Record(schema);

        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element child = (Element) node;
            Schema.Field field = getFieldBySource(schema, new SchemaBuilder.Source(child.getNodeName(), false));

            if (field != null) {
                Object datum = createDatum(field.schema(), child);
                record.put(field.name(), datum);
            } else {
                Schema.Field anyField = schema.getField(SchemaBuilder.OTHERS);
                if (anyField == null)
                    throw new IllegalStateException("Type doesn't support any element");

                if (record.get(SchemaBuilder.OTHERS) == null)
                    record.put(SchemaBuilder.OTHERS, new HashMap<String, Object>());

                @SuppressWarnings("unchecked")
                Map<String, String> map = (HashMap<String, String>) record.get(SchemaBuilder.OTHERS);
                map.put(child.getNodeName(), getContentAsText(child));
            }
        }

        NamedNodeMap attrMap = el.getAttributes();
        for (int i = 0; i < attrMap.getLength(); i++) {
            Attr attr = (Attr) attrMap.item(i);

            List<String> ignoredNamespaces = Arrays.asList("http://www.w3.org/2000/xmlns/", "http://www.w3.org/2001/XMLSchema-instance");
            if (ignoredNamespaces.contains(attr.getNamespaceURI())) continue;

            Schema.Field field = getFieldBySource(schema, new SchemaBuilder.Source(attr.getName(), true));

            if (field == null)
                throw new IllegalStateException("Unsupported attribute " + attr.getName());

            record.put(field.name(), attr.getValue());
        }

        return record;
    }

    static Schema.Field getFieldBySource(Schema schema, SchemaBuilder.Source source) {
        for (Schema.Field field : schema.getFields())
            if (source.toString().equals(field.getProp(SchemaBuilder.SOURCE)))
                return field;

        return null;
    }

    private  Object parseValue(Schema.Type type, String text) {
        if (type == Schema.Type.BOOLEAN)
            return "true".equals(text) || "1".equals(text);

        if (type == Schema.Type.INT)
            return Integer.parseInt(text);

        if (type == Schema.Type.LONG)
            return Long.parseLong(text);

        if (type == Schema.Type.FLOAT)
            return Float.parseFloat(text);

        if (type == Schema.Type.DOUBLE)
            return Double.parseDouble(text);

        if (type == Schema.Type.STRING)
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
