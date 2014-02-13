package ly.stealth.xmlavro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.util.*;
import java.util.Map;

public class Record extends Datum {
    private Type type;
    private Map<Field, Datum> datums = new HashMap<>();

    public Record(Type type) {
        this.type = type;
    }

    public Type getType() { return type; }

    public Map<Field, Datum> getDatums() { return Collections.unmodifiableMap(datums); }

    @SuppressWarnings("unchecked")
    public <D extends Datum> D getDatum(Field field) { return (D) datums.get(field); }

    public void setDatum(Field field, Datum datum) {
        if (!type.getFields().contains(field))
            throw new IllegalArgumentException("Unknown field " + field);

        datums.put(field, datum);
    }

    public Object getValue(Field field) {
        Datum datum = getDatum(field);
        return datum instanceof Value ? ((Value)datum).getObject() : null;
    }

    public Object toAvroDatum() {
        GenericRecord record = new GenericData.Record(type.toAvroSchema());

        for (Field field : datums.keySet())
            record.put(field.getAvroName(), datums.get(field).toAvroDatum());

        return record;
    }

    public static class Type extends Datum.Type {
        private QName qName;
        private List<Field> fields = new ArrayList<>();

        public Type(QName qName) {
            this.qName = qName;
        }

        public QName getQName() { return qName; }

        public Field getField(String name) { return getField(name, false); }

        public Field getField(String name, boolean attribute) { return getField(new QName(name), attribute); }

        public Field getField(QName qName) { return getField(qName, false); }

        public Field getField(QName qName, boolean attribute) { return getField(new Source(qName, attribute)); }

        public Field getField(Source source) {
            for (Field field : fields)
                if (field.getSource().equals(source))
                    return field;

            return null;
        }

        public Field getAnyElementField() { return getField(new Source(null, false)); }
        public Field getAnyAttributeField() { return getField(new Source(null, true)); }

        public boolean supportsAnyElement() { return getAnyElementField() != null; }
        public boolean supportsAnyAttribute() { return getAnyAttributeField() != null; }

        public List<Field> getFields() { return Collections.unmodifiableList(fields); }

        public void setFields(Collection<Field> fields) {
            for (Field field : new ArrayList<>(fields)) removeField(field);
            for (Field field : fields) addField(field);
        }

        public void addField(Field field) {
            field.parent = this;
            fields.add(field);
        }

        public void removeField(Field field) {
            fields.remove(field);
            field.parent = null;
        }

        @Override
        public boolean isAnonymous() { return qName == null; }

        @Override
        public boolean isPrimitive() { return false; }

        @Override
        public Schema toAvroSchema() {
            String name = qName != null ? qName.getName() : null;
            String namespace = qName != null ? qName.getNamespace() : null;
            Schema schema = org.apache.avro.Schema.createRecord(name, null, namespace, false);

            List<Schema.Field> fields = new ArrayList<>();
            for (Field field : this.fields) {
                Schema.Field f = new Schema.Field(field.getAvroName(), field.getType().toAvroSchema(), null, null);
                f.addProp("source", "" + field.getSource());
                fields.add(f);
            }

            schema.setFields(fields);
            return schema;
        }
    }

    public static class Field {
        private Record.Type parent;

        private Source source;
        private Datum.Type type;

        private String doc;

        public Field(Source source, Datum.Type type) {
            this.source = source;
            this.type = type;
        }

        public Source getSource() { return source; }

        @SuppressWarnings("unchecked")
        public <T extends Datum.Type> T getType() { return (T) type; }


        public String getDoc() { return doc; }
        public void setDoc(String doc) { this.doc = doc; }


        public String getAvroName() {
            String name = source.getAvroBaseName();
            int duplicates = 0;

            for (Field field : parent.getFields()) {
                if (field.equals(this)) break;

                if (field.getSource().getAvroBaseName().equals(name))
                    duplicates ++;
            }

            return name + (duplicates > 0 ? duplicates - 1 : "");
        }
    }

    public static class Source {
        // qName of element/attribute or null for any element/attribute
        private QName qName;
        // element or attribute
        private boolean attribute;

        public Source(String name) { this(new QName(name)); }

        public Source(QName qName) { this(qName, false); }

        public Source(QName qName, boolean attribute) {
            this.qName = qName;
            this.attribute = attribute;
        }


        public QName getQName() { return qName; }

        public boolean isElement() { return !isAttribute(); }
        public boolean isAttribute() { return attribute; }

        public boolean isWildcard() { return qName == null; }

        public String getAvroBaseName() {
            if (qName == null) return attribute ? "anyAttribute" : "anyElement";

            String name = qName.getName();
            name = name.replace(".", "_");

            return name;
        }


        public int hashCode() { return Objects.hash(qName, attribute); }

        public boolean equals(Object obj) {
            if (!(obj instanceof Source)) return false;
            Source source = (Source) obj;
            return Objects.equals(qName, source.qName) && attribute == source.attribute;
        }

        public String toString() {
            return (qName != null ? qName : "*") + " " + (attribute ? "attribute" : "element");
        }
    }
}
