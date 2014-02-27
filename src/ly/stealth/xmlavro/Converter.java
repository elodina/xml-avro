/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader;
import com.sun.org.apache.xerces.internal.xni.XMLResourceIdentifier;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLEntityResolver;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLInputSource;
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

public class Converter {
    public static final String SOURCE = "source";
    public static final String WILDCARD = "others";

    public static Schema createSchema(String xsd) { return new SchemaBuilder().createSchema(xsd); }
    public static Schema createSchema(File file) throws IOException { return new SchemaBuilder().createSchema(file); }
    public static Schema createSchema(Reader reader) { return new SchemaBuilder().createSchema(reader); }
    public static Schema createSchema(InputStream stream) { return new SchemaBuilder().createSchema(stream); }

    public static <T> T createDatum(Schema schema, File file) { return new DatumBuilder(schema).createDatum(file); }
    public static <T> T createDatum(Schema schema, String xml) { return new DatumBuilder(schema).createDatum(xml); }
    public static <T> T createDatum(Schema schema, Reader reader) { return new DatumBuilder(schema).createDatum(reader); }
    public static <T> T createDatum(Schema schema, InputStream stream) { return new DatumBuilder(schema).createDatum(stream); }

    public static class SchemaBuilder {
        private boolean debug;
        private File baseDir;

        private String rootElementName;
        private String rootElementNs;

        private Map<String, Schema> schemas = new LinkedHashMap<>();

        public boolean getDebug() { return debug; }
        public void setDebug(boolean debug) { this.debug = debug; }

        public File getBaseDir() { return baseDir; }
        public void setBaseDir(File baseDir) { this.baseDir = baseDir; }


        public String getRootElementName() { return rootElementName; }
        public void setRootElementName(String rootElementName) { this.rootElementName = rootElementName; }

        public String getRootElementNs() { return rootElementNs; }
        public void setRootElementNs(String rootElementNs) { this.rootElementNs = rootElementNs; }

        public Schema createSchema(String xsd) {
            return createSchema(new StringReader(xsd));
        }

        public Schema createSchema(File file) throws IOException {
            try (InputStream stream = new FileInputStream(file)) {
                return createSchema(stream);
            }
        }

        public Schema createSchema(Reader reader) {
            DOMInputImpl input = new DOMInputImpl();
            input.setCharacterStream(reader);
            return createSchema(input);
        }

        public Schema createSchema(InputStream stream) {
            DOMInputImpl input = new DOMInputImpl();
            input.setByteStream(stream);
            return createSchema(input);
        }

        public Schema createSchema(LSInput input) {
            ErrorHandler errorHandler = new ErrorHandler();

            XMLSchemaLoader loader = new XMLSchemaLoader();
            if (baseDir != null)
                loader.setEntityResolver(new EntityResolver(baseDir));

            loader.setErrorHandler(errorHandler);
            XSModel model = loader.load(input);

            if (errorHandler.exception != null)
                throw errorHandler.exception;

            return createSchema(model);
        }

        public Schema createSchema(XSModel model) {
            debug("Creating root schema");
            schemas.clear();

            XSElementDeclaration el = getRootElement(model);
            debug("Got root element declaration " + el.getName() + "{" + el.getNamespace() + "}");

            XSTypeDefinition type = el.getTypeDefinition();
            return createTypeSchema(type, false, false);
        }

        private XSElementDeclaration getRootElement(XSModel model) {
            if (rootElementName != null) {
                XSElementDeclaration el = model.getElementDeclaration(rootElementName, rootElementNs);
                if (el == null) throw new IllegalStateException("Root element declaration " + rootElementName + " not found");
                return el;
            }

            XSNamedMap elMap = model.getComponents(XSConstants.ELEMENT_DECLARATION);
            if (elMap.getLength() == 0) throw new IllegalStateException("No root element declaration");
            if (elMap.getLength() > 1) throw new IllegalStateException("Ambiguous root element declarations");

            return (XSElementDeclaration) elMap.item(0);
        }

