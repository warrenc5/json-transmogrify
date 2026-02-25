//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -Dpolyglot.engine.WarnInterpreterOnly=false --enable-native-access=ALL-UNNAMED
//DEPS mobi.mofokom:graalson-trax:1.0.4-SNAPSHOT
//FILES README.md
//MAIN_CLASS JsonT

import au.com.devnull.graalson.trax.GraalsonResult;
import au.com.devnull.graalson.trax.GraalsonSource;
import au.com.devnull.graalson.trax.GraalsonTransformerFactory;
import au.com.devnull.graalson.trax.GraalsonTransformerFactory.JsonMode;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

/**
 *
 * @author wozza
 */
public class JsonT {

    public static final String STDIN = "-",
        STDOUT = "-",
        UTF8 = "UTF-8",
        IDENTITY = "identity.js",
        SPACES = "4";

    public static enum Operation {
        DIFF(JsonMode.JSON_DIFF),
        MERGE(JsonMode.JSON_MERGE),
        PATCH(JsonMode.JSON_PATCH_DIFF), //uses stepwise operations
        APPLY(JsonMode.JSON_PATCH_APPLY); //uses stepwise operations

        private final JsonMode jsonMode;

        Operation(JsonMode jsonMode) {
            this.jsonMode = jsonMode;
        }

        public JsonMode getJsonMode() {
            return jsonMode;
        }

        public static Operation fromString(String name) {
            try {
                return valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /**
     * @param args the command line arguments
     *
     * Operation mode (diff, merge, patch, apply):
     *   args[0] = operation name
     *   args[1] = first operand  (file path) — passed as template source
     *   args[2] = second operand (file path) — passed as transform input
     *   args[3] = result file or "-" for stdout (optional, defaults to "-")
     *
     * Template mode (legacy positional args):
     *   args[0] = template file
     *   args[1] = input json   (or "-" for stdin)
     *   args[2] = output file  (or "-" for stdout)
     *   args[3] = charset
     *   args[4] = spaces
     */
    public static void main(String[] args) throws IOException, TransformerConfigurationException, TransformerException {
        if (args.length == 0 || Arrays.asList("/?", "-?", "-h", "--help").contains(args[0].trim())) {
            try (
                PrintWriter out = new PrintWriter(System.err);
                InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream("README.md")
            ) {
                int c;
                while ((c = in.read()) != -1) {
                    out.write(c);
                }
                out.flush();
            }
            System.exit(1);
        }

        GraalsonTransformerFactory.useJavaxXmlTransformTransformerFactory();

        String templateFile = args.length < 1 ? IDENTITY : args[0].isBlank() ? IDENTITY : args[0];

        Operation operation = Operation.fromString(templateFile);

        if (operation != null) {
            // Operation mode:
            //   args[0] = operation
            //   args[1] = operand1 (first source)
            //   args[2] = operand2 (second source / transform input)
            //   args[3] = result file or "-" for stdout

            if (args.length < 3) {
                System.err.println(
                    "Operation " +
                        operation +
                        " requires two operand arguments: " +
                        operation.name().toLowerCase() +
                        " <operand1> <operand2> [<output>]"
                );
                System.exit(1);
            }

            String operand1Path = args[1];
            String operand2Path = args[2];
            String outputFile = args.length < 4 ? STDOUT : args[3].isBlank() ? STDOUT : args[3];

            JsonWriter jwriter = createJsonWriter(outputFile, SPACES);

            Source operand1 = STDIN.equals(operand1Path)
                ? readStdinSource(UTF8)
                : new GraalsonSource(Path.of(operand1Path));
            Source operand2 = STDIN.equals(operand2Path)
                ? readStdinSource(UTF8)
                : new GraalsonSource(Path.of(operand2Path));

            Result result = new GraalsonResult(jwriter);

            System.err.println(
                "operation: " +
                    operation +
                    " operand1: " +
                    operand1Path +
                    " operand2: " +
                    operand2Path +
                    " result: " +
                    outputFile
            );

            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(GraalsonTransformerFactory.JSON_MODE_ATTRIBUTE, operation.getJsonMode());
            factory.newTemplates(operand1).newTransformer().transform(operand2, result);
        } else {
            String inputJson = args.length < 2 ? STDIN : args[1].isBlank() ? STDIN : args[1];
            String outputFile = args.length < 3 ? STDOUT : args[2].isBlank() ? STDOUT : args[2];
            String charset = args.length < 4 ? UTF8 : args[3].isBlank() ? UTF8 : args[3];
            String spaces = args.length < 5 ? SPACES : args[4].isBlank() ? SPACES : args[4];

            JsonWriter jwriter = createJsonWriter(outputFile, spaces);

            System.err.println(
                "template: " +
                    templateFile +
                    " source: " +
                    inputJson +
                    " result: " +
                    outputFile +
                    " charset: " +
                    charset
            );

            Source template = new GraalsonSource(Path.of(templateFile));

            Source source = STDIN.equals(inputJson) ? readStdinSource(charset) : new GraalsonSource(Path.of(inputJson));

            Result result = new GraalsonResult(jwriter);
            result.setSystemId(STDOUT.equals(outputFile) ? "<stdout>" : outputFile);

            System.err.println("template: " + template + " source: " + source + " result: " + result);

            TransformerFactory factory = TransformerFactory.newInstance();
            factory.setAttribute(GraalsonTransformerFactory.JSON_MODE_ATTRIBUTE, JsonMode.JSON_TRANSFORM);
            factory.newTemplates(template).newTransformer().transform(source, result);
        }
    }

    /**
     * Create a JsonWriter that writes to the given file, or stdout if the path is "-".
     */
    private static JsonWriter createJsonWriter(String outputFile, String spaces) throws IOException {
        Map<String, Object> config = new HashMap<>();
        config.put("spaces", Integer.valueOf(spaces));
        JsonWriterFactory wfactory = Json.createWriterFactory(config);
        return wfactory.createWriter(
            STDOUT.equals(outputFile) ? new PrintWriter(System.out) : new FileWriter(outputFile)
        );
    }

    /**
     * Build a GraalsonSource that reads JSON from standard input.
     */
    private static Source readStdinSource(String charset) {
        Reader reader;
        if (System.console() != null) {
            reader = new InputStreamReader(
                System.in,
                (UTF8.equalsIgnoreCase(charset))
                    ? java.nio.charset.StandardCharsets.UTF_8
                    : java.nio.charset.Charset.forName(charset)
            );
        } else {
            ReadableByteChannel channel = Channels.newChannel(System.in);
            reader = new InputStreamReader(
                Channels.newInputStream(channel),
                (UTF8.equalsIgnoreCase(charset))
                    ? java.nio.charset.StandardCharsets.UTF_8
                    : java.nio.charset.Charset.forName(charset)
            );
        }
        JsonReader jreader = Json.createReader(reader);
        return new GraalsonSource(jreader);
    }
}
