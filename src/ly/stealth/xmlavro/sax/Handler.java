package ly.stealth.xmlavro.sax;

import ly.stealth.xmlavro.DatumBuilder;
import org.apache.avro.Schema;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.OutputStream;
import org.w3c.dom.Text;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

public class Handler extends DefaultHandler {
    private final OutputStream outputStream;
    private final Schema schema;

    public static final String EMPTYSTRING = "";
    public static final String XML_PREFIX = "xml";
    public static final String XMLNS_PREFIX = "xmlns";
    public static final String XMLNS_STRING = "xmlns:";
    public static final String XMLNS_URI = "http://www.w3.org/2000/xmlns/";

    private Node _root = null;
    private Document _document = null;
    private Stack<Element> _nodeStk = new Stack<>();
    private Vector _namespaceDecls = null;

    Handler(Schema schema, OutputStream outputStream) {
        this.outputStream = outputStream;
        this.schema = schema;

        for (Schema.Field field : schema.getFields()) {
            System.out.println(field.name());
        }

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            _document = factory.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
        _root = _document;
    }

    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
        final Element tmp = (Element) _document.createElementNS(namespaceURI, qualifiedName);

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
            Node last = (Node) _nodeStk.peek();
            last.appendChild(tmp);
        }

        // Push this node onto stack
        _nodeStk.push(tmp);
    }

    public void endElement(String namespaceURI, String localName, String qualifiedName) throws SAXException {
        Element e =  _nodeStk.pop();
        DatumBuilder datumBuilder = new DatumBuilder(schema);
        Object datum = ""; // TODO: just to get started

        if (_nodeStk.size() == 0) {
            try {

                datum = datumBuilder.createDatum(e);
            } catch (Exception e2) {
                System.out.println("I say, what seems to be the matter here?");
                e2.printStackTrace();
            }

            try {
                outputStream.write((datum.toString() + "\n").getBytes());
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }


    }

    public void characters(char[] ch, int start, int length) {
        final String text = new String(ch, start, length).trim();
        final Node last = (Node)_nodeStk.peek();

        if (!text.isEmpty())
            last.setTextContent(text);

    }
}