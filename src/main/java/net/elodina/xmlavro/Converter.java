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
import java.util.ArrayList;
import java.util.List;

public class Converter {
    public static Schema createSchema(String xsd) {
        return new SchemaBuilder().createSchema(xsd);
    }

    public static Schema createSchema(File file) {
        return new SchemaBuilder().createSchema(file);
    }

    public static Schema createSchema(Reader reader) {
        return new SchemaBuilder().createSchema(reader);
    }

    public static Schema createSchema(InputStream stream) {
        return new SchemaBuilder().createSchema(stream);
    }

    public static <T> T createDatum(Schema schema, File file) {
        return new DatumBuilder(schema).createDatum(file);
    }

    public static <T> T createDatum(Schema schema, String xml) {
        List<T> datums = new DatumBuilder(schema).createDatum(xml);
        return datums.get(0);
    }

    public static <T> T createDatum(Schema schema, Reader reader) {
        return new DatumBuilder(schema).createDatum(reader);
    }

    public static <T> T createDatum(Schema schema, InputStream stream) {
        return new DatumBuilder(schema).createDatum(stream);
    }

    public static void main(String... args) throws IOException {
        Options opts;
        try {
            opts = new Options(args);
        } catch (IllegalArgumentException e) {
            System.out
                    .println("XML Avro converter.\nError: " + e.getMessage() + "\n" + "Usage: " + Options.USAGE + "\n");
            System.exit(1);
            return;
        }

        if (opts.outFormat == Options.Format.FILE) {
            System.out.println("Converting: \n" + opts.xsdFile + " -> " + opts.avscFile + "\n" + opts.xmlFile + " -> "
                    + opts.avroFile);
        }

        SchemaBuilder schemaBuilder = new SchemaBuilder();
        schemaBuilder.setDebug(opts.debug);
        if (opts.baseDir != null) {
            schemaBuilder.setResolver(new BaseDirResolver(opts.baseDir));
        }
        Schema schema = schemaBuilder.createSchema(opts.xsdFile);

        try (Writer writer = new FileWriter(opts.avscFile)) {
            writer.write(schema.toString(true));
        }

        DatumBuilder datumBuilder = new DatumBuilder(schema);
        List<Object> datums = null;
        if (opts.outFormat == Options.Format.FILE) {
            datums = datumBuilder.createDatum(opts.xmlFile);
        } else {
            BufferedInputStream br = new BufferedInputStream(System.in);
            datums = datumBuilder.createDatum(br);
        }
        try {
            OutputStream stream;
            if (opts.outFormat == Options.Format.FILE) {
                stream = new FileOutputStream(opts.avroFile);
            } else {
                stream = new BufferedOutputStream(System.out);
            }

            DatumWriter<Object> datumWriter = new SpecificDatumWriter<>(schema);
            // datumWriter.write(datum,EncoderFactory.get().directBinaryEncoder(stream,
            // null));
            DataFileWriter<Object> fileWriter = new DataFileWriter<>(datumWriter);
            fileWriter.setCodec(CodecFactory.snappyCodec());
            fileWriter.create(schema, stream);
            for (int i = 0; i < datums.size(); i++)
                fileWriter.append(datums.get(i));
            fileWriter.flush();
            fileWriter.close();
        } catch (Exception e) {
            throw e;
        }
    }

    private static class Options {
        static final String USAGE = "{-d|--debug} {-b|--baseDir <baseDir>} {-o|--output <outFormat>} <xsdFile> <xmlFile> {<avscFile>} {<avroFile>}";
        File xsdFile;
        File xmlFile;
        File avscFile;
        File avroFile;
        boolean debug;
        File baseDir;
        Format outFormat = Format.FILE;
        Options(String... args) {
            List<String> files = new ArrayList<>();

            for (int i = 0; i < args.length; i++) {
                String arg = args[i];

                if (arg.startsWith("-")) {
                    switch (arg) {
                        case "-d":
                        case "--debug":
                            debug = true;
                            break;
                        case "-b":
                        case "--baseDir":
                            if (i == args.length - 1)
                                throw new IllegalArgumentException("Base dir required");
                            i++;
                            baseDir = new File(args[i]);
                            break;
                        case "-o":
                        case "--output":
                            i++;
                            String value = args[i];
                            if (value.equalsIgnoreCase("file"))
                                outFormat = Format.FILE;
                            else if (value.equalsIgnoreCase("stdout") || value.equalsIgnoreCase("stream"))
                                outFormat = Format.STDOUT;
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported option " + arg);
                    }
                } else {
                    files.add(arg);
                }
            }

            if (outFormat == Format.FILE) {
                if (files.size() < 2 || files.size() > 4) {
                    throw new IllegalArgumentException("Incorrect number of in/out files. Expected [2..4]");
                }

                xsdFile = replaceBaseDir(files.get(0), baseDir);
                xmlFile = replaceBaseDir(files.get(1), baseDir);

                System.out.println(xsdFile.getAbsolutePath());

                avscFile = files.size() > 2 ? replaceBaseDir(files.get(2), baseDir) : replaceExtension(xsdFile, "avsc");
                avroFile = files.size() > 3 ? replaceBaseDir(files.get(3), baseDir) : replaceExtension(xmlFile, "avro");
            } else {
                if (files.size() != 1) {
                    throw new IllegalArgumentException("XSD File is mandatory");
                }

                xsdFile = replaceBaseDir(files.get(0), baseDir);
                avscFile = replaceExtension(xsdFile, "avsc");
            }
        }

        private static File replaceExtension(File file, String newExtension) {
            String fileName = file.getPath();

            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx != -1) {
                fileName = fileName.substring(0, dotIdx);
            }

            return new File(fileName + "." + newExtension);
        }

        private static File replaceBaseDir(String path, File baseDir) {
            File file = new File(path);

            if (baseDir == null || file.isAbsolute()) {
                return file;
            }
            return new File(baseDir, file.getPath());
        }

        enum Format {
            FILE, STDOUT
        }
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