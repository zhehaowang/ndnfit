package ucla.remap.ndnfit.network;

import android.database.Cursor;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.encoding.EncodingException;
import net.named_data.jndn.util.Blob;

import java.util.ArrayList;
import java.util.List;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

/**
 * Created by zhtaoxiang on 1/30/16.
 */
public class InsertionStatusChecker implements Runnable {
    private Face face;
    private NdnDBManager ndnDBManager = NdnDBManager.getInstance();
    private static final String TAG = "InsertionStatusChecker";

    public void setFace(Face face) {
        this.face = face;
    }

    @Override
    public void run() {
        checkInsertionStatus();
    }

    private void checkInsertionStatus() {

        try {
            Cursor pointCursor = ndnDBManager.getAllUnuploadedPoints();
            final List<Name> pointsConfirmation = new ArrayList<>();
            while (pointCursor.moveToNext()) {
                byte[] raw = pointCursor.getBlob(1);
                Data data = new Data();
                data.wireDecode(new Blob(raw));
                final Name name = data.getName();
                Interest confirmInterest = new Interest(new Name(NDNFitCommon.CONFIRM_PREFIX).append(name));
                confirmInterest.setInterestLifetimeMilliseconds(4000);
                face.expressInterest(confirmInterest, new OnData() {
                    @Override
                    public void onData(Interest interest, Data data) {
                        pointsConfirmation.add(name);
                    }
                }, new RequestDataTimeOut());
            }
            pointCursor.close();

            Cursor catalogCursor = ndnDBManager.getAllUnuploadedCatalog();
            final List<Name> catalogsConfirmation = new ArrayList<>();
            while (catalogCursor.moveToNext()) {
                byte[] raw = catalogCursor.getBlob(2);
                Data data = new Data();
                data.wireDecode(new Blob(raw));
                final Name name = data.getName();
                Interest confirmInterest = new Interest(new Name(NDNFitCommon.CONFIRM_PREFIX).append(name));
                confirmInterest.setInterestLifetimeMilliseconds(4000);
                face.expressInterest(confirmInterest, new OnData() {
                    @Override
                    public void onData(Interest interest, Data data) {
                        catalogsConfirmation.add(name);
                    }
                }, new RequestDataTimeOut());
            }
            catalogCursor.close();

            Thread.sleep(5000);

            for(Name one : catalogsConfirmation) {
                ndnDBManager.markCatalogUploaded(one);
                Log.d(TAG, "Marked " + one.toUri() + " as uploaded");
            }
            for(Name one : pointsConfirmation) {
                ndnDBManager.markPointUploaded(one);
                Log.d(TAG, "Marked " + one.toUri() + " as uploaded");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
