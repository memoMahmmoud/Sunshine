package apps.mai.sunshine;


import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import apps.mai.sunshine.data.WeatherContract;
import apps.mai.sunshine.data.WeatherContract.LocationEntry;
import apps.mai.sunshine.data.WeatherContract.WeatherEntry;
import apps.mai.sunshine.sync.SunshineSyncAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>{
    ListView forecastListView;
    ForecastAdapter forecastCursorAdapter;
    public static final int FORECAST_LOADER = 0;
    int mPosition;
    public static final String SELECTED_KEY_POSITION = "position";
    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
            WeatherEntry.COLUMN_DATE,
            WeatherEntry.COLUMN_SHORT_DESC,
            WeatherEntry.COLUMN_MAX_TEMP,
            WeatherEntry.COLUMN_MIN_TEMP,
            LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherEntry.COLUMN_WEATHER_ID,
            LocationEntry.COLUMN_COORD_LAT,
            LocationEntry.COLUMN_COORD_LONG
    };

    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;
    boolean mUseTodayLayout;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    public interface Callback {
       public void onItemSelected(Uri dateUri);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings: {
                Intent intent = new Intent(getContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.action_refresh: {
                updateWeather();
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        forecastListView = (ListView) view.findViewById(R.id.listview_forecast);



        // Sort order:  Ascending, by date.
        //String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";

        forecastCursorAdapter= new ForecastAdapter(getContext(),null,0);

        forecastListView.setAdapter(forecastCursorAdapter);
        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(i);
                String location_setting = Utility.getPreferredLocation(getContext());
                if (cursor!=null){
                    ((Callback) getActivity()).onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                            location_setting, cursor.getLong(COL_WEATHER_DATE)
                    ));

                }
                mPosition = i;
            }
        });
        if (savedInstanceState !=null && savedInstanceState.containsKey(SELECTED_KEY_POSITION)){
            mPosition = savedInstanceState.getInt(SELECTED_KEY_POSITION);

        }
        forecastCursorAdapter.setUseTodayLayout(mUseTodayLayout);
        return view;
    }// on create method



    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mPosition != ListView.INVALID_POSITION){
            outState.putInt(SELECTED_KEY_POSITION,mPosition);
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        //intialize this cursor loader for loader manager
        getLoaderManager().initLoader(FORECAST_LOADER,null,this);
        super.onActivityCreated(savedInstanceState);
    }
    void onLocationChanged( ) {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
    }





    private void updateWeather() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String locationValue = sharedPreferences.getString
                (getString(R.string.pref_location_key), getString(R.string.pref_location_default_value));

        //FetchWeatherTask fetchWeatherTask = new FetchWeatherTask(getContext());
        //fetchWeatherTask.execute(locationValue);

        //Intent intent = new Intent(getActivity(), SunshineService.class);
        //intent.putExtra(SunshineService.LOCATION_EXTRA,locationValue);
        //getActivity().startService(intent);
        /*Intent alarmIntent = new Intent(getActivity(), SunshineService.AlarmReceiver.class);
        alarmIntent.putExtra(SunshineService.LOCATION_EXTRA,locationValue);

        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService
                (Context.ALARM_SERVICE);
        //Wrap in a pending intent which only fires once.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(),0,alarmIntent,
                PendingIntent.FLAG_ONE_SHOT);
        //Set the AlarmManager to wake up the system even if the phone is sleep
        alarmManager.set(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+5000,pendingIntent);*/
        SunshineSyncAdapter.SyncImmediately(getActivity());
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String locationSetting = Utility.getPreferredLocation(getActivity());
        //Uri buildWeatherWithLocationUri = WeatherEntry.buildWeatherLocation(locationSetting);
        Uri buildWeatherWithLocationAndStartDateUri = WeatherEntry.buildWeatherLocationWithStartDate
                (locationSetting,System.currentTimeMillis());
        return new CursorLoader(getContext(),buildWeatherWithLocationAndStartDateUri,FORECAST_COLUMNS,null,null,null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        forecastCursorAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION){
            forecastListView.smoothScrollToPosition(mPosition);

        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

        forecastCursorAdapter.swapCursor(null);

    }
    public void setUseTodayLayout(boolean useTodayLayout){

        mUseTodayLayout = useTodayLayout;
        if (forecastCursorAdapter!=null){
            forecastCursorAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }
}

