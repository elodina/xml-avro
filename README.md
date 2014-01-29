# xml-avro

This project provides Converter to convert generic xml file to avro and vice versa.

## Running Project
1. git clone;
2. mvn package;
3. java -jar target/xml-avro*.jar avro &lt;xmlFile> &lt;avroFile>; // converts specified xml to avro file
4. java -jar target/xml-avro*.jar xml &lt;avroFile> &lt;xmlFile>;  // converts specified avro to xml file

## Usage
```
Usage:
 {xml|avro} input-file output-file
```

## Restrictions
Only restricted set of XML nodes is supported:
- elements;
- attributes;
- text nodes;

Following is not supported:
- namespaces;
- processing instructions;
- xml declarations;
- CDATA;
- comments;