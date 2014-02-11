package ly.stealth.xmlavro;

public interface Datum {
    Type getType();

    Object toAvroDatum();

    interface Type {
        QName getQName();

        boolean isAnonymous();

        boolean isPrimitive();

        org.apache.avro.Schema toAvroSchema();
    }
}
