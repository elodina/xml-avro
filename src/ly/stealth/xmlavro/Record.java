package ly.stealth.xmlavro;

import java.util.*;

public class Record implements Datum {
    private Type type;
    Map<String, Datum> datums = new HashMap<>();

    public Record(Type type) {
        this.type = type;
    }

    public Type getType() { return type; }

    public Map<String, Datum> getDatums() { return Collections.unmodifiableMap(datums); }

    public Datum getDatum(String name) { return datums.get(name); }

    public Object getValue(String name) {
        Datum datum = getDatum(name);
        return datum instanceof Value ? ((Value)datum).getObject() : null;
    }

    public static class Type implements Datum.Type {
        private QName qName;
        List<Field> fields = new ArrayList<>();

        public Type(QName qName) {
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
        public boolean isAnonymous() { return qName == null; }

        @Override
        public boolean isPrimitive() { return false; }
    }

    public static class Field {
        private String name;
        private Datum.Type type;
        private String doc;

        public Field(String name, Datum.Type type) { this(name, type, null); }

        public Field(String name, Datum.Type type, String doc) {
            this.name = name;
            this.type = type;
            this.doc = doc;
        }

        public String getName() { return name; }

        @SuppressWarnings("unchecked")
        public <T extends Datum.Type> T getType() { return (T) type; }

        public String getDoc() { return doc; }
    }
}