        private int typeLevel;
        private Schema createTypeSchema(XSTypeDefinition type, boolean optional, boolean array) {
            typeLevel++;
            Schema schema;

            if (type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE)
                schema = Schema.create(getPrimitiveType((XSSimpleTypeDefinition) type));
            else {
                String name = typeName(type);
                debug("Creating schema for " + (type.getAnonymous() ? "anonymous type " + name : "type " + type.getName()));

                schema = schemas.get(name);
                if (schema == null) schema = createRecordSchema(name, (XSComplexTypeDefinition) type);
            }

            if (array)
                schema = Schema.createArray(schema);
            else if (optional) {
                Schema optionalSchema = Schema.create(Schema.Type.NULL);
                schema = Schema.createUnion(Arrays.asList(schema, optionalSchema));
            }

            typeLevel--;
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

            Schema fieldSchema = createTypeSchema(type, optional, array);

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

        private void debug(String s) {
            if (!debug) return;

            char[] prefix = new char[typeLevel];
            Arrays.fill(prefix, '-');

            if (debug) System.out.println(new String(prefix) + s);
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

        private class EntityResolver implements XMLEntityResolver {
            private File baseDir;
            private EntityResolver(File baseDir) { this.baseDir = baseDir; }

            @Override
            public XMLInputSource resolveEntity(XMLResourceIdentifier id) throws XNIException, IOException {
                String systemId = id.getLiteralSystemId();
                debug("Resolving " + systemId);

                boolean absolute = true;
                try { absolute = new URI(systemId).isAbsolute(); }
                catch (URISyntaxException ignore) {}

                if (absolute) return null;

                File file = new File(baseDir, systemId);
                XMLInputSource source = new XMLInputSource(id);
                source.setByteStream(new FileInputStream(file));

                return source;
            }
        }
    }

    public static class DatumBuilder {
        public static Element parse(InputSource source) {
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

        private Schema schema;

        public DatumBuilder(Schema schema) {
            this.schema = schema;
        }

        @SuppressWarnings("unchecked")
        public <T> T createDatum(String xml) {
            return createDatum(new StringReader(xml));
        }

        @SuppressWarnings("unchecked")
        public <T> T createDatum(File file) {
            try (InputStream stream = new FileInputStream(file)) {
                return createDatum(stream);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T createDatum(Reader reader) {
            return createDatum(new InputSource(reader));
        }

        @SuppressWarnings("unchecked")
        public <T> T createDatum(InputStream stream) {
            return createDatum(new InputSource(stream));
        }

        @SuppressWarnings("unchecked")
        public <T> T createDatum(InputSource source) {
            return createDatum(parse(source));
        }

        @SuppressWarnings("unchecked")
        public <T> T createDatum(Element el) {
            return (T) createNodeDatum(schema, el);
        }

        private Object createNodeDatum(Schema schema, Node source) {
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

                return createNodeDatum(unionTypes.get(0), source);
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
                    Object datum = createNodeDatum(!array ? field.schema() : field.schema().getElementType(), child);

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

                Object datum = createNodeDatum(field.schema(), attr);
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

    private static class Options {
        static final String USAGE = "{-d|--debug} {-b|--baseDir <baseDir>} <xsdFile> <xmlFile> {<avscFile>} {<avroFile>}";

        File xsdFile;
        File xmlFile;

        File avscFile;
        File avroFile;

        boolean debug;
        File baseDir;

        Options(String... args) {
            List<String> files = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if (arg.startsWith("-"))
                    switch (arg) {
                        case "-d":
                        case "--debug":
                            debug = true;
                            break;
                        case "-b":
                        case "--baseDir":
                            if (i == args.length - 1) throw new IllegalArgumentException("Base dir required");
                            i++;
                            baseDir = new File(args[i]);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported option " + arg);
                    }
                else
                    files.add(arg);
            }

            if (files.size() < 2 || files.size() > 4)
                throw new IllegalArgumentException("Incorrect number of in/out files. Expected [2..4]");

            xsdFile = replaceBaseDir(files.get(0), baseDir);
            xmlFile = replaceBaseDir(files.get(1), baseDir);

            avscFile = files.size() > 2 ? replaceBaseDir(files.get(3), baseDir) : replaceExtension(xsdFile, "avsc");
            avroFile = files.size() > 3 ? replaceBaseDir(files.get(4), baseDir) : replaceExtension(xmlFile, "avro");
        }

        private static File replaceExtension(File file, String newExtension) {
            String fileName = file.getPath();

            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx != -1) fileName = fileName.substring(0, dotIdx);

            return new File(fileName + "." + newExtension);
        }

        private static File replaceBaseDir(String path, File baseDir) {
            File file = new File(path);
            if (baseDir == null || file.isAbsolute()) return file;
            return new File(baseDir, file.getPath());
        }
    }

    public static void main(String... args) throws IOException {
        Options opts;
        try {
            opts = new Options(args);
        } catch (IllegalArgumentException e) {
            System.out.println("XML Avro converter.\nError: " + e.getMessage() + "\n" + "Usage: " + Options.USAGE + "\n");
            System.exit(1);
            return;
        }

        System.out.println("Converting: \n" + opts.xsdFile + " -> " + opts.avscFile + "\n" + opts.xmlFile + " -> " + opts.avroFile);

        Element el;
        try (InputStream stream = new FileInputStream(opts.xmlFile)) {
            el = DatumBuilder.parse(new InputSource(stream));
        }

        SchemaBuilder schemaBuilder = new SchemaBuilder();
        schemaBuilder.setDebug(opts.debug);
        schemaBuilder.setBaseDir(opts.baseDir);

        schemaBuilder.setRootElementName(el.getTagName());
        schemaBuilder.setRootElementNs(el.getNamespaceURI());
        Schema schema = schemaBuilder.createSchema(opts.xsdFile);

        try (Writer writer = new FileWriter(opts.avscFile)) {
            writer.write(schema.toString(true));
        }

        DatumBuilder datumBuilder = new DatumBuilder(schema);
        Object datum = datumBuilder.createDatum(el);

        try (OutputStream stream = new FileOutputStream(opts.avroFile)) {
            DatumWriter<Object> datumWriter = new SpecificDatumWriter<>(schema);
            datumWriter.write(datum, EncoderFactory.get().directBinaryEncoder(stream, null));
        }
    }
}