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

public class TypeBuilder {
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
    private QName rootElementQName;

    // Type by QName, where QName:
    // - QName of type for named types
    // - QName of element for anonymous or simple types of root elements
    private Map<QName, Datum.Type> types = new LinkedHashMap<>();

    public TypeBuilder(String xsd) { this(parse(new StringReader(xsd))); }

    public TypeBuilder(File file) throws IOException { this(parse(file)); }

    public TypeBuilder(Reader reader) { this(parse(reader)); }
    public TypeBuilder(InputStream stream) { this(parse(stream)); }

    private TypeBuilder(XSModel schema) { this.schema = schema; }

    public QName getRootElementQName() { return rootElementQName; }
    public void setRootElementQName(QName rootElementQName) { this.rootElementQName = rootElementQName; }


    @SuppressWarnings("unchecked")
    public <T extends Datum.Type> T createType() {
        XSElementDeclaration el = getRootElement();

        XSTypeDefinition type = el.getTypeDefinition();

        types.clear();
        return (T) createType(type);
    }

    private XSElementDeclaration getRootElement() {
        if (rootElementQName != null) {
            XSElementDeclaration el = schema.getElementDeclaration(rootElementQName.getName(), rootElementQName.getNamespace());
            if (el == null) throw new IllegalStateException("Root element declaration " + rootElementQName + " not found");
            return el;
        }

        XSNamedMap elMap = schema.getComponents(XSConstants.ELEMENT_DECLARATION);
        if (elMap.getLength() == 0) throw new IllegalStateException("No root element declaration");
        if (elMap.getLength() > 1) throw new IllegalStateException("Ambiguous root element declarations");

        return (XSElementDeclaration) elMap.item(0);
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

        record.setFields(createFields(type));
        return record;
    }

    private List<Record.Field> createFields(XSComplexTypeDefinition type) {
        final Map<Record.Origin, Record.Field> fields = new LinkedHashMap<>();

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
                            Record.Field field = createField(el, el.getTypeDefinition());
                            fields.put(field.getOrigin(), field);
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
            XSAttributeDeclaration attr = attrUse.getAttrDeclaration();

            Record.Field field = createField(attr, attr.getTypeDefinition());
            fields.put(field.getOrigin(), field);
        }

        return new ArrayList<>(fields.values());
    }

    private Record.Field createField(XSObject originObj, XSTypeDefinition type) {
        if (!Arrays.asList(XSConstants.ELEMENT_DECLARATION, XSConstants.ATTRIBUTE_DECLARATION).contains(originObj.getType()))
            throw new IllegalArgumentException("Invalid origin object type " + originObj.getType());

        boolean simple = type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE;
        Datum.Type fieldType;

        if (simple) fieldType = Value.Type.valueOf((XSSimpleTypeDefinition) type);
        else {
            fieldType = types.get(new QName(type.getName(), type.getNamespace()));
            if (fieldType == null) fieldType = createRecord((XSComplexTypeDefinition) type);
        }

        QName qName = new QName(originObj.getName(), originObj.getNamespace());
        boolean attribute = originObj.getType() == XSConstants.ATTRIBUTE_DECLARATION;

        return new Record.Field(new Record.Origin(qName, attribute), fieldType);
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
