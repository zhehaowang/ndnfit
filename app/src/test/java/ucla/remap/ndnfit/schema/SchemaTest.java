package ucla.remap.ndnfit.timelocation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import ucla.remap.ndnfit.position.Position;
import ucla.remap.ndnfit.position.PositionFormatTester;
import ucla.remap.ndnfit.position.PositionList;

/**
 * Created by zhanght on 2015/12/21.
 */
public class SchemaTest {
    protected static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testTimeLocation() throws JsonProcessingException {
        TimeLocationList test = new TimeLocationList();
        test.addItem(new TimeLocation(3,3,3,3));
        test.addItem(new TimeLocation(1,1,1,1));
        test.addItem(new TimeLocation(2,2,2,2));
        test.addItem(new TimeLocation(2, 2, 2, System.currentTimeMillis()));
        String documentAsString = objectMapper.writeValueAsString(test.getItems());
        System.out.print(documentAsString);
        TimeLocationFormatTester tester = new TimeLocationFormatTester();
        assert(tester.isValid(test));
    }

    @Test
    public void testPosition() throws JsonProcessingException {
        PositionList test = new PositionList();
        test.addItem(new Position(3,3,3));
        test.addItem(new Position(1,1,1));
        test.addItem(new Position(2,2,2));
        test.addItem(new Position(2,2,System.currentTimeMillis()));
        String documentAsString = objectMapper.writeValueAsString(test.getItems());
        System.out.print(documentAsString);
        PositionFormatTester tester = new PositionFormatTester();
        assert(tester.isValid(test));
    }
}