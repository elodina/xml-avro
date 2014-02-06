package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import com.sun.org.apache.xerces.internal.xs.*;
import org.apache.avro.Schema;
import org.w3c.dom.ls.LSInput;

import java.io.*;
import java.util.*;

public class AvroTypes {
    private static XSModel parseSchema(File file) throws IOException {
        try (InputStream stream = new FileInputStream(file)) {
            return parseSchema(stream);
        }
    }

    private static XSModel parseSchema(InputStream stream) {
        DOMInputImpl input = new DOMInputImpl();
        input.setByteStream(stream);
        return parseSchema(input);
    }

    private static XSModel parseSchema(Reader reader) {
        DOMInputImpl input = new DOMInputImpl();
        input.setCharacterStream(reader);
        return parseSchema(input);
    }

    private static XSModel parseSchema(LSInput input) {
        ErrorHandler errorHandler = new ErrorHandler();

        XMLSchemaLoader loader = new XMLSchemaLoader();
        loader.setErrorHandler(errorHandler);
        XSModel schema = loader.load(input);

        if (errorHandler.exception != null)
            throw errorHandler.exception;

        return schema;
    }

    private XSModel schema;
    private Map<XSComplexTypeDefinition, Schema> records = new HashMap<>();

    public AvroTypes(String xsd) { this(parseSchema(new StringReader(xsd))); }
    public AvroTypes(File file) throws IOException { this(parseSchema(file)); }

    public AvroTypes(Reader reader) { this(parseSchema(reader)); }
    public AvroTypes(InputStream stream) { this(parseSchema(stream)); }

    private AvroTypes(XSModel schema) {
        this.schema = schema;

        XSNamedMap map = schema.getComponents(XSTypeDefinition.COMPLEX_TYPE);
        for (int i = 0; i < map.getLength(); i++) {
            XSComplexTypeDefinition type = (XSComplexTypeDefinition) map.item(i);
            if ("http://www.w3.org/2001/XMLSchema".equals(type.getNamespace())) continue;

            records.put(type, createRecordSchema(type));
        }
    }

    public XSModel getSchema() { return schema; }

    public Map<XSComplexTypeDefinition, Schema> getRecords() {
        return Collections.unmodifiableMap(records);
    }

    public Schema getRecord(XSComplexTypeDefinition typeDef) { return records.get(typeDef); }

    public Schema getRecordByRootElement(String name, String namespace) {
        XSElementDeclaration elDecl = schema.getElementDeclaration(name, namespace);
        if (elDecl == null) return null;

        // todo support simple type?
        XSComplexTypeDefinition typeDef = (XSComplexTypeDefinition) elDecl.getTypeDefinition();
        return getRecord(typeDef);
    }

    private Schema createRecordSchema(XSComplexTypeDefinition type) {
        Schema schema = Schema.createRecord(type.getName(), null, type.getNamespace(), false);
        schema.setFields(createFields(type));

        return schema;
    }

    private List<Schema.Field> createFields(XSComplexTypeDefinition type) {
        final Map<String, Schema.Field> fields = new LinkedHashMap<>();

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
                            Schema.Field field = createField(el);
                            fields.put(field.name(), field);
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

    private Schema.Field createField(XSElementDeclaration el) {
        XSTypeDefinition typeDef = el.getTypeDefinition();

        Schema schema = typeDef.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE ?
                Schema.create(Schema.Type.STRING) :
                createRecordSchema((XSComplexTypeDefinition) typeDef);

        // todo support of different types
        // todo use unique name
        return new Schema.Field(el.getName(), schema, null, null);
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
