package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.apache.avro.Schema;
import org.junit.Test;

import static junit.framework.Assert.*;

public class SchemaBuilderTest {
    @Test
    public void basic() {
        String xsd = "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                     "  <xsd:element name='root' type='xsd:string'/>" +
                     "</xsd:schema>";

        new SchemaBuilder(xsd);

        try { // no namespace
            new SchemaBuilder("<schema/>");
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

        assertEquals(Schema.create(Schema.Type.INT), SchemaBuilder.createSchema(xsd));
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

        Schema record = SchemaBuilder.createSchema(xsd);
        assertEquals("type", record.getName());

        assertEquals(2, record.getFields().size());

        Schema.Field field0 = record.getFields().get(0);
        assertEquals("s", field0.name());
        assertEquals(Schema.Type.STRING, field0.schema().getType());

        Schema.Field field1 = record.getFields().get(1);
        assertEquals("i", field1.name());
        assertEquals(Schema.Type.INT, field1.schema().getType());
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

        Schema record = SchemaBuilder.createSchema(xsd);
        assertEquals("outer", record.getName());

        Schema.Field innerField = record.getField("inner");
        assertEquals(Schema.Type.RECORD, innerField.schema().getType());

        Schema innerRecord = innerField.schema();
        assertEquals("inner", innerRecord.getName());
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

        Schema record = SchemaBuilder.createSchema(xsd);

        Schema.Field field = record.getField("node");
        Schema subRecord = field.schema();
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

        Schema record = SchemaBuilder.createSchema(xsd);
        assertEquals(2, record.getFields().size());
        assertNotNull(record.getField("element"));

        Schema.Field attrField = record.getField("attribute");
        assertNotNull(attrField);

        assertEquals(Schema.Type.STRING, attrField.schema().getType());
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

        Schema schema = SchemaBuilder.createSchema(xsd);

        assertEquals(2, schema.getFields().size());
        assertNotNull(schema.getField("field"));

        Schema.Field field0 = schema.getField("field0");
        assertEquals("" + new SchemaBuilder.Source("field", true), field0.getProp(SchemaBuilder.SOURCE));
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

        Schema record = SchemaBuilder.createSchema(xsd);
        assertEquals(2, record.getFields().size());

        Schema.Field wildcardField = record.getField(SchemaBuilder.OTHERS);
        assertEquals(Schema.Type.MAP, wildcardField.schema().getType());
    }
}
