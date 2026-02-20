//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 21
//JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -Dpolyglot.engine.WarnInterpreterOnly=false --enable-native-access=ALL-UNNAMED
//DEPS mobi.mofokom:graalson-trax:1.0.4-SNAPSHOT
//DEPS mobi.mofokom:graalson:1.0.7-SNAPSHOT
//FILES README.md
//MAIN_CLASS JsonT

import au.com.devnull.graalson.GraalsonProvider;
import au.com.devnull.graalson.trax.GraalsonResult;
import au.com.devnull.graalson.trax.GraalsonSource;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
            throws IOException, TransformerConfigurationException, TransformerException {
        if (args.length == 0 || Arrays.asList("/?", "-?", "-h", "--help").contains(args[0].trim())) {
            try (
                    PrintWriter out = new PrintWriter(System.err); InputStream in
                    = ClassLoader.getSystemClassLoader().getResourceAsStream("README.md")) {
                int c;
                while ((c = in.read()) != -1) {
                    out.write(c);
                }
                out.flush();
            }
            System.exit(1);
        }

        GraalsonProvider.useJavaxXmlTransformTransformerFactory();
        String templateFile = args.length < 1
                ? IDENTITY
                : args[0].isBlank()
                ? IDENTITY
                : args[0];
        String inputJson = args.length < 2
                ? STDIN
                : args[1].isBlank()
                ? STDIN
                : args[1];
        String outputFile = args.length < 3
                ? STDOUT
                : args[2].isBlank()
                ? STDOUT
                : args[2];
        String charset = args.length < 4
                ? UTF8
                : args[3].isBlank()
                ? UTF8
                : args[3];
        String spaces = args.length < 5
                ? SPACES
                : args[4].isBlank()
                ? SPACES
                : args[4];
        Map<String, Object> config = new HashMap<>();
        config.put("spaces", Integer.valueOf(spaces));
        JsonWriterFactory wfactory = Json.createWriterFactory(config);
        JsonWriter jwriter = wfactory.createWriter(
                STDOUT.equals(outputFile)
                ? new PrintWriter(System.out)
                : new FileWriter(outputFile)
        );
        Reader reader = null;
        JsonReader jreader = null;

        System.err.println(
                "template: "
                + templateFile
                + " source:"
                + inputJson
                + " result:"
                + outputFile
                + " charset:"
                + charset
        );
        if (STDIN.equals(inputJson)) {
            ReadableByteChannel channel = Channels.newChannel(System.in);

            reader = new InputStreamReader(
                    Channels.newInputStream(channel),
                    (UTF8.equalsIgnoreCase(charset))
                    ? java.nio.charset.StandardCharsets.UTF_8
                    : java.nio.charset.Charset.forName(charset)
            );
            jreader = Json.createReader(reader);
        } else if (System.console() != null
                && (inputJson == null || inputJson.equals("-"))) {
            reader = new InputStreamReader(
                    System.in,
                    (UTF8.equalsIgnoreCase(charset))
                    ? java.nio.charset.StandardCharsets.UTF_8
                    : java.nio.charset.Charset.forName(charset)
            );
            jreader = Json.createReader(reader);
        } else if (inputJson != null) {
            try {
                InputStream jsonStream
                        = ClassLoader.getSystemClassLoader().getResourceAsStream(
                                inputJson
                        );
                if (jsonStream == null) {
                    jsonStream = new java.io.FileInputStream(inputJson);
                }
                reader = new InputStreamReader(
                        jsonStream,
                        (charset == null || charset.equalsIgnoreCase("unknown"))
                        ? java.nio.charset.StandardCharsets.UTF_8
                        : java.nio.charset.Charset.forName(charset)
                );
                jreader = Json.createReader(reader);
            } catch (RuntimeException | IOException x) {
                System.err.println(inputJson + " - " + x.getMessage());
            }
        }
        Source template = null;
        try {
            template = new GraalsonSource(templateFile);
        } catch (Exception e) {
            template = new GraalsonSource(templateFile, new FileReader(templateFile));
        }

        Source source = new GraalsonSource(jreader);
        Result result = new GraalsonResult(jwriter);

        System.err.println(
                "template: "
                + template
                + " source:"
                + source
                + " result: "
                + result
                + " writer:"
                + jwriter
        );

        TransformerFactory.newInstance()
                .newTemplates(template)
                .newTransformer()
                .transform(source, result);
    }
}
