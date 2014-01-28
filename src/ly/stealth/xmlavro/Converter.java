package ly.stealth.xmlavro;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Converter {
    private static Schema schema;
    static {
        try {
            schema = new Schema.Parser().parse(new File("src/ly/stealth/xmlavro/xml.avsc")); // todo fixme
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void convertXml(File xmlFile, File avroFile) throws IOException, SAXException {
        Document doc = parse(xmlFile);
        DatumWriter<GenericRecord> datumWriter = new SpecificDatumWriter<>(schema);

        try (DataFileWriter<GenericRecord> fileWriter = new DataFileWriter<>(datumWriter)) {
            fileWriter.create(schema, avroFile);
            fileWriter.append(wrap(doc.getDocumentElement()));
        }
    }

    private static GenericData.Record wrap(Element el) {
        GenericData.Record record = new GenericData.Record(schema);
        record.put("name", el.getNodeName());

        List<GenericData.Record> childRecords = new ArrayList<>();
        NodeList childNodes = el.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
                childRecords.add(wrap((Element) node));
        }

        record.put("children", childRecords);
        return record;
    }

    private static Document parse(File file) throws IOException, SAXException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return builder.parse(file);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void convertAvro(File avroFile, File xmlFile) throws IOException {
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(schema);
        DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(avroFile, datumReader);

        GenericRecord record = dataFileReader.next();

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        Element el = unwrap(record, doc);
        doc.appendChild(el);

        saveDocument(doc, xmlFile);
    }

    private static Element unwrap(GenericRecord record, Document doc) {
        String name = "" + record.get("name");
        Element el = doc.createElement(name);

        @SuppressWarnings("unchecked")
        GenericArray<GenericRecord> childArray = (GenericArray<GenericRecord>) record.get("children");

        for (GenericRecord childRecord : childArray) {
            Element childEl = unwrap(childRecord, doc);
            el.appendChild(childEl);
        }

        return el;
    }

    private static void saveDocument(Document doc, File file) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(file));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, SAXException {
        convertXml(new File("1.xml"), new File("1.avro"));
        convertAvro(new File("1.avro"), new File("1_.xml"));
    }
}
