package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.junit.Test;

import static junit.framework.Assert.*;

public class AvroSchemaTest {
    @Test
    public void basic() {
        String xsd = "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                     "  <xsd:element name='root' type='xsd:string'/>" +
                     "</xsd:schema>";

        new AvroSchema(xsd);

        try { // no namespace
            new AvroSchema("<schema/>");
            fail();
        } catch (XMLParseException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("http://www.w3.org/2001/XMLSchema"));
            assertTrue(message, message.contains("namespace"));
        }
    }

    @Test
    public void rootPrimitive() {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "   <xsd:element name='i' type='xsd:int'/>" +
                "</xsd:schema>";

        AvroSchema schema = new AvroSchema(xsd);
        assertEquals(AvroSchema.Primitive.INT, schema.getRootType("i"));
    }

    @Test
    public void rootRecord() {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "  <xsd:complexType name='type'>" +
                "    <xsd:sequence>" +
                "      <xsd:element name='s' type='xsd:string'/>" +
                "      <xsd:element name='i' type='xsd:int'/>" +
                "    </xsd:sequence>" +
                "  </xsd:complexType>" +
                "  <xsd:element name='root' type='type'/>" +
                "</xsd:schema>";

        AvroSchema schema = new AvroSchema(xsd);

        AvroSchema.Record record = schema.getRootType("root");
        assertEquals(new AvroSchema.QName("type"), record.getQName());

        assertEquals(2, record.getFields().size());

        AvroSchema.Field field0 = record.getFields().get(0);
        assertEquals("s", field0.getName());
        assertEquals(AvroSchema.Primitive.STRING, field0.getType());

        AvroSchema.Field field1 = record.getFields().get(1);
        assertEquals("i", field1.getName());
        assertEquals(AvroSchema.Primitive.INT, field1.getType());
    }

    @Test
    public void nestedRecords() {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "  <xsd:complexType name='outer'>" +
                "    <xsd:sequence>" +
                "      <xsd:element name='inner' type='inner'/>" +
                "    </xsd:sequence>" +
                "  </xsd:complexType>" +
                "  <xsd:complexType name='inner'>" +
                "    <xsd:sequence>" +
                "      <xsd:element name='value' type='xsd:string'/>" +
                "    </xsd:sequence>" +
                "  </xsd:complexType>" +
                "  <xsd:element name='root' type='outer'/>" +
                "</xsd:schema>";

        AvroSchema schema = new AvroSchema(xsd);

        AvroSchema.Record record = schema.getRootType("root");
        assertEquals(new AvroSchema.QName("outer"), record.getQName());

        AvroSchema.Field innerField = record.getField("inner");
        assertTrue(innerField.getType().getClass().getName(), innerField.getType() instanceof AvroSchema.Record);

        AvroSchema.Record innerRecord = innerField.getType();
        assertEquals("inner", innerRecord.getQName().getName());
    }

    @Test
    public void nestedRecursiveRecords() {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "  <xsd:complexType name='type'>" +
                "    <xsd:sequence>" +
                "      <xsd:element name='node' type='type' minOccurs='0'/>" +
                "    </xsd:sequence>" +
                "  </xsd:complexType>" +
                "  <xsd:element name='root' type='type'/>" +
                "</xsd:schema>";

        AvroSchema.Record record = new AvroSchema(xsd).getRootType("root");

        AvroSchema.Field field = record.getField("node");
        AvroSchema.Record subRecord = field.getType();
        assertSame(record, subRecord);
    }
}
