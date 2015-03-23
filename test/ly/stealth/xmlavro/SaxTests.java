package ly.stealth.xmlavro;

import ly.stealth.xmlavro.Converter;
import ly.stealth.xmlavro.Source;
import ly.stealth.xmlavro.TestData;
import ly.stealth.xmlavro.sax.SaxClient;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
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
import java.util.Collections;

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

    @Test
    public void SchemaBuilder_validName() {
        fail("Unimplemented");
    }


    @Test
    public void rootIntPrimitive() {
        fail("Unimplemented");
    }

    @Test
    public void rootLongPrimitive() {
        fail("Unimplemented");
    }

    @Test
    public void rootDoublePrimitive() {
        fail("Unimplemented");
    }

    @Test
    public void rootUnsignedLongShouldBeKeptAsAvroString() {
        fail("Unimplemented");
    }

    @Test
    public void rootDateTimePrimitive() {
        fail("Unimplemented");
    }

    @Test
    public void severalRoots() {
        fail("Unimplemented");
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
    public void recordWithWildcardField() throws IOException, SAXException, JSONException {
//        Schema schema = Converter.createSchema(TestData.recordWithWildcardField.xsd);
//        assertEquals(2, schema.getFields().size());
//
//        Schema.Field wildcardField = schema.getField(Source.WILDCARD);
//        assertEquals(Schema.Type.MAP, wildcardField.schema().getType());
//
//        SaxClient saxClient = new SaxClient();
//        ByteArrayOutputStream out = new ByteArrayOutputStream();
//        InputStream inputStream = new ByteArrayInputStream(TestData.recordWithWildcardField.xmlWithTwoWildcard.getBytes());
//        saxClient.readStream(schema, inputStream, out);
//
//        JSONObject record = (JSONObject) JSONParser.parseJSON(out.toString());
//        assertEquals("field", record.get("field"));
//
//
//        @SuppressWarnings("unchecked")
//        java.util.Map<String, String> map = (java.util.Map<String, String>) record.get(Source.WILDCARD);
//
//        assertEquals(2, map.size());
//        assertEquals("field0", map.get("field0"));
//        assertEquals("field1", map.get("field1"));
//
//        record = Converter.createDatum(schema, TestData.recordWithWildcardField.xmlWithNoWildcard);
//
//        assertEquals("field", record.get("field"));
//        assertEquals(Collections.emptyMap(), record.get(Source.WILDCARD));

        fail("Unimplemented");
    }

    @Test
    public void severalWildcards() {
        fail("Unimplemented");
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
    public void array() {
        fail("Unimplemented");
    }

    @Test
    public void choiceElements() {
        fail("Unimplemented");
    }

    @Test
    public void arrayOfUnboundedChoiceElements() {
        fail("Unimplemented");
    }

    @Test
    public void arrayOfChoiceElements() {
        fail("Unimplemented");
    }

    @Test
    public void arrayFromComplexTypeChoiceElements() throws JSONException {
        fail("Unimplemented");
    }


}
