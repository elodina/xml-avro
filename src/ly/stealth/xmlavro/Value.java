package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import org.apache.avro.Schema;

public class Value implements Datum {
    private Type type;
    private Object object;

    public Value(Type type, Object object) {
        this.type = type;
        this.object = object;
    }

    public Type getType() { return type; }

    public Object getObject() { return object; }

    public Object toAvroDatum() { return getObject(); }

    public static enum Type implements Datum.Type {
        NULL,       // no value
        BOOLEAN,    // a binary value
        INT,        // 32-bit signed integer
        LONG,       // 64-bit signed integer
        FLOAT,      // single precision (32-bit) IEEE 754 floating-point number
        DOUBLE,     // double precision (64-bit) IEEE 754 floating-point number
        BYTES,      // sequence of 8-bit unsigned bytes
        STRING;     // unicode character sequence

        static Type valueOf(XSSimpleTypeDefinition type) {
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

        private Schema.Type toAvroType() {
            switch (this) {
                case NULL:
                    return Schema.Type.NULL;
                case BOOLEAN:
                    return Schema.Type.BOOLEAN;
                case INT:
                    return Schema.Type.INT;
                case LONG:
                    return Schema.Type.LONG;
                case FLOAT:
                    return Schema.Type.FLOAT;
                case DOUBLE:
                    return Schema.Type.DOUBLE;
                case BYTES:
                    return Schema.Type.BYTES;
                case STRING:
                    return Schema.Type.STRING;
                default:
                    throw new UnsupportedOperationException("Unsupported type " + this);
            }
        }

        @Override
        public Schema toAvroSchema() {
            return Schema.create(toAvroType());
        }
    }
}
