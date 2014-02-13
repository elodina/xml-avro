package ly.stealth.xmlavro;

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

        Datum.Type type = Datum.Type.create(xsd);
        Value value = Datum.create(type, xml);

        assertEquals(Value.Type.INT, value.getType());
        assertEquals(1, value.getObject());
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

        Record.Type type = Datum.Type.create(xsd);
        Record record = Datum.create(type, xml);

        assertEquals(1, record.getValue(type.getField("i")));
        assertEquals("s", record.getValue(type.getField("s")));
        assertEquals(1.0, record.getValue(type.getField("d")));
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

        Record.Type type = Datum.Type.create(xsd);
        Record record = Datum.create(type, xml);

        assertEquals("id", record.getValue(type.getField("id", true)));
        assertEquals("name", record.getValue(type.getField("name")));
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

        Record.Type type = Datum.Type.create(xsd);
        Record record = Datum.create(type, xml);

        assertEquals("value", record.getValue(type.getField("field")));
        assertEquals("value0", record.getValue(type.getField("field", true)));
    }
}
