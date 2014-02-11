package ly.stealth.xmlavro;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;

public class Converter {
    private Schema schema;

    public Converter(Schema schema) {
        this.schema = schema;
    }

    public <D extends Datum> D convert(String s) throws SAXException {
        try { return convert(new StringReader(s)); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    public <D extends Datum> D convert(Reader reader) throws IOException, SAXException { return convert(new InputSource(reader)); }

    public <D extends Datum> D convert(InputStream stream) throws IOException, SAXException { return convert(new InputSource(stream)); }

    public <D extends Datum> D convert(InputSource source) throws IOException, SAXException {
        try {
            DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = docBuilder.parse(source);
            return convert(doc.getDocumentElement());
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public <D extends Datum> D convert(Element el) {
        Datum.Type type = schema.getRootType(new QName(el.getTagName(), el.getNamespaceURI()));
        return createDatum(type, el);
    }

    private <D extends Datum> D createDatum(Datum.Type type, Element el) {
        @SuppressWarnings("unchecked")
        D datum = (D) (type.isPrimitive() ? createValue((Value.Type) type, el) : createRecord((Record.Type) type, el));
        return datum;
    }

    private Value createValue(Value.Type type, Element el) {
        String text = el.getTextContent();
        return new Value(type, parseValue(type, text));
    }

    private  Record createRecord(Record.Type type, Element el) {
        Record record = new Record(type);

        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element child = (Element) node;

            Record.Field field = type.getField(child.getTagName());
            record.datums.put(field.getName(), createDatum(field.getType(), child));
        }

        return record;
    }

    private  Object parseValue(Value.Type type, String text) {
        switch (type) {
            case BOOLEAN:
                return "true".equals(text) || "1".equals(text);
            case INT:
                return Integer.parseInt(text);
            case LONG:
                return Long.parseLong(text);
            case FLOAT:
                return Float.parseFloat(text);
            case DOUBLE:
                return Double.parseDouble(text);
            case STRING:
                return text;
            default:
                throw new UnsupportedOperationException("Unsupported type " + type);
        }
    }

}
