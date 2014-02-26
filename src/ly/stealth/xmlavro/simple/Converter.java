/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ly.stealth.xmlavro.simple;

import org.apache.avro.Protocol;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.util.Utf8;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Converter {
    private static Protocol protocol;
    static {
        try {
            InputStream stream = Converter.class.getResourceAsStream("xml.avsc");
            if (stream == null) throw new IllegalStateException("Classpath should include xml.avsc");

            protocol = Protocol.parse(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void xmlToAvro(File xmlFile, File avroFile) throws IOException, SAXException {
        Schema schema = protocol.getType("Element");

        Document doc = parse(xmlFile);
        DatumWriter<GenericRecord> datumWriter = new SpecificDatumWriter<>(schema);

        try (DataFileWriter<GenericRecord> fileWriter = new DataFileWriter<>(datumWriter)) {
            fileWriter.create(schema, avroFile);
            fileWriter.append(wrapElement(doc.getDocumentElement()));
        }
    }

    private static GenericData.Record wrapElement(Element el) {
        GenericData.Record record = new GenericData.Record(protocol.getType("Element"));
        record.put("name", el.getNodeName());

        NamedNodeMap attributeNodes = el.getAttributes();
        List<GenericData.Record> attrRecords = new ArrayList<>();
        for (int i = 0; i < attributeNodes.getLength(); i++) {
            Attr attr = (Attr) attributeNodes.item(i);
            attrRecords.add(wrapAttr(attr));
        }
        record.put("attributes", attrRecords);

        List<Object> childArray = new ArrayList<>();
        NodeList childNodes = el.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE)
                childArray.add(wrapElement((Element) node));

            if (node.getNodeType() == Node.TEXT_NODE)
                childArray.add(node.getTextContent());
        }
        record.put("children", childArray);

        return record;
    }

    private static GenericData.Record wrapAttr(Attr attr) {
        GenericData.Record record = new GenericData.Record(protocol.getType("Attribute"));

        record.put("name", attr.getName());
        record.put("value", attr.getValue());

        return record;
    }

    private static Document parse(File file) throws IOException, SAXException {
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            return builder.parse(file);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static void avroToXml(File avroFile, File xmlFile) throws IOException {
        DatumReader<GenericRecord> datumReader = new GenericDatumReader<>(protocol.getType("Element"));
        DataFileReader<GenericRecord> dataFileReader = new DataFileReader<>(avroFile, datumReader);

        GenericRecord record = dataFileReader.next();

        Document doc;
        try {
            doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }

        Element el = unwrapElement(record, doc);
        doc.appendChild(el);

        saveDocument(doc, xmlFile);
    }

    private static Element unwrapElement(GenericRecord record, Document doc) {
        String name = "" + record.get("name");
        Element el = doc.createElement(name);

        @SuppressWarnings("unchecked")
        GenericArray<GenericRecord> attrArray = (GenericArray<GenericRecord>) record.get("attributes");
        for (GenericRecord attrRecord : attrArray)
            el.setAttributeNode(unwrapAttr(attrRecord, doc));

        @SuppressWarnings("unchecked")
        GenericArray<Object> childArray = (GenericArray<Object>) record.get("children");
        for (Object childObj : childArray) {
            if (childObj instanceof GenericRecord)
                el.appendChild(unwrapElement((GenericRecord) childObj, doc));

            if (childObj instanceof Utf8)
                el.appendChild(doc.createTextNode("" + childObj));
        }

        return el;
    }

    private static Attr unwrapAttr(GenericRecord record, Document doc) {
        Attr attr = doc.createAttribute("" + record.get("name"));
        attr.setValue("" + record.get("value"));
        return attr;
    }

    private static void saveDocument(Document doc, File file) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.transform(new DOMSource(doc), new StreamResult(file));
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws IOException, SAXException {
        if (args.length != 3 || !Arrays.asList("xml", "avro").contains(args[0])) {
            System.out.println("Usage: \n {xml|avro} input-file output-file\n");
            System.exit(1);
        }

        File inputFile = new File(args[1]);
        File outputFile = new File(args[2]);

        String conversion = args[0];
        switch (conversion) {
            case "xml":
                avroToXml(inputFile, outputFile);
                break;
            case "avro":
                xmlToAvro(inputFile, outputFile);
                break;
        }
    }
}
