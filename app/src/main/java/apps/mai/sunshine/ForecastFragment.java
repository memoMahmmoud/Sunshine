package apps.mai.sunshine;


import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

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

/**
 * A simple {@link Fragment} subclass.
 */
public class ForecastFragment extends Fragment {
    ListView forecastListView;
    ArrayAdapter<String> forecastArrayAdapter;

    // These two need to be declared outside the try/catch
    // so that they can be closed in the finally block.
    HttpURLConnection urlConnection = null;
    BufferedReader reader = null;

    // Will contain the raw JSON response as a string.
    String forecastJsonStr = null;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment,menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:{
                Intent intent=new Intent(getContext(),SettingsActivity.class);
                startActivity(intent);
                return true;
            }

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_main, container, false);
        forecastListView= (ListView) view.findViewById(R.id.listview_forecast);

        forecastArrayAdapter=new ArrayAdapter<String>(getActivity(),R.layout.list_item_forecast,
                R.id.list_item_forecast_textview);

        FetchWeatherTask fetchWeatherTask=new FetchWeatherTask();
        fetchWeatherTask.execute();



        forecastListView.setAdapter(forecastArrayAdapter);
        forecastListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String Forecast=forecastArrayAdapter.getItem(i);
                //Toast.makeText(getActivity(),Forecast,Toast.LENGTH_LONG).show();
                Intent intent=new Intent(getActivity(),DetailActivity.class);
                intent.putExtra("forecast",Forecast);
                startActivity(intent);
            }
        });

        return view;
    }// on create method

    //To make readable date as "Thu Jul 21"
    private String getReadableDate(Long dateInLong){
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(dateInLong);
    }

    //To make read high and low temperatures as "18/16"
    private String makeReadHighTemperature(double high,double low){
        long highTemp=Math.round(high);
        long lowTemp=Math.round(low);
        return highTemp+"/"+lowTemp;
    }
    private String[] getWeatherDateFromJson(String jsonString, int numDays) throws JSONException {
        String[] forecast_array= new String[numDays];
        GregorianCalendar gregorianCalendar=new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        gregorianCalendar.setTime(new Date(System.currentTimeMillis()));
        gregorianCalendar.set(Calendar.HOUR_OF_DAY, 0);
        gregorianCalendar.set(Calendar.MINUTE, 0);
        gregorianCalendar.set(Calendar.SECOND, 0);
        gregorianCalendar.set(Calendar.MILLISECOND, 0);


        JSONObject jsonObject=new JSONObject(jsonString);
        JSONArray jsonArrayForList= jsonObject.getJSONArray("list");
        for (int i=0;i<numDays;i++){
            JSONObject jsonObjectDay=jsonArrayForList.getJSONObject(i);
            JSONArray jsonArrayForWeather=jsonObjectDay.getJSONArray("weather");
            JSONObject jsonObjectForWeather=jsonArrayForWeather.getJSONObject(0);
            String description=jsonObjectForWeather.getString("description");
            if (i!=0){
                gregorianCalendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            String date=getReadableDate(gregorianCalendar.getTime().getTime());
            JSONObject jsonObjectForTemp=jsonObjectDay.getJSONObject("temp");
            double high=jsonObjectForTemp.getDouble("max");
            double low=jsonObjectForTemp.getDouble("min");
            String highLow=makeReadHighTemperature(high,low);
            forecast_array[i]=date+" - "+description+" - "+highLow;

        }
        return forecast_array;

    }

    private class FetchWeatherTask extends AsyncTask<Void,Void,String[]>{
        private final String LOG_TAG=FetchWeatherTask.class.getSimpleName();


        @Override
        protected String[] doInBackground(Void... voids) {

            try {
                final String FORECAST_URL_BASE="http://api.openweathermap.org/data/2.5/forecast/daily?";
                final int numDays=7;
                Uri builtUri=Uri.parse(FORECAST_URL_BASE).buildUpon()
                        .appendQueryParameter("APPID","77f3ee73bd02d6e631bcab3258134519")
                        .appendQueryParameter("q","Mansoura")
                        .appendQueryParameter("cnt", String.valueOf(numDays))
                        .appendQueryParameter("units","metric")
                        .build();

                URL urlForWhetherMap=new URL(builtUri.toString());

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection= (HttpURLConnection) urlForWhetherMap.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStreamReader inputStreamReader=new InputStreamReader(urlConnection.getInputStream());
                reader=new BufferedReader(inputStreamReader);
                StringBuffer stringBuffer=new StringBuffer();
                String line;
                while ((line=reader.readLine())!=null){
                    stringBuffer.append(line+"\n");
                }
                if (stringBuffer.length()!=0){
                    forecastJsonStr=stringBuffer.toString();
                    Log.v(LOG_TAG,"foresacast String :"+forecastJsonStr);
                    return getWeatherDateFromJson(forecastJsonStr,numDays);

                }
                else {
                    return null;
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
                return null;
            } finally {
                if (urlConnection!=null){
                    urlConnection.disconnect();
                }
                if(reader!=null){
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }//finally in try-catch

        }//finish do in background

        @Override
        protected void onPostExecute(String[] strings) {
            //Toast.makeText(getActivity(),strings.toString(),Toast.LENGTH_LONG).show();
            Log.v(LOG_TAG, Arrays.toString(strings));
            forecastArrayAdapter.clear();
            if(strings!=null){
                for (String s:
                        strings) {
                    forecastArrayAdapter.add(s);
                }
            }


        }
    }//finsh of async task


}
