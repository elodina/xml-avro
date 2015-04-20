package ly.stealth.xmlavro.sax;

import java.io.*;
import java.util.TimeZone;

import org.apache.avro.Schema;
import org.xml.sax.*;
import org.xml.sax.helpers.*;

/**
 * Entry for passing an InputStream to the Sax Avro Handler to be returned as Avro in the provided OutputStream
 */
public class SaxClient {

    private AvroSaxHandler.ParsingDepth parsingDepth = AvroSaxHandler.ParsingDepth.ROOT;
    private TimeZone timeZone = null;

    /**
     * If you want to use a different time zone for the generated avro
     * @param timeZone
     * @return
     */
    public SaxClient withTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    /**
     * Control the parsing strategy, i.e. how will the Sax Parser split the XML file
     * @param depth
     * @return
     */
    public SaxClient withParsingDepth(AvroSaxHandler.ParsingDepth depth) {
        parsingDepth = depth;
        return this;
    }

    /**
     *
     * @param schema
     * The schema of the provided xml
     * @param inputStream
     * The xml data to feed into the Sax Parser
     * @param outputStream
     * Where to stream the results of the Sax Parser to
     * @throws SAXException
     * @throws IOException
     */
    public void readStream(Schema schema, InputStream inputStream, OutputStream outputStream) throws SAXException, IOException {
        XMLReader parser = XMLReaderFactory.createXMLReader(
                "com.sun.org.apache.xerces.internal.parsers.SAXParser"
        );

        AvroSaxHandler handler = new AvroSaxHandler(schema, outputStream)
            .withTimeZone(timeZone)
            .withParsingDepth(parsingDepth);

        parser.setContentHandler(handler);
        InputSource source = new InputSource(inputStream);
        parser.parse(source);
    }

}