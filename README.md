# xml-avro
This project provides Converter to convert generic xsd/xml to asvc/avro files.
Avro schema and avro files are generated from xsd schema and xsd files.

## Running Project
1. git clone;
2. mvn package;
3. java -jar target/xml-avro*.jar avro &lt;xmlFile> &lt;avroFile>; // converts specified xml to avro file
4. java -jar target/xml-avro*.jar xml &lt;avroFile> &lt;xmlFile>;  // converts specified avro to xml file

## Usage
```
XML Avro converter.
Usage: <xsdFile> <xmlFile> {<avscFile>} {<avroFile>}
```
