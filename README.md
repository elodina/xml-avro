# xml-avro

This project provides Converter to convert generic xml file to avro and vice versa.

## Running Project
1. git clone it;
2. mvn package;
3. java -jar target/xml-avro*.jar avro <xmlFile> <avroFile>; // convert specified xml to avro file
4. java -jar target/xml-avro*.jar xml <avroFile> <xmlFile>;  // convert specified avro to xml file

## Usage
```
Usage:
 {xml|avro} input-file output-file
```

## Restrictions
Only restricted set of XML is supported:
- elements;
- attributes;
- text nodes;

Not supported right now:
- namespaces;
- processing instructions;
- xml declarations;
- CDATA;
- comments;