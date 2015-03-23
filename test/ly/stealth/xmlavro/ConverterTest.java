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
      DatumBuilder.setDefaultTimeZone(TimeZone.getTimeZone("UTC-0"));

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
    public void severalRootsOne() {
        Schema schema = Converter.createSchema(TestData.severalRoots.xsd);

        String xml = "<i>5</i>";
        GenericData.Record record = Converter.createDatum(schema, xml);
        assertEquals(null, record.get("r"));
        assertEquals(5, record.get("i"));
    }

    @Test
    public void severalRootsTwo() {
        Schema schema = Converter.createSchema(TestData.severalRoots.xsd);

        String xml = "<r><s>s</s></r>";
        GenericData.Record record = Converter.createDatum(schema, xml);
        GenericData.Record subRecord = (GenericData.Record) record.get("r");
        assertEquals("s", subRecord.get("s"));
    }

    @Test
    public void rootRecord() {
        Schema schema = Converter.createSchema(TestData.rootRecord.xsd);
        assertEquals(Schema.Type.RECORD, schema.getType());
        assertEquals("AnonType_root", schema.getName());
        assertEquals(3, schema.getFields().size());

        assertEquals(Schema.Type.INT, schema.getField("i").schema().getType());
        assertEquals(Schema.Type.STRING, schema.getField("s").schema().getType());
        assertEquals(Schema.Type.DOUBLE, schema.getField("d").schema().getType());

        GenericData.Record record = Converter.createDatum(schema, TestData.rootRecord.xml);

        assertEquals(1, record.get("i"));
        assertEquals("s", record.get("s"));
        assertEquals(1.0, record.get("d"));
    }

    @Test
    public void nestedRecursiveRecords() throws JSONException {
        Schema schema = Converter.createSchema(TestData.nestedRecursiveRecords.xsd);

        Schema.Field field = schema.getField("node");
        Schema subSchema = field.schema();
        assertSame(schema, subSchema.getTypes().get(1));

        GenericData.Record record = Converter.createDatum(schema, TestData.nestedRecursiveRecords.xml);

        JSONAssert.assertEquals(TestData.nestedRecursiveRecords.datum, record.toString(), false);

        GenericData.Record child = (GenericData.Record) record.get("node");
        assertEquals(record.getSchema(), child.getSchema());

        assertNull(child.get("node"));
    }

    @Test
    public void attributes() {
        Schema schema = Converter.createSchema(TestData.attributes.xsd);

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
        Schema schema = Converter.createSchema(TestData.uniqueFieldNames.xsd);

        GenericData.Record record = Converter.createDatum(schema, TestData.uniqueFieldNames.xml);

        assertEquals("value", record.get("field"));
        assertEquals("value0", record.get("field0"));
    }

    @Test
    public void recordWithWildcardField() {
        Schema schema = Converter.createSchema(TestData.recordWithWildcardField.xsd);
        assertEquals(2, schema.getFields().size());

        Schema.Field wildcardField = schema.getField(Source.WILDCARD);
        assertEquals(Schema.Type.MAP, wildcardField.schema().getType());

        GenericData.Record record = Converter.createDatum(schema, TestData.recordWithWildcardField.xmlWithTwoWildcard);
        assertEquals("field", record.get("field"));

        @SuppressWarnings("unchecked")
        java.util.Map<String, String> map = (java.util.Map<String, String>) record.get(Source.WILDCARD);

        assertEquals(2, map.size());
        assertEquals("field0", map.get("field0"));
        assertEquals("field1", map.get("field1"));
    }

    @Test
    public void recordWithNoWildcardField() {
        Schema schema = Converter.createSchema(TestData.recordWithWildcardField.xsd);
        assertEquals(2, schema.getFields().size());

        Schema.Field wildcardField = schema.getField(Source.WILDCARD);
        assertEquals(Schema.Type.MAP, wildcardField.schema().getType());

        GenericData.Record record = Converter.createDatum(schema, TestData.recordWithWildcardField.xmlWithNoWildcard);

        assertEquals("field", record.get("field"));
        assertEquals(Collections.emptyMap(), record.get(Source.WILDCARD));
    }

    @Test
    public void optionalElementValues() {
        Schema schema = Converter.createSchema(TestData.optionalElementValues.xsd);
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
        Schema schema = Converter.createSchema(TestData.array.xsd);
        GenericData.Record record = Converter.createDatum(schema, TestData.array.xml);
        assertEquals(Arrays.asList("1", "2", "3"), record.get("value"));
    }

    @Test
    public void choiceElementsOne() {
        Schema schema = Converter.createSchema(TestData.choiceElements.xsd);
        String xml = "<root><s>s</s></root>";
        GenericData.Record record = Converter.createDatum(schema, xml);
        assertEquals("s", record.get("s"));
    }

    @Test
    public void choiceElementsTwo() {
        Schema schema = Converter.createSchema(TestData.choiceElements.xsd);
        String xml = "<root><i>1</i></root>";
        GenericData.Record  record = Converter.createDatum(schema, xml);
        assertEquals(1, record.get("i"));
    }

    @Test
    public void arrayOfChoiceElements() {
      Schema schema = Converter.createSchema(TestData.arrayOfChoiceElements.xsd);
      assertEquals(Schema.Type.ARRAY, schema.getType());
      final Schema elementType = schema.getElementType();
      assertEquals(Schema.Type.RECORD, elementType.getType());

      assertEquals(2, elementType.getFields().size());

      GenericData.Array record = Converter.createDatum(schema, TestData.arrayOfChoiceElements.xml);
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
        Schema schema = Converter.createSchema(TestData.arrayFromComplexTypeChoiceElements.xsd);
        Object datum = Converter.createDatum(schema, TestData.arrayFromComplexTypeChoiceElements.xml);

        JSONAssert.assertEquals(TestData.arrayFromComplexTypeChoiceElements.datum, datum.toString(), false);
    }

    @Test
    public void arrayFromComplexTypeSequenceOfChoiceElements() throws JSONException {

        // When
        Schema schema = Converter.createSchema(TestData.arrayFromComplexTypeSequenceOfChoiceElements.xsd);
        Object datum = Converter.createDatum(schema, TestData.arrayFromComplexTypeSequenceOfChoiceElements.xml);

        // Then
        JSONAssert.assertEquals(TestData.arrayFromComplexTypeSequenceOfChoiceElements.schema, schema.toString(), false);
        JSONAssert.assertEquals(TestData.arrayFromComplexTypeSequenceOfChoiceElements.datum, datum.toString(), false);
    }

}
