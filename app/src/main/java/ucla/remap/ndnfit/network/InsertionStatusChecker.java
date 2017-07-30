package ucla.remap.ndnfit.network;

import android.database.Cursor;
import android.util.Log;

import net.named_data.jndn.Data;
import net.named_data.jndn.Face;
import net.named_data.jndn.Interest;
import net.named_data.jndn.Name;
import net.named_data.jndn.OnData;
import net.named_data.jndn.util.Blob;

import java.util.ArrayList;
import java.util.List;

import ucla.remap.ndnfit.NDNFitCommon;
import ucla.remap.ndnfit.ndndb.NdnDBManager;

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
    Log.d(TAG, "checkInsertionStatus is called, time: " + System.currentTimeMillis());
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

      Cursor cKeyCatalogCursor = ndnDBManager.getAllUnuploaedCKeyCatalog();
      final List<Name> cKeyCatalogsConfirmation = new ArrayList<>();
      while (cKeyCatalogCursor.moveToNext()) {
        byte[] raw = cKeyCatalogCursor.getBlob(1);
        Data data = new Data();
        data.wireDecode(new Blob(raw));
        final Name name = data.getName();
        Interest confirmInterest = new Interest(new Name(NDNFitCommon.CONFIRM_PREFIX).append(name));
        confirmInterest.setInterestLifetimeMilliseconds(4000);
        face.expressInterest(confirmInterest, new OnData() {
          @Override
          public void onData(Interest interest, Data data) {
            cKeyCatalogsConfirmation.add(name);
          }
        }, new RequestDataTimeOut());
      }
      cKeyCatalogCursor.close();

      Cursor cKeyCursor = ndnDBManager.getAllUnuploadedCKey();
      final List<Name> cKeyConfirmation = new ArrayList<>();
      while (cKeyCursor.moveToNext()) {
        byte[] raw = cKeyCursor.getBlob(1);
        Data data = new Data();
        data.wireDecode(new Blob(raw));
        final Name name = data.getName();
        Interest confirmInterest = new Interest(new Name(NDNFitCommon.CONFIRM_PREFIX).append(name));
        confirmInterest.setInterestLifetimeMilliseconds(4000);
        face.expressInterest(confirmInterest, new OnData() {
          @Override
          public void onData(Interest interest, Data data) {
            cKeyConfirmation.add(name);
          }
        }, new RequestDataTimeOut());
      }
      cKeyCursor.close();

      Thread.sleep(5000);

      for (Name one : catalogsConfirmation) {
        ndnDBManager.markCatalogUploaded(one);
        Log.d(TAG, "Marked " + one.toUri() + " as uploaded");
      }
      for (Name one : pointsConfirmation) {
        ndnDBManager.markPointUploaded(one);
        Log.d(TAG, "Marked " + one.toUri() + " as uploaded");
      }
      for (Name one : cKeyCatalogsConfirmation) {
        ndnDBManager.markCKeyCatalogUploaded(one);
        Log.d(TAG, "Marked " + one.toUri() + " as uploaded");
      }
      for (Name one : cKeyConfirmation) {
        ndnDBManager.markCKeyUploaded(one);
        Log.d(TAG, "Marked " + one.toUri() + " as uploaded");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
