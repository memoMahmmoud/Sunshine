package apps.mai.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class DetailActivity extends AppCompatActivity {
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState==null){
            getSupportFragmentManager().beginTransaction().
                    add(R.id.container,new DetailFragment()).commit();
            toolbar= (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //inflate the menu for detail activity
        getMenuInflater().inflate(R.menu.detail,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.action_settings:
                Intent intent=new Intent(this,SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);

        }
    }

    public static class DetailFragment extends Fragment{
        private static final String LOG_TAG=DetailFragment.class.getSimpleName();
        private static final String FORECAST_SHARE_HASHTAG="#Sunshine App";
        private String forecast;
        TextView textView;

        public DetailFragment(){
            setHasOptionsMenu(true);
        }

        @Nullable
        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View rootView=inflater.inflate(R.layout.fragment_detail,container,false);
            textView= (TextView) rootView.findViewById(R.id.forecast_string);
            Intent intent=getActivity().getIntent();
            if (intent!=null&&intent.hasExtra(Intent.EXTRA_TEXT)){
                forecast=intent.getStringExtra(Intent.EXTRA_TEXT);
                textView.setText(forecast);
            }

            return rootView;
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            super.onCreateOptionsMenu(menu, inflater);
            inflater.inflate(R.menu.detailfragment,menu);
            //retreive share menu item
            MenuItem shareItem=menu.findItem(R.id.menu_item_share);
            //getting shared action provider and attached to our intent
            ShareActionProvider shareActionProvider=new ShareActionProvider(getContext());
            shareActionProvider.setShareIntent(createShareForecastIntent());
            MenuItemCompat.setActionProvider(shareItem,shareActionProvider);
            if (shareActionProvider != null ) {
                shareActionProvider.setShareIntent(createShareForecastIntent());
            }
            else {
                Log.v(LOG_TAG,"Share Action Provider is null");
            }

        }

        private Intent createShareForecastIntent(){
            Intent intent=new Intent(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT,forecast+FORECAST_SHARE_HASHTAG);
            return intent;
        }
    }
}
