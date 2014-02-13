package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import com.sun.org.apache.xerces.internal.xs.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.w3c.dom.*;
import org.w3c.dom.ls.LSInput;
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

public class Converter {
    public static final String SOURCE = "source";
    public static final String OTHERS = "others";

    public static Schema createSchema(String xsd) { return new SchemaBuilder(xsd).createSchema(); }
    public static Schema createSchema(File file) throws IOException { return new SchemaBuilder(file).createSchema(); }
    public static Schema createSchema(Reader reader) { return new SchemaBuilder(reader).createSchema(); }
    public static Schema createSchema(InputStream stream) { return new SchemaBuilder(stream).createSchema(); }

    public static <T> T createDatum(Schema schema, File file) { return new DatumBuilder(schema, file).createDatum(); }
    public static <T> T createDatum(Schema schema, String xml) { return new DatumBuilder(schema, xml).createDatum(); }
    public static <T> T createDatum(Schema schema, Reader reader) { return new DatumBuilder(schema, reader).createDatum(); }
    public static <T> T createDatum(Schema schema, InputStream stream) { return new DatumBuilder(schema, stream).createDatum(); }

    public static class SchemaBuilder {
        private static XSModel parse(File file) throws IOException {
            try (InputStream stream = new FileInputStream(file)) {
                return parse(stream);
            }
        }

        private static XSModel parse(InputStream stream) {
            DOMInputImpl input = new DOMInputImpl();
            input.setByteStream(stream);
            return parse(input);
        }

        private static XSModel parse(Reader reader) {
            DOMInputImpl input = new DOMInputImpl();
            input.setCharacterStream(reader);
            return parse(input);
        }

        private static XSModel parse(LSInput input) {
            ErrorHandler errorHandler = new ErrorHandler();

            XMLSchemaLoader loader = new XMLSchemaLoader();
            loader.setErrorHandler(errorHandler);
            XSModel schema = loader.load(input);

            if (errorHandler.exception != null)
                throw errorHandler.exception;

            return schema;
        }

        private XSModel schema;
        private String rootElementName;

        // Type by QName, where QName:
        // - QName of type for named types
        // - QName of element for anonymous or simple types of root elements
        private Map<String, Schema> types = new LinkedHashMap<>();

        public SchemaBuilder(String xsd) { this(parse(new StringReader(xsd))); }

        public SchemaBuilder(File file) throws IOException { this(parse(file)); }

        public SchemaBuilder(Reader reader) { this(parse(reader)); }
        public SchemaBuilder(InputStream stream) { this(parse(stream)); }

        private SchemaBuilder(XSModel schema) { this.schema = schema; }

        public String getRootElementName() { return rootElementName; }
        public void setRootElementName(String rootElementName) { this.rootElementName = rootElementName; }


        @SuppressWarnings("unchecked")
        public Schema createSchema() {
            XSElementDeclaration el = getRootElement();
            XSTypeDefinition type = el.getTypeDefinition();

            types.clear();
            return createType(type);
        }

        private XSElementDeclaration getRootElement() {
            if (rootElementName != null) {
                XSElementDeclaration el = schema.getElementDeclaration(rootElementName, null);
                if (el == null) throw new IllegalStateException("Root element declaration " + rootElementName + " not found");
                return el;
            }

            XSNamedMap elMap = schema.getComponents(XSConstants.ELEMENT_DECLARATION);
            if (elMap.getLength() == 0) throw new IllegalStateException("No root element declaration");
            if (elMap.getLength() > 1) throw new IllegalStateException("Ambiguous root element declarations");

            return (XSElementDeclaration) elMap.item(0);
        }

        private Schema createType(XSTypeDefinition type) {
            if (type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE)
                return Schema.create(getPrimitiveType((XSSimpleTypeDefinition) type));
            else
                return createRecord((XSComplexTypeDefinition) type);
        }

        private Schema.Type getPrimitiveType(XSSimpleTypeDefinition type) {
            switch (type.getBuiltInKind()) {
                case XSConstants.BOOLEAN_DT: return Schema.Type.BOOLEAN;
                case XSConstants.INT_DT: return Schema.Type.INT;
                case XSConstants.LONG_DT: return Schema.Type.LONG;
                case XSConstants.FLOAT_DT: return Schema.Type.FLOAT;
                case XSConstants.DOUBLE_DT: return Schema.Type.DOUBLE;
                default: return Schema.Type.STRING;
            }
        }

        private Schema createRecord(XSComplexTypeDefinition type) {
            String name = type.getName();

            Schema record = Schema.createRecord(name, null, null, false);
            if (name != null) types.put(name, record);

            record.setFields(createFields(type));
            return record;
        }

        private List<Schema.Field> createFields(XSComplexTypeDefinition type) {
            final Map<String, Schema.Field> fields = new LinkedHashMap<>();

            XSParticle particle = type.getParticle();
            if (particle == null) return new ArrayList<>(fields.values());

            XSTerm term = particle.getTerm();
            if (term.getType() != XSConstants.MODEL_GROUP)
                throw new UnsupportedOperationException("Unsupported term type " + term.getType());

            XSModelGroup group = (XSModelGroup) term;
            new Object() {
                void collectElementFields(XSModelGroup group) {
                    XSObjectList particles = group.getParticles();

                    for (int j = 0; j < particles.getLength(); j++) {
                        XSParticle particle = (XSParticle) particles.item(j);
                        XSTerm term = particle.getTerm();

                        switch (term.getType()) {
                            case XSConstants.ELEMENT_DECLARATION:
                                XSElementDeclaration el = (XSElementDeclaration) term;
                                Schema.Field field = createField(fields.values(), el, el.getTypeDefinition());
                                fields.put(field.getProp(SOURCE), field);
                                break;
                            case XSConstants.MODEL_GROUP:
                                XSModelGroup subGroup = (XSModelGroup) term;
                                collectElementFields(subGroup);
                                break;
                            case XSConstants.WILDCARD:
                                field = createField(fields.values(), term, null);
                                fields.put(field.getProp(SOURCE), field);
                                break;
                            default:
                                throw new UnsupportedOperationException("Unsupported term type " + term.getType());
                        }
                    }
                }
            }.collectElementFields(group);

            XSObjectList attrUses = type.getAttributeUses();
            for (int i = 0; i < attrUses.getLength(); i++) {
                XSAttributeUse attrUse = (XSAttributeUse) attrUses.item(i);
                XSAttributeDeclaration attr = attrUse.getAttrDeclaration();

                Schema.Field field = createField(fields.values(), attr, attr.getTypeDefinition());
                fields.put(field.getProp(SOURCE), field);
            }

            return new ArrayList<>(fields.values());
        }

