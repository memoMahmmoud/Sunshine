package apps.mai.sunshine.service;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;

import apps.mai.sunshine.R;
import apps.mai.sunshine.data.WeatherContract;

/**
 * Created by Mai_ on 17-Sep-16.
 */
public class SunshineService extends IntentService {
    public static final String LOCATION_EXTRA = "location";
    private static final String LOG_TAG = SunshineService.class.getSimpleName();
    public SunshineService() {
        super("SunshineService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String location = intent.getStringExtra(LOCATION_EXTRA);
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String forecastJsonStr;

        try {
            final String FORECAST_URL_BASE = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final int NUM_DAYS = 14;
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            final String UNITS = prefs.getString(getString(R.string.pref_units_key),
                    getString(R.string.pref_units_default_value));
            Uri builtUri = Uri.parse(FORECAST_URL_BASE).buildUpon()
                    .appendQueryParameter("AppID", getString(R.string.app_id_weather_map_api))
                    .appendQueryParameter("q", location)
                    .appendQueryParameter("cnt", String.valueOf(NUM_DAYS))
                    .appendQueryParameter("units", UNITS)
                    .build();

            URL urlForWhetherMap = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) urlForWhetherMap.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            InputStreamReader inputStreamReader = new InputStreamReader(urlConnection.getInputStream());
            reader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuffer.append(line + "\n");
            }
            if (stringBuffer.length() != 0) {
                forecastJsonStr = stringBuffer.toString();
                Log.e(LOG_TAG, "foresacast String :" + forecastJsonStr);
                getWeatherDateFromJson(forecastJsonStr,location);

            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
        } catch (JSONException e) {
            Log.e(LOG_TAG,"Error ", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e("PlaceholderFragment", "Error closing stream", e);
                }
            }
        }//finally in try-catch


    }
    private void getWeatherDateFromJson(String jsonString,String location_setting) throws JSONException {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        gregorianCalendar.setTime(new Date(System.currentTimeMillis()));
        gregorianCalendar.set(Calendar.HOUR_OF_DAY, 0);
        gregorianCalendar.set(Calendar.MINUTE, 0);
        gregorianCalendar.set(Calendar.SECOND, 0);
        gregorianCalendar.set(Calendar.MILLISECOND, 0);


        JSONObject jsonObject = new JSONObject(jsonString);
        JSONObject jSONObjectForCityKey = jsonObject.getJSONObject("city");
        String city_name = jSONObjectForCityKey.getString("name");
        JSONObject jsonObjectForCoordKey = jSONObjectForCityKey.getJSONObject("coord");
        double lat = jsonObjectForCoordKey.getDouble("lat");
        double lon = jsonObjectForCoordKey.getDouble("lon");

        long locationId = addLocation(location_setting,city_name,lat,lon);

        JSONArray jsonArrayForList = jsonObject.getJSONArray("list");
        // Insert the new weather information into the database
        Vector<ContentValues> cVVector = new Vector<ContentValues>(jsonArrayForList.length());
        // this jsonArrayForList contain all arrays for weather data
        for (int i = 0; i < jsonArrayForList.length(); i++) {
            JSONObject jsonObjectDay = jsonArrayForList.getJSONObject(i);
            JSONArray jsonArrayForWeather = jsonObjectDay.getJSONArray("weather");
            JSONObject jsonObjectForWeather = jsonArrayForWeather.getJSONObject(0);
            String description = jsonObjectForWeather.getString("description");
            double humidity = jsonObjectDay.getDouble("humidity");
            double pressure = jsonObjectDay.getDouble("pressure");
            double windSpeed = jsonObjectDay.getDouble("speed");
            double windDirection = jsonObjectDay.getDouble("deg");
            //for weather icon
            int weather_id = jsonObjectForWeather.getInt("id");
            if (i != 0) {
                gregorianCalendar.add(Calendar.DAY_OF_MONTH, 1);
            }

            long date = gregorianCalendar.getTime().getTime();
            JSONObject jsonObjectForTemp = jsonObjectDay.getJSONObject("temp");
            double high = jsonObjectForTemp.getDouble("max");
            double low = jsonObjectForTemp.getDouble("min");


            ContentValues weatherValues = new ContentValues();

            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOCATION_ID, locationId);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, date);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, weather_id);

            cVVector.add(weatherValues);

        }
        // add to database
        if ( cVVector.size() > 0 ) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);
        }
        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocation(
                location_setting);

        // Students: Uncomment the next lines to display what what you stored in the bulkInsert
        Cursor cur = getContentResolver().query(weatherForLocationUri,
                null, null, null, sortOrder);

        cVVector = new Vector<ContentValues>(cur.getCount());
        if ( cur.moveToFirst() ) {
            do {
                ContentValues cv = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cur, cv);
                cVVector.add(cv);
            } while (cur.moveToNext());
        }
        cur.close();
        Log.e("mai", "FetchWeatherTask Complete. " + cVVector.size() + " Inserted");
        //String[] resultStrs = convertContentValuesToUXFormat(cVVector);
        //return resultStrs;

    }
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId;

        // First, check if the location with this city name exists in the db
        Cursor locationCursor = getContentResolver().query(
                WeatherContract.LocationEntry.CONTENT_URI,
                new String[]{WeatherContract.LocationEntry._ID},
                WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(WeatherContract.LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, lon);

            // Finally, insert location data into the database.
            Uri insertedUri = getContentResolver().insert(
                    WeatherContract.LocationEntry.CONTENT_URI,
                    locationValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            locationId = ContentUris.parseId(insertedUri);
        }

        locationCursor.close();
        // Wait, that worked?  Yes!
        return locationId;
    }
    public static class AlarmReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            String location = intent.getStringExtra(SunshineService.LOCATION_EXTRA);
            Intent sendIntent = new Intent(context,SunshineService.class);
            sendIntent.putExtra(SunshineService.LOCATION_EXTRA,location);
            context.startService(sendIntent);

        }
    }

}
