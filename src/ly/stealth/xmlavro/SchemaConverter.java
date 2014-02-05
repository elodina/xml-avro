package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.dom.DOMInputImpl;
import com.sun.org.apache.xerces.internal.impl.xs.XMLSchemaLoader;
import com.sun.org.apache.xerces.internal.xni.XNIException;
import com.sun.org.apache.xerces.internal.xni.parser.XMLErrorHandler;
import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import com.sun.org.apache.xerces.internal.xs.*;
import org.apache.avro.Schema;
import org.w3c.dom.ls.LSInput;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SchemaConverter {
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
    private List<Schema> avroSchemas = new ArrayList<>();

    public SchemaConverter(String xsd) { this(parseSchema(new StringReader(xsd))); }
    public SchemaConverter(File file) throws IOException { this(parseSchema(file)); }

    public SchemaConverter(Reader reader) { this(parseSchema(reader)); }
    public SchemaConverter(InputStream stream) { this(parseSchema(stream)); }

    private SchemaConverter(XSModel schema) {
        this.schema = schema;
    }

    public List<Schema> getAvroSchemas() { return Collections.unmodifiableList(avroSchemas); }

    public void convert() {
        XSNamedMap map = schema.getComponents(XSTypeDefinition.COMPLEX_TYPE);
        for (int i = 0; i < map.getLength(); i++) {
            XSTypeDefinition type = (XSTypeDefinition) map.item(i);

            if ("http://www.w3.org/2001/XMLSchema".equals(type.getNamespace()))
                continue;

            avroSchemas.add(wrapComplexType((XSComplexTypeDefinition) type));
        }
    }

    private Schema wrapComplexType(XSComplexTypeDefinition type) {
        Schema schema = Schema.createRecord(type.getName(), "", type.getNamespace(), false);

        List<Schema.Field> fields = new ArrayList<>();
        XSParticle particle = type.getParticle();
        if (particle != null) {
            XSTerm term = particle.getTerm();

            switch (term.getType()) {
                case XSConstants.MODEL_GROUP:
                    XSModelGroup group = (XSModelGroup) term;

                    XSObjectList particles = group.getParticles();
                    for (int j = 0; j < particles.getLength(); j++) {
                        XSParticle groupParticle = (XSParticle) particles.item(j);
                        XSTerm groupTerm = groupParticle.getTerm();

                        switch (groupTerm.getType()) {
                            case XSConstants.ELEMENT_DECLARATION:
                                XSElementDeclaration elDecl = (XSElementDeclaration) groupTerm;
                                fields.add(wrapElementDecl(elDecl));
                                break;
                            default:
                                throw new UnsupportedOperationException("Unsupported term type " + term.getType());
                        }
                    }

                    // todo;
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported term type " + term.getType());
            }
        }

        schema.setFields(fields);
        return schema;
    }

    private Schema.Field wrapElementDecl(XSElementDeclaration elDecl) {
        // todo support of types
        // todo check is el type is simple type
        return new Schema.Field(elDecl.getName(), Schema.create(Schema.Type.STRING), null, null);
    }

    public static void main(String[] args) throws IOException, SAXException {
        SchemaConverter converter = new SchemaConverter(new File("test.xsd"));
        converter.convert();

        for (Schema schema : converter.getAvroSchemas())
            System.out.println(schema.toString(true));
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
