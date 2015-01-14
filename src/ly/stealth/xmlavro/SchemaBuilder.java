package ly.stealth.xmlavro;

import org.apache.avro.Schema;
import org.apache.xerces.dom.DOMInputImpl;
import org.apache.xerces.impl.Constants;
import org.apache.xerces.impl.xs.XMLSchemaLoader;
import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.xni.XMLResourceIdentifier;
import org.apache.xerces.xni.XNIException;
import org.apache.xerces.xni.parser.XMLEntityResolver;
import org.apache.xerces.xni.parser.XMLErrorHandler;
import org.apache.xerces.xni.parser.XMLInputSource;
import org.apache.xerces.xni.parser.XMLParseException;
import org.apache.xerces.xs.*;
import org.w3c.dom.DOMError;
import org.w3c.dom.DOMErrorHandler;
import org.w3c.dom.DOMLocator;
import org.w3c.dom.ls.LSInput;

import java.io.*;
import java.util.*;

public class SchemaBuilder {
    private boolean debug;
    private Resolver resolver;

    private static Map<Short, Schema.Type> primitives = new HashMap<>();
    static {
        primitives.put(XSConstants.BOOLEAN_DT, Schema.Type.BOOLEAN);

        primitives.put(XSConstants.INT_DT, Schema.Type.INT);
        primitives.put(XSConstants.BYTE_DT, Schema.Type.INT);
        primitives.put(XSConstants.SHORT_DT, Schema.Type.INT);
        primitives.put(XSConstants.UNSIGNEDBYTE_DT, Schema.Type.INT);
        primitives.put(XSConstants.UNSIGNEDSHORT_DT, Schema.Type.INT);

        primitives.put(XSConstants.INTEGER_DT, Schema.Type.STRING);
        primitives.put(XSConstants.NEGATIVEINTEGER_DT, Schema.Type.STRING);
        primitives.put(XSConstants.NONNEGATIVEINTEGER_DT, Schema.Type.STRING);
        primitives.put(XSConstants.POSITIVEINTEGER_DT, Schema.Type.STRING);
        primitives.put(XSConstants.NONPOSITIVEINTEGER_DT, Schema.Type.STRING);

        primitives.put(XSConstants.LONG_DT, Schema.Type.LONG);
        primitives.put(XSConstants.UNSIGNEDINT_DT, Schema.Type.LONG);

        primitives.put(XSConstants.FLOAT_DT, Schema.Type.FLOAT);

        primitives.put(XSConstants.DOUBLE_DT, Schema.Type.DOUBLE);
        primitives.put(XSConstants.DECIMAL_DT, Schema.Type.DOUBLE);

        primitives.put(XSConstants.DATETIME_DT, Schema.Type.LONG);
    }

    private Map<String, Schema> schemas = new LinkedHashMap<>();

    public boolean getDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    public Resolver getResolver() { return resolver; }
    public void setResolver(Resolver resolver) { this.resolver = resolver; }


    public Schema createSchema(String xsd) {
        return createSchema(new StringReader(xsd));
    }

