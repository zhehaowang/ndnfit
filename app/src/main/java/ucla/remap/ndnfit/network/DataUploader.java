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

    @Override
    public void run() {
        insertDataIntoRepo();
    }

    public void setFace(Face face) {
        this.face = face;
    }

    private void insertDataIntoRepo() {
        Log.e("haitao", "insertDataIntoRepo");
        try {
            Cursor catalogCursor = ndnDBManager.getAllCatalog();
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
                                        System.out.println("Insert started for " + name.toUri());
                                        transferedCatalogs.add(name);
                                    }
                                },
                                new BasicInsertion.SimpleCallback() {
                                    public void exec() {
                                        // For failure, already printed the error.
                                    }
                                });
            }

            Cursor pointCursor = ndnDBManager.getAllPoints();
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
                                        System.out.println("Insert started for " + name.toUri());
                                        transferedPoints.add(name);
                                    }
                                },
                                new BasicInsertion.SimpleCallback() {
                                    public void exec() {
                                        // For failure, already printed the error.
                                    }
                                });
            }
            Thread.sleep(4000);

            //check if the data is inserted into repo successfully
            for(final Name one : transferedCatalogs) {
                BasicInsertion.insertCheck(face, NDNFitCommon.REPO_COMMAND_PREFIX, one,
                        new BasicInsertion.SimpleCallback() {
                            public void exec() {
                                System.out.println("Insert succeeded for " + one.toUri());
                            }
                        },
                        new BasicInsertion.SimpleCallback() {
                            public void exec() {
                                transferedCatalogs.remove(one);
                                // For failure, already printed the error.
                            }
                        });
            }

            for(final Name one : transferedPoints) {
                BasicInsertion.insertCheck(face, NDNFitCommon.REPO_COMMAND_PREFIX, one,
                        new BasicInsertion.SimpleCallback() {
                            public void exec() {
                                System.out.println("Insert succeeded for " + one.toUri());
                            }
                        },
                        new BasicInsertion.SimpleCallback() {
                            public void exec() {
                                transferedCatalogs.remove(one);
                                // For failure, already printed the error.
                            }
                        });
            }


            for(Name one : transferedCatalogs) {
                ndnDBManager.deleteCatalog(one);
                System.out.println("Delete" + one.toUri());
            }
            for(Name one : transferedPoints) {
                ndnDBManager.deletePoint(one);
                System.out.println("Delete" + one.toUri());
            }
        } catch (Exception e) {
            System.out.println("exception: " + e.getMessage());
        }
    }
}
