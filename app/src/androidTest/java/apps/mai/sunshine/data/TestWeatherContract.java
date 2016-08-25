package apps.mai.sunshine.data;

import android.net.Uri;
import android.test.AndroidTestCase;

/**
 * Created by Mai_ on 18-Aug-16.
 */
public class TestWeatherContract extends AndroidTestCase{

    private static final String TEST_WEATHER_LOCATION = "/North Pole";
    private static final long TEST_WEATHER_DATE = 1419033600L;  // December 20th, 2014

    public void testBuildWeatherLocation(){
        Uri uriLocation = WeatherContract.WeatherEntry.buildWeatherLocation(TEST_WEATHER_LOCATION);
        assertNotNull("uri location is null",uriLocation);
        //check is location segment added to the end of the
        assertEquals("location segement didn't add to uri correctly",TEST_WEATHER_LOCATION,
                uriLocation.getLastPathSegment());
        assertEquals("weather location does not match expecterd uri",
                "content://apps.mai.sunshine/weather/%2FNorth%20Pole",uriLocation.toString());
    }


}
