package apps.mai.sunshine;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by Mai_ on 25-Aug-16.
 */
public class ForecastAdapter extends CursorAdapter {
    private static final int VIEW_TYPE_TODAY = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;
    public ForecastAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }
    // to make adapter show 2 different layouts, one for today and the second for others
    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0) ? VIEW_TYPE_TODAY : VIEW_TYPE_FUTURE_DAY ;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int view_type = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        if (view_type == VIEW_TYPE_TODAY){
            layoutId = R.layout.list_item_forecast_today;
        }
        else if(view_type == VIEW_TYPE_FUTURE_DAY){
            layoutId = R.layout.list_item_forecast;
        }
        View view= LayoutInflater.from(context).inflate(layoutId,parent,false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();
        // Read weather icon ID from cursor
        int weatherId = cursor.getInt(ForecastFragment.COL_WEATHER_CONDITION_ID);
        Log.e("mai",""+weatherId);
        if (cursor.getPosition() ==0){
            int imageResource = Utility.getArtResourceForWeatherCondition(weatherId);
            if (imageResource!=-1){
                viewHolder.iconView.setImageResource(imageResource);
            }
        }
        else
        {
            int imageResource = Utility.getIconResourceForWeatherCondition(weatherId);
            if (imageResource!=-1){
                viewHolder.iconView.setImageResource(imageResource);
                //////
            }
        }

        long dateInMillis = cursor.getLong(ForecastFragment.COL_WEATHER_DATE);
        viewHolder.dateView.setText(Utility.getFriendlyDayString(context, dateInMillis));

        // Read weather forecast from cursor
        String description = cursor.getString(ForecastFragment.COL_WEATHER_DESC);
        viewHolder.descriptionView.setText(description);

        // Read user preference for metric or imperial temperature units
        boolean isMetric = Utility.isMetric(context);
        // Read high temperature from cursor
        double high = cursor.getDouble(ForecastFragment.COL_WEATHER_MAX_TEMP);
        viewHolder.highTempView.setText(Utility.formatTemperature(context,high, isMetric));

        // Read low temperature from cursor
        double low = cursor.getDouble(ForecastFragment.COL_WEATHER_MIN_TEMP);
        viewHolder.lowTempView.setText(Utility.formatTemperature(context,low, isMetric));
    }
    //cache children views for forecast list item
    public static class ViewHolder{
        public final ImageView iconView;
        public final TextView dateView;
        public final TextView descriptionView;
        public final TextView highTempView;
        public final TextView lowTempView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dateView = (TextView) view.findViewById(R.id.list_item_date_textview);
            descriptionView = (TextView) view.findViewById(R.id.list_item_forecast_textview);
            highTempView = (TextView) view.findViewById(R.id.list_item_high_textview);
            lowTempView = (TextView) view.findViewById(R.id.list_item_low_textview);
        }
    }
}
