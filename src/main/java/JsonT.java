
import au.com.devnull.graalson.trax.GraalsonResult;
import au.com.devnull.graalson.trax.GraalsonSource;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException, TransformerConfigurationException, TransformerException {
        System.setProperty("javax.xml.transform.TransformerFactory", "au.com.devnull.graalson.trax.GraalsonTransformerFactory");

        String inputJson = args.length < 1 ? null : args[0];//read from stream or file
        String templateFile = args.length < 2 ? null : args[1];
        String outputFile = args.length < 3 ? null : args[2] == null ? "unknown" : args[2];
        String charset = args.length < 4 ? null : args[3] == null ? "unknown" : args[3];

        Map<String, Object> config = new HashMap<>();
        config.put("spaces", Integer.valueOf(4));
        JsonWriterFactory wfactory = Json.createWriterFactory(config);

        JsonWriter jwriter = wfactory.createWriter(outputFile == null ? new PrintWriter(System.out) : new FileWriter(outputFile));

        Reader reader = null;
        if (System.inheritedChannel() != null) {
            System.err.print("unsupported");
        } else {
            //reader = 
        }
        JsonReader jreader = Json.createReader(ClassLoader.getSystemClassLoader().getResourceAsStream(inputJson));

        Source template = new GraalsonSource(templateFile);
        Source source = new GraalsonSource(jreader);
        Result result = new GraalsonResult(jwriter);

        System.out.println("template: " + template + " source:" + source + " writer:" + jwriter);
        //((GraalsonSource)source).writeStructure(jwriter);

        TransformerFactory.newInstance().newTemplates(template).newTransformer().transform(source, result);
    }

}
