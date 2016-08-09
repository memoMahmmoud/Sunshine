package apps.mai.sunshine.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import apps.mai.sunshine.data.WeatherContract.WeatherEntry;
import apps.mai.sunshine.data.WeatherContract.LocationEntry;

/**
 * Created by Mai_ on 06-Aug-16.
 */
public class WeatherDBHelper extends SQLiteOpenHelper {
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "weather.db";

    public WeatherDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        final String CREATE_SQL_WEATHER_TABLE = "CREATE TABLE "+ WeatherEntry.TABLE_NAME+"("+
                WeatherEntry._ID+ " INTEGER PRIMARY KEY AUTOINCREMENT, "+
                WeatherEntry.COLUMN_LOCATION_ID+ " INTEGER NOT NULL, "+
                WeatherEntry.COLUMN_DATE+ " INTEGER NOT NULL, "+
                WeatherEntry.COLUMN_SHORT_DESC+ " TEXT NOT NULL, "+
                WeatherEntry.COLUMN_WEATHER_ID+ " INTEGER NOT NULL, "+

                WeatherEntry.COLUMN_MIN_TEMP+" REAL NOT NULL, " +
                WeatherEntry.COLUMN_MAX_TEMP+" REAL NOT NULL, " +

                WeatherEntry.COLUMN_HUMIDITY+ " REAL NOT NULL, " +
                WeatherEntry.COLUMN_PRESSURE+ " REAL NOT NULL, " +
                WeatherEntry.COLUMN_WIND_SPEED+ " REAL NOT NULL, " +
                WeatherEntry.COLUMN_DEGREES+ " REAL NOT NULL, " +
                // Set up the location column as a foreign key to location table.
                //this constraint means I can't add data in weather table unless I have id from location table
                "FOREIGN KEY(" + WeatherEntry.COLUMN_LOCATION_ID + ") REFERENCES " +
                LocationEntry.TABLE_NAME + "(" + LocationEntry._ID + ")," +

                // To assure the application have just one weather entry per day
                // per location, it's created a UNIQUE constraint with REPLACE strategy
                "UNIQUE (" + WeatherEntry.COLUMN_DATE + ", " +
                WeatherEntry.COLUMN_LOCATION_ID + ") ON CONFLICT REPLACE);";

        final String CREATE_SQL_LOCATION_TABLE = "CREATE TABLE "+LocationEntry.TABLE_NAME+"("+
                LocationEntry._ID+" INTEGER PRIMARY KEY AUTOINCREMENT, "+
                LocationEntry.COLUMN_LOCATION_SETTING+" TEXT UNIQUE NOT NULL, "+
                LocationEntry.COLUMN_CITY_NAME+" TEXT NOT NULL, "+
                LocationEntry.COLUMN_COORD_LAT+" REAL NOT NULL, "+
                LocationEntry.COLUMN_COORD_LONG+" REAL NOT NULL);";

        sqLiteDatabase.execSQL(CREATE_SQL_LOCATION_TABLE);
        sqLiteDatabase.execSQL(CREATE_SQL_WEATHER_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+LocationEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+WeatherEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);

    }
}
