package ly.stealth.xmlavro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;

import static junit.framework.Assert.*;

public class SchemaTests {

    @Test
    public void basic() {
        String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root' type='xs:string'/>" +
                "</xs:schema>";

        Converter.createSchema(xsd);

        try { // no namespace
            Converter.createSchema("<schema/>");
            fail();
        } catch (ConverterException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("http://www.w3.org/2001/XMLSchema"));
            assertTrue(message, message.contains("namespace"));
        }
    }

    @Test
    public void uniqueFieldNames() {
        Schema schema = Converter.createSchema(TestData.uniqueFieldNames.xsd);

        assertEquals(2, schema.getFields().size());
        Schema.Field field = schema.getField("field");
        assertNotNull(field);
        assertEquals("" + new Source("field", true), field.getProp(Source.SOURCE));

        Schema.Field field0 = schema.getField("field0");
        assertEquals("" + new Source("field", false), field0.getProp(Source.SOURCE));
    }

    @Test
    public void optionalElementValues() {
        Schema schema = Converter.createSchema(TestData.optionalElementValues.xsd);
        assertEquals(2, schema.getFields().size());

        Schema.Field requiredField = schema.getField("required");
        assertEquals(Schema.Type.STRING, requiredField.schema().getType());

        Schema.Field optionalField = schema.getField("optional");
        Schema optionalSchema = optionalField.schema();
        assertEquals(Schema.Type.UNION, optionalSchema.getType());

        assertEquals(
                Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)),
                optionalSchema.getTypes()
        );
    }

    @Test
    public void array() {
        Schema schema = Converter.createSchema(TestData.array.xsd);
        Schema.Field valueField = schema.getField("value");
        assertEquals(Schema.Type.ARRAY, valueField.schema().getType());
        assertEquals(Schema.Type.STRING, valueField.schema().getElementType().getType());
    }

    @Test
    public void SchemaBuilder_validName() {
        SchemaBuilder builder = new SchemaBuilder();

        assertNull(builder.validName(null));
        assertEquals("", builder.validName(""));

        assertEquals("a1", builder.validName("$a#1"));

        assertEquals("a_1", builder.validName("a.1"));
        assertEquals("a_1", builder.validName("a-1"));

        // built-in types
        assertEquals("string0", builder.validName("string"));
        assertEquals("record1", builder.validName("record"));
    }

    @Test
    public void arrayOfChoiceElements() {
        Schema schema = Converter.createSchema(TestData.arrayOfChoiceElements.xsd);
        assertEquals(Schema.Type.ARRAY, schema.getType());
        final Schema elementType = schema.getElementType();
        assertEquals(Schema.Type.RECORD, elementType.getType());
        assertEquals(2, elementType.getFields().size());
    }

    @Test
    public void arrayFromComplexTypeChoiceElements() throws JSONException {
        Schema schema = Converter.createSchema(TestData.arrayFromComplexTypeChoiceElements.xsd);
        JSONAssert.assertEquals("{" +
                "    'type': 'array'," +
                "    'items': {" +
                "        'type': 'record'," +
                "        'name': 'AnonType_root'," +
                "        'fields': [" +
                "            {" +
                "                'name': 's'," +
                "                'type': ['null', 'string']" +
                "            }," +
                "            {" +
                "                'name': 'i'," +
                "                'type': ['null', 'int']" +
                "            }" +
                "        ]" +
                "    }" +
                "}", schema.toString(), false);
    }

    @Test
    public void arrayOfUnboundedChoiceElements() {
        String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root'>" +
                "    <xs:complexType>" +
                "      <xs:choice maxOccurs='unbounded'>" +
                "        <xs:element name='s' type='xs:string'/>" +
                "        <xs:element name='i' type='xs:int'/>" +
                "      </xs:choice>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);
        assertEquals(Schema.Type.ARRAY, schema.getType());
        final Schema elementType = schema.getElementType();
        assertEquals(Schema.Type.RECORD, elementType.getType());
    }

    @Test
    public void choiceElements() {
        Schema schema = Converter.createSchema(TestData.choiceElements.xsd);
        assertEquals(Schema.Type.RECORD, schema.getType());
        assertEquals(2, schema.getFields().size());

        Schema.Field sField = schema.getField("s");
        assertEquals(Schema.Type.UNION, sField.schema().getType());
        assertEquals(
                Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)),
                sField.schema().getTypes()
        );

        Schema.Field iField = schema.getField("i");
        assertEquals(Schema.Type.UNION, iField.schema().getType());
        assertEquals(Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.INT)), iField.schema().getTypes());
    }

    @Test
    public void severalWildcards() {
        Schema schema = Converter.createSchema(TestData.severalWildcards.xsd);
        assertEquals(1, schema.getFields().size());

        Schema.Field field = schema.getField(Source.WILDCARD);
        assertEquals(null, field.getProp(Source.SOURCE));
    }

    @Test
    public void severalRoots() {
        Schema schema = Converter.createSchema(TestData.severalRoots.xsd);
        assertEquals(Schema.Type.RECORD, schema.getType());
        assertTrue("Schema should have a valid name", schema.getName() != null && !schema.getName().isEmpty());
        assertEquals(Source.DOCUMENT, schema.getProp(Source.SOURCE));
        assertEquals(2, schema.getFields().size());

        Schema.Field field0 = schema.getFields().get(0);
        assertEquals("" + new Source("i"), field0.getProp(Source.SOURCE));
        assertEquals(Schema.Type.UNION, field0.schema().getType());
        assertEquals(Schema.Type.INT, field0.schema().getTypes().get(1).getType());
        assertEquals(Schema.Type.NULL, field0.schema().getTypes().get(0).getType());

        Schema.Field field1 = schema.getFields().get(1);
        assertEquals("" + new Source("r"), field1.getProp(Source.SOURCE));
        assertEquals(Schema.Type.UNION, field1.schema().getType());
        assertEquals(Schema.Type.RECORD, field1.schema().getTypes().get(1).getType());
        assertEquals(Schema.Type.NULL, field1.schema().getTypes().get(0).getType());
    }


}
