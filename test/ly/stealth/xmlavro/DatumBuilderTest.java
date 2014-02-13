package ly.stealth.xmlavro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.junit.Test;
import org.xml.sax.SAXException;

import static junit.framework.Assert.assertEquals;

public class DatumBuilderTest {
    @Test
    public void rootPrimitive() throws SAXException {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "   <xsd:element name='i' type='xsd:int'/>" +
                "</xsd:schema>";

        String xml = "<i>1</i>";

        Schema schema = SchemaBuilder.createSchema(xsd);
        Object datum = DatumBuilder.createDatum(schema, xml);
        assertEquals(1, datum);
    }

    @Test
    public void rootRecord() throws SAXException {
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

        String xml =
                "<root>" +
                "  <i>1</i>" +
                "  <s>s</s>" +
                "  <d>1.0</d>" +
                "</root>";

        Schema schema = SchemaBuilder.createSchema(xsd);
        GenericData.Record record = DatumBuilder.createDatum(schema, xml);

        assertEquals(1, record.get("i"));
        assertEquals("s", record.get("s"));
        assertEquals(1.0, record.get("d"));
    }

    @Test
    public void attributes() throws SAXException {
        String xsd =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema'>" +
                "   <xsd:element name='user'>" +
                "     <xsd:complexType>" +
                "       <xsd:sequence>" +
                "         <xsd:element name='name' type='xsd:string'/>" +
                "       </xsd:sequence>" +
                "       <xsd:attribute name='id' type='xsd:string'/>" +
                "     </xsd:complexType>" +
                "   </xsd:element>" +
                "</xsd:schema>";

        String xml =
                "<user id='id'>" +
                "  <name>name</name>" +
                "</user>";

        Schema schema = SchemaBuilder.createSchema(xsd);
        GenericData.Record record = DatumBuilder.createDatum(schema, xml);

        assertEquals("id", record.get("id"));
        assertEquals("name", record.get("name"));
    }

    @Test
    public void uniqueFieldNames() throws SAXException {
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

        String xml = "<root field='value0'><field>value</field></root>";

        Schema schema = SchemaBuilder.createSchema(xsd);
        GenericData.Record record = DatumBuilder.createDatum(schema, xml);

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

        String xml =
                "<root>" +
                "  <field>field</field>" +
                "  <field0>field0</field0>" +
                "  <field1>field1</field1>" +
                "</root>";

        Schema schema = SchemaBuilder.createSchema(xsd);
        GenericData.Record record = DatumBuilder.createDatum(schema, xml);
        assertEquals("field", record.get("field"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> map = (java.util.Map<String, String>) record.get(SchemaBuilder.OTHERS);

        assertEquals(2, map.size());
        assertEquals("field0", map.get("field0"));
        assertEquals("field1", map.get("field1"));
    }
}
