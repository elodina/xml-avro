package ly.stealth.xmlavro;

import ly.stealth.xmlavro.sax.SaxClient;
import org.apache.avro.Schema;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.TimeZone;

import static junit.framework.Assert.*;

public class SaxTests {

    @Test
    public void arrayFromComplexTypeSequenceOfChoiceElements() throws JSONException, IOException, SAXException {
        SaxClient saxClient = new SaxClient();
        Schema schema = Converter.createSchema(TestData.arrayFromComplexTypeSequenceOfChoiceElements.xsd);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.arrayFromComplexTypeSequenceOfChoiceElements.xml.getBytes());

        saxClient.readStream(schema, inputStream, out);

        JSONAssert.assertEquals(TestData.arrayFromComplexTypeSequenceOfChoiceElements.datum, out.toString(), false);

        inputStream.close();
    }

    public String rootPrimitiveWithType(String xmlType, String xmlValue, TimeZone timeZone) throws IOException, SAXException {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                        "   <xs:element name='value' type='" + xmlType + "'/>" +
                        "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);

        String xml = "<value>" + xmlValue + "</value>";

        SaxClient saxClient = new SaxClient().withTimeZone(timeZone);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        return out.toString();
    }

    @Test
    public void rootIntPrimitive() throws IOException, SAXException {
        assertEquals(-1, Integer.parseInt(rootPrimitiveWithType("xs:int", "-1", null).trim()));
        assertEquals(1, Integer.parseInt(rootPrimitiveWithType("xs:unsignedByte", "1", null).trim()));
        assertEquals(5, Integer.parseInt(rootPrimitiveWithType("xs:unsignedShort", "5", null).trim()));
    }

    @Test
    public void rootLongPrimitive() throws IOException, SAXException {
        assertEquals(20, Long.parseLong(rootPrimitiveWithType("xs:long", "20", null).trim()));
        assertEquals(30, Long.parseLong(rootPrimitiveWithType("xs:unsignedInt", "30", null).trim()));
    }

    @Test
    public void rootDoublePrimitive() throws IOException, SAXException {
        assertEquals(999999999.999999999, Double.parseDouble(rootPrimitiveWithType("xs:decimal", "999999999.999999999", null).trim()));
    }

    @Test
    public void rootUnsignedLongShouldBeKeptAsAvroString() throws IOException, SAXException {
        assertEquals("18446744073709551615", rootPrimitiveWithType("xs:unsignedLong", "18446744073709551615", null).trim());
    }

    @Test
    public void rootDateTimePrimitive() throws IOException, SAXException {
        assertEquals(1414681113000L, Long.parseLong(rootPrimitiveWithType("xs:dateTime", "2014-10-30T14:58:33", null).trim()));
        assertEquals(1410353913000L, Long.parseLong(rootPrimitiveWithType("xs:dateTime", "2014-09-10T12:58:33", null).trim()));

        TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");

        assertEquals(1414681113000L, Long.parseLong(rootPrimitiveWithType("xs:dateTime", "2014-10-30T07:58:33", timeZone).trim()));
        assertEquals(1410353913000L, Long.parseLong(rootPrimitiveWithType("xs:dateTime", "2014-09-10T05:58:33", timeZone).trim()));
    }

    @Test
    public void severalRootsOne() throws IOException, SAXException, JSONException {
//        fail("Unimplemented");
        Schema schema = Converter.createSchema(TestData.severalRoots.xsd);

        String xml = "<i>5</i>";
        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
        assertEquals(JSONObject.NULL, record.get("r"));
        assertEquals(5, record.get("i"));
    }

    @Test
    public void severalRootsTwo() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.severalRoots.xsd);

        String xml = "<r><s>s</s></r>";
        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
        JSONObject subObject = (JSONObject) record.get("r");
        assertEquals("s", subObject.get("s"));
    }

    @Test
    public void rootRecord() throws IOException, SAXException, JSONException {
        SaxClient saxClient = new SaxClient();
        Schema schema = Converter.createSchema(TestData.rootRecord.xsd);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.rootRecord.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
        assertEquals(1, record.get("i"));
        assertEquals("s", record.get("s"));
        assertEquals(1.0, record.get("d"));
        inputStream.close();
    }

    @Test
    public void nestedRecursiveRecords() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.nestedRecursiveRecords.xsd);

        Schema.Field field = schema.getField("node");
        Schema subSchema = field.schema();
        assertSame(schema, subSchema.getTypes().get(1));

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.nestedRecursiveRecords.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONAssert.assertEquals(TestData.nestedRecursiveRecords.datum, out.toString(), false);
    }

    @Test
    public void attributes() throws JSONException, IOException, SAXException {
        Schema schema = Converter.createSchema(TestData.attributes.xsd);

        Schema.Field required = schema.getField("required");
        assertEquals(Schema.Type.STRING, required.schema().getType());

        assertNull(schema.getField("prohibited"));

        Schema.Field optional = schema.getField("optional");
        assertEquals(Schema.Type.UNION, optional.schema().getType());
        assertEquals(
                Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)),
                optional.schema().getTypes()
        );

        String xml = "<root required='required' optional='optional'/>";
        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
        assertEquals("required", record.get("required"));
        assertEquals("optional", record.get("optional"));

        inputStream.close();
        out.close();

        xml = "<root required='required'/>";
        inputStream = new ByteArrayInputStream(xml.getBytes());
        out = new ByteArrayOutputStream();
        saxClient.readStream(schema, inputStream, out);

        record = (JSONObject) JSONParser.parseJSON(out.toString());
        assertEquals("required", record.get("required"));
        assertEquals(record.get("optional"), JSONObject.NULL);


    }

    @Test
    public void uniqueFieldNames() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.uniqueFieldNames.xsd);

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.uniqueFieldNames.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
        assertEquals("value", record.get("field"));
        assertEquals("value0", record.get("field0"));
    }

    @Test
    public void recordWithWildcardFieldOne() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.recordWithWildcardField.xsd);
        assertEquals(2, schema.getFields().size());

        Schema.Field wildcardField = schema.getField(Source.WILDCARD);
        assertEquals(Schema.Type.MAP, wildcardField.schema().getType());

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.recordWithWildcardField.xmlWithTwoWildcard.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
        assertEquals("field", record.get("field"));


        @SuppressWarnings("unchecked")
        JSONObject map = (JSONObject) record.get(Source.WILDCARD);

        assertEquals(2, map.length());
        assertEquals("field0", map.get("field0"));
        assertEquals("field1", map.get("field1"));
    }

    @Test
    public void recordWithNoWildcardField() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.recordWithWildcardField.xsd);
        assertEquals(2, schema.getFields().size());

        Schema.Field wildcardField = schema.getField(Source.WILDCARD);
        assertEquals(Schema.Type.MAP, wildcardField.schema().getType());

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.recordWithWildcardField.xmlWithNoWildcard.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());

        assertEquals("field", record.get("field"));
        assertEquals(new JSONObject().toString(), record.get(Source.WILDCARD).toString());
    }

    @Test
    public void optionalElementValues() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.optionalElementValues.xsd);

        String xml = "<root><required>required</required></root>";

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());

        assertEquals("required", record.get("required"));
        assertEquals(record.get("optional"), JSONObject.NULL);
    }

    @Test
    public void optionalElementValuesTestTwo() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.optionalElementValues.xsd);

        String xml = "<root>" +
                "  <required>required</required>" +
                "  <optional>optional</optional>" +
                "</root>";

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());

        assertEquals("optional", record.get("optional"));
    }

    @Test
    public void array() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.array.xsd);

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.array.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
        assertEquals(new JSONArray(Arrays.asList("1", "2", "3")).toString(), record.get("value").toString());
    }

    @Test
    public void choiceElementsOne() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.choiceElements.xsd);
        String xml = "<root><s>s</s></root>";

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
        assertEquals("s", record.get("s"));
    }

    @Test
    public void choiceElementsTwo() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.choiceElements.xsd);
        String xml = "<root><i>1</i></root>";
        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
        assertEquals(1, record.get("i"));
    }

    @Test
    public void arrayOfChoiceElements() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.arrayOfChoiceElements.xsd);

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.arrayOfChoiceElements.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONArray record = (JSONArray) JSONParser.parseJSON(out.toString());

        JSONObject firstRecord = (JSONObject) record.get(0);
        assertEquals("s", firstRecord.get("s"));

        JSONObject secondRecord = (JSONObject) record.get(1);
        assertEquals(1, secondRecord.get("i"));

        JSONObject thirdRecord = (JSONObject) record.get(2);
        assertEquals(2, thirdRecord.get("i"));
    }

    @Test
    public void arrayFromComplexTypeChoiceElements() throws JSONException, IOException, SAXException {
        Schema schema = Converter.createSchema(TestData.arrayFromComplexTypeChoiceElements.xsd);

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.arrayOfChoiceElements.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        JSONAssert.assertEquals(TestData.arrayFromComplexTypeChoiceElements.datum, out.toString(), false);
    }


}
