package ly.secore.KeyLoader.Database;

import java.io.File;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Reader class for reading JSON files.
 * This class uses Jackson to read a JSON file and print its contents.
 */
public class Reader {
    public static Product[] readJSONFile(String filePath) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(new File(filePath));

            // Extract the "products" array
            JsonNode productsNode = rootNode.path("products");

            // Convert to Product[]
            return objectMapper.readerFor(Product[].class).readValue(productsNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read file: " + filePath, e);
        }
    }
}
