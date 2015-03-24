package ly.stealth.xmlavro;

import ly.stealth.xmlavro.sax.Handler;
import ly.stealth.xmlavro.sax.SaxClient;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.SeekableByteArrayInput;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
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

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        Object record =  fileReader.next();

        JSONAssert.assertEquals(TestData.arrayFromComplexTypeSequenceOfChoiceElements.datum, record.toString(), false);

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

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);

        Object record =  fileReader.next();
        return record.toString().trim();
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
        DatumBuilder.setDefaultTimeZone(TimeZone.getTimeZone("UTC-0"));

        assertEquals(1414681113000L, Long.parseLong(rootPrimitiveWithType("xs:dateTime", "2014-10-30T14:58:33", null).trim()));
        assertEquals(1410353913000L, Long.parseLong(rootPrimitiveWithType("xs:dateTime", "2014-09-10T12:58:33", null).trim()));

        TimeZone timeZone = TimeZone.getTimeZone("America/Los_Angeles");

        assertEquals(1414681113000L, Long.parseLong(rootPrimitiveWithType("xs:dateTime", "2014-10-30T07:58:33", timeZone).trim()));
        assertEquals(1410353913000L, Long.parseLong(rootPrimitiveWithType("xs:dateTime", "2014-09-10T05:58:33", timeZone).trim()));
    }

    @Test
    public void severalRootsOne() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.severalRoots.xsd);

        String xml = "<i>5</i>";
        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

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

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        GenericData.Record subObject = (GenericData.Record) record.get("r");
        assertEquals("s", subObject.get("s").toString());
    }

    @Test
    public void rootRecord() throws IOException, SAXException, JSONException {
        SaxClient saxClient = new SaxClient();
        Schema schema = Converter.createSchema(TestData.rootRecord.xsd);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.rootRecord.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals(1, record.get("i"));
        assertEquals("s", record.get("s").toString());
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

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        JSONAssert.assertEquals(TestData.nestedRecursiveRecords.datum, record.toString(), false);
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

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals("required", record.get("required").toString());
        assertEquals("optional", record.get("optional").toString());
    }

    @Test
    public void attributesTwo() throws JSONException, IOException, SAXException {
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


        SaxClient saxClient = new SaxClient();
        String xml = "<root required='required'/>";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals("required", record.get("required").toString());
        assertEquals(record.get("optional"), null);

    }

    @Test
    public void uniqueFieldNames() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.uniqueFieldNames.xsd);

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.uniqueFieldNames.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals("value", record.get("field").toString());
        assertEquals("value0", record.get("field0").toString());
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

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals("field", record.get("field").toString());

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> map = (java.util.Map<String, String>) record.get(Source.WILDCARD);

        assertEquals(2, map.size());
        assertEquals(map.keySet().toArray()[0].toString(), "field1".toString());
        assertEquals(map.values().toArray()[0].toString(),"field1".toString());
        assertEquals(map.keySet().toArray()[1].toString(), "field0".toString());
        assertEquals(map.values().toArray()[1].toString(), "field0".toString());
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

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals("field", record.get("field").toString());
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

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals("required", record.get("required").toString());
        assertEquals(record.get("optional"), null);
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

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals("optional", record.get("optional").toString());
    }

    @Test
    public void array() throws Exception {
        Schema schema = Converter.createSchema(TestData.array.xsd);

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.array.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader1 = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);

        GenericData.Record record =  (GenericData.Record) fileReader1.next();
        assertEquals(Arrays.asList("1", "2", "3").toString(), record.get("value").toString());
    }

    @Test
    public void multiLevelParsingTest() throws Exception {
        Schema schema = Converter.createSchema(TestData.multiLevelParsingTest.xsd);

        SaxClient saxClient = new SaxClient().withParsingDepth(Handler.ParsingDepth.ROOT_PLUS_ONE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.multiLevelParsingTest.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader1 = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);

        GenericData.Array record =  (GenericData.Array) fileReader1.next();

        GenericData.Record firstRecord = (GenericData.Record) record.get(0);
        assertEquals("s", firstRecord.get("s").toString());

        record = (GenericData.Array) fileReader1.next();
        GenericData.Record secondRecord = (GenericData.Record) record.get(0);
        assertEquals(1, secondRecord.get("i"));

        record = (GenericData.Array) fileReader1.next();
        GenericData.Record thirdRecord = (GenericData.Record) record.get(0);
        assertEquals(2, thirdRecord.get("i"));

        assertFalse(fileReader1.hasNext());
    }

    @Test
    public void choiceElementsOne() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.choiceElements.xsd);
        String xml = "<root><s>s</s></root>";

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals("s", record.get("s").toString());
    }

    @Test
    public void choiceElementsTwo() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.choiceElements.xsd);
        String xml = "<root><i>1</i></root>";
        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Record record =  (GenericData.Record) fileReader.next();

        assertEquals(1, record.get("i"));
    }

    @Test
    public void arrayOfChoiceElements() throws IOException, SAXException, JSONException {
        Schema schema = Converter.createSchema(TestData.arrayOfChoiceElements.xsd);

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.arrayOfChoiceElements.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        GenericData.Array record =  (GenericData.Array) fileReader.next();

        GenericData.Record firstRecord = (GenericData.Record) record.get(0);
        assertEquals("s", firstRecord.get("s").toString());

        GenericData.Record secondRecord = (GenericData.Record) record.get(1);
        assertEquals(1, secondRecord.get("i"));

        GenericData.Record thirdRecord = (GenericData.Record) record.get(2);
        assertEquals(2, thirdRecord.get("i"));
    }

    @Test
    public void arrayFromComplexTypeChoiceElements() throws JSONException, IOException, SAXException {
        Schema schema = Converter.createSchema(TestData.arrayFromComplexTypeChoiceElements.xsd);

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.arrayOfChoiceElements.xml.getBytes());
        saxClient.readStream(schema, inputStream, out);

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        Object record =  fileReader.next();

        JSONAssert.assertEquals(TestData.arrayFromComplexTypeChoiceElements.datum, record.toString(), false);
    }

    @Test
    public void largeFile() throws JSONException, IOException, SAXException {
        byte[] encoded = Files.readAllBytes(Paths.get("xml/sax/largeFile.xsd"));
        String xsd = new String(encoded, Charset.defaultCharset());

        Schema schema = Converter.createSchema(xsd);

        SaxClient saxClient = new SaxClient();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new FileInputStream(new File("xml/sax/largeFile.xml"));
        saxClient.readStream(schema, inputStream, out);

        byte[] expected = Files.readAllBytes(Paths.get("xml/sax/largeFile.avro"));
        String avro = new String(expected, Charset.defaultCharset());

        out.flush();
        out.close();

        GenericDatumReader datumReader = new GenericDatumReader();
        org.apache.avro.file.FileReader fileReader = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);
        Object record = fileReader.next();

        assertEquals(avro.length(), record.toString().length());
    }




}
