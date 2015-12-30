package ucla.remap.ndnfit.schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

/**
 * Created by zhanght on 2015/12/21.
 */
public class TimeLocationTest {
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void test() throws JsonProcessingException {
        TimeLocationList test = new TimeLocationList();
        test.addItem(new TimeLocation(3,3,3,3));
        test.addItem(new TimeLocation(1,1,1,1));
        test.addItem(new TimeLocation(2,2,2,2));
        test.addItem(new TimeLocation(2, 2, 2, System.currentTimeMillis()));
        String documentAsString = objectMapper.writeValueAsString(test.getItems());
        System.out.print(documentAsString);
        FormatTester tester = new FormatTester();
        assert(tester.isValid(test));
    }
}