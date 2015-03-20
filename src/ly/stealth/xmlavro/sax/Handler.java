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
    private boolean inAge = false;

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
    private Node _lastSibling = null;
    private Node _nextSibling = null;

//    private class NameAndBuffer {
//
//        private final String name;
//        private final StringBuffer buffer;
//
//        NameAndBuffer(final String name, final StringBuffer buffer) {
//            this.name = name;
//            this.buffer = buffer;
//        }
//
//        public String getName() {
//            return name;
//        }
//
//    }

//    private Stack<NameAndBuffer> lookup = new Stack<>();

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

    private String currentRootName = null;
    private StringBuffer stringBuffer = new StringBuffer();


    public void startDocument() {
//        _nodeStk.push(_document.getDocumentElement());
    }

    public void endDocument() {
//        //_nodeStk.pop();
//        //Element e =  _nodeStk.pop();
//        DatumBuilder datumBuilder = new DatumBuilder(schema);
//        Object datum = ""; // TODO: just to get started
//
//        try {
//
//            datum = datumBuilder.createDatum(_document.getDocumentElement());
//        } catch (Exception e2) {
//            System.out.println("I say, what seems to be the matter here?");
//            e2.printStackTrace();
//        }
//
//        try {
//            outputStream.write((datum.toString() + "\n").getBytes());
//        } catch (IOException e1) {
//            e1.printStackTrace();
//        }
    }


    private void doStuff(String namespace, String qName, Attributes attrs) {
        final Element tmp = (Element) _document.createElementNS(namespace, qName);

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
        final int nattrs = attrs.getLength();
        for (int i = 0; i < nattrs; i++)
        {
            if (attrs.getLocalName(i) == null)
            {
                tmp.setAttribute(attrs.getQName(i), attrs.getValue(i));
            }
            else
            {
                tmp.setAttributeNS(attrs.getURI(i), attrs.getQName(i),
                        attrs.getValue(i));
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

    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {

////        if (lookup.get(localName) != null)
////            throw new RuntimeException("there is already an item here with the same name... blowing up");
//
//
//        currentRootName = localName;
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        try {
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Element e = builder.newDocument().createElement(localName);
//            for (int i = 0; i < atts.getLength(); i++) {
//                e.setAttributeNode(atts.);
//            }
//        } catch (ParserConfigurationException e) {
//            e.printStackTrace();
//        }
//
//        if (lookup.size() == 0) {
//            try {
//                outputStream.write("buffer is empty\n".getBytes());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        lookup.push(new NameAndBuffer(localName, new StringBuffer()));
////        NameAndBuffer nameAndBuffer = lookup.get(localName)
//
////        try {
////            outputStream.write((schema.getProp(localName) + "\n").getBytes());
////        } catch (IOException e) {
////            e.printStackTrace();
////        }
//
//        if (localName.equals("age")) inAge = true;

        doStuff(namespaceURI, qualifiedName, atts);
    }

    public void endElement(String namespaceURI, String localName, String qualifiedName) throws SAXException {
//
//        NameAndBuffer nameAndBuffer = lookup.pop();
//
//        try {
//            outputStream.write((nameAndBuffer.getName() + "\n").getBytes());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        lookup.remove(localName);
//
//        if (currentRootName == localName) {
//            DatumBuilder datumBuilder = new DatumBuilder(schema);
//
//            //Element e = DatumBuilder.parse()
////            datumBuilder.createDatum()
//        }
//
//
////        System.out.println("endElement");
//        if (localName.equals("age")) inAge = false;

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

//    public void characters(char[] ch, int start, int length) throws SAXException {
//        if (inAge) {
//            for (int i = start; i < start+length; i++) {
//                System.out.println(ch[i]);
//            }
//        }
//    }

    public void characters(char[] ch, int start, int length) {
        final String text = new String(ch, start, length).trim();
        final Node last = (Node)_nodeStk.peek();

        if (!text.isEmpty())
            last.setTextContent(text);

//        // No text nodes can be children of root (DOM006 exception)
//        if (last != _document) {
//            final String text = new String(ch, start, length);
//            if( _lastSibling != null && _lastSibling.getNodeType() == Node.TEXT_NODE ){
//                ((Text)_lastSibling).appendData(text);
//            }
//            else if (last == _root && _nextSibling != null) {
//                _lastSibling = last.insertBefore(_document.createTextNode(text), _nextSibling);
//            }
//            else {
//                _lastSibling = last.appendChild(_document.createTextNode(text));
//            }
//
//        }
    }
}