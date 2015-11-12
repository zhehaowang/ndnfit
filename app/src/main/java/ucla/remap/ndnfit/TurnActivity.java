package ucla.remap.ndnfit;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import ucla.remap.ndnfit.listview.TurnInfo;
import ucla.remap.ndnfit.listview.TurnItem;
import ucla.remap.ndnfit.listview.TurnListAdapter;


public class TurnActivity extends ActionBarActivity {
    private static final String TAG = "TurnActivity";
    public static final int TRACK_ACTIVITY = 1003;

    private ListView mListView;
    private TurnListAdapter listAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_turn);

        // Set the adapter
        mListView = (ListView) findViewById(R.id.turn_list);
        listAdapter = new TurnListAdapter(this);
        mListView.setAdapter(listAdapter);

        // link an event handler
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getBaseContext(), TrackActivity.class);
                TurnItem item = listAdapter.getItem(position);
                intent.putExtra("turn_id", item.getId());
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                Toast.makeText(getApplicationContext(), "Click " + item.getId(), Toast.LENGTH_SHORT).show();
                startActivityForResult(intent, TRACK_ACTIVITY);
            }
        });
    }

    protected void onResume() {
        super.onResume();
        prepareData();
    }

    private void prepareData() {
        listAdapter.clearItems();
        ArrayList<TurnInfo> list = (ArrayList<TurnInfo>) getIntent().getSerializableExtra("turns");
        Resources res = getResources();
        Drawable icon =  res.getDrawable(R.drawable.sports);
        for(TurnInfo info : list) {
            listAdapter.addItem(new TurnItem(icon, info));
        }
        listAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_turn, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
