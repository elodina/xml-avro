package ly.stealth.xmlavro;

import com.sun.org.apache.xerces.internal.xni.parser.XMLParseException;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.junit.Test;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

public class ConverterTest {
    @Test
    public void basic() {
        String xsd = "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                     "  <xsd:element name='root' type='xsd:string'/>" +
                     "</xsd:schema>";

        Converter.createSchema(xsd);

        try { // no namespace
            Converter.createSchema("<schema/>");
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

        Schema schema = Converter.createSchema(xsd);
        assertEquals(Schema.Type.INT, schema.getType());

        String xml = "<i>1</i>";
        assertEquals(1, Converter.createDatum(schema, xml));
    }

    @Test
    public void rootRecord() {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "   <xsd:element name='root'>" +
                "     <xsd:complexType>" +
                "       <xsd:sequence>" +
                "         <xsd:element name='i' type='xsd:int'/>" +
                "         <xsd:element name='s' type='xsd:string'/>" +
                "         <xsd:element name='d' type='xsd:double'/>" +
                "       </xsd:sequence>" +
                "     </xsd:complexType>" +
                "   </xsd:element>" +
                "</xsd:schema>";

        Schema schema = Converter.createSchema(xsd);
        assertEquals(Schema.Type.RECORD, schema.getType());
        assertEquals(3, schema.getFields().size());

        assertEquals(Schema.Type.INT, schema.getField("i").schema().getType());
        assertEquals(Schema.Type.STRING, schema.getField("s").schema().getType());
        assertEquals(Schema.Type.DOUBLE, schema.getField("d").schema().getType());

        String xml =
                "<root>" +
                "  <i>1</i>" +
                "  <s>s</s>" +
                "  <d>1.0</d>" +
                "</root>";

        GenericData.Record record = Converter.createDatum(schema, xml);

        assertEquals(1, record.get("i"));
        assertEquals("s", record.get("s"));
        assertEquals(1.0, record.get("d"));
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

        Schema schema = Converter.createSchema(xsd);

        Schema.Field field = schema.getField("node");
        Schema subRecord = field.schema();
        assertSame(schema, subRecord);

        String xml = "<root><node></node></root>";
        GenericData.Record record = Converter.createDatum(schema, xml);

        GenericData.Record child = (GenericData.Record) record.get("node");
        assertEquals(record.getSchema(), child.getSchema());

        assertNull(child.get("node"));
    }

    @Test
    public void attributes() {
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

        Schema schema = Converter.createSchema(xsd);
        assertEquals(2, schema.getFields().size());
        assertNotNull(schema.getField("element"));

        Schema.Field attrField = schema.getField("attribute");
        assertNotNull(attrField);
        assertEquals(Schema.Type.STRING, attrField.schema().getType());

        String xml =
                "<root attribute='attribute'>" +
                "  <element>element</element>" +
                "</root>";

        GenericData.Record record = Converter.createDatum(schema, xml);
        assertEquals("element", record.get("element"));
        assertEquals("attribute", record.get("attribute"));
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

        Schema schema = Converter.createSchema(xsd);

        assertEquals(2, schema.getFields().size());
        assertNotNull(schema.getField("field"));

        Schema.Field field0 = schema.getField("field0");
        assertEquals("" + new Converter.Source("field", true), field0.getProp(Converter.SOURCE));

        String xml = "<root field='value0'><field>value</field></root>";
        GenericData.Record record = Converter.createDatum(schema, xml);

        assertEquals("value", record.get("field"));
        assertEquals("value0", record.get("field0"));
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

        Schema schema = Converter.createSchema(xsd);
        assertEquals(2, schema.getFields().size());

        Schema.Field wildcardField = schema.getField(Converter.OTHERS);
        assertEquals(Schema.Type.MAP, wildcardField.schema().getType());

        String xml =
                "<root>" +
                "  <field>field</field>" +
                "  <field0>field0</field0>" +
                "  <field1>field1</field1>" +
                "</root>";

        GenericData.Record record = Converter.createDatum(schema, xml);
        assertEquals("field", record.get("field"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> map = (java.util.Map<String, String>) record.get(Converter.OTHERS);

        assertEquals(2, map.size());
        assertEquals("field0", map.get("field0"));
        assertEquals("field1", map.get("field1"));
    }
}
