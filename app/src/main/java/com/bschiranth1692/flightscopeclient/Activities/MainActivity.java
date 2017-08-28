package com.bschiranth1692.flightscopeclient.Activities;

import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bschiranth1692.flightscopeclient.Fragments.MainFragment;
import com.bschiranth1692.flightscopeclient.R;

public class MainActivity extends AppCompatActivity implements MainFragment.OnMainFragmentInteractionListener{

    //Fragment Manager for fragment transactions
    FragmentManager fragmentManager;
    Resources resources;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        resources = getResources();
        fragmentManager = getSupportFragmentManager();

        if(savedInstanceState == null) {

            //add new fragment
            fragmentManager.beginTransaction().add(R.id.mainRootId
                    , MainFragment.newInstance(),getString(R.string.main_fragment_tag)).
                    commit();

        }
    }


    //interface method implemented by activity, activity will execute the method
    @Override
    public void onSetClicked(String ipaddres, String port) {

        //send ip and port to connection activity
        Intent intent = new Intent(getApplicationContext(),ConnectionActivity.class);

        intent.putExtra(getString(R.string.ip),ipaddres);
        intent.putExtra(getString(R.string.port),port);

        startActivity(intent);  //start new connection activity
    }
}
