package ly.stealth.xmlavro.sax;

import java.io.*;
import java.util.TimeZone;

import org.apache.avro.Schema;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class SaxClient {

    private TimeZone timeZone = null;

    public SaxClient withTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public void readStream(Schema schema, InputStream inputStream, OutputStream outputStream) throws SAXException, IOException {
        // specify the SAXParser
        XMLReader parser = XMLReaderFactory.createXMLReader(
                "com.sun.org.apache.xerces.internal.parsers.SAXParser"
        );
        Handler handler = new Handler(schema, outputStream);
        handler.withTimeZone(timeZone);
        parser.setContentHandler(handler);
        InputSource source = new InputSource(inputStream);
        parser.parse(source);
    }

}