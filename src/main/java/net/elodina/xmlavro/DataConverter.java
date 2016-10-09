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
package net.elodina.xmlavro;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.file.CodecFactory;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;

public class DataConverter {
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
		return new DatumBuilder(schema).createDatum(xml);
	}

	public static <T> T createDatum(Schema schema, Reader reader) {
		return new DatumBuilder(schema).createDatum(reader);
	}

	public static <T> T createDatum(Schema schema, InputStream stream) {
		return new DatumBuilder(schema).createDatum(stream);
	}

	private static class Options {
		static final String USAGE = "{-s|--stream|--stdout} {-f|--file} <avscFile> {<xmlFile>} {<avroFile>} {-sb|--splitby <splitBy>}";

		enum Format {
			FILE, STREAM
		}

		File xmlFile;
		File avscFile;
		File avroFile;
		String splitBy;

		Format outFormat = Format.FILE;

		Options(String... args) {
			List<String> files = new ArrayList<>();

			for (int i = 0; i < args.length; i++) {
				String arg = args[i];

				if (arg.startsWith("-"))
					switch (arg) {
					case "-s":
					case "--stream":
					case "--stdout":
						outFormat = Format.STREAM;
						break;
					case "-f":
					case "--file":
						outFormat = Format.FILE;
						break;
					case "-sb":
					case "--splitby":
						i++;
						splitBy = args[i];
						break;
					default:
						throw new IllegalArgumentException("Unsupported option " + arg);
					}
				else
					files.add(arg);
			}

			if (outFormat == Format.FILE) {
				if (files.size() < 2 || files.size() > 3)
					throw new IllegalArgumentException("Incorrect number of in/out files. Expected [2..3]");

				avscFile = replaceBaseDir(files.get(0), null);
				xmlFile = replaceBaseDir(files.get(1), null);
				avroFile = files.size() > 2 ? replaceBaseDir(files.get(3), null) : replaceExtension(xmlFile, "avro");
			} else {
				if (files.size() != 1)
					throw new IllegalArgumentException("Avro Schema is mandatory");

				avscFile = replaceBaseDir(files.get(0), null);
			}
		}

		private static File replaceExtension(File file, String newExtension) {
			String fileName = file.getPath();

			int dotIdx = fileName.lastIndexOf('.');
			if (dotIdx != -1)
				fileName = fileName.substring(0, dotIdx);

			return new File(fileName + "." + newExtension);
		}

		private static File replaceBaseDir(String path, File baseDir) {
			File file = new File(path);

			if (baseDir == null || file.isAbsolute())
				return file;
			return new File(baseDir, file.getPath());
		}
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

		if (opts.outFormat == Options.Format.FILE)
			System.out.println("Converting: " + opts.xmlFile + " -> " + opts.avroFile);

		Schema schema = new Schema.Parser().parse(opts.avscFile);

		DatumBuilder datumBuilder = new DatumBuilder(schema);
		Object datum = null;
		if (opts.outFormat == Options.Format.FILE)
			datum = datumBuilder.createDatum(opts.xmlFile);
		else {
			BufferedInputStream br = new BufferedInputStream(System.in);
			datum = datumBuilder.createDatum(br);
		}
		try {
			OutputStream stream;
			if (opts.outFormat == Options.Format.FILE)
				stream = new FileOutputStream(opts.avroFile);
			else
				stream = new BufferedOutputStream(System.out);

			DatumWriter<Object> datumWriter = new SpecificDatumWriter<>(schema);
			DataFileWriter<Object> fileWriter = new DataFileWriter<>(datumWriter);
			fileWriter.setCodec(CodecFactory.snappyCodec());
			fileWriter.create(schema, stream);
			fileWriter.append(datum);
			fileWriter.flush();
			fileWriter.close();
		} catch (Exception e) {
			throw e;
		}
	}
}