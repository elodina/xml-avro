package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import com.sun.org.apache.xerces.internal.xs.*;
import org.w3c.dom.ls.LSInput;

import java.io.*;
import java.util.*;

public class AvroSchema {
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
    // Type by QName, where QName:
    // - type QName for named types
    // - element QName for anonymous or simple types of root elements
    private Map<QName, Type> types = new LinkedHashMap<>();

    public AvroSchema(String xsd) { this(parse(new StringReader(xsd))); }

    public AvroSchema(File file) throws IOException { this(parse(file)); }

    public AvroSchema(Reader reader) { this(parse(reader)); }
    public AvroSchema(InputStream stream) { this(parse(stream)); }

    private AvroSchema(XSModel schema) {
        this.schema = schema;
        initTypes();
    }

    public <T extends Type> T getRootType(String name) { return getRootType(new QName(name)); }

    @SuppressWarnings("unchecked")
    public <T extends Type> T getRootType(QName rootElement) {
        XSElementDeclaration el = schema.getElementDeclaration(rootElement.getName(), rootElement.getNamespace());
        if (el == null) throw new IllegalArgumentException("Root element definition " + rootElement + " not found");

        XSTypeDefinition type = el.getTypeDefinition();
        boolean anonymousOrSimple = type.getAnonymous() || type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE;
        QName name = anonymousOrSimple ? new QName(el.getName(), el.getNamespace()) : new QName(type.getName(), type.getNamespace());

        return (T) types.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T extends Type> T getNamedType(QName qName) {
        return (T) types.get(qName);
    }

    private void initTypes() {
        XSNamedMap typeMap = schema.getComponents(XSConstants.TYPE_DEFINITION);
        for (int i = 0; i < typeMap.getLength(); i++) {
            XSTypeDefinition xmlType = (XSTypeDefinition) typeMap.item(i);
            if ("http://www.w3.org/2001/XMLSchema".equals(xmlType.getNamespace())) continue;

            Type type = createType(xmlType);
            if (!type.isAnonymous()) types.put(type.getQName(), type);
        }

        XSNamedMap elMap = schema.getComponents(XSConstants.ELEMENT_DECLARATION);
        for (int i = 0; i < elMap.getLength(); i++) {
            XSElementDeclaration el = (XSElementDeclaration) elMap.item(i);
            Type type = createType(el.getTypeDefinition());

            if (type.isAnonymous() || type.isPrimitive())
                types.put(new QName(el.getName(), el.getNamespace()), type);
        }
    }

    private Type createType(XSTypeDefinition type) {
        if (type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE)
            return Primitive.valueOf((XSSimpleTypeDefinition) type);
        else
            return createRecord((XSComplexTypeDefinition) type);
    }

    private Record createRecord(XSComplexTypeDefinition type) {
        QName qName = !type.getAnonymous() ? new QName(type.getName(), type.getNamespace()) : null;

        Record record = new Record(qName);
        if (qName != null) types.put(qName, record);

        record.fields.addAll(createFields(type));
        return record;
    }

    private List<Field> createFields(XSComplexTypeDefinition type) {
        final Map<String, Field> fields = new LinkedHashMap<>();

        XSParticle particle = type.getParticle();
        if (particle == null) return new ArrayList<>(fields.values());

        XSTerm term = particle.getTerm();
        if (term.getType() != XSConstants.MODEL_GROUP)
            throw new UnsupportedOperationException("Unsupported term type");

        XSModelGroup group = (XSModelGroup) term;
        new Object() {
            void collectFields(XSModelGroup group) {
                XSObjectList particles = group.getParticles();

                for (int j = 0; j < particles.getLength(); j++) {
                    XSParticle particle = (XSParticle) particles.item(j);
                    XSTerm term = particle.getTerm();

                    switch (term.getType()) {
                        case XSConstants.ELEMENT_DECLARATION:
                            XSElementDeclaration el = (XSElementDeclaration) term;
                            Field field = createField(el);
                            fields.put(field.name, field);
                            break;
                        case XSConstants.MODEL_GROUP:
                            XSModelGroup subGroup = (XSModelGroup) term;
                            collectFields(subGroup);
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported term type " + term.getType());
                    }
                }
            }
        }.collectFields(group);

        return new ArrayList<>(fields.values());
    }

    private Field createField(XSElementDeclaration el) {
        XSTypeDefinition type = el.getTypeDefinition();

        boolean simple = type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE;
        Type fieldType;

        if (simple) fieldType = Primitive.valueOf((XSSimpleTypeDefinition) type);
        else {
            fieldType = getNamedType(new QName(type.getName(), type.getNamespace()));
            if (fieldType == null) fieldType = createRecord((XSComplexTypeDefinition) type);
        }

        return new Field(el.getName(), fieldType);
    }

    public interface Type {
        QName getQName();
        boolean isAnonymous();
        boolean isPrimitive();
    }

    public static enum Primitive implements Type {
        NULL,       // no value
        BOOLEAN,    // a binary value
        INT,        // 32-bit signed integer
        LONG,       // 64-bit signed integer
        FLOAT,      // single precision (32-bit) IEEE 754 floating-point number
        DOUBLE,     // double precision (64-bit) IEEE 754 floating-point number
        BYTES,      // sequence of 8-bit unsigned bytes
        STRING;     // unicode character sequence

        static Primitive valueOf(XSSimpleTypeDefinition type) {
            switch (type.getBuiltInKind()) {
                case XSConstants.BOOLEAN_DT: return BOOLEAN;
                case XSConstants.INT_DT: return INT;
                case XSConstants.LONG_DT: return LONG;
                case XSConstants.FLOAT_DT: return FLOAT;
                case XSConstants.DOUBLE_DT: return DOUBLE;
                default: return STRING;
            }
        }


        @Override
        public QName getQName() { return new QName(name()); }

        @Override
        public boolean isAnonymous() { return false; }

        @Override
        public boolean isPrimitive() { return true; }
    }

    public class Record implements Type {
        private QName qName;
        private List<Field> fields = new ArrayList<>();

        public Record(QName qName) {
            this.qName = qName;
        }

        public QName getQName() { return qName; }

        public List<Field> getFields() { return Collections.unmodifiableList(fields); }

        public Field getField(String name) {
            for (Field field : fields)
                if (field.name.equals(name))
                    return field;

            throw new IllegalArgumentException("Field " + name + " not found");
        }


        @Override
        public boolean isAnonymous() { return qName != null; }

        @Override
        public boolean isPrimitive() { return false; }
    }

    public static class Field {
        private String name;
        private Type type;
        private String doc;

        public Field(String name, Type type) { this(name, type, null); }

        public Field(String name, Type type, String doc) {
            this.name = name;
            this.type = type;
            this.doc = doc;
        }

        public String getName() { return name; }

        @SuppressWarnings("unchecked")
        public <T extends Type> T getType() { return (T) type; }

        public String getDoc() { return doc; }
    }

    public static class QName {
        private String name, namespace;

        public QName(String name) { this(name, null); }

        public QName(String name, String namespace) {
            if (name == null) throw new NullPointerException("qName");
            this.name = name;
            this.namespace = namespace;
        }

        public String getName() { return name; }
        public String getNamespace() { return namespace; }

        public int hashCode() {
            return Objects.hash(name, namespace);
        }

        public boolean equals(Object obj) {
            if (!(obj instanceof QName)) return false;
            QName qName = (QName) obj;
            return name.equals(qName.name) && Objects.equals(namespace, qName.namespace);
        }

        public String toString() {
            return (namespace != null ? "{" + namespace + "}" : "") + name;
        }
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
