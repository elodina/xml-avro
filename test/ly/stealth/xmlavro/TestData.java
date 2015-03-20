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

    }




}
