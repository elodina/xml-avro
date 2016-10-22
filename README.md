# xml-avro
This project provides Converter to convert generic xsd/xml to asvc/avro files.
Avro schema and avro file are generated from xsd schema and xml file.

Additionally it includes simple schemaless converter that converts xml to avro and avro to xml.

## Running Project
1. git clone
2. gradle build
3. java -jar target/xml-avro*.jar <xsdFile> <xmlFile> {<avscFile>} {<avroFile>} // converts specified xml+xsd to avro+asvc files
4. java -cp target/xml-avro*.jar net.elodina.xmlavro.simple.Converter avro <xmlFile> <avroFile> // convert xml to avro
5. java -cp target/xml-avro*.jar net.elodina.xmlavro.simple.Converter xml <avroFile> <xmlFile> // convert avro to xml

## Schema-based converter
Usage:
```
XSD to AVSC Usage : {-d|--debug} {-b|--baseDir <baseDir>} {-xsd|--toAvsc <xsdFile> {<avscFile>}}
XML to AVRO Usage : {-s|--stream|--stdout} {-xml|--toAvro <avscFile> {<xmlFile>} {<avroFile>} {-sb|--splitby <splitBy>}}
Mixed Usage : {-d|--debug} {-b|--baseDir <baseDir>} {-xsd|--toAvsc <xsdFile> {<avscFile>}} {-s|--stream|--stdout} {-xml|--toAvro {<xmlFile>} {<avroFile>} {-sb|--splitby <splitBy>}}
```
## Restrictions
Schema-based converter currently only supports conversion in one direction: from xml to avro.

Converter has following restriction:
- xml docs with multiple namespaces are not supported;

## Simple converter
Usage:
```
{avro|xml} <inFile> <outFile>
```
Note: simple converter uses predefined general avro schema located at src/ly/stealth/xmlavro/simple/xml.avsc