package com.bschiranth1692.flightscopeclient.Fragments;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.bschiranth1692.flightscopeclient.R;

public class MainFragment extends Fragment {

    //String keys for the bundle
    public static final String IP_PARAM = "ipparam";
    public static final String PORT_PARAM = "portparam";

    //IP and port strings
    private String ipAddress;
    private String port;

    //UI objects
    Button setButton;
    EditText ipEditText, portEditText;

    //interface instance
    OnMainFragmentInteractionListener mListener;

    public MainFragment() {
        // Required empty public constructor
    }


    // new instance of fragment
    public static MainFragment newInstance() {
        
        Bundle args = new Bundle();
        MainFragment fragment = new MainFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //retains fragment on orientation change
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        //listener object
        if(context instanceof OnMainFragmentInteractionListener) {
            mListener = (OnMainFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement Listener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        //find views for UI objects
        setButton = (Button) rootView.findViewById(R.id.setButtonId);
        ipEditText = (EditText) rootView.findViewById(R.id.editIpId);
        portEditText = (EditText) rootView.findViewById(R.id.editPortId);


        //set button click listener
        setButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //get edit text values from edit text box
                ipAddress = ipEditText.getText().toString();
                port = portEditText.getText().toString();

                //checking for empty edit text
                if(TextUtils.isEmpty(ipAddress)) {
                    ipEditText.setError("Dont leave blank");
                    return;
                } else if (TextUtils.isEmpty(port)) {
                    portEditText.setError("Dont leave blank");
                    return;
                }

                //IP format check
                if (!Patterns.IP_ADDRESS.matcher(ipAddress).matches()) {
                    // ip is incorrect, set error
                    ipEditText.setError("Enter Valid IP");
                    return;
                }

                //port length check
                if(port.length() != 4) {
                    //invalid port, set error
                    portEditText.setError("Enter Valid Port");
                    return;
                }

                //call listener method implemented in the activity
                mListener.onSetClicked(ipAddress,port);
            }
        });

        //return the view to be created by the fragment
        return rootView;
    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;   //interface listener must be cleared during detach
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save IP and Port on orientation change
        outState.putString(IP_PARAM,ipEditText.getText().toString());
        outState.putString(PORT_PARAM,portEditText.getText().toString());
    }

    //interface to communicate with activity
    public interface OnMainFragmentInteractionListener {
        void onSetClicked(String ipaddress,String port);
    }
}
