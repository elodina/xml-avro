package ly.stealth.xmlavro;

import org.apache.avro.Schema;

import java.util.HashMap;

public class Map extends Datum {
    private Type type;

    private java.util.Map<String, Datum> values = new HashMap<>();

    public Map(Type type) {
        this.type = type;
    }

    public Datum getValue(String key) { return values.get(key); }

    public void setValue(String key, Datum value) {
        if (value != null) values.put(key, value);
        else values.remove(key);
    }

    public Object getValueObject(String key) {
        Datum datum = getValue(key);
        return datum instanceof Value ? ((Value) datum).getObject() : null;
    }

    public int size() { return values.size(); }

    @Override
    public Type getType() { return type; }

    @Override
    public Object toAvroDatum() {
        java.util.Map<String, Object> result = new HashMap<>();

        for (String key : values.keySet()) {
            Datum value = values.get(key);
            result.put(key, value.toAvroDatum());
        }

        return result;
    }

    public static class Type extends Datum.Type {
        private Value.Type valueType;

        public Type(Value.Type valueType) {
            this.valueType = valueType;
        }

        @Override
        public QName getQName() { return null; }

        @Override
        public boolean isAnonymous() { return true; }

        @Override
        public boolean isPrimitive() { return false; }

        @Override
        public Schema toAvroSchema() {
            return Schema.createMap(valueType.toAvroSchema());
        }
    }
}
