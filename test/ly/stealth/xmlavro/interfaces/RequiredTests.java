package ly.stealth.xmlavro.interfaces;

import org.apache.avro.Schema;
import org.json.JSONException;

public interface RequiredTests {
    
    public void basic();

    
    public void rootIntPrimitive();

    
    public void rootLongPrimitive();

    
    public void rootDoublePrimitive();

    
    public void rootUnsignedLongShouldBeKeptAsAvroString();

    
    public void rootDateTimePrimitive() ;

    public void severalRoots();
    
    public void rootRecord();

    
    public void nestedRecursiveRecords();

    
    public void attributes();
    
    public void uniqueFieldNames();

    
    public void recordWithWildcardField();

    
    public void severalWildcards();

    
    public void optionalElementValues();
    
    public void array();

    
    public void choiceElements();

    
    public void arrayOfUnboundedChoiceElements();
    
    public void arrayOfChoiceElements();

    
    public void arrayFromComplexTypeChoiceElements() throws JSONException;

    
    public void arrayFromComplexTypeSequenceOfChoiceElements() throws JSONException;
    
    public void SchemaBuilder_validName();
}
