package ly.stealth.xmlavro.sax;

import ly.stealth.xmlavro.DatumBuilder;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Stack;
import java.util.TimeZone;
import java.util.Vector;

public class Handler extends DefaultHandler {
    private final OutputStream outputStream;
    private final Schema schema;

    private TimeZone timeZone = null;
    private int parsingDepth = 0;

    public static final String EMPTYSTRING = "";
    public static final String XML_PREFIX = "xml";
    public static final String XMLNS_PREFIX = "xmlns";
    public static final String XMLNS_STRING = "xmlns:";
    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    private Node _root = null;
    private Document _document = null;
    private Stack<Element> _nodeStk = new Stack<>();
    private Vector _namespaceDecls = null;

    private DataFileWriter dataFileWriter;

    public enum ParsingDepth {
        ROOT,
        ROOT_PLUS_ONE
    }

    Handler(Schema schema, OutputStream outputStream) {
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
            dataFileWriter.create(schema, outputStream);  //appendTo(new File("xml/sax/foo.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Handler withTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    public Handler withParsingDepth(ParsingDepth depth) {
        if (depth == ParsingDepth.ROOT)
            parsingDepth = 1;
        else
            parsingDepth = 2;
        return this;
    }

    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
        final Element tmp = _document.createElementNS(namespaceURI, qualifiedName);

        // Add namespace declarations first
        if (_namespaceDecls != null)
        {
            final int nDecls = _namespaceDecls.size();
            for (int i = 0; i < nDecls; i++)
            {
                final String prefix = (String) _namespaceDecls.elementAt(i++);

                if (prefix == null || prefix.equals(EMPTYSTRING))
                {
                    tmp.setAttributeNS(XMLNS_URI, XMLNS_PREFIX,
                            (String) _namespaceDecls.elementAt(i));
                }
                else
                {
                    tmp.setAttributeNS(XMLNS_URI, XMLNS_STRING + prefix,
                            (String) _namespaceDecls.elementAt(i));
                }
            }
            _namespaceDecls.clear();
        }

        // Add attributes to element
        final int nattrs = atts.getLength();
        for (int i = 0; i < nattrs; i++)
        {
            if (atts.getLocalName(i) == null)
            {
                tmp.setAttribute(atts.getQName(i), atts.getValue(i));
            }
            else
            {
                tmp.setAttributeNS(atts.getURI(i), atts.getQName(i),
                        atts.getValue(i));
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

        DatumBuilder datumBuilder = new DatumBuilder(schema);

        Object datum = null; // TODO: just to get started

        if (depthOfStackAtStartOfProcess == parsingDepth) {
            try {
                if (depthOfStackAtStartOfProcess == 2) {
                    datum = datumBuilder.createDatum( (Element) _root );
                    _root.removeChild(recentlyPoppedElement);
                } else {
                    datum = datumBuilder.createDatum( recentlyPoppedElement );

                }

            } catch (Exception e2) {
                System.out.println("I say, what seems to be the matter here?");
                e2.printStackTrace();
            }


            try {
                //outputStream.write((datum.toString() + "\n").getBytes());
                dataFileWriter.append(datum);
            } catch (IOException e1) {
                e1.printStackTrace();
            }


        }

    }

    public void endDocument () throws SAXException
    {
        try {
            dataFileWriter.flush();
            dataFileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void characters(char[] ch, int start, int length) {
        final String text = new String(ch, start, length).trim();
        final Node last = _nodeStk.peek();

        if (!text.isEmpty())
            last.setTextContent(text);

    }
}