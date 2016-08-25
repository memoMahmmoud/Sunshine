package apps.mai.sunshine.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.test.AndroidTestCase;

import java.util.Map;
import java.util.Set;

import apps.mai.sunshine.utils.PollingCheck;

/**
 * Created by Mai_ on 09-Aug-16.
 */
public class TestUtilities extends AndroidTestCase {
    static final String TEST_LOCATION = "79052";
    static final long TEST_DATE = 1419032211L;

    static ContentValues createNorthPoleLocationValues() {
        // Create a new map of values, where column names are the keys
        ContentValues testValues = new ContentValues();
        testValues.put(WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION);
        testValues.put(WeatherContract.LocationEntry.COLUMN_CITY_NAME, "Mansoura");
        testValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LAT, 64.7488);
        testValues.put(WeatherContract.LocationEntry.COLUMN_COORD_LONG, -147.353);

        return testValues;
    }
    static Long insertLocationValues(Context context){
        WeatherDBHelper weatherDBHelper=new WeatherDBHelper(context);
        SQLiteDatabase sqLiteDatabase=weatherDBHelper.getWritableDatabase();
        long row_id=sqLiteDatabase.insert(WeatherContract.LocationEntry.TABLE_NAME,null,
                createNorthPoleLocationValues());
        sqLiteDatabase.close();
        return row_id;

    }
    static Long insertWeatherValues(Context context){
        WeatherDBHelper weatherDBHelper=new WeatherDBHelper(context);
        SQLiteDatabase sqLiteDatabase=weatherDBHelper.getWritableDatabase();
        long row_id=sqLiteDatabase.insert(WeatherContract.WeatherEntry.TABLE_NAME,null,
                createWeatherValues(insertLocationValues(context)));
        sqLiteDatabase.close();
        return row_id;


    }

    static ContentValues createWeatherValues(long locationRowId) {
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_LOCATION_ID, locationRowId);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DATE, TEST_DATE);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_DEGREES, 1.1);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_HUMIDITY, 1.2);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_PRESSURE, 1.3);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP, 75);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP, 65);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WIND_SPEED, 5.5);
        weatherValues.put(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID, 321);

        return weatherValues;
    }
    public static void validateCursor(String error, Cursor cursorTest, ContentValues expectedContentValues){
        assertTrue("Empty cursor returned: "+error,cursorTest.moveToFirst());
        validateCurrentRecord(error,cursorTest,expectedContentValues);
        cursorTest.close();
    }
    static void validateCurrentRecord(String error, Cursor valueCursor, ContentValues expectedValues) {
        Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
        for (Map.Entry<String, Object> entry : valueSet) {
            String columnName = entry.getKey();
            int idx = valueCursor.getColumnIndex(columnName);
            assertFalse("Column '" + columnName + "' not found. " + error, idx == -1);
            String expectedValue = entry.getValue().toString();
            assertEquals("Value '" + entry.getValue().toString() +
                    "' did not match the expected value '" +
                    expectedValue + "'. " + error, expectedValue, valueCursor.getString(idx));
        }
    }
    static class TestContentObserver extends ContentObserver {
        final HandlerThread handlerThread;
        boolean mContentChanged;
        private TestContentObserver(HandlerThread handlerThread) {
            super(new Handler(handlerThread.getLooper()));
            this.handlerThread = handlerThread;
        }
        // On earlier versions of Android, this onChange method is called
        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }
        //this method for api 16 and greater
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            mContentChanged = true;
        }

        static TestContentObserver getTestContentObserver() {
            HandlerThread ht = new HandlerThread("ContentObserverThread");
            ht.start();
            return new TestContentObserver(ht);
        }





        public void waitForNotificationOrFail() {
            // Note: The PollingCheck class is taken from the Android CTS (Compatibility Test Suite).
            // It's useful to look at the Android CTS source for ideas on how to test your Android
            // applications.  The reason that PollingCheck works is that, by default, the JUnit
            // testing framework is not running on the main Android application thread.
            new PollingCheck(5000) {
                @Override
                protected boolean check() {
                    return mContentChanged;
                }
            }.run();
            handlerThread.quit();
        }
    }

    static TestContentObserver getTestContentObserver() {
        return TestContentObserver.getTestContentObserver();
    }

}
