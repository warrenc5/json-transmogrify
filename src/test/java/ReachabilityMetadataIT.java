import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.json.Json;
import jakarta.json.JsonPatch;
import jakarta.json.JsonStructure;
import java.io.FileReader;
import java.io.IOException;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 *
 * @author wozza
 */
public class ReachabilityMetadataIT {

    public static final String DATA = "/native-image/reachability-metadata.json";

    @Test
    @Disabled
    public void testDiff() throws IOException, TransformerException {
        JsonStructure orig = Json.createReader(ClassLoader.getSystemResourceAsStream("META-INF" + DATA)).readObject();
        JsonStructure gen = Json.createReader(new FileReader("target" + DATA)).readObject();

        JsonPatch diff = Json.createDiff(orig, gen);

        assertNotNull(diff);
        assertNotNull(diff.toJsonArray());
        assertFalse(diff.toJsonArray().isEmpty());
        assertNotNull(diff.toJsonArray().getFirst());
        System.out.println(diff.toString());
    }
}
