package ly.stealth.xmlavro;

public class TestData {

    public static final String xsd_rootRecord =
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

    public static final String xml_rootRecord =
            "<root>" +
                    "  <i>1</i>" +
                    "  <s>s</s>" +
                    "  <d>1.0</d>" +
                    "</root>";


    // Given
    public static final String xsd_arrayFromComplexTypeSequenceOfChoiceElements = "<xs:schema xmlns:xs='http://www.w3.org/2001/XMLSchema'>" +
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

    public static final String xml_arrayFromComplexTypeSequenceOfChoiceElements =
            "<root>" +
                    "<s>s</s>" +
                    "<i>1</i>" +
                    "<x>x1</x>" +
                    "<y>2</y>" +
                    "</root>";

    public static final String json_arrayFromComplexTypeSequenceOfChoiceElements = "{" +
            "    's': 's'," +
            "    'i': 1," +
            "    'type0': [" +
            "        {'x': 'x1'}," +
            "        {'y': 2}" +
            "    ]" +
            "}";

}
