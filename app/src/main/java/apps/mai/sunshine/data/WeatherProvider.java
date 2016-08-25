package apps.mai.sunshine.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.Nullable;

import apps.mai.sunshine.data.WeatherContract.LocationEntry;
import apps.mai.sunshine.data.WeatherContract.WeatherEntry;

/**
 * Created by Mai_ on 18-Aug-16.
 */
public class WeatherProvider extends ContentProvider{
    static final int WEATHER = 100;
    static final int WEATHER_WITH_LOCATION = 101;
    static final int WEATHER_WITH_LOCATION_AND_DATE = 102;
    static final int LOCATION = 300;

    WeatherDBHelper weatherDBHelper;
    UriMatcher uriMatcher=buildUriMatcher();

    private static final SQLiteQueryBuilder mSQLiteQueryBuilder;

    static{
        mSQLiteQueryBuilder= new SQLiteQueryBuilder();

        //This is an inner join which looks like
        //weather INNER JOIN location ON weather.location_id = location._id
        mSQLiteQueryBuilder.setTables(
                WeatherContract.WeatherEntry.TABLE_NAME + " INNER JOIN " +
                        WeatherContract.LocationEntry.TABLE_NAME +
                        " ON " + WeatherContract.WeatherEntry.TABLE_NAME +
                        "." + WeatherEntry.COLUMN_LOCATION_ID +
                        " = " + WeatherContract.LocationEntry.TABLE_NAME +
                        "." + WeatherContract.LocationEntry._ID);
    }



