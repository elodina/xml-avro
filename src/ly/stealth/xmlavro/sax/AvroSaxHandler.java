package ly.stealth.xmlavro.sax;

import ly.stealth.xmlavro.DatumBuilder;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericDatumWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Stack;
import java.util.TimeZone;

/**
 * Custom Sax parser that can read in xml from an input stream and output Avro files to an output stream
 */
public class AvroSaxHandler extends DefaultHandler {
    private final OutputStream outputStream;
    private final Schema schema;

    private TimeZone timeZone = null;
    private int parsingDepth = 0;

    private Node _root = null;
    private Document _document = null;
    private Stack<Element> _nodeStk = new Stack<>();

    private DataFileWriter dataFileWriter;

    public enum ParsingDepth {
        /**
         * Parses the whole file in one go: i.e. the user does not get any advantage over loading whole dom
         */
        ROOT,
        /**
         * Iterates over elements indented under the root node. This is where this handler provides value to the caller
         */
        ROOT_PLUS_ONE
    }

    /**
     * @param schema
     * The schema of the XML file to be fed into the system
     * @param outputStream
     * The output stread to push generated Avro to
     */
    AvroSaxHandler(Schema schema, OutputStream outputStream) {
        this.outputStream = outputStream;
        this.schema = schema;

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            _document = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        _root = _document;

        dataFileWriter = new DataFileWriter(new GenericDatumWriter(schema));
        try {
            dataFileWriter.create(schema, outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param timeZone
     * The timezone to use for generating avro
     * @return
     * The self instance (builder pattern)
     */
    public AvroSaxHandler withTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    /**
     * @param depth
     * Whether to read the whole DOM or to read indented elements individually to avoid running out of memory
     * @return
     * The self instance (builder pattern)
     */
    public AvroSaxHandler withParsingDepth(ParsingDepth depth) {
        if (depth == ParsingDepth.ROOT)
            parsingDepth = 1;
        else
            parsingDepth = 2;
        return this;
    }

    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
        final Element tmp = _document.createElementNS(namespaceURI, qualifiedName);

        // Add attributes to element
        final int nattrs = atts.getLength();
        for (int i = 0; i < nattrs; i++)
        {
            if (atts.getLocalName(i) == null) {
                tmp.setAttribute(atts.getQName(i), atts.getValue(i));
            } else {
                tmp.setAttributeNS(atts.getURI(i), atts.getQName(i), atts.getValue(i));
            }
        }

        // Append this new node onto current stack node
        if (_nodeStk.size() > 0) {
            Node last = _nodeStk.peek();
            last.appendChild(tmp);
        } else {
            _root = tmp;
            _document.appendChild(tmp);
        }

        // Push this node onto stack
        _nodeStk.push(tmp);
    }

    public void endElement(String namespaceURI, String localName, String qualifiedName) throws SAXException {
        int depthOfStackAtStartOfProcess = _nodeStk.size();
        Element recentlyPoppedElement = _nodeStk.pop();

        if (timeZone != null) {
            DatumBuilder.setDefaultTimeZone(timeZone);
        }

        if (depthOfStackAtStartOfProcess == parsingDepth) {
            DatumBuilder datumBuilder = new DatumBuilder(schema);
            Object datum;
            if (depthOfStackAtStartOfProcess == 2) {
                datum = datumBuilder.createDatum( (Element) _root );
                _root.removeChild(recentlyPoppedElement);
            } else {
                datum = datumBuilder.createDatum( recentlyPoppedElement );
            }

            try {
                dataFileWriter.append(datum);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Encountered problem writing the datum to the output stream: '" + e.getMessage() + "'");
            } finally {

            }
        }
    }

    private void closeOutputStreams() {
        try {
            dataFileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Encountered problem flushing data file writer: '" + e.getMessage() + "'");
        } finally {
            try {
                dataFileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Encountered problem closing data file writer: '" + e.getMessage() + "'");
            } finally {
                try {
                    outputStream.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Encountered problem flushing output stream: '" + e.getMessage() + "'");
                } finally {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        throw new RuntimeException("Encountered problem closing output stream: '" + e.getMessage() + "'");
                    }
                }
            }
        }
    }

    public void endDocument () throws SAXException {
        closeOutputStreams();
    }

    public void characters(char[] ch, int start, int length) {
        final String text = new String(ch, start, length).trim();
        final Node last = _nodeStk.peek();

        if (!text.isEmpty())
            last.setTextContent(text);

    }
}