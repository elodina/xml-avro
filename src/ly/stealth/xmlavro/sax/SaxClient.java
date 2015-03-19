package ly.stealth.xmlavro.sax;

import java.io.*;

import org.apache.avro.Schema;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class SaxClient {

    public void readFile(Schema schema, String xmlFileName, OutputStream outputStream) throws SAXException, IOException {
        // specify the SAXParser
        XMLReader parser = XMLReaderFactory.createXMLReader(
                "com.sun.org.apache.xerces.internal.parsers.SAXParser"
        );
        // setup the handler
        ContentHandler handler = new Handler(outputStream);
        parser.setContentHandler(handler);
        // open the file
        FileInputStream in = new FileInputStream(xmlFileName);
        InputSource source = new InputSource(in);
        // parse the data
        parser.parse(source);
        // print an empty line under the data
        System.out.println();
        // close the file
        in.close();
    }

}