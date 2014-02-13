package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.xs.XSConstants;
import com.sun.org.apache.xerces.internal.xs.XSSimpleTypeDefinition;
import org.apache.avro.Schema;

public class Value extends Datum {
    private Type type;
    private Object object;

    public Value(Type type, Object object) {
        this.type = type;
        this.object = object;
    }

    public Type getType() { return type; }

    public Object getObject() { return object; }

    public Object toAvroDatum() { return getObject(); }

    public static class Type extends Datum.Type {
        public static final Type NULL = new Type("null", Schema.Type.NULL);
        public static final Type BOOLEAN = new Type("boolean", Schema.Type.BOOLEAN);
        public static final Type INT = new Type("int", Schema.Type.INT);
        public static final Type LONG = new Type("long", Schema.Type.LONG);
        public static final Type FLOAT = new Type("float", Schema.Type.FLOAT);
        public static final Type DOUBLE = new Type("double", Schema.Type.DOUBLE);
        public static final Type BYTES = new Type("bytes", Schema.Type.BYTES);
        public static final Type STRING = new Type("string", Schema.Type.STRING);

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

        private String name;
        private Schema.Type avroType;

        private Type(String name, Schema.Type avroType) {
            this.name = name;
            this.avroType = avroType;
        }

        @Override
        public QName getQName() { return new QName(name); }

        @Override
        public boolean isAnonymous() { return false; }

        @Override
        public boolean isPrimitive() { return true; }

        @Override
        public Schema toAvroSchema() { return Schema.create(avroType); }
    }
}
