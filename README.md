# xml-avro
This project provides Converter to convert generic xsd/xml to asvc/avro files.
Avro schema and avro file are generated from xsd schema and xml file.

Additionally it includes simple schemaless converter that converts xml to avro and avro to xml.

## Running Project
1. git clone;
2. mvn package;
3. java -jar target/xml-avro*.jar <xsdFile> <xmlFile> {<avscFile>} {<avroFile>} // converts specified xml+xsd to avro+asvc files
4. java -cp target/xml-avro*.jar net.elodina.xmlavro.simple.Converter avro <xmlFile> <avroFile> // convert xml to avro
5. java -cp target/xml-avro*.jar net.elodina.xmlavro.simple.Converter xml <avroFile> <xmlFile> // convert avro to xml

## Schema-based converter
Usage:
```
XML Avro converter.
Usage: "{-d|--debug} {-b|--baseDir <baseDir>} <xsdFile> <xmlFile> {<avscFile>} {<avroFile>}"
```
## Restrictions
Schema-based converter currently only supports conversion in one direction: from xml to avro.

Converter has following restriction:
- xml docs with multiple namespaces are not supported;
- complex type extensions are not supported;

## Simple converter
Usage:
```
{avro|xml} <inFile> <outFile>
```
Note: simple converter uses predefined general avro schema located at src/ly/stealth/xmlavro/simple/xml.avsc