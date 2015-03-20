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
        // setup the handler
        ContentHandler handler = new Handler(schema, outputStream);
        parser.setContentHandler(handler);
        // convert to input source
        InputSource source = new InputSource(inputStream);
        // parse the data
        parser.parse(source);
        // print an empty line under the data
        System.out.println();
    }

}