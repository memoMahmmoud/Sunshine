package apps.mai.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
    private final static String LOG_TAG=MainActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container,new ForecastFragment(),"forecast fragment").commit();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            //when user click on setting menu itemm
            // open setting activity
            case R.id.action_settings:{
                Intent intent=new Intent(this,SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            //when user click on map menu itemm
            // look for app on device that will open the map
            case R.id.action_map:{
                openPreferredMapLocation();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void openPreferredMapLocation(){

        //get location stored in shared preference
        SharedPreferences sharedPreferences= PreferenceManager.getDefaultSharedPreferences(this);
        String location=sharedPreferences.getString(getString(R.string.pref_location_key),
                getString(R.string.pref_location_default_value));

        // build implict intent for view the map
        Intent intent=new Intent(Intent.ACTION_VIEW);
        Uri geoLocation=Uri.parse("geo:0,0?").buildUpon()
                .appendQueryParameter("q",location)
                .build();
        intent.setData(geoLocation);
        //we check first if there is one app or more to handle map intent to avoid crashing
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
        //avoid crashing
        else {
            Log.v(LOG_TAG,"couldn't open map");
        }

    }

}
