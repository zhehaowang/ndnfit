package ucla.remap.ndnfit.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhtaoxiang on 1/26/16.
 */
public class UpdateInfoList {
    private final List<UpdateInfo> items = new ArrayList<>();

    public void addItem(UpdateInfo one) {
        items.add(one);
    }

    public List<UpdateInfo> getItems() {
        return items;
    }
}
