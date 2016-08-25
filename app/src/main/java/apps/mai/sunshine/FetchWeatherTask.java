package apps.mai.sunshine;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Vector;

import apps.mai.sunshine.data.WeatherContract.LocationEntry;
import apps.mai.sunshine.data.WeatherContract.WeatherEntry;

/**
 * Created by Mai_ on 18-Aug-16.
 */
public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {
    private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
    private HttpURLConnection urlConnection;
    BufferedReader reader;
    Context context;
    String forecastJsonStr;
    ArrayAdapter<String> forecastArrayAdapter;
    FetchWeatherTask(Context context,ArrayAdapter<String> forecastArrayAdapter){
        this.context = context;
        this.forecastArrayAdapter = forecastArrayAdapter;
    }

    @Override
    protected String[] doInBackground(String... strings) {

        try {
            final String FORECAST_URL_BASE = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final int NUM_DAYS = 14;
            final String LOCATION = strings[0];
            Uri builtUri = Uri.parse(FORECAST_URL_BASE).buildUpon()
                    .appendQueryParameter("AppID", context.getString(R.string.app_id_weather_map_api))
                    .appendQueryParameter("q", LOCATION)
                    .appendQueryParameter("cnt", String.valueOf(NUM_DAYS))
                    .appendQueryParameter("units", "metric")
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
                Log.v(LOG_TAG, "foresacast String :" + forecastJsonStr);
                return getWeatherDateFromJson(forecastJsonStr,LOCATION);

            } else {
                return null;
            }

        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            return null;
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Error ", e);
            return null;
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

    }//finish do in background
    private String[] getWeatherDateFromJson(String jsonString,String location_setting) throws JSONException {
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

            weatherValues.put(WeatherEntry.COLUMN_LOCATION_ID, locationId);
            weatherValues.put(WeatherEntry.COLUMN_DATE, date);
            weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
            weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
            weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
            weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
            weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
            weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
            weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
            weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weather_id);

            cVVector.add(weatherValues);

        }
        // add to database
        if ( cVVector.size() > 0 ) {
            ContentValues[] cvArray = new ContentValues[cVVector.size()];
            cVVector.toArray(cvArray);
            context.getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, cvArray);
        }
        // Sort order:  Ascending, by date.
        String sortOrder = WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocation(
                location_setting);

        // Students: Uncomment the next lines to display what what you stored in the bulkInsert
        Cursor cur = context.getContentResolver().query(weatherForLocationUri,
                null, null, null, sortOrder);

        cVVector = new Vector<ContentValues>(cur.getCount());
        if ( cur.moveToFirst() ) {
            do {
                ContentValues cv = new ContentValues();
                DatabaseUtils.cursorRowToContentValues(cur, cv);
                cVVector.add(cv);
            } while (cur.moveToNext());
        }

        Log.e("mai", "FetchWeatherTask Complete. " + cVVector.size() + " Inserted");
        String[] resultStrs = convertContentValuesToUXFormat(cVVector);
        return resultStrs;

    }
    String[] convertContentValuesToUXFormat(Vector<ContentValues> cvv) {
        // return strings to keep UI functional for now
        String[] resultStrs = new String[cvv.size()];
        for ( int i = 0; i < cvv.size(); i++ ) {
            ContentValues weatherValues = cvv.elementAt(i);
            String highAndLow = makeReadHighTemperature(
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MAX_TEMP),
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MIN_TEMP));
            resultStrs[i] = (getReadableDate(
                    weatherValues.getAsLong(WeatherEntry.COLUMN_DATE))) +
                    " - " + weatherValues.getAsString(WeatherEntry.COLUMN_SHORT_DESC) +
                    " - " + highAndLow;


        }
        return resultStrs;
    }
    //To make readable date as "Thu Jul 21"
    private String getReadableDate(Long dateInLong) {
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(dateInLong);
    }

    //To make read high and low temperatures as "18/16"
    private String makeReadHighTemperature(double high, double low) {
        long highTemp, lowTemp;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String units = sharedPreferences.getString(context.getString(R.string.pref_units_key),
                context.getString(R.string.pref_units_default_value));
        //user select imperial units
        if (!units.contains(context.getString(R.string.pref_units_default_value))) {
            high = high * 1.8 + 32;
            low = low * 1.8 + 32;
        }//user select imperial units

        highTemp = Math.round(high);
        lowTemp = Math.round(low);


        return highTemp + "/" + lowTemp;
    }
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId;

        // First, check if the location with this city name exists in the db
        Cursor locationCursor = context.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                new String[]{LocationEntry._ID},
                LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
                new String[]{locationSetting},
                null);

        if (locationCursor.moveToFirst()) {
            int locationIdIndex = locationCursor.getColumnIndex(LocationEntry._ID);
            locationId = locationCursor.getLong(locationIdIndex);
        } else {
            // Now that the content provider is set up, inserting rows of data is pretty simple.
            // First create a ContentValues object to hold the data you want to insert.
            ContentValues locationValues = new ContentValues();

            // Then add the data, along with the corresponding name of the data type,
            // so the content provider knows what kind of value is being inserted.
            locationValues.put(LocationEntry.COLUMN_CITY_NAME, cityName);
            locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
            locationValues.put(LocationEntry.COLUMN_COORD_LAT, lat);
            locationValues.put(LocationEntry.COLUMN_COORD_LONG, lon);

            // Finally, insert location data into the database.
            Uri insertedUri = context.getContentResolver().insert(
                    LocationEntry.CONTENT_URI,
                    locationValues
            );

            // The resulting URI contains the ID for the row.  Extract the locationId from the Uri.
            locationId = ContentUris.parseId(insertedUri);
        }

        locationCursor.close();
        // Wait, that worked?  Yes!
        return locationId;
    }


    @Override
    protected void onPostExecute(String[] strings) {
        //Toast.makeText(getActivity(),strings.toString(),Toast.LENGTH_LONG).show();
        Log.v(LOG_TAG, Arrays.toString(strings));
        forecastArrayAdapter.clear();
        if (strings != null) {
            for (String s :
                    strings) {
                forecastArrayAdapter.add(s);
            }
        }


    }
}//finsh of async task