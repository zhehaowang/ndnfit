package ucla.remap.ndnfit.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import java.io.File;
import java.io.IOException;

/**
 * Created by zhanght on 2015/12/23.
 */
public class CatalogFormatTester {
    private static final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory.byDefault();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    File schemaFile = new File("./src/main/res/raw/catalog.json");
    JsonSchema jsonSchema;

    public CatalogFormatTester() {
        try {
            jsonSchema = jsonSchemaFactory.getJsonSchema(schemaFile.toURI().toString());
        } catch(ProcessingException e) {
            e.printStackTrace();
        }
    }

    public boolean isValid(Catalog data) {
        try {
            String documentAsString = objectMapper.writeValueAsString(data.getPointTime());
            JsonNode documentNode = objectMapper.readTree(documentAsString);
            ProcessingReport report = jsonSchema.validate(documentNode);
            return report.isSuccess();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (ProcessingException e) {
            e.printStackTrace();
            return false;
        }
    }
}
