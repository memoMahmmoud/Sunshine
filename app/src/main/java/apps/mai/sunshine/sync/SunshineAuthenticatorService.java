package apps.mai.sunshine.sync;

/**
 * Created by Mai_ on 19-Sep-16.
 */

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;

/**
 * The service which allows the sync adapter framework to access the authenticator.
 */
public class SunshineAuthenticatorService extends Service{
    private SunshineAuthenticator sunshineAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        sunshineAuthenticator = new SunshineAuthenticator(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return sunshineAuthenticator.getIBinder();
    }
}
