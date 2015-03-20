package ly.stealth.xmlavro.sax;

import ly.stealth.xmlavro.Converter;
import ly.stealth.xmlavro.ConverterException;
import ly.stealth.xmlavro.TestData;
import org.apache.avro.Schema;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Arrays;

import static junit.framework.Assert.*;

public class SaxTests {

    String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
            "  <xs:element name='root'>" +
            "    <xs:complexType>" +
            "     <xs:sequence>" +
            "        <xs:element name='s' type='xs:string'/>" +
            "        <xs:element name='i' type='xs:int'/>" +
            "        <xs:choice maxOccurs='2'>" +
            "          <xs:element name='x' type='xs:string'/>" +
            "          <xs:element name='y' type='xs:int'/>" +
            "        </xs:choice>" +
            "     </xs:sequence>" +
            "    </xs:complexType>" +
            "  </xs:element>" +
            "</xs:schema>";

//    String xml =
//            "<root>" +
//                    "<s>s</s>" +
//                    "<i>1</i>" +
//                    "<x>x1</x>" +
//                    "<y>2</y>" +
//                    "</root>";


//    @Test
//    public void noFile() throws IOException {
//
//        SaxClient saxClient = new SaxClient();
//        Schema schema = Converter.createSchema(xsd);
//
//        // open the file
//        FileInputStream in = new FileInputStream("file.xml");
//
//        try {
//            saxClient.readStream(schema, in, System.out);
//            fail( "My method didn't throw when I expected it to" );
//        } catch (Exception e) {
//            assertTrue("we should have a file not found here", e.toString().contains("FileNotFoundException"));
//        }
//
//        in.close();
//    }

    @Test
    public void arrayFromComplexTypeSequenceOfChoiceElements() throws JSONException, IOException, SAXException {
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        File file = temporaryFolder.newFile("readInAFileAndOutputToFile.temp");
        SaxClient saxClient = new SaxClient();
        Schema schema = Converter.createSchema(TestData.arrayFromComplexTypeSequenceOfChoiceElements.xsd);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.arrayFromComplexTypeSequenceOfChoiceElements.xml.getBytes());

        saxClient.readStream(schema, inputStream, out);

        JSONAssert.assertEquals(TestData.arrayFromComplexTypeSequenceOfChoiceElements.datum, out.toString(), false);

        file.delete();
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
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        File file = temporaryFolder.newFile("rootRecord.temp");
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
    public void nestedRecursiveRecords() {
        fail("Unimplemented");
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
    public void uniqueFieldNames() {
        fail("Unimplemented");
    }

    @Test
    public void recordWithWildcardField() {
        fail("Unimplemented");
    }

    @Test
    public void severalWildcards() {
        fail("Unimplemented");
    }

    @Test
    public void optionalElementValues() {
        fail("Unimplemented");
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
