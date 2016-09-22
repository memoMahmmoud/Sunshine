package apps.mai.sunshine;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import apps.mai.sunshine.sync.SunshineSyncAdapter;

public class MainActivity extends AppCompatActivity implements ForecastFragment.Callback{
    private final static String LOG_TAG=MainActivity.class.getSimpleName();
    Toolbar toolbar;
    private final String FORECAST_FRAGMENT_TAG = "FFTAG";
    private String mLocation;
    private boolean mTwoPane;
    private static final String DETAIL_FRAGMENT_TAG = "deail_fragment_tag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocation = Utility.getPreferredLocation(this);

        toolbar= (Toolbar) findViewById(R.id.toolbar);

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setIcon(R.drawable.ic_logo);
        getSupportActionBar().setTitle("");

        if (findViewById(R.id.forecast_detail)!= null){
            // there were 2 pane
            if (savedInstanceState == null){
                getSupportFragmentManager().beginTransaction().replace(R.id.forecast_detail,
                        new DetailFragment(),
                        DETAIL_FRAGMENT_TAG).commit();
            }

            mTwoPane = true;
        }
        else {
            mTwoPane = false;
            //getSupportActionBar().setElevation(0);
        }
        ForecastFragment forecastFragment = (ForecastFragment)getSupportFragmentManager().
                findFragmentById(R.id.forecast_fragment);
        forecastFragment.setUseTodayLayout(!mTwoPane);

        SunshineSyncAdapter.initializeSyncAdapter(this);


    }

    /*@Override
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
    }*/




    @Override
    protected void onResume() {
        super.onResume();
        String location = Utility.getPreferredLocation( this );
        //Log.e("mai","Main Activity"+location);
        //update the location in our second pane using the fragment manager
        if (location != null && !location.equals(mLocation)) {
            ForecastFragment forecastFragment = (ForecastFragment)getSupportFragmentManager().
                    findFragmentById(R.id.forecast_fragment);

            if ( forecastFragment != null ) {
                forecastFragment.onLocationChanged();
            }
            DetailFragment df = (DetailFragment)getSupportFragmentManager().findFragmentByTag(DETAIL_FRAGMENT_TAG);
            if ( null != df ) {
                df.onLocationChanged(location);
            }
            mLocation = location;
        }
    }

    @Override
    public void onItemSelected(Uri dateUri) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putParcelable(DetailFragment.DETAIL_URI, dateUri);
            DetailFragment fragment = new DetailFragment();
            fragment.setArguments(args);
            getSupportFragmentManager().beginTransaction().replace
                    (R.id.forecast_detail, fragment, DETAIL_FRAGMENT_TAG).commit();
            } else {
            Intent intent = new Intent(this, DetailActivity.class).setData(dateUri);
            startActivity(intent);
        }
    }
}
