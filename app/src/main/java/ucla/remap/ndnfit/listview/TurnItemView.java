package ucla.remap.ndnfit.listview;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import ucla.remap.ndnfit.R;

/**
 * Created by nightzen on 5/4/15.
 */
public class TurnItemView extends LinearLayout {
    /**
     * Icon
     */
    private ImageView mIcon;

    /**
     * TextView for Start Time
     */
    private TextView mTxtStartTime;

    /**
     * TextView for End Time
     */
    private TextView mTxtEndTime;

    private TurnItem mItem;

    public TurnItemView(Context context, TurnItem aItem) {
        super(context);

        // Layout Inflation
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.view_turn_item, this, true);

        // Set Icon
        mIcon = (ImageView) findViewById(R.id.iconItem);
        mIcon.setImageDrawable(aItem.getIcon());

        mItem = aItem;

        // Set Text 01
        mTxtStartTime = (TextView) findViewById(R.id.dataStartTime);
        mTxtStartTime.setText(aItem.getStartTime());

        // Set Text 02
        mTxtEndTime = (TextView) findViewById(R.id.dataEndTime);
        mTxtEndTime.setText(aItem.getEndTime());
    }

    public void setStartTime(String time){
        this.mTxtStartTime.setText(time);
    }

    public void setEndTime(String time) {
        this.mTxtEndTime.setText(time);
    }
    /**
     * set Icon
     *
     * @param icon
     */
    public void setIcon(Drawable icon) {
        mIcon.setImageDrawable(icon);
    }

    public int getId() {
        return mItem.getId();
    }
}

