package ly.stealth.xmlavro.sax;

import java.io.*;

import org.apache.avro.Schema;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class SaxClient {

    public void readStream(Schema schema, InputStream inputStream, OutputStream outputStream) throws SAXException, IOException {
        // specify the SAXParser
        XMLReader parser = XMLReaderFactory.createXMLReader(
                "com.sun.org.apache.xerces.internal.parsers.SAXParser"
        );
        ContentHandler handler = new Handler(schema, outputStream);
        parser.setContentHandler(handler);
        InputSource source = new InputSource(inputStream);
        parser.parse(source);
    }

}