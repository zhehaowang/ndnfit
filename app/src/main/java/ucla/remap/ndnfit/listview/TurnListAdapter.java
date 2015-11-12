package ucla.remap.ndnfit.listview;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nightzen on 5/4/15.
 */
public class TurnListAdapter extends BaseAdapter{
    private Context mContext;

    private List<TurnItem> mItems = new ArrayList<TurnItem>();

    public TurnListAdapter(Context context) {
        mContext = context;
    }

    public void addItem(TurnItem it) {
        mItems.add(it);
    }

    public void setListItems(List<TurnItem> lit) {
        mItems = lit;
    }

    public int getCount() {
        return mItems.size();
    }

    public TurnItem getItem(int position) {
        return mItems.get(position);
    }

    public boolean areAllItemsSelectable() {
        return false;
    }

    public boolean isSelectable(int position) {
        try {
            return mItems.get(position).isSelectable();
        } catch (IndexOutOfBoundsException ex) {
            return false;
        }
    }

    public void clearItems() {
        mItems.clear();
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        TurnItemView itemView;
        if (convertView == null) {
            itemView = new TurnItemView(mContext, mItems.get(position));
        } else {
            itemView = (TurnItemView) convertView;

            itemView.setIcon(mItems.get(position).getIcon());
            itemView.setStartTime(mItems.get(position).getStartTime());
            itemView.setEndTime(mItems.get(position).getEndTime());
        }

        return itemView;
    }
}
