package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.junit.Test;

import static junit.framework.Assert.*;

public class SchemaTest {
    @Test
    public void basic() {
        String xsd = "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                     "  <xsd:element name='root' type='xsd:string'/>" +
                     "</xsd:schema>";

        new Schema(xsd);

        try { // no namespace
            new Schema("<schema/>");
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

        Schema schema = new Schema(xsd);
        assertEquals(Value.Type.INT, schema.getRootType("i"));
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

        Schema schema = new Schema(xsd);

        Record.Type record = schema.getRootType("root");
        assertEquals(new QName("type"), record.getQName());

        assertEquals(2, record.getFields().size());

        Record.Field field0 = record.getFields().get(0);
        assertEquals("s", field0.getName());
        assertEquals(Value.Type.STRING, field0.getType());

        Record.Field field1 = record.getFields().get(1);
        assertEquals("i", field1.getName());
        assertEquals(Value.Type.INT, field1.getType());
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

        Schema schema = new Schema(xsd);

        Record.Type record = schema.getRootType("root");
        assertEquals(new QName("outer"), record.getQName());

        Record.Field innerField = record.getField("inner");
        assertTrue(innerField.getType().getClass().getName(), innerField.getType() instanceof Record.Type);

        Record.Type innerRecord = innerField.getType();
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

        Record.Type record = new Schema(xsd).getRootType("root");

        Record.Field field = record.getField("node");
        Record.Type subRecord = field.getType();
        assertSame(record, subRecord);
    }
}
