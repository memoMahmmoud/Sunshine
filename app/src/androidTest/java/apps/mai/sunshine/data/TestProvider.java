package apps.mai.sunshine.data;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.test.AndroidTestCase;
import android.util.Log;

import apps.mai.sunshine.data.WeatherContract.LocationEntry;
import apps.mai.sunshine.data.WeatherContract.WeatherEntry;

/**
 * Created by Mai_ on 18-Aug-16.
 */
public class TestProvider extends AndroidTestCase {
    public void testGetType(){
        Uri uri_weather = WeatherContract.WeatherEntry.CONTENT_URI;
        assertEquals("weather uri didn't get expected type",
                mContext.getContentResolver().getType(uri_weather),WeatherEntry.CONTENT_TYPE);

        Uri WeatherWithLocation = WeatherEntry.buildWeatherLocation("35511");
        assertEquals("weather with location uri didn't get expected type",
                mContext.getContentResolver().getType(WeatherWithLocation),
                WeatherEntry.CONTENT_TYPE);

        Uri WeatherWithLocationAndDate = WeatherEntry.
                buildWeatherLocationWithDate("35511",1419120000L);
        assertEquals("weather with location uri didn't get expected type",
                mContext.getContentResolver().getType(WeatherWithLocationAndDate),
                WeatherEntry.CONTENT_ITEM_TYPE);

        Uri Location = WeatherContract.LocationEntry.CONTENT_URI;
        assertEquals("weather with location uri didn't get expected type",
                mContext.getContentResolver().getType(Location),
                WeatherContract.LocationEntry.CONTENT_TYPE);


    }
    public void testBasicWeatherQuery(){
        long location_row_id = TestUtilities.insertLocationValues(mContext);

        ContentValues weatherContentValues = TestUtilities.createWeatherValues(location_row_id);

        long weather_row_id = TestUtilities.insertWeatherValues(mContext);
        assertTrue("unable to insert row in weather table",weather_row_id!=-1);


        Cursor cursor=mContext.getContentResolver().query(WeatherEntry.CONTENT_URI,null,null,null,null);
        TestUtilities.validateCursor("test weather query not success",cursor,weatherContentValues);
    }
    public void testBasicLocationQuery(){
        ContentValues locationContentValues = TestUtilities.createNorthPoleLocationValues();
        long location_row_id = TestUtilities.insertLocationValues(mContext);
        assertTrue("location row inserted successfully",location_row_id != -1);
        Cursor testCursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI,
                null,null,null,null);
        TestUtilities.validateCursor("test location query not success",
                testCursor,locationContentValues);

    }
    public void testInsertLocation(){
        WeatherDBHelper weatherDBHelper = new WeatherDBHelper(mContext);

        ContentValues locationContentValues = TestUtilities.createNorthPoleLocationValues();

        // Register a content observer for our insert.  This time, directly with the content resolver
        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(LocationEntry.CONTENT_URI, true, tco);
        Uri testLocationUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI,
                locationContentValues);
        // Did our content observer get called?  Students:  If this fails, your insert location
        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);
        long id_row_location = ContentUris.parseId(testLocationUri);
        assertTrue(id_row_location != -1);
        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating LocationEntry.",
                cursor, locationContentValues);

        ContentValues weatherContentValues = TestUtilities.createWeatherValues(id_row_location);
        tco = TestUtilities.getTestContentObserver();
        mContext.getContentResolver().registerContentObserver(WeatherEntry.CONTENT_URI, true, tco);
        Uri weatherInsertUri = mContext.getContentResolver().insert(WeatherEntry.CONTENT_URI,
                weatherContentValues);
        assertTrue(weatherInsertUri != null);
        tco.waitForNotificationOrFail();
        mContext.getContentResolver().unregisterContentObserver(tco);


        Cursor weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.CONTENT_URI,  // Table to Query
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null // columns to group by
        );

        TestUtilities.validateCursor("testInsertReadProvider. Error validating WeatherEntry insert.",
                weatherCursor, weatherContentValues);

        // Add the location values in with the weather data so that we can make
        // sure that the join worked and we actually get all the values back
        weatherContentValues.putAll(locationContentValues);

        // Get the joined Weather and Location data
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocation(TestUtilities.TEST_LOCATION),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Weather and Location Data.",
                weatherCursor, weatherContentValues);

        // Get the joined Weather and Location data with a start date
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithStartDate(
                        TestUtilities.TEST_LOCATION, TestUtilities.TEST_DATE),
                null, // leaving "columns" null just returns all the columns.
                null, // cols for "where" clause
                null, // values for "where" clause
                null  // sort order
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Weather and Location Data with start date.",
                weatherCursor, weatherContentValues);

        // Get the joined Weather data for a specific date
        weatherCursor = mContext.getContentResolver().query(
                WeatherEntry.buildWeatherLocationWithDate(TestUtilities.TEST_LOCATION, TestUtilities.TEST_DATE),
                null,
                null,
                null,
                null
        );
        TestUtilities.validateCursor("testInsertReadProvider.  Error validating joined Weather and Location data for a specific date.",
                weatherCursor, weatherContentValues);
    }
    public void testDelete(){

        mContext.getContentResolver().delete(WeatherEntry.CONTENT_URI,null,null);
        mContext.getContentResolver().delete(LocationEntry.CONTENT_URI,null,null);
        Cursor cursor = mContext.getContentResolver().query(WeatherEntry.CONTENT_URI,null,null,null,null);
        assertEquals("records not deleted from weather table",0,cursor.getCount());
        cursor.close();

        cursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI,null,null,null,null);
        assertEquals("records not deleted from location table",0,cursor.getCount());
        cursor.close();
    }
    public void testUpdate(){
        ContentValues values = TestUtilities.createNorthPoleLocationValues();
        Uri locationUri = mContext.getContentResolver().
                insert(LocationEntry.CONTENT_URI, values);
        long locationRowId = ContentUris.parseId(locationUri);

        // Verify we got a row back.
        assertTrue(locationRowId != -1);
        Log.d("maiiiiii", "New row id: " + locationRowId);

        ContentValues updatedValues = new ContentValues(values);
        updatedValues.put(LocationEntry._ID, locationRowId);
        updatedValues.put(LocationEntry.COLUMN_CITY_NAME, "Santa's Village");

        // Create a cursor with observer to make sure that the content provider is notifying
        // the observers as expected
        Cursor locationCursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI, null, null, null, null);

        TestUtilities.TestContentObserver tco = TestUtilities.getTestContentObserver();
        locationCursor.registerContentObserver(tco);

        int count = mContext.getContentResolver().update(
                LocationEntry.CONTENT_URI, updatedValues, LocationEntry._ID + "= ?",
                new String[] { Long.toString(locationRowId)});
        assertEquals(count, 1);

        // Test to make sure our observer is called.  If not, we throw an assertion.
        //
        // Students: If your code is failing here, it means that your content provider
        // isn't calling getContext().getContentResolver().notifyChange(uri, null);
        tco.waitForNotificationOrFail();

        locationCursor.unregisterContentObserver(tco);
        locationCursor.close();

        // A cursor is your primary interface to the query results.
        Cursor cursor = mContext.getContentResolver().query(
                LocationEntry.CONTENT_URI,
                null,   // projection
                LocationEntry._ID + " = " + locationRowId,
                null,   // Values for the "where" clause
                null    // sort order
        );

        TestUtilities.validateCursor("testUpdateLocation.  Error validating location entry update.",
                cursor, updatedValues);

        cursor.close();
    }

}
