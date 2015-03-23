package ly.stealth.xmlavro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.junit.Test;

import java.util.Arrays;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

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

}
