package apps.mai.sunshine.data;

import android.content.UriMatcher;
import android.net.Uri;
import android.test.AndroidTestCase;

/**
 * Created by Mai_ on 18-Aug-16.
 */
public class TestUriMatcher extends AndroidTestCase {
    public static final Uri TEST_WEATHER_DIRECTORY = WeatherContract.WeatherEntry.CONTENT_URI;
    public static final Uri TEST_WEATHER_WITH_LOCATION = WeatherContract.WeatherEntry.
            buildWeatherLocation("London, Uk");
    public static final Uri TEST_WEATHER_WITH_LOCATION_DATE =WeatherContract.WeatherEntry.
            buildWeatherLocationWithDate("London, Uk",1419033600L);
    public static final Uri TEST_LOCATION_DIRECTORY = WeatherContract.LocationEntry.CONTENT_URI;
    //public static final String
    UriMatcher uriMatcher=WeatherProvider.buildUriMatcher();

    public void textBuildUriMatcher(){
        assertEquals("the weather uri not match",WeatherProvider.WEATHER,
                uriMatcher.match(TEST_WEATHER_DIRECTORY));

        assertEquals("the weather with location uri don't match",
                uriMatcher.match(TEST_WEATHER_WITH_LOCATION),WeatherProvider.WEATHER_WITH_LOCATION);

        assertEquals("the weather with location and date uri don't match",
                uriMatcher.match(TEST_WEATHER_WITH_LOCATION_DATE),WeatherProvider.WEATHER_WITH_LOCATION_AND_DATE);

        assertEquals("the location uri don't match",uriMatcher.match(TEST_LOCATION_DIRECTORY),
                WeatherProvider.LOCATION);
    }
}
