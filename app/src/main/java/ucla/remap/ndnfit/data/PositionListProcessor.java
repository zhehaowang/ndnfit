package ucla.remap.ndnfit.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ucla.remap.ndnfit.NDNFitCommon;

/**
 * Created by zhanght on 1/1/2016.
 */
public class PositionListProcessor {
    private final List<Position> items = new ArrayList<>();
//    private final List<List<Long>> catalog_timestamps = new ArrayList<>();
    private final List<List<Position>> group_items = new ArrayList<>();
    private final Turn turnInfo = new Turn();

    private static final int NUM_OF_ITEMS_EACH_RANGE = 50;

    public List<Position> getItems() {
        return items;
    }

    public void setItems(List<Position> items) {
        this.items.addAll(items);
        Collections.sort(this.items);
    }

    public void addItem(Position one) {
        this.items.add(one);
        Collections.sort(this.items);
    }

    /**
     * sort all the items and split them into different 10 minutes ranges
     */
    public void processItems() {
        Collections.sort(items);
        // This code records the start timestamp of a turn;
        turnInfo.setStartTimeStamp(items.get(0).getTimeStamp());

        long timeRangeEnd = (turnInfo.getStartTimeStamp() / NDNFitCommon.CATALOG_TIME_RANGE + 1) * NDNFitCommon.CATALOG_TIME_RANGE;
        //This piece of code splits all the items into different 10 minutes ranges
        int count = 0;
        List<Position> oneRangeItems = new ArrayList<>();
        group_items.add(oneRangeItems);
        for (Position onePosition : items) {
            if (onePosition.getTimeStamp() >= timeRangeEnd) {
                timeRangeEnd += NDNFitCommon.CATALOG_TIME_RANGE;
                count = 0;
                oneRangeItems = new ArrayList<>();
                group_items.add(oneRangeItems);
            }
            if(count >= NUM_OF_ITEMS_EACH_RANGE) {
                count = 0;
                oneRangeItems = new ArrayList<>();
                group_items.add(oneRangeItems);
            }
            oneRangeItems.add(onePosition);
        }

        // This code records the finish timestamp of a turn;
        turnInfo.setFinishTimeStamp(oneRangeItems.get(0).getTimeStamp());
    }

    public List<List<Position>> getGroupItems() {
        return group_items;
    }

    public Turn getTurn() {
        return turnInfo;
    }
}
