package ucla.remap.ndnfit.network;

import android.database.Cursor;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Name;
import net.named_data.jndn.util.Blob;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by zhtaoxiang on 1/7/16.
 */
public class DataUploader implements Runnable {
    private Face face;
    private NdnDBManager ndnDBManager = NdnDBManager.getInstance();
    private static final String TAG = "DataUploader";

    @Override
    public void run() {
        insertDataIntoRepo();
    }

    public void setFace(Face face) {
        this.face = face;
    }

    private void insertDataIntoRepo() {
        Log.d(TAG, "insertDataIntoRepo");
        try {
            Cursor catalogCursor = ndnDBManager.getAllUnuploadedCatalog();
            final List<Name> transferedCatalogs = new ArrayList<>();
            while (catalogCursor.moveToNext()) {
                byte[] raw = catalogCursor.getBlob(2);
                Data data = new Data();
                data.wireDecode(new Blob(raw));
                final Name name = data.getName();
                BasicInsertion.requestInsert
                        (face, NDNFitCommon.REPO_COMMAND_PREFIX, name,
                                new BasicInsertion.SimpleCallback() {
                                    public void exec() {
                                        Log.d(TAG,"Insert started for " + name.toUri());
                                        transferedCatalogs.add(name);
                                    }
                                },
                                new BasicInsertion.SimpleCallback() {
                                    public void exec() {
                                        // For failure, already printed the error.
                                    }
                                });
            }

            Cursor pointCursor = ndnDBManager.getAllUnuploadedPoints();
            final List<Name> transferedPoints = new ArrayList<>();
            while(pointCursor.moveToNext()) {
                byte[] raw = pointCursor.getBlob(1);
                Data data = new Data();
                data.wireDecode(new Blob(raw));
                final Name name = data.getName();
                BasicInsertion.requestInsert
                        (face, NDNFitCommon.REPO_COMMAND_PREFIX, name,
                                new BasicInsertion.SimpleCallback() {
                                    public void exec() {
                                        Log.d(TAG, "Insert started for " + name.toUri());
                                        transferedPoints.add(name);
                                    }
                                },
                                new BasicInsertion.SimpleCallback() {
                                    public void exec() {
                                        // For failure, already printed the error.
                                    }
                                });
            }
            catalogCursor.close();
            pointCursor.close();
            Thread.sleep(4000);

            for(Name one : transferedCatalogs) {
                ndnDBManager.markCatalogUploaded(one);
                Log.d(TAG, "Delete" + one.toUri());
            }
            for(Name one : transferedPoints) {
                ndnDBManager.markPointUploaded(one);
                Log.d(TAG, "Delete" + one.toUri());
            }
        } catch (Exception e) {
            Log.e(TAG, "exception: " + e.getMessage());
        }
    }
}
