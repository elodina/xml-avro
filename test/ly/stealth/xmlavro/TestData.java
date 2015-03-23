package ly.stealth.xmlavro;

public class TestData {

    public class rootRecord {

        public static final String xsd =
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

        public static final String xml =
                "<root>" +
                        "  <i>1</i>" +
                        "  <s>s</s>" +
                        "  <d>1.0</d>" +
                        "</root>";

    }

    public class arrayFromComplexTypeSequenceOfChoiceElements {

        public static final String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
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

        public static final String xml =
                "<root>" +
                        "<s>s</s>" +
                        "<i>1</i>" +
                        "<x>x1</x>" +
                        "<y>2</y>" +
                        "</root>";

        public static final String schema = "{" +
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
                "}";

        public static final String datum = "{" +
                "    's': 's'," +
                "    'i': 1," +
                "    'type0': [" +
                "        {'x': 'x1'}," +
                "        {'y': 2}" +
                "    ]" +
                "}";
    }


    public class attributes {

        public static final String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                        "  <xs:element name='root'>" +
                        "    <xs:complexType>" +
                        "      <xs:attribute name='required' use='required'/>" +
                        "      <xs:attribute name='prohibited' use='prohibited'/>" +
                        "      <xs:attribute name='optional' use='optional'/>" +
                        "    </xs:complexType>" +
                        "  </xs:element>" +
                        "</xs:schema>";

    }

    public class nestedRecursiveRecords {

        public static final String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:complexType name='type'>" +
                "    <xs:sequence>" +
                "      <xs:element name='node' type='type' minOccurs='0'/>" +
                "    </xs:sequence>" +
                "  </xs:complexType>" +
                "  <xs:element name='root' type='type'/>" +
                "</xs:schema>";

        public static final String datum = "{\"node\": {\"node\": null}}";

        public static final String xml = "<root><node></node></root>";

    }

    public class uniqueFieldNames {

        public static final String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                        "  <xs:complexType name='type'>" +
                        "    <xs:sequence>" +
                        "      <xs:element name='field' type='xs:string'/>" +
                        "    </xs:sequence>" +
                        "    <xs:attribute name='field' type='xs:string'/>" +
                        "  </xs:complexType>" +
                        "  <xs:element name='root' type='type'/>" +
                        "</xs:schema>";

        public static final String xml = "<root field='value'><field>value0</field></root>";

    }

    public class recordWithWildcardField {

        public static final String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                        "  <xs:complexType name='type'>" +
                        "    <xs:sequence>" +
                        "      <xs:element name='field' type='xs:string'/>" +
                        "      <xs:any/>" +
                        "    </xs:sequence>" +
                        "  </xs:complexType>" +
                        "  <xs:element name='root' type='type'/>" +
                        "</xs:schema>";

        // Two wildcard-matched elements
        public static final String xmlWithTwoWildcard =
                "<root>" +
                        "  <field>field</field>" +
                        "  <field0>field0</field0>" +
                        "  <field1>field1</field1>" +
                        "</root>";

        // No wildcard-matched element
        public static final String xmlWithNoWildcard = "<root><field>field</field></root>";

    }

    public class optionalElementValues {

        public static final String xsd =
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


    }

    public class array {

        public static final String xsd =
                "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                        "  <xs:element name='root'>" +
                        "    <xs:complexType>" +
                        "      <xs:sequence>" +
                        "        <xs:element name='value' type='xs:string' maxOccurs='unbounded'/>" +
                        "      </xs:sequence>" +
                        "    </xs:complexType>" +
                        "  </xs:element>" +
                        "</xs:schema>";

        public static final String xml = "<root>" +
                "  <value>1</value>" +
                "  <value>2</value>" +
                "  <value>3</value>" +
                "</root>";

    }


    public class arrayOfChoiceElements {

        public static final String xsd =
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

        public static final String xml = "<root><s>s</s><i>1</i><i>2</i></root>";

    }

    public class arrayFromComplexTypeChoiceElements {

        public static final String xsd = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
                "  <xs:element name='root'>" +
                "    <xs:complexType>" +
                "      <xs:choice maxOccurs='unbounded'>" +
                "        <xs:element name='s' type='xs:string'/>" +
                "        <xs:element name='i' type='xs:int'/>" +
                "      </xs:choice>" +
                "    </xs:complexType>" +
                "  </xs:element>" +
                "</xs:schema>";

        public static final String xml = "<root>" +
                "<s>s</s>" +
                "<i>1</i>" +
                "<i>2</i>" +
                "</root>";

        public static final String datum = "[" +
                "{'s': 's'}," +
                "{'i': 1}," +
                "{'i': 2}" +
                "]";

    }







}
