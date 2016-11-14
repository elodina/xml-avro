/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.elodina.xmlavro;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;

import java.io.*;
import java.util.List;

public class AdvancedConverter {
    InputStream xmlIn;
    OutputStream avroOut;
    FileWriter avscOut;
    FileInputStream xsdIn;
    boolean debug;
    File baseDir;

    public static void main(String... args) throws IOException {
        Options opts;
        try {
            if (args.length == 0)
                throw new IllegalArgumentException("No Arguments specified");
            opts = new Options(args);
        } catch (IllegalArgumentException e) {
            System.out
                    .println("XML Avro converter\nError: " + e.getMessage() + "\n\n" + Options.USAGE + "\n");
            System.exit(1);
            return;
        }

        AdvancedConverter c = new AdvancedConverter();

        c.debug = opts.debug;
        c.baseDir = opts.baseDir;

        for (int i = 0; i < opts.modes.size(); i++) {
            Options.Mode mode = opts.modes.get(i);
            switch (mode) {
                case XSD:
                    c.xsdIn = new FileInputStream(opts.xsdFile);
                    c.avscOut = new FileWriter(opts.avscFile);
                    if (!opts.stdout) {
                        System.out.println("Converting: " + opts.xsdFile + " -> " + opts.avscFile);
                    }
                    c.convertXSD();
                    break;
                case XML:
                    if (opts.stdout) {
                        c.xmlIn = new BufferedInputStream(System.in);
                        c.avroOut = new BufferedOutputStream(System.out);
                    } else {
                        c.xmlIn = new FileInputStream(opts.xmlFile);
                        c.avroOut = new FileOutputStream(opts.avroFile);
                        System.out.println("Converting: " + opts.xmlFile + " -> " + opts.avroFile);
                    }
                    c.convertXML(opts.avscFile, opts.split);
                    break;
            }
        }
    }

    private void convertXSD() throws IOException {
        SchemaBuilder schemaBuilder = new SchemaBuilder();
        schemaBuilder.setDebug(debug);
        if (baseDir != null) {
            schemaBuilder.setResolver(new BaseDirResolver(baseDir));
        }
        Schema schema = schemaBuilder.createSchema(xsdIn);
        xsdIn.close();
        writeAvsc(schema);
    }

    private void writeAvsc(Schema schema) throws IOException {
        avscOut.write(schema.toString(true));
        avscOut.close();
    }

    private void convertXML(File avscFile, String split) throws IOException {
        Schema schema = new Schema.Parser().parse(avscFile);
        DatumBuilder datumBuilder = new DatumBuilder(schema, split);
        List<Object> datums = datumBuilder.createDatum(xmlIn);
        xmlIn.close();
        writeAvro(schema, datums);
    }

    private void writeAvro(Schema schema, List<Object> datums) throws IOException {
        DatumWriter<Object> datumWriter = new SpecificDatumWriter<>(schema);
        DataFileWriter<Object> fileWriter = new DataFileWriter<>(datumWriter);
        fileWriter.setCodec(CodecFactory.snappyCodec());
        fileWriter.create(schema, avroOut);
        for (int i = 0; i < datums.size(); i++)
            fileWriter.append(datums.get(i));
        fileWriter.flush();
        avroOut.close();
        fileWriter.close();
    }

    private static class BaseDirResolver implements SchemaBuilder.Resolver {
        private File baseDir;

        private BaseDirResolver(File baseDir) {
            this.baseDir = baseDir;
            // Change Working directory to the base directory
            System.setProperty("user.dir", baseDir.getAbsolutePath());
        }

        public InputStream getStream(String systemId) {
            File file = new File(baseDir, systemId);

            try {
                return new FileInputStream(file);
            } catch (FileNotFoundException e) {
                return null;
            }
        }
    }
}