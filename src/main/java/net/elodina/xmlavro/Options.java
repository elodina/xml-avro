package net.elodina.xmlavro;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Options {
	static final String USAGE = "{-d|--debug} {-b|--baseDir <baseDir>} {-s|--stream|--stdout} {-xsd|--toAvsc <xsdFile> {<avscFile>}} "
			+ "{-xml|--toAvro <xmlFile> {<avroFile>} {-sb|--splitby <splitBy>}}";

	File xsdFile;
	File xmlFile;

	File avscFile;
	File avroFile;

	boolean debug = false;
	boolean stdout = false;

	List<Mode> modes = new ArrayList<Mode>();
	String split = "";

	File baseDir;

	enum Mode {
		XSD, XML;
	}

	public Options(String... args) {
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
						throw new IllegalArgumentException("Base directory location missing");
					i++;
					baseDir = new File(args[i]);
					break;
				case "-s":
				case "--stdout":
				case "--stream":
					stdout = true;
					break;
				case "-xsd":
				case "--toAvsc":
					if (i == args.length - 1)
						throw new IllegalArgumentException("XSD File missing");
					i++;
					xsdFile = replaceBaseDir(args[i], baseDir);

					if (i <= args.length - 1 && !args[i + 1].startsWith("-")) {
						i++;
						avscFile = replaceBaseDir(args[i], baseDir);
					} else
						avscFile = replaceExtension(xsdFile, "avsc");
					modes.add(Mode.XSD);
					break;

				case "-xml":
				case "--toAvro":
					if (!stdout && i == args.length - 1)
						throw new IllegalArgumentException("XML File missing");
					if (!stdout) {
						i++;
						xmlFile = replaceBaseDir(args[i], baseDir);

						if (i <= args.length - 1 && !args[i + 1].startsWith("-")) {
							i++;
							xmlFile = replaceBaseDir(args[i], baseDir);
						} else
							avroFile = replaceExtension(xsdFile, "avro");
					}
					modes.add(Mode.XML);
					break;

				case "-sb":
				case "--splitby":
					if (i == args.length - 1)
						throw new IllegalArgumentException("Split element name missing");
					i++;
					split = args[i];
				default:
					throw new IllegalArgumentException("Unsupported option " + arg);
				}
			} else {
				throw new IllegalArgumentException("Unsupported aruguments format " + arg);
			}

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
}
