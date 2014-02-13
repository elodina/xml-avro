package ly.stealth.xmlavro;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.*;

public abstract class Datum {
    public static <D extends Datum> D create(Type type, File file) { return new DatumBuilder(type, file).createDatum(); }
    public static <D extends Datum> D create(Type type, String xml) { return new DatumBuilder(type, xml).createDatum(); }
    public static <D extends Datum> D create(Type type, Reader reader) { return new DatumBuilder(type, reader).createDatum(); }
    public static <D extends Datum> D create(Type type, InputStream stream) { return new DatumBuilder(type, stream).createDatum(); }

    public abstract Type getType();

    public abstract Object toAvroDatum();

    public void write(File file) throws IOException {
        try (OutputStream stream = new FileOutputStream(file)) {
            write(stream);
        }
    }

    public void write(OutputStream stream) throws IOException {
        DatumWriter<Object> datumWriter = new SpecificDatumWriter<>(getType().toAvroSchema());
        datumWriter.write(toAvroDatum(), EncoderFactory.get().directBinaryEncoder(stream, null));
    }

    public static abstract class Type {
        public static <T extends Type> T create(String xsd) { return new TypeBuilder(xsd).createType(); }
        public static <T extends Type> T create(File file) throws IOException { return new TypeBuilder(file).createType(); }
        public static <T extends Type> T create(Reader reader) { return new TypeBuilder(reader).createType(); }
        public static <T extends Type> T create(InputStream stream) { return new TypeBuilder(stream).createType(); }

        public abstract QName getQName();

        public abstract boolean isAnonymous();

        public abstract boolean isPrimitive();

        public abstract org.apache.avro.Schema toAvroSchema();

        public String toJson(boolean pretty) {
            return this.toAvroSchema().toString(pretty);
        }

        public void write(File file) throws IOException {
            try (OutputStream stream = new FileOutputStream(file)) {
                write(stream);
            }
        }

        public void write(OutputStream stream) throws IOException {
            stream.write(toJson(true).getBytes("UTF-8"));
        }
    }
}
