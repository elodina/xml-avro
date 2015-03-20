package ly.stealth.xmlavro.sax;

import ly.stealth.xmlavro.Converter;
import ly.stealth.xmlavro.TestData;
import ly.stealth.xmlavro.interfaces.RequiredTests;
import org.apache.avro.Schema;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONParser;

import java.io.*;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

public class SaxTests implements RequiredTests {

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
    public void arrayFromComplexTypeSequenceOfChoiceElements() throws JSONException {
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        File file = null;
        try {
            file = temporaryFolder.newFile("readInAFileAndOutputToFile.temp");
        } catch (IOException e) {
            e.printStackTrace();
            fail("unexpected");
        }
        SaxClient saxClient = new SaxClient();
        Schema schema = Converter.createSchema(TestData.xsd_arrayFromComplexTypeSequenceOfChoiceElements);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.xml_arrayFromComplexTypeSequenceOfChoiceElements.getBytes());

        try {
            saxClient.readStream(schema, inputStream, out);
        } catch (Exception e) {
            fail("I was not expecting any failures in this test but got: " + e.getMessage());
        }

        JSONAssert.assertEquals(TestData.json_arrayFromComplexTypeSequenceOfChoiceElements, out.toString(), false);

        file.delete();
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail("unexpected");
        }
    }

    @Test
    public void SchemaBuilder_validName() {
        fail("Unimplemented");
    }


    @Test
    public void basic() {
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
    public void rootRecord() {
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        File file = null;
        try {
            file = temporaryFolder.newFile("rootRecord.temp");
        } catch (IOException e) {
            e.printStackTrace();
            fail("that was unexpected");
        }
        SaxClient saxClient = new SaxClient();
        Schema schema = Converter.createSchema(TestData.xsd_rootRecord);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream inputStream = new ByteArrayInputStream(TestData.xml_rootRecord.getBytes());
        try {
            saxClient.readStream(schema, inputStream, out);
        } catch (Exception e) {
            fail("I was not expecting any failures in this test but got: " + e.getMessage());
        }

        JSONObject record = null;
        try {
            record = (JSONObject) JSONParser.parseJSON(out.toString());
            assertEquals(1, record.get("i"));
            assertEquals("s", record.get("s"));
            assertEquals(1.0, record.get("d"));
        } catch (JSONException e) {
            e.printStackTrace();
            fail("unexpected...");
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            fail("that was not expected at all");
        }
        file.delete();
    }

    @Test
    public void nestedRecursiveRecords() {
        fail("Unimplemented");
    }

    @Test
    public void attributes() {
        fail("Unimplemented");
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
