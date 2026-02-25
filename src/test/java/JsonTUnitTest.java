
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.File;
import java.io.IOException;
import javax.xml.transform.TransformerException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.io.FileInputStream;

/**
 *
 * @author wozza
 */
public class JsonTUnitTest {

    @Test
    public void testTransform() throws IOException, TransformerException {
        File tempFile = File.createTempFile("output", ".json");
        JsonT.main(new String[]{"template.js", "ALL_FINES.json", tempFile.getAbsolutePath()});
        Assertions.assertTrue(tempFile.exists(), "Temp file does not exist");
        Assertions.assertTrue(tempFile.length() > 0, "Temp file is empty");
        try (FileInputStream fis = new FileInputStream(tempFile); JsonReader reader = Json.createReader(fis)) {
            JsonObject jsonObject = reader.readObject();
            Object resultsObj = jsonObject.get("results");
            Assertions.assertTrue(resultsObj instanceof java.util.List);
            java.util.List<?> results = (java.util.List<?>) resultsObj;
            //Assertions.assertEquals(662, results.size());
            Assertions.assertEquals(9, results.size());
        }
    }
}
