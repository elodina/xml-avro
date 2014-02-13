package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.junit.Test;

import static junit.framework.Assert.*;

public class TypeBuilderTest {
    @Test
    public void basic() {
        String xsd = "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                     "  <xsd:element name='root' type='xsd:string'/>" +
                     "</xsd:schema>";

        new TypeBuilder(xsd);

        try { // no namespace
            new TypeBuilder("<schema/>");
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

        assertEquals(Value.Type.INT, Datum.Type.create(xsd));
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

        Record.Type record = Datum.Type.create(xsd);
        assertEquals(new QName("type"), record.getQName());

        assertEquals(2, record.getFields().size());

        Record.Field field0 = record.getFields().get(0);
        assertEquals("s", field0.getAvroName());
        assertEquals(Value.Type.STRING, field0.getType());

        Record.Field field1 = record.getFields().get(1);
        assertEquals("i", field1.getAvroName());
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

        Record.Type record = Datum.Type.create(xsd);
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

        Record.Type record = Datum.Type.create(xsd);

        Record.Field field = record.getField("node");
        Record.Type subRecord = field.getType();
        assertSame(record, subRecord);
    }

    @Test
    public void complexTypeAttributes() {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "  <xsd:complexType name='type'>" +
                "    <xsd:sequence>" +
                "      <xsd:element name='element' type='xsd:string'/>" +
                "    </xsd:sequence>" +
                "    <xsd:attribute name='attribute' type='xsd:string'/>" +
                "  </xsd:complexType>" +
                "  <xsd:element name='root' type='type'/>" +
                "</xsd:schema>";

        Record.Type record = Datum.Type.create(xsd);
        assertEquals(2, record.getFields().size());
        assertNotNull(record.getField("element"));

        Record.Field attrField = record.getField("attribute", true);
        assertNotNull(attrField);

        assertEquals("attribute", attrField.getAvroName());
        assertEquals(Value.Type.STRING, attrField.getType());
    }

    @Test
    public void uniqueFieldNames() {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "  <xsd:complexType name='type'>" +
                "    <xsd:sequence>" +
                "      <xsd:element name='field' type='xsd:string'/>" +
                "    </xsd:sequence>" +
                "    <xsd:attribute name='field' type='xsd:string'/>" +
                "  </xsd:complexType>" +
                "  <xsd:element name='root' type='type'/>" +
                "</xsd:schema>";

        Record.Type type = Datum.Type.create(xsd);

        assertEquals(2, type.getFields().size());
        Record.Field field = type.getField("field");
        assertEquals("field", field.getAvroName());

        Record.Field field0 = type.getField("field", true);
        assertEquals("field0", field0.getAvroName());
    }

    @Test
    public void recordWithWildcardField() {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "  <xsd:complexType name='type'>" +
                "    <xsd:sequence>" +
                "      <xsd:element name='field' type='xsd:string'/>" +
                "      <xsd:any/>" +
                "    </xsd:sequence>" +
                "  </xsd:complexType>" +
                "  <xsd:element name='root' type='type'/>" +
                "</xsd:schema>";

        Record.Type record = Datum.Type.create(xsd);
        assertTrue(record.supportsAnyElement());
        assertEquals(2, record.getFields().size());

        Record.Field wildcardField = record.getFields().get(1);
        assertNull(wildcardField.getSource().getQName());
        assertTrue(wildcardField.getSource().isWildcard());
        assertTrue("" + wildcardField.getType(), wildcardField.getType() instanceof Map.Type);
    }
}
