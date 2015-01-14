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
package ly.stealth.xmlavro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;

import static junit.framework.Assert.*;

public class ConverterTest {
    @Test
    public void basic() {
        String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                     "  <xs:element name='root' type='xs:string'/>" +
                     "</xs:schema>";

        Converter.createSchema(xsd);

        try { // no namespace
            Converter.createSchema("<schema/>");
            fail();
        } catch (ConverterException e) {
            String message = e.getMessage();
            assertTrue(message, message.contains("http://www.w3.org/2001/XMLSchema"));
            assertTrue(message, message.contains("namespace"));
        }
    }

    @Test
    public void rootIntPrimitive() {
        rootPrimitiveWithType("xs:int", "-1", Schema.Type.INT, -1);
        rootPrimitiveWithType("xs:unsignedByte", "1", Schema.Type.INT, 1);
        rootPrimitiveWithType("xs:unsignedShort", "5", Schema.Type.INT, 5);
    }

    @Test
    public void rootLongPrimitive() {
        rootPrimitiveWithType("xs:long", "20", Schema.Type.LONG, (long) 20);
        rootPrimitiveWithType("xs:unsignedInt", "30", Schema.Type.LONG, (long) 30);
    }

    @Test
    public void rootDoublePrimitive() {
        rootPrimitiveWithType("xs:decimal", "999999999.999999999", Schema.Type.DOUBLE, 999999999.999999999);
    }

    @Test
    public void rootUnsignedLongShouldBeKeptAsAvroString() {
        rootPrimitiveWithType("xs:unsignedLong", "18446744073709551615", Schema.Type.STRING, "18446744073709551615");
    }

    @Test
    public void rootDateTimePrimitive() {
      rootPrimitiveWithType("xs:dateTime", "2014-10-30T14:58:33", Schema.Type.LONG, 1414681113000L);
      rootPrimitiveWithType("xs:dateTime", "2014-09-10T12:58:33", Schema.Type.LONG, 1410353913000L);

      DatumBuilder.setDefaultTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
      rootPrimitiveWithType("xs:dateTime", "2014-10-30T07:58:33", Schema.Type.LONG, 1414681113000L);
      rootPrimitiveWithType("xs:dateTime", "2014-09-10T05:58:33", Schema.Type.LONG, 1410353913000L);
    }

    public <T> void rootPrimitiveWithType(String xmlType, String xmlValue, Schema.Type avroType, T avroValue) {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "   <xs:element name='value' type='" + xmlType + "'/>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);
        assertEquals(avroType, schema.getType());

        String xml = "<value>" + xmlValue + "</value>";
        assertEquals(avroValue, Converter.createDatum(schema, xml));
    }

    @Test
    public void severalRoots() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "   <xs:element name='i' type='xs:int'/>" +
                "   <xs:element name='r'>" +
                "     <xs:complexType>" +
                "       <xs:sequence>" +
                "         <xs:element name='s' type='xs:string'/>" +
                "       </xs:sequence>" +
                "     </xs:complexType>" +
                "   </xs:element>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);
        assertEquals(Schema.Type.RECORD, schema.getType());
        assertTrue("Schema should have a valid name", schema.getName() != null && !schema.getName().isEmpty());
        assertEquals(Source.DOCUMENT, schema.getProp(Source.SOURCE));
        assertEquals(2, schema.getFields().size());

        Schema.Field field0 = schema.getFields().get(0);
        assertEquals("" + new Source("i"), field0.getProp(Source.SOURCE));
        assertEquals(Schema.Type.UNION, field0.schema().getType());
        assertEquals(Schema.Type.INT, field0.schema().getTypes().get(1).getType());
        assertEquals(Schema.Type.NULL, field0.schema().getTypes().get(0).getType());

        Schema.Field field1 = schema.getFields().get(1);
        assertEquals("" + new Source("r"), field1.getProp(Source.SOURCE));
        assertEquals(Schema.Type.UNION, field1.schema().getType());
        assertEquals(Schema.Type.RECORD, field1.schema().getTypes().get(1).getType());
        assertEquals(Schema.Type.NULL, field1.schema().getTypes().get(0).getType());

        String xml = "<i>5</i>";
        GenericData.Record record = Converter.createDatum(schema, xml);
        assertEquals(null, record.get("r"));
        assertEquals(5, record.get("i"));

