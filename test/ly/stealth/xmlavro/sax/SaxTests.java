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
package ly.stealth.xmlavro.sax;

import junit.framework.Assert;
import ly.stealth.xmlavro.Converter;
import org.apache.avro.Schema;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

public class SaxTests {

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


    @Test
    public void noFile() {

        SaxClient saxClient = new SaxClient();
        Schema schema = Converter.createSchema(xsd);
        try {
            saxClient.readFile(schema, "file.xml", System.out);
            fail( "My method didn't throw when I expected it to" );
        } catch (Exception e) {
            assertTrue("we should have a file not found here", e.toString().contains("FileNotFoundException"));
        }
    }

    @Test
    public void readInAFileWithNoExceptions() throws IOException {
        TemporaryFolder temporaryFolder = new TemporaryFolder();
        File file = temporaryFolder.newFile("foo.bar");
        SaxClient saxClient = new SaxClient();
        Schema schema = Converter.createSchema(xsd);
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            saxClient.readFile(schema, "xml/sax/basic.xml", fileOutputStream);
        } catch (Exception e) {
            fail("I was not expecting any failures in this test");
        }

        File expectedOutput = new File("test/ly/stealth/xmlavro/sax/outputs/expected_output_one");
        Assert.assertEquals("The response should be foo", new String(Files.readAllBytes(expectedOutput.toPath())), new String(Files.readAllBytes(file.toPath())));

    }





}
