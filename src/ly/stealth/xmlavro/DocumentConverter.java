package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.xs.*;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;

public class DocumentConverter {
    private static Element parse(File file) throws IOException, SAXException {
        try (InputStream stream = new FileInputStream(file)) {
            return parse(new InputSource(stream));
        }
    }

    private static Element parse(InputSource source) throws IOException, SAXException {
        try {
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return documentBuilder.parse(source).getDocumentElement();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private AvroTypes avroTypes;
    private Element el;

    public DocumentConverter(AvroTypes avroTypes, File file) throws IOException, SAXException { this(avroTypes, parse(file)); }

    private DocumentConverter(AvroTypes avroTypes, Element el) {
        this.el = el;
        this.avroTypes = avroTypes;
    }

    public void convert(File file) throws IOException {
        try (OutputStream stream = new FileOutputStream(file)) {
            convert(stream);
        }
    }

    public void convert(OutputStream stream) throws IOException {
        XSElementDeclaration elDecl = avroTypes.getSchema().getElementDeclaration(el.getTagName(), el.getNamespaceURI());
        // todo support simple types
        XSComplexTypeDefinition typeDef = (XSComplexTypeDefinition) elDecl.getTypeDefinition();

        Schema schema = avroTypes.getRecord(typeDef);
        GenericRecord record = convertComplexElement(el, typeDef);

        DatumWriter<GenericRecord> datumWriter = new SpecificDatumWriter<>(schema);

        try (DataFileWriter<GenericRecord> fileWriter = new DataFileWriter<>(datumWriter)) {
            fileWriter.create(schema, stream);
            fileWriter.append(record);
        }
    }

    private static XSTypeDefinition getElementTypeDef(XSComplexTypeDefinition typeDef, Element el) {
        XSParticle particle = typeDef.getParticle();
        XSTerm term = particle.getTerm();

        if (term.getType() != XSConstants.MODEL_GROUP)
            throw new UnsupportedOperationException("Unsupported term type " + term.getType());

        XSModelGroup group = (XSModelGroup) term;
        XSObjectList particles = group.getParticles();
        for (int i = 0; i < particles.getLength(); i++) {
            XSParticle groupParticle = (XSParticle) particles.item(i);
            XSTerm groupTerm = groupParticle.getTerm();

            if (groupTerm.getType() == XSConstants.ELEMENT_DECLARATION) {
                XSElementDeclaration elDecl = (XSElementDeclaration) groupTerm;
                boolean sameNames = elDecl.getName().equals(el.getTagName());
                boolean sameNamespaces = el.getNamespaceURI() == null ? elDecl.getNamespace() == null :
                        el.getNamespaceURI().equals(elDecl.getNamespace());

                if (sameNames && sameNamespaces) return elDecl.getTypeDefinition();
            } else
                throw new UnsupportedOperationException("Unsupported term type " + groupTerm.getType());
        }

        return null;
    }

    private GenericRecord convertComplexElement(Element el, XSComplexTypeDefinition typeDef) {
        Schema schema = avroTypes.getRecord(typeDef);
        GenericData.Record record = new GenericData.Record(schema);

        NodeList nodes = el.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;

            Element child = (Element) node;
            XSTypeDefinition childTypeDef = getElementTypeDef(typeDef, child);

            Object value;
            if (childTypeDef.getTypeCategory() == XSTypeDefinition.SIMPLE_TYPE) value = child.getTextContent();
            else value = convertComplexElement(child, (XSComplexTypeDefinition) childTypeDef);

            record.put(child.getTagName(), value);
        }

        return record;
    }

    public static void main(String[] args) throws IOException, SAXException {
        AvroTypes types = new AvroTypes(new File("test.xsd"));
        for (Schema schema : types.getRecords().values()) {
            System.out.println(schema.toString(true));
        }

        DocumentConverter converter = new DocumentConverter(types, new File("test.xml"));
        converter.convert(new File("test.avro"));
    }
}
