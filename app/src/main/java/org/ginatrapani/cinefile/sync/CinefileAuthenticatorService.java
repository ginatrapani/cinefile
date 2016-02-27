package org.ginatrapani.cinefile.sync;

import android.os.IBinder;
import android.app.Service;
import android.content.Intent;

/**
 * The service which allows the sync adapter framework to access the authenticator.
 */
public class CinefileAuthenticatorService extends Service {
    // Instance field that stores the authenticator object
    private CinefileAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new CinefileAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}