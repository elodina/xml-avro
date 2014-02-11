package ly.stealth.xmlavro;

public interface Datum {
    interface Type {
        QName getQName();
        boolean isAnonymous();
        boolean isPrimitive();
    }
}
