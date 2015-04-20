# xml-avro
This project provides Converter to convert generic xsd/xml to asvc/avro files.
Avro schema and avro file are generated from xsd schema and xml file.

Additionally it includes simple schemaless converter that converts xml to avro and avro to xml.

## Running Project
1. git clone;
2. mvn package;
3. java -jar target/xml-avro*.jar <xsdFile> <xmlFile> {<avscFile>} {<avroFile>} // converts specified xml+xsd to avro+asvc files
4. java -cp target/xml-avro*.jar ly.stealth.xmlavro.simple.Converter avro <xmlFile> <avroFile> // convert xml to avro
5. java -cp target/xml-avro*.jar ly.stealth.xmlavro.simple.Converter xml <avroFile> <xmlFile> // convert avro to xml

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

## In code usage

There are two ways to generate an avro representation from XML, using Dom based parsing and using Sax based parsing. Dom parsing is fine for small documents but loads the entire file into memory. Sax parsing allows for you to stream over the contents of a large file and stream the result to an output stream.

### Dom Parsing
```java
Schema schema = Converter.createSchema(yourXsd);
GenericData.Record record = Converter.createDatum(schema, yourXml);
```
### Sax Parsing

Lets imagine you are loading a several gigabyte xml file... loading into memory is not an option. This solution will allow you to stream the contents, for example imagine an xml structure

```
root
  item
  item
    sub-item
  item
```  

In its current form the sax parser allows (schema permitting) the user to stream through the file and only load each "item" into memory one at a time and write its value to an output stream. More complex usages may come in the future if the demand is there... 

```java
Schema schema = Converter.createSchema(TestData.multiLevelParsingTest.xsd);

SaxClient saxClient = new SaxClient().withParsingDepth(AvroSaxHandler.ParsingDepth.ROOT_PLUS_ONE);
ByteArrayOutputStream out = new ByteArrayOutputStream();
InputStream inputStream = new ByteArrayInputStream(xmlFile.getBytes());
saxClient.readStream(schema, inputStream, out);

GenericDatumReader datumReader = new GenericDatumReader();
org.apache.avro.file.FileReader fileReader1 = DataFileReader.openReader(new SeekableByteArrayInput(out.toByteArray()), datumReader);

// obviously in the real world you would iterate over the items but as an example
GenericData.Array record =  (GenericData.Array) fileReader1.next();
GenericData.Record firstRecord = (GenericData.Record) record.get(0);

record = (GenericData.Array) fileReader1.next();
GenericData.Record secondRecord = (GenericData.Record) record.get(0);
```




