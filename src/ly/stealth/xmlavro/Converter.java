package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import com.sun.org.apache.xerces.internal.xs.*;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
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
    public static final String WILDCARD = "others";

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

        private XSModel model;
        private String rootElementName;

        private Map<String, Schema> schemas = new LinkedHashMap<>();

        public SchemaBuilder(String xsd) { this(parse(new StringReader(xsd))); }

        public SchemaBuilder(File file) throws IOException { this(parse(file)); }

        public SchemaBuilder(Reader reader) { this(parse(reader)); }
        public SchemaBuilder(InputStream stream) { this(parse(stream)); }

        private SchemaBuilder(XSModel model) { this.model = model; }

        public String getRootElementName() { return rootElementName; }
        public void setRootElementName(String rootElementName) { this.rootElementName = rootElementName; }


        @SuppressWarnings("unchecked")
        public Schema createSchema() {
            schemas.clear();

            XSElementDeclaration el = getRootElement();
            XSTypeDefinition type = el.getTypeDefinition();

            return createSchema(type, false, false);
        }

        private XSElementDeclaration getRootElement() {
            if (rootElementName != null) {
                XSElementDeclaration el = model.getElementDeclaration(rootElementName, null);
                if (el == null) throw new IllegalStateException("Root element declaration " + rootElementName + " not found");
                return el;
            }

            XSNamedMap elMap = model.getComponents(XSConstants.ELEMENT_DECLARATION);
            if (elMap.getLength() == 0) throw new IllegalStateException("No root element declaration");
            if (elMap.getLength() > 1) throw new IllegalStateException("Ambiguous root element declarations");

            return (XSElementDeclaration) elMap.item(0);
        }

        private Schema createSchema(XSTypeDefinition type, boolean optional, boolean array) {
            Schema schema;

            if (type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE)
                schema = Schema.create(getPrimitiveType((XSSimpleTypeDefinition) type));
            else {
                String name = typeName(type);
                schema = schemas.get(name);
                if (schema == null) schema = createRecordSchema(name, (XSComplexTypeDefinition) type);
            }

            if (array)
                schema = Schema.createArray(schema);
            else if (optional) {
                Schema optionalSchema = Schema.create(Schema.Type.NULL);
                schema = Schema.createUnion(Arrays.asList(schema, optionalSchema));
            }

            return schema;
        }

        private Schema createRecordSchema(String name, XSComplexTypeDefinition type) {
            Schema record = Schema.createRecord(name, null, null, false);
            schemas.put(name, record);

            record.setFields(createFields(type));
            return record;
        }

        private List<Schema.Field> createFields(XSComplexTypeDefinition type) {
            final Map<String, Schema.Field> fields = new LinkedHashMap<>();

            XSObjectList attrUses = type.getAttributeUses();
            for (int i = 0; i < attrUses.getLength(); i++) {
                XSAttributeUse attrUse = (XSAttributeUse) attrUses.item(i);
                XSAttributeDeclaration attrDecl = attrUse.getAttrDeclaration();

                boolean optional = !attrUse.getRequired();
                Schema.Field field = createField(fields.values(), attrDecl, attrDecl.getTypeDefinition(), optional, false);
                fields.put(field.getProp(SOURCE), field);
            }

            XSParticle particle = type.getParticle();
            if (particle == null) return new ArrayList<>(fields.values());

            XSTerm term = particle.getTerm();
            if (term.getType() != XSConstants.MODEL_GROUP)
                throw new UnsupportedOperationException("Unsupported term type " + term.getType());

            XSModelGroup group = (XSModelGroup) term;
            createGroupFields(group, fields, false);

            return new ArrayList<>(fields.values());
        }

        private void createGroupFields(XSModelGroup group, Map<String, Schema.Field> fields, boolean forceOptional) {
            XSObjectList particles = group.getParticles();

            for (int j = 0; j < particles.getLength(); j++) {
                XSParticle particle = (XSParticle) particles.item(j);
                boolean insideChoice = group.getCompositor() == XSModelGroup.COMPOSITOR_CHOICE;

                boolean optional = insideChoice || particle.getMinOccurs() == 0;
                boolean array = particle.getMaxOccurs() > 1 || particle.getMaxOccursUnbounded();

                XSTerm term = particle.getTerm();

                switch (term.getType()) {
                    case XSConstants.ELEMENT_DECLARATION:
                        XSElementDeclaration el = (XSElementDeclaration) term;
                        Schema.Field field = createField(fields.values(), el, el.getTypeDefinition(), forceOptional || optional, array);
                        fields.put(field.getProp(SOURCE), field);
                        break;
                    case XSConstants.MODEL_GROUP:
                        XSModelGroup subGroup = (XSModelGroup) term;
                        createGroupFields(subGroup, fields, forceOptional || insideChoice);
                        break;
                    case XSConstants.WILDCARD:
                        field = createField(fields.values(), term, null, forceOptional || optional, array);
                        fields.put(field.getProp(SOURCE), field);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported term type " + term.getType());
                }
            }
        }

        private Schema.Field createField(Iterable<Schema.Field> fields, XSObject source, XSTypeDefinition type, boolean optional, boolean array) {
            List<Short> supportedTypes = Arrays.asList(XSConstants.ELEMENT_DECLARATION, XSConstants.ATTRIBUTE_DECLARATION, XSConstants.WILDCARD);
            if (!supportedTypes.contains(source.getType()))
                throw new IllegalArgumentException("Invalid source object type " + source.getType());

            boolean wildcard = source.getType() == XSConstants.WILDCARD;
            if (wildcard) {
                Schema map = Schema.createMap(Schema.create(Schema.Type.STRING));
                return new Schema.Field(WILDCARD, map, null, null);
            }

            Schema fieldSchema = createSchema(type, optional, array);

            String name = validName(source.getName());
            name = uniqueFieldName(fields, name);

            Schema.Field field = new Schema.Field(name, fieldSchema, null, null);

            boolean attribute = source.getType() == XSConstants.ATTRIBUTE_DECLARATION;
            field.addProp(SOURCE, "" + new Source(source.getName(), attribute));

            return field;
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

        static String uniqueFieldName(Iterable<Schema.Field> fields, String name) {
            int duplicates = 0;

            for (Schema.Field field : fields) {
                if (field.name().equals(name))
                    duplicates++;
            }

            return name + (duplicates > 0 ? duplicates - 1 : "");
        }

        String typeName(XSTypeDefinition type) {
            String name = validName(type.getName());
            return name != null ? name : nextTypeName();
        }

        String validName(String name) {
            if (name == null) return null;

            char[] chars = name.toCharArray();
            char[] result = new char[chars.length];

            int p = 0;
            for (char c : chars) {
                boolean valid =
                        c >= 'a' && c <= 'z' ||
                        c >= 'A' && c <= 'z' ||
                        c >= '0' && c <= '9' ||
                        c == '_';

                boolean separator = c == '.' || c == '-';

                if (valid) {
                    result[p] = c;
                    p++;
                } else if (separator) {
                    result[p] = '_';
                    p++;
                }
            }

            String s = new String(result, 0, p);

            // handle built-in types
            try {
                Schema.Type.valueOf(s.toUpperCase());
                s += typeName++;
            } catch (IllegalArgumentException ignore) {}

            return s;
        }

        private int typeName;
        private String nextTypeName() { return "type" + typeName++; }

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

        private Object createDatum(Schema schema, Node source) {
            if (!Arrays.asList(Node.ELEMENT_NODE, Node.ATTRIBUTE_NODE).contains(source.getNodeType()))
                throw new IllegalArgumentException("Unsupported node type " + source.getNodeType());

            if (PRIMITIVES.contains(schema.getType()))
                return createValue(schema.getType(), source.getTextContent());

            if (schema.getType() == Schema.Type.UNION) {
                // Unions could exist only in form of [type, null],
                // which means optional value
                List<Schema> unionTypes = schema.getTypes();
                if (unionTypes.size() != 2 || unionTypes.get(1).getType() != Schema.Type.NULL)
                    throw new IllegalStateException("Unsupported union type " + schema);

                return createDatum(unionTypes.get(0), source);
            }

            if (schema.getType() == Schema.Type.RECORD)
                return createRecord(schema, (Element)source);

            throw new IllegalStateException("Unsupported schema type " + schema.getType());
        }

        private Object createValue(Schema.Type type, String text) {
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

        private GenericData.Record createRecord(Schema schema, Element el) {
            GenericData.Record record = new GenericData.Record(schema);

            // initialize arrays
            for (Schema.Field field : record.getSchema().getFields())
                if (field.schema().getType() == Schema.Type.ARRAY)
                    record.put(field.name(), new ArrayList<>());

            NodeList nodes = el.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;

                Element child = (Element) node;
                Schema.Field field = getFieldBySource(schema, new Source(child.getNodeName(), false));

                if (field != null) {
                    boolean array = field.schema().getType() == Schema.Type.ARRAY;
                    Object datum = createDatum(!array ? field.schema() : field.schema().getElementType(), child);

                    if (!array)
                        record.put(field.name(), datum);
                    else {
                        @SuppressWarnings("unchecked")
                        List<Object> values = (List<Object>) record.get(field.name());
                        values.add(datum);
                    }
                } else {
                    Schema.Field anyField = schema.getField(WILDCARD);
                    if (anyField == null)
                        throw new IllegalStateException("Type doesn't support any element");

                    if (record.get(WILDCARD) == null)
                        record.put(WILDCARD, new HashMap<String, Object>());

                    @SuppressWarnings("unchecked")
                    Map<String, String> map = (HashMap<String, String>) record.get(WILDCARD);
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

                Object datum = createDatum(field.schema(), attr);
                record.put(field.name(), datum);
            }

            return record;
        }

        static Schema.Field getFieldBySource(Schema schema, Source source) {
            for (Schema.Field field : schema.getFields())
                if (source.toString().equals(field.getProp(SOURCE)))
                    return field;

            return null;
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

    public static void main(String[] args) throws IOException {
        if (args.length < 2 || args.length > 4) {
            System.out.println("XML Avro converter.\nUsage: <xsdFile> <xmlFile> {<avscFile>} {<avroFile>}\n");
            return;
        }

        File xsdFile = new File(args[0]);
        File xmlFile = new File(args[1]);

        File avscFile = args.length > 2 ? new File(args[3]) : replaceExtension(xsdFile, "avsc");
        File avroFile = args.length > 3 ? new File(args[4]) : replaceExtension(xmlFile, "avro");

        System.out.println("converting: \n" + xsdFile + " -> " + avscFile + "\n" + xmlFile + " -> " + avroFile);

        Schema schema = createSchema(xsdFile);
        try (Writer writer = new FileWriter(avscFile)) {
            writer.write(schema.toString(true));
        }

        Object datum = createDatum(schema, xmlFile);

        try (OutputStream stream = new FileOutputStream(avroFile)) {
            DatumWriter<Object> datumWriter = new SpecificDatumWriter<>(schema);
            datumWriter.write(datum, EncoderFactory.get().directBinaryEncoder(stream, null));
        }
    }

    private static File replaceExtension(File file, String newExtension) {
        String fileName = file.getPath();

        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx != -1) fileName = fileName.substring(0, dotIdx);

        return new File(fileName + "." + newExtension);
    }
}