package apps.mai.sunshine;

import android.test.AndroidTestCase;

import apps.mai.sunshine.data.WeatherContract;

/**
 * Created by Mai_ on 18-Aug-16.
 */
public class TestFetchWeatherTask extends AndroidTestCase{
    static final String ADD_LOCATION_SETTING = "Sunnydale, CA";
    static final String ADD_LOCATION_CITY = "Sunnydale";
    static final double ADD_LOCATION_LAT = 34.425833;
    static final double ADD_LOCATION_LON = -119.714167;
    public void addLocation(){
        // start from a clean state
        getContext().getContentResolver().delete(WeatherContract.LocationEntry.CONTENT_URI,
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING+" = ?",
                new String[]{ADD_LOCATION_SETTING});
        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask(getContext(),null);
        long loction_row_id = fetchWeatherTask.addLocation(ADD_LOCATION_SETTING,ADD_LOCATION_CITY,ADD_LOCATION_LAT,
                ADD_LOCATION_LON);
        assertTrue(loction_row_id != -1);

    }
}
