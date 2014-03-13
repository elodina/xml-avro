package ly.stealth.xmlavro;

import java.util.Objects;

class Source {
    public static final String SOURCE = "source";
    public static final String DOCUMENT = "document";
    public static final String WILDCARD = "others";

    // name of element/attribute
    private String name;
    // element or attribute
    private boolean attribute;

    public Source(String name) { this(name, false); }

    public Source(String name, boolean attribute) {
        this.name = name;
        this.attribute = attribute;
    }

    public String getName() { return name; }

    public boolean isElement() { return !isAttribute(); }
    public boolean isAttribute() { return attribute; }

    public int hashCode() { return Objects.hash(name, attribute); }

    public boolean equals(Object obj) {
        if (!(obj instanceof Source)) return false;
        Source source = (Source) obj;
        return name.equals(source.name) && attribute == source.attribute;
    }

    public String toString() {
        return (attribute ? "attribute" : "element") + " " + name;
    }
}
