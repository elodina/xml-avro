package ly.stealth.xmlavro;

import java.util.Objects;

public class QName {
    private String name, namespace;

    public QName(String name) { this(name, null); }

    public QName(String name, String namespace) {
        if (name == null) throw new NullPointerException("qName");
        this.name = name;
        this.namespace = namespace;
    }

    public String getName() { return name; }
    public String getNamespace() { return namespace; }

    public int hashCode() {
        return Objects.hash(name, namespace);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof QName)) return false;
        QName qName = (QName) obj;
        return name.equals(qName.name) && Objects.equals(namespace, qName.namespace);
    }

    public String toString() {
        return (namespace != null ? "{" + namespace + "}" : "") + name;
    }
}
