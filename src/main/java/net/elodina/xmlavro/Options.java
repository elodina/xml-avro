package net.elodina.xmlavro;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Options {
    static final String USAGE1 = "{-d|--debug} {-b|--baseDir <baseDir>} {-xsd|--toAvsc <xsdFile> {<avscFile>}}";
    static final String USAGE2 = "{-s|--stream|--stdout} {-xml|--toAvro <avscFile> {<xmlFile>} {<avroFile>} {-sb|--splitby <splitBy>}}";
    static final String USAGE3 = "{-d|--debug} {-b|--baseDir <baseDir>} {-xsd|--toAvsc <xsdFile> {<avscFile>}} {-s|--stream|--stdout} {-xml|--toAvro {<xmlFile>} {<avroFile>} {-sb|--splitby <splitBy>}}";

    static final String USAGE = "XSD to AVSC Usage : "+USAGE1 + "\nXML to AVRO Usage : " + USAGE2 + "\nMixed Usage : " + USAGE3;

    File xsdFile = null;
    File xmlFile = null;

    File avscFile = null;
    File avroFile = null;

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
                            throw new IllegalArgumentException("Base directory location missing in arguments");
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
                            throw new IllegalArgumentException("XSD File missing in arguments");
                        i++;
                        xsdFile = replaceBaseDir(args[i], baseDir);

                        if (i < args.length - 1 && !args[i + 1].startsWith("-")) {
                            i++;
                            avscFile = replaceBaseDir(args[i], baseDir);
                        } else
                            avscFile = replaceExtension(xsdFile, "avsc");
                        modes.add(Mode.XSD);
                        break;

                    case "-xml":
                    case "--toAvro":
                        if (avscFile == null && i == args.length - 1)
                            throw new IllegalArgumentException("AVSC File missing in arguments");
                        else if (avscFile == null) {
                            i++;
                            avscFile = replaceBaseDir(args[i], baseDir);
                        }
                        if (!stdout && i == args.length - 1)
                            throw new IllegalArgumentException("XML File missing in arguments");
                        if (!stdout) {
                            i++;
                            xmlFile = replaceBaseDir(args[i], baseDir);

                            if (i < args.length - 1 && !args[i + 1].startsWith("-")) {
                                i++;
                                xmlFile = replaceBaseDir(args[i], baseDir);
                            } else
                                avroFile = replaceExtension(xmlFile, "avro");
                        }
                        modes.add(Mode.XML);
                        break;

                    case "-sb":
                    case "--splitby":
                        if (i == args.length - 1)
                            throw new IllegalArgumentException("Split element name missing");
                        i++;
                        split = args[i];
                        break;
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
