package apps.mai.sunshine;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

public class DetailActivity extends AppCompatActivity{
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



}
