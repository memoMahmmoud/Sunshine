package apps.mai.sunshine.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.text.format.Time;
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

import apps.mai.sunshine.MainActivity;
import apps.mai.sunshine.R;
import apps.mai.sunshine.Utility;
import apps.mai.sunshine.data.WeatherContract;

/**
 * Created by Mai_ on 19-Sep-16.
 */
public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();
    public static final int SYNC_INTERVAL = 180 * 60;
    public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
    private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC
    };
    private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
    private static final int WEATHER_NOTIFICATION_ID = 3004;

    // these indices must match the projection
    private static final int INDEX_WEATHER_ID = 0;
    private static final int INDEX_MAX_TEMP = 1;
    private static final int INDEX_MIN_TEMP = 2;
    private static final int INDEX_SHORT_DESC = 3;

    public SunshineSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
    }

    @Override
    public void onPerformSync(Account account, Bundle bundle, String s, ContentProviderClient contentProviderClient, SyncResult syncResult) {
        Log.e(LOG_TAG,"onPerformSync called .......");
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        String locationValue = sharedPreferences.getString
                (getContext().getString(R.string.pref_location_key),
                        getContext().getString(R.string.pref_location_default_value));
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;
        String forecastJsonStr;

        try {
            final String FORECAST_URL_BASE = "http://api.openweathermap.org/data/2.5/forecast/daily?";
            final int NUM_DAYS = 14;
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            final String UNITS = prefs.getString(getContext().getString(R.string.pref_units_key),
                    getContext().getString(R.string.pref_units_default_value));
            Uri builtUri = Uri.parse(FORECAST_URL_BASE).buildUpon()
                    .appendQueryParameter("AppID",getContext().getString(R.string.app_id_weather_map_api))
                    .appendQueryParameter("q", locationValue)
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
                getWeatherDateFromJson(forecastJsonStr,locationValue);

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
        Time dayTime = new Time();
        dayTime.setToNow();

        // we start at the day returned by local time. Otherwise this is a mess.
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

        // now we work exclusively in UTC
        dayTime = new Time();
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
            getContext().getContentResolver().bulkInsert(WeatherContract.WeatherEntry.CONTENT_URI, cvArray);

            /*getContext().getContentResolver().delete(WeatherContract.WeatherEntry.CONTENT_URI,
                    WeatherContract.WeatherEntry.COLUMN_DATE + " <= ?",
                    new String[] {Long.toString(dayTime.setJulianDay(julianStartDay-1))});*/

            notifyWeather();
        }
        Log.d(LOG_TAG, "Sync Complete. " + cVVector.size() + " Inserted");


    }
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        long locationId;

        // First, check if the location with this city name exists in the db
        Cursor locationCursor = getContext().getContentResolver().query(
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
            Uri insertedUri = getContext().getContentResolver().insert(
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
    public static void SyncImmediately(Context context){
        Bundle bundle = new Bundle();
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED,true);
        bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL,true);
        ContentResolver.requestSync(getSyncAccount(context),
                context.getString(R.string.content_authority),bundle);

    }
    public static Account getSyncAccount(Context context){
        AccountManager accountManager = (AccountManager) context.getSystemService
                (Context.ACCOUNT_SERVICE);
        Account account = new Account(context.getString(R.string.app_name),
                context.getString(R.string.sync_account_type));
        //the account is does not exist
        if (accountManager.getPassword(account) == null){
            if (!accountManager.addAccountExplicitly(account,"",null)){
                return null;
            }
            onAccountCreated(account, context);
        }
        return account;
    }
    /**
     * Helper method to schedule the sync adapter periodic execution
     */
    public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
        Account account = getSyncAccount(context);
        String authority = context.getString(R.string.content_authority);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // we can enable inexact timers in our periodic sync
            SyncRequest request = new SyncRequest.Builder().
                    syncPeriodic(syncInterval, flexTime).
                    setSyncAdapter(account, authority).
                    setExtras(new Bundle()).build();
            ContentResolver.requestSync(request);
            Log.e("maiiiiiiiiiiiiiii","uuuuuuuuuuuuuuuuu");
        } else {
            ContentResolver.addPeriodicSync(account,
                    authority, new Bundle(), syncInterval);
            Log.e("maiiiiiiiiiiiiiii","uuuuuuuuuuuuuuuuu");
        }
    }


    private static void onAccountCreated(Account newAccount, Context context) {
        /*
         * Since we've created an account
         */
        SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);


        /*
         * Without calling setSyncAutomatically, our periodic sync will not be enabled.
         */
        ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);

        /*
         * Finally, let's do a sync to get things started
         */
        SyncImmediately(context);
    }

    public static void initializeSyncAdapter(Context context) {

        getSyncAccount(context);
    }

    public void notifyWeather(){
        Context context = getContext();
        //checking the last update and notify if it' the first of the day
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
        boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
                Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

        if (displayNotifications){
            String lastNotificationKey = context.getString(R.string.pref_last_notification);
            long lastSync = prefs.getLong(lastNotificationKey, 0);
            if (System.currentTimeMillis()-lastSync >= DAY_IN_MILLIS){
                String location = Utility.getPreferredLocation(context);
                Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(location,
                        System.currentTimeMillis());
                Cursor cursor = context.getContentResolver().query(weatherUri,NOTIFY_WEATHER_PROJECTION,
                        null,null,null);
                if (cursor.moveToFirst()){
                    int weather_icon_id = cursor.getInt(INDEX_WEATHER_ID);
                    double high = cursor.getDouble(INDEX_MAX_TEMP);
                    double low = cursor.getDouble(INDEX_MIN_TEMP);
                    String desc = cursor.getString(INDEX_SHORT_DESC);

                    int weatherResourceId = Utility.getIconResourceForWeatherCondition(weather_icon_id);
                    String title = context.getString(R.string.app_name);

                    // Define the text of the forecast.
                    String contentText = String.format(context.getString(R.string.format_notification),
                            desc,
                            Utility.formatTemperature(context,high),
                            Utility.formatTemperature(context, low));

                    //build your notification here.

                    //refreshing last sync
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putLong(lastNotificationKey, System.currentTimeMillis());
                    editor.commit();

                    NotificationCompat.Builder mBuilder =
                            new NotificationCompat.Builder(context).setSmallIcon(weatherResourceId)
                                    .setContentTitle(title).setContentText(contentText);
                    // Creates an explicit intent for an Activity in your app
                    Intent resultIntent = new Intent(context, MainActivity.class);

                    // The stack builder object will contain an artificial back stack for the
                    // started Activity.
                    // This ensures that navigating backward from the Activity leads out of
                    // your application to the Home screen.
                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                    // Adds the Intent that starts the Activity to the top of the stack
                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =stackBuilder.getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT);
                    mBuilder.setContentIntent(resultPendingIntent);
                    NotificationManager mNotificationManager =
                            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    // mId allows you to update the notification later on.
                    mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());

                }
                cursor.close();

            }
        }


    }
}
