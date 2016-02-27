package org.ginatrapani.cinefile.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by ginatrapani on 2/27/16.
 */
public class CinefileSyncService extends Service {

    private final String LOG_TAG = CinefileSyncService.class.getSimpleName();

    private static final Object sSyncAdapterLock = new Object();

    private static CinefileSyncAdapter sCinefileSyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d(LOG_TAG, "Creating sync service");
        synchronized (sSyncAdapterLock) {
            if (sCinefileSyncAdapter == null) {
                sCinefileSyncAdapter = new CinefileSyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sCinefileSyncAdapter.getSyncAdapterBinder();
    }
}