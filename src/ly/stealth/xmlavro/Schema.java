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

public class Schema {
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
    // - QName of type for named types
    // - QName of element for anonymous or simple types of root elements
    private Map<QName, Datum.Type> types = new LinkedHashMap<>();

    public Schema(String xsd) { this(parse(new StringReader(xsd))); }

    public Schema(File file) throws IOException { this(parse(file)); }

    public Schema(Reader reader) { this(parse(reader)); }
    public Schema(InputStream stream) { this(parse(stream)); }

    private Schema(XSModel schema) {
        this.schema = schema;
        initTypes();
    }

    public <T extends Datum.Type> T getRootType(String name) { return getRootType(new QName(name)); }

    @SuppressWarnings("unchecked")
    public <T extends Datum.Type> T getRootType(QName rootQName) {
        XSElementDeclaration el = schema.getElementDeclaration(rootQName.getName(), rootQName.getNamespace());
        if (el == null) throw new IllegalArgumentException("Root element definition " + rootQName + " not found");

        XSTypeDefinition type = el.getTypeDefinition();
        boolean anonymousOrSimple = type.getAnonymous() || type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE;
        QName name = anonymousOrSimple ? new QName(el.getName(), el.getNamespace()) : new QName(type.getName(), type.getNamespace());

        return (T) types.get(name);
    }

    public <T extends Datum.Type> T getNamedType(String name) { return getNamedType(new QName(name)); }

    @SuppressWarnings("unchecked")
    public <T extends Datum.Type> T getNamedType(QName qName) {
        return (T) types.get(qName);
    }

    public void write(QName rootQName, File file) throws IOException {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8")) {
            save(rootQName, writer);
        }
    }

    public void save(QName rootQName, Writer writer) throws IOException {
        Datum.Type type = getRootType(rootQName);
        org.apache.avro.Schema schema = type.toAvroSchema();
        writer.write(schema.toString(true));
    }

    private void initTypes() {
        XSNamedMap typeMap = schema.getComponents(XSConstants.TYPE_DEFINITION);
        for (int i = 0; i < typeMap.getLength(); i++) {
            XSTypeDefinition xmlType = (XSTypeDefinition) typeMap.item(i);
            if ("http://www.w3.org/2001/XMLSchema".equals(xmlType.getNamespace())) continue;

            Datum.Type type = createType(xmlType);
            if (!type.isAnonymous()) types.put(type.getQName(), type);
        }

        XSNamedMap elMap = schema.getComponents(XSConstants.ELEMENT_DECLARATION);
        for (int i = 0; i < elMap.getLength(); i++) {
            XSElementDeclaration el = (XSElementDeclaration) elMap.item(i);
            Datum.Type type = createType(el.getTypeDefinition());

            if (type.isAnonymous() || type.isPrimitive())
                types.put(new QName(el.getName(), el.getNamespace()), type);
        }
    }

    private Datum.Type createType(XSTypeDefinition type) {
        if (type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE)
            return Value.Type.valueOf((XSSimpleTypeDefinition) type);
        else
            return createRecord((XSComplexTypeDefinition) type);
    }

    private Record.Type createRecord(XSComplexTypeDefinition type) {
        QName qName = !type.getAnonymous() ? new QName(type.getName(), type.getNamespace()) : null;

        Record.Type record = new Record.Type(qName);
        if (qName != null) types.put(qName, record);

        record.fields.addAll(createFields(type));
        return record;
    }

    private List<Record.Field> createFields(XSComplexTypeDefinition type) {
        final Map<String, Record.Field> fields = new LinkedHashMap<>();

        XSParticle particle = type.getParticle();
        if (particle == null) return new ArrayList<>(fields.values());

        XSTerm term = particle.getTerm();
        if (term.getType() != XSConstants.MODEL_GROUP)
            throw new UnsupportedOperationException("Unsupported term type");

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
                            Record.Field field = createField(el.getName(), el.getTypeDefinition());
                            fields.put(field.getName(), field);
                            break;
                        case XSConstants.MODEL_GROUP:
                            XSModelGroup subGroup = (XSModelGroup) term;
                            collectElementFields(subGroup);
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
            XSAttributeDeclaration attrDecl = attrUse.getAttrDeclaration();

            Record.Field field = createField(attrDecl.getName(), attrDecl.getTypeDefinition());
            fields.put(field.getName(), field);
        }

        return new ArrayList<>(fields.values());
    }

    private Record.Field createField(String name, XSTypeDefinition type) {
        boolean simple = type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE;
        Datum.Type fieldType;

        if (simple) fieldType = Value.Type.valueOf((XSSimpleTypeDefinition) type);
        else {
            fieldType = getNamedType(new QName(type.getName(), type.getNamespace()));
            if (fieldType == null) fieldType = createRecord((XSComplexTypeDefinition) type);
        }

        return new Record.Field(name, fieldType);
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