        private Schema.Field createField(Iterable<Schema.Field> fields, XSObject source, XSTypeDefinition type) {
            List<Short> types = Arrays.asList(XSConstants.ELEMENT_DECLARATION, XSConstants.ATTRIBUTE_DECLARATION, XSConstants.WILDCARD);
            if (!types.contains(source.getType()))
                throw new IllegalArgumentException("Invalid origin object type " + source.getType());

            boolean wildcard = source.getType() == XSConstants.WILDCARD;
            if (wildcard) {
                Schema map = Schema.createMap(Schema.create(Schema.Type.STRING));
                return new Schema.Field(OTHERS, map, null, null);
            }

            boolean simple = type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE;
            Schema fieldType;

            if (simple) fieldType = Schema.create(getPrimitiveType((XSSimpleTypeDefinition) type));
            else {
                fieldType = this.types.get(type.getName());
                if (fieldType == null) fieldType = createRecord((XSComplexTypeDefinition) type);
            }

            boolean attribute = source.getType() == XSConstants.ATTRIBUTE_DECLARATION;

            int duplicates = 0;
            for (Schema.Field field : fields)
                if (field.name().equals(source.getName()))
                    duplicates++;
            String name = source.getName() + (duplicates > 0 ? duplicates - 1 : "");

            Schema.Field field = new Schema.Field(name, fieldType, null, null);
            field.addProp(SOURCE, "" + new Source(source.getName(), attribute));

            return field;
        }

        private static class ErrorHandler implements XMLErrorHandler {
            XMLParseException exception;

            @Override
            public void warning(String domain, String key, XMLParseException exception) throws XNIException {
                if (this.exception == null) this.exception = exception;
            }

            @Override
            public void error(String domain, String key, XMLParseException exception) throws XNIException {
                if (this.exception == null) this.exception = exception;
            }

            @Override
            public void fatalError(String domain, String key, XMLParseException exception) throws XNIException {
                if (this.exception == null) this.exception = exception;
            }
        }
    }

    public static class DatumBuilder {
        private static Element parse(File file) {
            try (InputStream stream = new FileInputStream(file)) {
                return parse(new InputSource(stream));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private static Element parse(InputSource source) {
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

        public DatumBuilder(Schema schema, File file) { this(schema, parse(file)); }
        public DatumBuilder(Schema schema, String xml) { this(schema, new StringReader(xml)); }
        public DatumBuilder(Schema schema, Reader reader) { this(schema, parse(new InputSource(reader))); }
        public DatumBuilder(Schema schema, InputStream stream) { this(schema, parse(new InputSource(stream))); }

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
                Schema.Field field = getFieldBySource(schema, new Source(child.getNodeName(), false));

                if (field != null) {
                    Object datum = createDatum(field.schema(), child);
                    record.put(field.name(), datum);
                } else {
                    Schema.Field anyField = schema.getField(OTHERS);
                    if (anyField == null)
                        throw new IllegalStateException("Type doesn't support any element");

                    if (record.get(OTHERS) == null)
                        record.put(OTHERS, new HashMap<String, Object>());

                    @SuppressWarnings("unchecked")
                    Map<String, String> map = (HashMap<String, String>) record.get(OTHERS);
                    map.put(child.getNodeName(), getContentAsText(child));
                }
            }

            NamedNodeMap attrMap = el.getAttributes();
            for (int i = 0; i < attrMap.getLength(); i++) {
                Attr attr = (Attr) attrMap.item(i);

                List<String> ignoredNamespaces = Arrays.asList("http://www.w3.org/2000/xmlns/", "http://www.w3.org/2001/XMLSchema-instance");
                if (ignoredNamespaces.contains(attr.getNamespaceURI())) continue;

                Schema.Field field = getFieldBySource(schema, new Source(attr.getName(), true));

                if (field == null)
                    throw new IllegalStateException("Unsupported attribute " + attr.getName());

                record.put(field.name(), attr.getValue());
            }

            return record;
        }

        static Schema.Field getFieldBySource(Schema schema, Source source) {
            for (Schema.Field field : schema.getFields())
                if (source.toString().equals(field.getProp(SOURCE)))
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

    public static class Source {
        // name of element/attribute
        private String name;
        // element or attribute
        private boolean attribute;

        public Source(String name) { this(name, false); }

        public Source(String name, boolean attribute) {
            this.name = name;
            this.attribute = attribute;
        }

        public String getName() { return name; }

        public boolean isElement() { return !isAttribute(); }
        public boolean isAttribute() { return attribute; }

        public int hashCode() { return Objects.hash(name, attribute); }

        public boolean equals(Object obj) {
            if (!(obj instanceof Source)) return false;
            Source source = (Source) obj;
            return name.equals(source.name) && attribute == source.attribute;
        }

        public String toString() {
            return (attribute ? "attribute" : "element") + " " + name;
        }
    }
}