    public static UriMatcher buildUriMatcher(){
        UriMatcher uriMatcher=new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY,WeatherContract.PATH_WEATHER,WEATHER);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY,WeatherContract.PATH_WEATHER+"/*",
                WEATHER_WITH_LOCATION);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY,WeatherContract.PATH_WEATHER+"/*/#",
                WEATHER_WITH_LOCATION_AND_DATE);
        uriMatcher.addURI(WeatherContract.CONTENT_AUTHORITY,WeatherContract.PATH_LOCATION,LOCATION);
        return uriMatcher;
    }
    @Override
    public boolean onCreate() {
        weatherDBHelper=new WeatherDBHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] strings, String s, String[] strings1, String s1) {
        Cursor cursor;
        switch (uriMatcher.match(uri)){
            case LOCATION:{
                cursor = weatherDBHelper.getReadableDatabase().
                        query(LocationEntry.TABLE_NAME,strings,s,strings1,null,null,s1);
                break;
            }
            case WEATHER: {
                cursor = weatherDBHelper.getReadableDatabase().
                        query(WeatherEntry.TABLE_NAME,strings,s,strings1,null,null,s1);
                break;
            }
            case WEATHER_WITH_LOCATION: {
                long start_date = WeatherEntry.getStartDateFromUri(uri);
                String location = WeatherEntry.getLocationSettingFromUri(uri);
                String[] arg_selections;
                String selection;
                if (start_date == 0){
                    selection =LocationEntry.TABLE_NAME + "." +
                            LocationEntry.COLUMN_LOCATION_SETTING + "= ?";
                    arg_selections=new String[]{WeatherContract.WeatherEntry.getLocationSettingFromUri(uri)};
                }
                else{
                    selection = LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND "+
                            WeatherEntry.TABLE_NAME+"."+WeatherEntry.COLUMN_DATE+" = ? ";
                    arg_selections = new String[]{location,Long.toString(start_date)};
                }
                cursor = mSQLiteQueryBuilder.query(weatherDBHelper.getReadableDatabase(), strings,
                        selection,arg_selections,null, null, s1);
                break;
            }
            case WEATHER_WITH_LOCATION_AND_DATE: {
                long date = WeatherEntry.getStartDateFromUri(uri);
                String location = WeatherEntry.getLocationSettingFromUri(uri);
                cursor = mSQLiteQueryBuilder.query(weatherDBHelper.getReadableDatabase(),strings,
                        (LocationEntry.TABLE_NAME + "." + LocationEntry.COLUMN_LOCATION_SETTING + " = ? AND "+
                        WeatherEntry.TABLE_NAME+"."+WeatherEntry.COLUMN_DATE+" = ? "),
                        new String[]{location,Long.toString(date)},null,null,s1);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown uri: "+uri);

        }
        cursor.setNotificationUri(getContext().getContentResolver(),uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        int matcher=uriMatcher.match(uri);
        switch (matcher){
            case LOCATION:
                return WeatherContract.LocationEntry.CONTENT_TYPE;
            case WEATHER:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER_WITH_LOCATION:
                return WeatherContract.WeatherEntry.CONTENT_TYPE;
            case WEATHER_WITH_LOCATION_AND_DATE:
                return WeatherContract.WeatherEntry.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("unknow uri: "+uri);

        }
    }


    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        WeatherDBHelper weatherDBHelper = new WeatherDBHelper(getContext());
        SQLiteDatabase db = weatherDBHelper.getWritableDatabase();
        Uri uri1;
        switch (uriMatcher.match(uri)) {
            case LOCATION: {
                normalizeDate(contentValues);
                long loc_row_id = db.insert(LocationEntry.TABLE_NAME, null, contentValues);
                if (loc_row_id > 0) {
                    uri1 = LocationEntry.buildLocationUri(loc_row_id);
                } else {
                    throw new android.database.SQLException("failed to insert row through " + uri);
                }
                break;
            }
            case WEATHER: {
                long weather_row_id = db.insert(WeatherEntry.TABLE_NAME, null, contentValues);
                if (weather_row_id > 0) {
                    uri1 = WeatherEntry.buildWeatherUri(weather_row_id);
                } else {
                    throw new android.database.SQLException("failed to insert row through " + uri);
                }

                break;
            }
            default:
                throw new UnsupportedOperationException("unknown uri: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri,null);
        db.close();
        return uri1;

    }

    @Override
    public int delete(Uri uri, String s, String[] strings) {
        final SQLiteDatabase db = weatherDBHelper.getWritableDatabase();
        int rowsDeleted;
        switch (uriMatcher.match(uri)){
            case LOCATION:{
                rowsDeleted = db.delete(LocationEntry.TABLE_NAME,s,strings);
                break;
            }
            case WEATHER:{
                rowsDeleted = db.delete(WeatherEntry.TABLE_NAME,s,strings);
                break;
            }
            default:{
                throw new UnsupportedOperationException("Unknown uri: "+uri);
            }

        }
        if (rowsDeleted != 0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        db.close();
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String s, String[] strings) {
        SQLiteDatabase db = weatherDBHelper.getWritableDatabase();
        int rowsUpdated;
        switch (uriMatcher.match(uri)){
            case LOCATION:{
               rowsUpdated = db.update(LocationEntry.TABLE_NAME,contentValues,s,strings);
                break;
            }
            case WEATHER:{
                normalizeDate(contentValues);
                rowsUpdated = db.update(WeatherEntry.TABLE_NAME,contentValues,s,strings);
                break;
            }
            default:
                throw new UnsupportedOperationException("Unknown Uri: "+uri);

        }
        db.close();
        if (rowsUpdated > 0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return rowsUpdated;
    }
    private void normalizeDate(ContentValues values) {
        // normalize the date value
        if (values.containsKey(WeatherContract.WeatherEntry.COLUMN_DATE)) {
            long dateValue = values.getAsLong(WeatherContract.WeatherEntry.COLUMN_DATE);
            values.put(WeatherContract.WeatherEntry.COLUMN_DATE, WeatherContract.normalizeDate(dateValue));
        }
    }
    // to add more lines in database at the same time
    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        final SQLiteDatabase db = weatherDBHelper.getWritableDatabase();

        switch (uriMatcher.match(uri)){
            case WEATHER:
                db.beginTransaction();
                int returnCount = 0;
                try {
                    for (ContentValues contentValues : values) {
                        normalizeDate(contentValues);
                        long _id = db.insert(WeatherEntry.TABLE_NAME, null, contentValues);
                        if (_id != -1) {
                            returnCount++;
                        }
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
                getContext().getContentResolver().notifyChange(uri,null);
                return returnCount;

            default:
                return super.bulkInsert(uri, values);


        }
    }
}