        xml = "<r><s>s</s></r>";
        record = Converter.createDatum(schema, xml);
        GenericData.Record subRecord = (GenericData.Record) record.get("r");
        assertEquals("s", subRecord.get("s"));
    }

    @Test
    public void rootRecord() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "   <xs:element name='root'>" +
                "     <xs:complexType>" +
                "       <xs:sequence>" +
                "         <xs:element name='i' type='xs:int'/>" +
                "         <xs:element name='s' type='xs:string'/>" +
                "         <xs:element name='d' type='xs:double'/>" +
                "       </xs:sequence>" +
                "     </xs:complexType>" +
                "   </xs:element>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);
        assertEquals(Schema.Type.RECORD, schema.getType());
        assertEquals("AnonType_root", schema.getName());
        assertEquals(3, schema.getFields().size());

        assertEquals(Schema.Type.INT, schema.getField("i").schema().getType());
        assertEquals(Schema.Type.STRING, schema.getField("s").schema().getType());
        assertEquals(Schema.Type.DOUBLE, schema.getField("d").schema().getType());

        String xml =
                "<root>" +
                "  <i>1</i>" +
                "  <s>s</s>" +
                "  <d>1.0</d>" +
                "</root>";

        GenericData.Record record = Converter.createDatum(schema, xml);

        assertEquals(1, record.get("i"));
        assertEquals("s", record.get("s"));
        assertEquals(1.0, record.get("d"));
    }

    @Test
    public void nestedRecursiveRecords() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:complexType name='type'>" +
                "    <xs:sequence>" +
                "      <xs:element name='node' type='type' minOccurs='0'/>" +
                "    </xs:sequence>" +
                "  </xs:complexType>" +
                "  <xs:element name='root' type='type'/>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);

        Schema.Field field = schema.getField("node");
        Schema subSchema = field.schema();
        assertSame(schema, subSchema.getTypes().get(1));

        String xml = "<root><node></node></root>";
        GenericData.Record record = Converter.createDatum(schema, xml);

        GenericData.Record child = (GenericData.Record) record.get("node");
        assertEquals(record.getSchema(), child.getSchema());

        assertNull(child.get("node"));
    }

    @Test
    public void attributes() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root'>" +
                "    <xs:complexType>" +
                "      <xs:attribute name='required' use='required'/>" +
                "      <xs:attribute name='prohibited' use='prohibited'/>" +
                "      <xs:attribute name='optional' use='optional'/>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);

        Schema.Field required = schema.getField("required");
        assertEquals(Schema.Type.STRING, required.schema().getType());

        assertNull(schema.getField("prohibited"));

        Schema.Field optional = schema.getField("optional");
        assertEquals(Schema.Type.UNION, optional.schema().getType());
        assertEquals(
                Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)),
                optional.schema().getTypes()
        );

        String xml = "<root required='required' optional='optional'/>";
        GenericData.Record record = Converter.createDatum(schema, xml);
        assertEquals("required", record.get("required"));
        assertEquals("optional", record.get("optional"));

        xml = "<root required='required'/>";
        record = Converter.createDatum(schema, xml);
        assertEquals("required", record.get("required"));
        assertNull(record.get("optional"));
    }

    @Test
    public void uniqueFieldNames() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:complexType name='type'>" +
                "    <xs:sequence>" +
                "      <xs:element name='field' type='xs:string'/>" +
                "    </xs:sequence>" +
                "    <xs:attribute name='field' type='xs:string'/>" +
                "  </xs:complexType>" +
                "  <xs:element name='root' type='type'/>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);

        assertEquals(2, schema.getFields().size());
        Schema.Field field = schema.getField("field");
        assertNotNull(field);
        assertEquals("" + new Source("field", true), field.getProp(Source.SOURCE));

        Schema.Field field0 = schema.getField("field0");
        assertEquals("" + new Source("field", false), field0.getProp(Source.SOURCE));

        String xml = "<root field='value'><field>value0</field></root>";
        GenericData.Record record = Converter.createDatum(schema, xml);

        assertEquals("value", record.get("field"));
        assertEquals("value0", record.get("field0"));
    }

    @Test
    public void recordWithWildcardField() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:complexType name='type'>" +
                "    <xs:sequence>" +
                "      <xs:element name='field' type='xs:string'/>" +
                "      <xs:any/>" +
                "    </xs:sequence>" +
                "  </xs:complexType>" +
                "  <xs:element name='root' type='type'/>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);
        assertEquals(2, schema.getFields().size());

        Schema.Field wildcardField = schema.getField(Source.WILDCARD);
        assertEquals(Schema.Type.MAP, wildcardField.schema().getType());

        // Two wildcard-matched elements
        String xml =
                "<root>" +
                "  <field>field</field>" +
                "  <field0>field0</field0>" +
                "  <field1>field1</field1>" +
                "</root>";

        GenericData.Record record = Converter.createDatum(schema, xml);
        assertEquals("field", record.get("field"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> map = (java.util.Map<String, String>) record.get(Source.WILDCARD);

        assertEquals(2, map.size());
        assertEquals("field0", map.get("field0"));
        assertEquals("field1", map.get("field1"));

        // No wildcard-matched element
        xml = "<root><field>field</field></root>";
        record = Converter.createDatum(schema, xml);

        assertEquals("field", record.get("field"));
        assertEquals(Collections.emptyMap(), record.get(Source.WILDCARD));
    }

    @Test
    public void severalWildcards() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root'>" +
                "    <xs:complexType>" +
                "      <xs:sequence>" +
                "        <xs:any/>" +
                "        <xs:any/>" +
                "      </xs:sequence>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);
        assertEquals(1, schema.getFields().size());

        Schema.Field field = schema.getField(Source.WILDCARD);
        assertEquals(null, field.getProp(Source.SOURCE));
    }

    @Test
    public void optionalElementValues() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root'>" +
                "    <xs:complexType>" +
                "      <xs:sequence>" +
                "        <xs:element name='required' type='xs:string'/>" +
                "        <xs:element name='optional' type='xs:string' minOccurs='0'/>" +
                "      </xs:sequence>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);
        assertEquals(2, schema.getFields().size());

        Schema.Field requiredField = schema.getField("required");
        assertEquals(Schema.Type.STRING, requiredField.schema().getType());

        Schema.Field optionalField = schema.getField("optional");
        Schema optionalSchema = optionalField.schema();
        assertEquals(Schema.Type.UNION, optionalSchema.getType());

        assertEquals(
                Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)),
                optionalSchema.getTypes()
        );

        String xml = "<root><required>required</required></root>";
        GenericData.Record record = Converter.createDatum(schema, xml);

        assertEquals("required", record.get("required"));
        assertNull(record.get("optional"));

        xml = "<root>" +
              "  <required>required</required>" +
              "  <optional>optional</optional>" +
              "</root>";

        record = Converter.createDatum(schema, xml);
        assertEquals("optional", record.get("optional"));
    }

    @Test
    public void array() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root'>" +
                "    <xs:complexType>" +
                "      <xs:sequence>" +
                "        <xs:element name='value' type='xs:string' maxOccurs='unbounded'/>" +
                "      </xs:sequence>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);
        Schema.Field valueField = schema.getField("value");
        assertEquals(Schema.Type.ARRAY, valueField.schema().getType());
        assertEquals(Schema.Type.STRING, valueField.schema().getElementType().getType());

        String xml = "<root>" +
                     "  <value>1</value>" +
                     "  <value>2</value>" +
                     "  <value>3</value>" +
                     "</root>";

        GenericData.Record record = Converter.createDatum(schema, xml);
        assertEquals(Arrays.asList("1", "2", "3"), record.get("value"));
    }

    @Test
    public void choiceElements() {
        String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root'>" +
                "    <xs:complexType>" +
                "      <xs:choice>" +
                "        <xs:element name='s' type='xs:string'/>" +
                "        <xs:element name='i' type='xs:int'/>" +
                "      </xs:choice>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        Schema schema = Converter.createSchema(xsd);
        assertEquals(Schema.Type.RECORD, schema.getType());
        assertEquals(2, schema.getFields().size());

        Schema.Field sField = schema.getField("s");
        assertEquals(Schema.Type.UNION, sField.schema().getType());
        assertEquals(
                Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)),
                sField.schema().getTypes()
        );

        Schema.Field iField = schema.getField("i");
        assertEquals(Schema.Type.UNION, iField.schema().getType());
        assertEquals(Arrays.asList(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.INT)), iField.schema().getTypes());

        String xml = "<root><s>s</s></root>";
        GenericData.Record record = Converter.createDatum(schema, xml);
        assertEquals("s", record.get("s"));

        xml = "<root><i>1</i></root>";
        record = Converter.createDatum(schema, xml);
        assertEquals(1, record.get("i"));
    }

    @Test
    public void arrayOfUnboundedChoiceElements() {
      String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
              "  <xs:element name='root'>" +
              "    <xs:complexType>" +
              "      <xs:choice maxOccurs='unbounded'>" +
              "        <xs:element name='s' type='xs:string'/>" +
              "        <xs:element name='i' type='xs:int'/>" +
              "      </xs:choice>" +
              "    </xs:complexType>" +
              "  </xs:element>" +
              "</xs:schema>";

      Schema schema = Converter.createSchema(xsd);
      assertEquals(Schema.Type.ARRAY, schema.getType());
      final Schema elementType = schema.getElementType();
      assertEquals(Schema.Type.RECORD, elementType.getType());
    }

    @Test
    public void arrayOfChoiceElements() {
      String xsd =
              "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                      "  <xs:element name='root'>" +
                      "    <xs:complexType>" +
                      "      <xs:choice maxOccurs='3'>" +
                      "        <xs:element name='s' type='xs:string'/>" +
                      "        <xs:element name='i' type='xs:int'/>" +
                      "      </xs:choice>" +
                      "    </xs:complexType>" +
                      "  </xs:element>" +
                      "</xs:schema>";

      Schema schema = Converter.createSchema(xsd);
      assertEquals(Schema.Type.ARRAY, schema.getType());
      final Schema elementType = schema.getElementType();
      assertEquals(Schema.Type.RECORD, elementType.getType());

      assertEquals(2, elementType.getFields().size());

      String xml = "<root><s>s</s><i>1</i><i>2</i></root>";
      GenericData.Array record = Converter.createDatum(schema, xml);
      Object firstRecord = record.get(0);
      assertTrue(firstRecord instanceof GenericData.Record);
      assertEquals("s", ((GenericData.Record) firstRecord).get("s"));

      Object secondRecord = record.get(1);
      assertTrue(secondRecord instanceof GenericData.Record);
      assertEquals(1, ((GenericData.Record) secondRecord).get("i"));

      Object thirdRecord = record.get(2);
      assertTrue(thirdRecord instanceof GenericData.Record);
      assertEquals(2, ((GenericData.Record) thirdRecord).get("i"));
    }

    @Test
    public void arrayFromComplexTypeChoiceElements() throws JSONException {
        // Given
        String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root'>" +
                "    <xs:complexType>" +
                "      <xs:choice maxOccurs='unbounded'>" +
                "        <xs:element name='s' type='xs:string'/>" +
                "        <xs:element name='i' type='xs:int'/>" +
                "      </xs:choice>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        String xml = "<root>" +
                "<s>s</s>" +
                "<i>1</i>" +
                "<i>2</i>" +
                "</root>";

        // When
        Schema schema = Converter.createSchema(xsd);
        Object datum = Converter.createDatum(schema, xml);


        // Then
        JSONAssert.assertEquals("{" +
                "    'type': 'array'," +
                "    'items': {" +
                "        'type': 'record'," +
                "        'name': 'AnonType_root'," +
                "        'fields': [" +
                "            {" +
                "                'name': 's'," +
                "                'type': ['null', 'string']" +
                "            }," +
                "            {" +
                "                'name': 'i'," +
                "                'type': ['null', 'int']" +
                "            }" +
                "        ]" +
                "    }" +
                "}", schema.toString(), false);

        JSONAssert.assertEquals("[" +
                "{'s': 's'}," +
                "{'i': 1}," +
                "{'i': 2}" +
                "]", datum.toString(), false);
    }

    @Test
    public void arrayFromComplexTypeSequenceOfChoiceElements() throws JSONException {
        // Given
        String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root'>" +
                "    <xs:complexType>" +
                "     <xs:sequence>" +
                "        <xs:element name='s' type='xs:string'/>" +
                "        <xs:element name='i' type='xs:int'/>" +
                "        <xs:choice maxOccurs='2'>" +
                "          <xs:element name='x' type='xs:string'/>" +
                "          <xs:element name='y' type='xs:int'/>" +
                "        </xs:choice>" +
                "     </xs:sequence>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        String xml = 
                "<root>" +
                    "<s>s</s>" +
                    "<i>1</i>" +
                    "<x>x1</x>" +
                    "<y>2</y>" +
                "</root>";

        // When
        Schema schema = Converter.createSchema(xsd);
        Object datum = Converter.createDatum(schema, xml);

        // Then
        JSONAssert.assertEquals("{" +
                "    'type': 'record'," +
                "    'fields': [" +
                "        {" +
                "            'name': 's'," +
                "            'type': 'string'" +
                "        }," +
                "        {" +
                "            'name': 'i'," +
                "            'type': 'int'" +
                "        }," +
                "        {" +
                "            'name': 'type0'," +
                "            'type': {" +
                "                'type': 'array'," +
                "                'items': {" +
                "                    'type': 'record'," +
                "                    'name': 'type1'," +
                "                    'fields': [" +
                "                        {" +
                "                            'name': 'x'," +
                "                            'type': ['null','string']" +
                "                        }," +
                "                        {" +
                "                            'name': 'y'," +
                "                            'type': ['null','int']" +
                "                        }" +
                "                    ]" +
                "                }" +
                "            }" +
                "        }" +
                "    ]" +
                "}", schema.toString(), false);

        JSONAssert.assertEquals("{" +
                "    's': 's'," +
                "    'i': 1," +
                "    'type0': [" +
                "        {'x': 'x1'}," +
                "        {'y': 2}" +
                "    ]" +
                "}", datum.toString(), false);
    }

    @Test
    public void SchemaBuilder_validName() {
        SchemaBuilder builder = new SchemaBuilder();

        assertNull(builder.validName(null));
        assertEquals("", builder.validName(""));

        assertEquals("a1", builder.validName("$a#1"));

        assertEquals("a_1", builder.validName("a.1"));
        assertEquals("a_1", builder.validName("a-1"));

        // built-in types
        assertEquals("string0", builder.validName("string"));
        assertEquals("record1", builder.validName("record"));
    }
}
