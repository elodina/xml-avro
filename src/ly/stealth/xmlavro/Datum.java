package ly.stealth.xmlavro;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

public interface Datum {
    Type getType();

    Object toAvroDatum();

    public abstract class Type {
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
    }
}
