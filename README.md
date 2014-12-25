# xml-avro
This project provides Converter to convert generic xsd/xml to asvc/avro files.
Avro schema and avro file are generated from xsd schema and xsd file.

## Running Project
1. git clone;
2. mvn package;
3. java -jar target/xml-avro*.jar <xsdFile> <xmlFile> {<avscFile>} {<avroFile>} // converts specified xml+xsd to avro+asvc files

## Usage
```
XML Avro converter.
Usage: "{-d|--debug} {-b|--baseDir <baseDir>} <xsdFile> <xmlFile> {<avscFile>} {<avroFile>}"
```
## Restrictions
Converter has following restriction:
- xml docs with multiple namespaces are not supported;
- complex type extensions are not supported;