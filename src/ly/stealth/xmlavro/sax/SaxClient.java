package ly.stealth.xmlavro.sax;

import java.io.*;
import java.util.TimeZone;

import org.apache.avro.Schema;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

public class SaxClient {

    private Handler.ParsingDepth parsingDepth = Handler.ParsingDepth.ROOT;
    private TimeZone timeZone = null;

    public SaxClient withTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public SaxClient withParsingDepth(Handler.ParsingDepth depth) {
        parsingDepth = depth;
        return this;
    }

    public void readStream(Schema schema, InputStream inputStream, OutputStream outputStream) throws SAXException, IOException {
        XMLReader parser = XMLReaderFactory.createXMLReader(
                "com.sun.org.apache.xerces.internal.parsers.SAXParser"
        );

        Handler handler = new Handler(schema, outputStream)
            .withTimeZone(timeZone)
            .withParsingDepth(parsingDepth);

        parser.setContentHandler(handler);
        InputSource source = new InputSource(inputStream);
        parser.parse(source);
    }

}