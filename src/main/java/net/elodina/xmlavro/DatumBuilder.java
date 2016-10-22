package net.elodina.xmlavro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.bind.DatatypeConverter;
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
    private static final List<Schema.Type> PRIMITIVES;
    private static TimeZone defaultTimeZone = TimeZone.getTimeZone("UTC-0");

    static {
        PRIMITIVES = Collections.unmodifiableList(Arrays.asList(Schema.Type.STRING, Schema.Type.INT, Schema.Type.LONG,
                Schema.Type.FLOAT, Schema.Type.DOUBLE, Schema.Type.BOOLEAN, Schema.Type.NULL));
    }

    private Schema schema;
    private boolean caseSensitiveNames = true;
    private String split;

    public DatumBuilder(Schema schema) {
        this.schema = schema;
        split = "";
    }

    public DatumBuilder(Schema schema, String split) {
        this.schema = schema;
        if (split == null)
            this.split = "";
        else
            this.split = split;
    }

    public static Element parse(InputSource source) {
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            builderFactory.setNamespaceAware(true);

            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document doc = builder.parse(source);
            return doc.getDocumentElement();
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new ConverterException(e);
        }
    }

    public static TimeZone getDefaultTimeZone() {
        return defaultTimeZone;
    }

    public static void setDefaultTimeZone(TimeZone timeZone) {
        defaultTimeZone = timeZone;
    }

    private static long parseDateTime(String text) {
        Calendar c = DatatypeConverter.parseDateTime(text);
        c.setTimeZone(defaultTimeZone);
        return c.getTimeInMillis();
    }

    public boolean isCaseSensitiveNames() {
        return caseSensitiveNames;
    }

    public void setCaseSensitiveNames(boolean caseSensitiveNames) {
        this.caseSensitiveNames = caseSensitiveNames;
    }

    public <T> T createDatum(String xml) {
        return createDatum(new StringReader(xml));
    }

    public <T> T createDatum(File file) {
        try (InputStream stream = new FileInputStream(file)) {
            return createDatum(stream);
        } catch (IOException e) {
            throw new ConverterException(e);
        }
    }

    public <T> T createDatum(Reader reader) {
        return createDatum(new InputSource(reader));
    }

    public <T> T createDatum(InputStream stream) {
        return createDatum(new InputSource(stream));
    }

    public <T> T createDatum(InputSource source) {
        return createDatum(parse(source));
    }

    @SuppressWarnings("unchecked")
    public <T> T createDatum(Element ele) {
        ArrayList<Node> eleList = new ArrayList<Node>();
        if (!split.equals(""))
            eleList = searchElement(ele, split);
        else
            eleList.add(ele);
        List<Object> datums = new ArrayList<Object>();
        for (int i = 0; i < eleList.size(); i++) {
            datums.add(createNodeDatum(schema, eleList.get(i), false));
        }
        return (T) datums;
    }

    private ArrayList<Node> searchElement(Node ele, String split) {
        String name = ele.getLocalName();
        if (name.equals(split)) {
            ArrayList<Node> eleList = new ArrayList<Node>();
            eleList.add(ele);
            return eleList;
        } else {
            NodeList elements = ((Element) ele).getElementsByTagName(split);
            if (elements.getLength() == 0) {
                elements = ele.getChildNodes();
                for (int i = 0; i < elements.getLength(); i++) {
                    ArrayList<Node> nodes = searchElement(elements.item(i), split);
                    if (nodes != null)
                        return nodes;
                }
                return null;
            } else {
                ArrayList<Node> eleList = new ArrayList<Node>();
                for (int i = 0; i < elements.getLength(); i++) {
                    eleList.add(elements.item(i));
                }
                return eleList;
            }
        }
    }

    private Object createNodeDatum(Schema schema, Node source, boolean setRecordFromNode) {
        if (!Arrays.asList(Node.ELEMENT_NODE, Node.ATTRIBUTE_NODE, Node.TEXT_NODE).contains(source.getNodeType()))
            throw new IllegalArgumentException("Unsupported node type " + source.getNodeType());

        if (PRIMITIVES.contains(schema.getType()))
            return createValue(schema.getType(), source.getTextContent());

        if (schema.getType() == Schema.Type.UNION)
            return createUnionDatum(schema, source);

        if (schema.getType() == Schema.Type.RECORD)
            return createRecord(schema, (Element) source, setRecordFromNode);

        if (schema.getType() == Schema.Type.ARRAY)
            return createArray(schema, (Element) source);

        throw new ConverterException("Unsupported schema type " + schema.getType());
    }

    @SuppressWarnings("unchecked")
    private Object createArray(Schema schema, Element el) {
        NodeList childNodes = el.getChildNodes();
        Schema elementType = schema.getElementType();
        int numElements = childNodes.getLength();
        @SuppressWarnings("rawtypes")
        GenericData.Array array = new GenericData.Array(numElements, schema);

        for (int i = 0; i < numElements; i++) {
            Node childNode = childNodes.item(i);
            if (childNode.getNodeType() != Node.ELEMENT_NODE)
                continue;
            Element child = (Element) childNodes.item(i);
            // noinspection unchecked
            array.add(createNodeDatum(elementType, child, true));
        }
        return array;
    }

    private Object createUnionDatum(Schema union, Node source) {
        List<Schema> types = union.getTypes();

        boolean optionalNode = types.size() == 2 && types.get(0).getType() == Schema.Type.NULL;
        if (!optionalNode)
            throw new ConverterException("Unsupported union types " + types);

        return createNodeDatum(types.get(1), source, false);
    }

    private Object createValue(Schema.Type type, String text) {
        if (type == Schema.Type.BOOLEAN)
            return "true".equals(text) || "1".equals(text);

        if (type == Schema.Type.INT)
            return Integer.parseInt(text);

        if (type == Schema.Type.LONG)
            return text.contains("T") ? parseDateTime(text) : Long.parseLong(text);

        if (type == Schema.Type.FLOAT)
            return Float.parseFloat(text);

        if (type == Schema.Type.DOUBLE)
            return Double.parseDouble(text);

        if (type == Schema.Type.STRING)
            return text;

        throw new ConverterException("Unsupported type " + type);
    }

    private GenericData.Record createRecord(Schema schema, Element el, boolean setRecordFieldFromNode) {
        GenericData.Record record = new GenericData.Record(schema);

        // initialize arrays and wildcard maps
        for (Schema.Field field : record.getSchema().getFields()) {
            if (field.schema().getType() == Schema.Type.ARRAY)
                record.put(field.name(), new ArrayList<>());

            if (field.name().equals(Source.WILDCARD))
                record.put(field.name(), new HashMap<String, Object>());
        }

        boolean rootRecord = Source.DOCUMENT.equals(schema.getProp(Source.SOURCE));

        if (setRecordFieldFromNode) {
            setFieldFromNode(schema, record, el);
        } else {
            NodeList nodes = rootRecord ? el.getOwnerDocument().getChildNodes() : el.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                setFieldFromNode(schema, record, nodes.item(i));
            }
        }

        if (!rootRecord) {
            NamedNodeMap attrMap = el.getAttributes();
            for (int i = 0; i < attrMap.getLength(); i++) {
                Attr attr = (Attr) attrMap.item(i);

                List<String> ignoredNamespaces = Arrays.asList("http://www.w3.org/2000/xmlns/",
                        "http://www.w3.org/2001/XMLSchema-instance");
                if (ignoredNamespaces.contains(attr.getNamespaceURI()))
                    continue;

                List<String> ignoredNames = Arrays.asList("xml:lang");
                if (ignoredNames.contains(attr.getName()))
                    continue;

                if (!setRecordFieldFromNode) {
                    Schema.Field field = getFieldBySource(schema, new Source(attr.getName(), true));
                    if (field == null) {
                        // Handle wildcard attributes
                        Schema.Field anyField = schema.getField(Source.WILDCARD);
                        if (anyField == null)
                            throw new ConverterException("Could not find attribute " + attr.getName() + " in Avro Schema " + schema.getName()
                                    + " , neither as specific attribute nor 'any' attribute");

                        @SuppressWarnings("unchecked")
                        Map<String, String> map = (HashMap<String, String>) record.get(Source.WILDCARD);
                        map.put(attr.getName(), attr.getValue());
//                        throw new ConverterException("Unsupported attribute " + attr.getName());
                    } else {
                        Object datum = createNodeDatum(field.schema(), attr, false);
                        record.put(field.name(), datum);
                    }
                }
            }
            // Royce - Added for element value (when attributes are available)
            String eleValue = el.getTextContent();
            if (eleValue != null && !eleValue.equals("")) {
                Schema.Field field = getFieldBySource(schema, new Source("text_value", false));
                if (field != null) {
                    Node attr = el.getFirstChild();
                    Object datum = createNodeDatum(field.schema(), attr, false);
                    record.put(field.name(), datum);
                }
            }
        }

        return record;
    }

    private void setFieldFromNode(Schema schema, GenericData.Record record, Node node) {
        if (node.getNodeType() != Node.ELEMENT_NODE)
            return;

        Element child = (Element) node;
        boolean setRecordFromNode = false;
        final String fieldName = child.getLocalName();

        Schema.Field field = getFieldBySource(schema, new Source(fieldName, false));
        if (field == null) {
            field = getNestedFieldBySource(schema, new Source(fieldName, false));
            setRecordFromNode = true;
        }

        if (field != null) {
            boolean array = field.schema().getType() == Schema.Type.ARRAY;
            Object datum = createNodeDatum(!array ? field.schema() : field.schema().getElementType(), child,
                    setRecordFromNode);

            if (!array)
                record.put(field.name(), datum);
            else {
                @SuppressWarnings("unchecked")
                List<Object> values = (List<Object>) record.get(field.name());
                values.add(datum);
            }
        } else {
            Schema.Field anyField = schema.getField(Source.WILDCARD);
            if (anyField == null)
                throw new ConverterException("Could not find field " + fieldName + " in Avro Schema " + schema.getName()
                        + " , neither as specific field nor 'any' element");

            @SuppressWarnings("unchecked")
            Map<String, String> map = (HashMap<String, String>) record.get(Source.WILDCARD);
            map.put(fieldName, getContentAsText(child));
        }
    }

    Schema.Field getFieldBySource(Schema schema, Source source) {
        if (schema.getType() == Schema.Type.UNION) {
            return getFieldBySource(schema.getTypes().get(1), source);
        } else {
            for (Schema.Field field : schema.getFields()) {
                String fieldSource = field.getProp(Source.SOURCE);
                if (caseSensitiveNames && source.toString().equals(fieldSource))
                    return field;
                if (!caseSensitiveNames && source.toString().equalsIgnoreCase(fieldSource))
                    return field;
            }

            return null;
        }
    }

    @SuppressWarnings("incomplete-switch")
    Schema.Field getNestedFieldBySource(Schema schema, Source source) {
        if (schema.getType() != Schema.Type.RECORD) {
            return null;
        }

        for (Schema.Field field : schema.getFields()) {
            Schema topSchema = field.schema();
            switch (topSchema.getType()) {
                case ARRAY: {
                    if (!PRIMITIVES.contains(topSchema.getElementType().getType())) {
                        Schema.Field fieldBySource = getFieldBySource(topSchema.getElementType(), source);
                        if (fieldBySource != null) {
                            return field;
                        }
                    }
                }
                break;
            }
        }

        return null;
    }

    private String getContentAsText(Element el) {
        if (el.getTextContent().length() == 0)
            return "";

        StringWriter writer = new StringWriter();
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(el), new StreamResult(writer));
        } catch (TransformerException impossible) {
            throw new ConverterException(impossible);
        }

        String result = "" + writer.getBuffer();

        // trim element's start tag
        int startTag = result.indexOf(el.getLocalName());
        startTag = result.indexOf('>', startTag);
        result = result.substring(startTag + 1);

        // trim element's end tag
        int endTag = result.lastIndexOf("</");
        result = result.substring(0, endTag);

        return result;
    }
}
