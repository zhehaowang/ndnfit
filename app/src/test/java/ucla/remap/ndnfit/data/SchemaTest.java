package ucla.remap.ndnfit.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;

import ucla.remap.ndnfit.timelocation.TimeLocation;
import ucla.remap.ndnfit.timelocation.TimeLocationFormatTester;
import ucla.remap.ndnfit.timelocation.TimeLocationList;

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
        PositionListProcessor test = new PositionListProcessor();
        test.addItem(new Position(3,3,3));
        test.addItem(new Position(1,1,1));
        test.addItem(new Position(2,2,2));
        test.addItem(new Position(2,2,System.currentTimeMillis()));
        String documentAsString = objectMapper.writeValueAsString(test.getItems());
        System.out.print(documentAsString);
        PositionFormatTester tester = new PositionFormatTester();
        assert(tester.isValid(test));
    }

    @Test
    public void testTurn() throws JsonProcessingException {
        Turn turn = new Turn();
        turn.setStartTimeStamp(1);
        turn.setFinishTimeStamp(2);
        String documentAsString = objectMapper.writeValueAsString(turn);
        System.out.print(documentAsString);
        TurnFormatTester tester = new TurnFormatTester();
        assert(tester.isValid(turn));
    }

    @Test
    public void testCatalog() throws JsonProcessingException {
        Catalog catalog = new Catalog();
        catalog.addPointTime(3);
        catalog.addPointTime(1);
        catalog.addPointTime(2);
        String documentAsString = objectMapper.writeValueAsString(catalog.getPointTime());
        System.out.print(documentAsString);
        CatalogFormatTester tester = new CatalogFormatTester();
        assert(tester.isValid(catalog));
    }
}