package com.bschiranth1692.flightscopeclient.Activities;

import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.bschiranth1692.flightscopeclient.Fragments.ConnectionFragment;
import com.bschiranth1692.flightscopeclient.R;

public class ConnectionActivity extends AppCompatActivity {

    String ip,port;

    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        //fragment manager for fragment transactions
        fragmentManager = getSupportFragmentManager();

        //get the intent passed from previous activity and check for null
        Intent previousIntent = getIntent();
        if(previousIntent != null && previousIntent.getExtras() != null) {

            //get the ip and port from intent sent by main activity
            ip = previousIntent.getStringExtra(getString(R.string.ip));
            port = previousIntent.getStringExtra(getString(R.string.port));

            //fragment transactions
            if(savedInstanceState == null) {

                fragmentManager.beginTransaction().add(R.id.connectionRootId,
                        ConnectionFragment.newInstance(ip,port),getString(R.string.conn_fragment_tag))
                        .commit();
            }

        }
    }
}
