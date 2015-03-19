package ly.stealth.xmlavro.sax;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.io.OutputStream;

public class Handler extends DefaultHandler {
    private boolean inAge = false;

    private final OutputStream outputStream;

    Handler(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void startElement(String namespaceURI, String localName, String qualifiedName, Attributes atts) throws SAXException {
        try {
            outputStream.write("startElement\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (localName.equals("age")) inAge = true;
    }

    public void endElement(String namespaceURI, String localName, String qualifiedName) throws SAXException {

        try {
            outputStream.write("endElement\n".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        System.out.println("endElement");
        if (localName.equals("age")) inAge = false;
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        if (inAge) {
            for (int i = start; i < start+length; i++) {
                System.out.println(ch[i]);
            }
        }
    }
}