    public Schema createSchema(File file) throws ConverterException {
        try (InputStream stream = new FileInputStream(file)) {
            return createSchema(stream);
        } catch (IOException e) {
            throw new ConverterException(e);
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
        if (resolver != null)
            loader.setEntityResolver(new EntityResolver(resolver));

        loader.setErrorHandler(errorHandler);
        loader.setParameter(Constants.DOM_ERROR_HANDLER, errorHandler);

        XSModel model = loader.load(input);

        errorHandler.throwExceptionIfHasError();
        return createSchema(model);
    }

    public Schema createSchema(XSModel model) {
        schemas.clear();

        Map<Source, Schema> schemas = new LinkedHashMap<>();
        XSNamedMap rootEls = model.getComponents(XSConstants.ELEMENT_DECLARATION);

        for (int i = 0; i < rootEls.getLength(); i++) {
            XSElementDeclaration el = (XSElementDeclaration) rootEls.item(i);
            XSTypeDefinition type = el.getTypeDefinition();

            debug("Processing root element " + el.getName() + "{" + el.getNamespace() + "}");
            Schema schema = createTypeSchema(type, false, false);
            schemas.put(new Source(el.getName()), schema);
        }

        if (schemas.size() == 0) throw new ConverterException("No root element declaration");
        if (schemas.size() == 1) return schemas.values().iterator().next();

        return createRootRecordSchema(schemas);
    }

    private Schema createRootRecordSchema(Map<Source, Schema> schemas) {
        List<Schema.Field> fields = new ArrayList<>();

        for (Source source : schemas.keySet()) {
            Schema schema = schemas.get(source);
            Schema nullSchema = Schema.create(Schema.Type.NULL);
            Schema optionalSchema = Schema.createUnion(Arrays.asList(nullSchema, schema));

            Schema.Field field = new Schema.Field(source.getName(), optionalSchema, null, null);
            field.addProp(Source.SOURCE, "" + source);
            fields.add(field);
        }

        Schema schema = Schema.createRecord(nextTypeName(), "", "", false);
        schema.setFields(fields);
        schema.addProp(Source.SOURCE, Source.DOCUMENT);
        return schema;
    }

    private int typeLevel;

    private Schema createTypeSchema(XSTypeDefinition type, boolean optional, boolean array) {
        typeLevel++;
        Schema schema;

        if (type.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE)
            schema = Schema.create(getPrimitiveType((XSSimpleTypeDefinition) type));
        else {
            String name = complexTypeName(type);
            debug("Creating schema for type " + name);

            schema = schemas.get(name);
            if (schema == null) schema = createRecordSchema(name, (XSComplexTypeDefinition) type);
        }

        if (array || isGroupTypeWithMultipleOccurs(type))
            schema = Schema.createArray(schema);
        else if (optional) {
            Schema nullSchema = Schema.create(Schema.Type.NULL);
            schema = Schema.createUnion(Arrays.asList(nullSchema, schema));
        }

        typeLevel--;
        return schema;
    }

    private boolean isGroupTypeWithMultipleOccurs(XSTypeDefinition type) {
      return type instanceof XSComplexTypeDefinition &&
              isGroupTypeWithMultipleOccurs(((XSComplexTypeDefinition) type).getParticle());
    }

    private boolean isGroupTypeWithMultipleOccurs(XSParticle particle) {
      if (particle == null) return false;

      XSTerm term = particle.getTerm();
      if (term.getType() != XSConstants.MODEL_GROUP) return false;

      XSModelGroup group = (XSModelGroup) term;
      final short compositor = group.getCompositor();
      switch(compositor) {
        case XSModelGroup.COMPOSITOR_CHOICE:
        case XSModelGroup.COMPOSITOR_SEQUENCE:
          return particle.getMaxOccurs() > 1 || particle.getMaxOccursUnbounded();
        default:
          return false;
      }
    }

    private Schema createGroupSchema(String name, XSModelGroup groupTerm) {
      Schema record = Schema.createRecord(name, null, null, false);
      schemas.put(name, record);

      Map<String, Schema.Field> fields = new HashMap<>();
      createGroupFields(groupTerm, fields, false);
      record.setFields(new ArrayList<>(fields.values()));

      return Schema.createArray(record);
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
            fields.put(field.getProp(Source.SOURCE), field);
        }

        XSParticle particle = type.getParticle();
        if (particle == null) return new ArrayList<>(fields.values());

        XSTerm term = particle.getTerm();
        if (term.getType() != XSConstants.MODEL_GROUP)
            throw new ConverterException("Unsupported term type " + term.getType());

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
                    fields.put(field.getProp(Source.SOURCE), field);
                    break;
                case XSConstants.MODEL_GROUP:
                    XSModelGroup subGroup = (XSModelGroup) term;
                    if (particle.getMaxOccurs() <= 1 && !particle.getMaxOccursUnbounded())
                        createGroupFields(subGroup, fields, forceOptional || insideChoice);
                    else {
                        String fieldName = nextTypeName();
                        fields.put(fieldName, new Schema.Field(fieldName, createGroupSchema(nextTypeName(), subGroup), null, null));
                    }
                    break;
                case XSConstants.WILDCARD:
                    field = createField(fields.values(), term, null, forceOptional || optional, array);
                    fields.put(field.getProp(Source.SOURCE), field);
                    break;
                default:
                    throw new ConverterException("Unsupported term type " + term.getType());
            }
        }
    }

    private Schema.Field createField(Iterable<Schema.Field> fields, XSObject source, XSTypeDefinition type, boolean optional, boolean array) {
        List<Short> supportedTypes = Arrays.asList(XSConstants.ELEMENT_DECLARATION, XSConstants.ATTRIBUTE_DECLARATION, XSConstants.WILDCARD);
        if (!supportedTypes.contains(source.getType()))
            throw new ConverterException("Invalid source object type " + source.getType());

        boolean wildcard = source.getType() == XSConstants.WILDCARD;
        if (wildcard) {
            Schema map = Schema.createMap(Schema.create(Schema.Type.STRING));
            return new Schema.Field(Source.WILDCARD, map, null, null);
        }

        Schema fieldSchema = createTypeSchema(type, optional, array);

        String name = validName(source.getName());
        name = uniqueFieldName(fields, name);

        Schema.Field field = new Schema.Field(name, fieldSchema, null, null);

        boolean attribute = source.getType() == XSConstants.ATTRIBUTE_DECLARATION;
        field.addProp(Source.SOURCE, "" + new Source(source.getName(), attribute));

        return field;
    }

    private Schema.Type getPrimitiveType(XSSimpleTypeDefinition type) {
        Schema.Type avroType = primitives.get(type.getBuiltInKind());
        return avroType == null ? Schema.Type.STRING : avroType;
    }

    static String uniqueFieldName(Iterable<Schema.Field> fields, String name) {
        int duplicates = 0;

        for (Schema.Field field : fields) {
            if (field.name().equals(name))
                duplicates++;
        }

        return name + (duplicates > 0 ? duplicates - 1 : "");
    }

    String complexTypeName(XSTypeDefinition type) {
        String name = validName(((XSComplexTypeDecl) type).getTypeName());
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

    private static class ErrorHandler implements XMLErrorHandler, DOMErrorHandler {
        XMLParseException exception;
        DOMError error;

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

        @Override
        public boolean handleError(DOMError error) {
            if (this.error == null) this.error = error;
            return false;
        }

        void throwExceptionIfHasError() {
            if (exception != null)
                throw new ConverterException(exception);

            if (error != null) {
                if (error.getRelatedException() instanceof Throwable)
                    throw new ConverterException((Throwable) error.getRelatedException());

                DOMLocator locator = error.getLocation();
                String location = "at:" + locator.getUri() + ", line:" + locator.getLineNumber() + ", char:" + locator.getColumnNumber();
                throw new ConverterException(location + " " + error.getMessage());
            }
        }
    }

    private class EntityResolver implements XMLEntityResolver {
        private Resolver resolver;
        private EntityResolver(Resolver resolver) { this.resolver = resolver; }

        @Override
        public XMLInputSource resolveEntity(XMLResourceIdentifier id) throws XNIException, IOException {
            String systemId = id.getLiteralSystemId();
            debug("Resolving " + systemId);

            XMLInputSource source = new XMLInputSource(id);
            source.setByteStream(resolver.getStream(systemId));
            return source;
        }
    }

    public static interface Resolver {
        InputStream getStream(String systemId);
    }
}
