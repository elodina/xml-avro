package ly.stealth.xmlavro;

import org.junit.Test;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

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

}
