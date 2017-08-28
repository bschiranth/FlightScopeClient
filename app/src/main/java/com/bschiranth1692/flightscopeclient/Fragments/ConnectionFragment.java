package com.bschiranth1692.flightscopeclient.Fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.bschiranth1692.flightscopeclient.R;
import com.bschiranth1692.flightscopeclient.Utils.ToastMaker;
import com.bschiranth1692.flightscopeclient.Utils.Utils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;

import static android.R.attr.elegantTextHeight;
import static android.R.attr.fragment;
import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static com.bschiranth1692.flightscopeclient.Utils.Utils.getRealPathFromURI;


public class ConnectionFragment extends Fragment implements View.OnClickListener{


    String ip,port,filePath;

    //UI obejcts
    Button captureButton , sendButton;
    ProgressBar progressBar;
    TextView ipAndPortText,statusText, percentText;

    //Uri for the saved video
    Uri videoUri;

    //Async task to upload video
    ClientAsyncTask clientAsyncTask;

    //handler to post UI updates to UI thread
    Handler handler = new Handler();

    //codes for checking camera result
    public static final int VIDEO_CAPTURED = 101;
    public static final int PERMISSION_REQUEST_CODE = 200;

    //keys for saving various parameters
    private static final String PORT_PARAM = "port_param";
    private static final String IP_PARAM = "ip_param";
    private static final String FILE_PATH = "file_path";
    private static final String PROGRESS = "progress_value";


    public ConnectionFragment() {
        // Required empty public constructor
    }

    public static ConnectionFragment newInstance(String ip, String port) {

        ConnectionFragment fragment = new ConnectionFragment();

        //saved ip and port sent by the connection activity
        Bundle args = new Bundle();
        args.putString(IP_PARAM,ip);
        args.putString(PORT_PARAM,port);

        fragment.setArguments(args); //set the bundle to the fragment

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //retain fragment
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_connection, container, false);

        //find the id for the UI objects
        ipAndPortText = (TextView) view.findViewById(R.id.addrId);
        statusText = (TextView) view.findViewById(R.id.statusId);
        percentText = (TextView) view.findViewById(R.id.percentTextId);
        captureButton = (Button) view.findViewById(R.id.captureVideoId);
        sendButton = (Button) view.findViewById(R.id.sendButtonId);
        progressBar = (ProgressBar) view.findViewById(R.id.progressId);

        //video not yet captured, so disable send button
        //fade out button to indicate that it is disabled
        if(videoUri == null){
            sendButton.setEnabled(false);
            sendButton.setAlpha(0.5f);
        }

        //set click listeners to the buttons
        captureButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);

        if(savedInstanceState != null){
            //get filepath for saved video file on orientaion change
            filePath = savedInstanceState.getString(FILE_PATH);
            if(filePath != null) {
                statusText.setText("Video saved to "+filePath+". Click Send.");
            }else {
                statusText.setText("Press Capture Button to capture video");
            }

            //get the progress from previous orientation
            progressBar.setProgress(savedInstanceState.getInt(PROGRESS));
            percentText.setText(savedInstanceState.getInt(PROGRESS)+" %");
        }

        //get the ip and port from the bundle saved in newInstance()
        Bundle bundle = getArguments();
        ip = bundle.getString(IP_PARAM);
        port = bundle.getString(PORT_PARAM);

        if(bundle != null) {
            ipAndPortText.setText("IP : "+ip+" and Port : "+port);
        }else {
            ipAndPortText.setText("Nothing received");
        }

        //return the view to create for the fragment
        return view;
    }

    //button listeners for capture button and send button
    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.captureVideoId:

                //check if device has camera and start recording video
                if (Utils.hasCamera(getActivity())) {
                    captureButton.setEnabled(true);
                    sendButton.setAlpha(1f);
                    startRecording();
                } else {
                    captureButton.setEnabled(false);
                    captureButton.setAlpha(0.5f);
                    ToastMaker.makeLongToast(getActivity(),"No Camera Found");
                    return;
                }

                break;

            case R.id.sendButtonId:

                //check for Wifi connection and SD card in the device
                if( getContext() != null && !Utils.isConnected(getContext()) ) {
                    ToastMaker.makeLongToast(getContext(),"Please turn on Wifi");
                    return;
                } else if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                    ToastMaker.makeLongToast(getContext(),"Please insert SD card");
                    return;
                }

                statusText.setText("Sending...");

                //ask write exteranal storage permission for android 6.0 and greater
                String requiredPermission = "android.permission.WRITE_EXTERNAL_STORAGE";
                int checkVal = getContext().checkCallingOrSelfPermission(requiredPermission);
                if(checkVal == PackageManager.PERMISSION_GRANTED){
                    //permission already given, start sending video
                    startVideoTransfer();
                }else if (Utils.shouldAskPermission()) {
                    //for android 6 and above, ask permission
                    String[] perms = {"android.permission.WRITE_EXTERNAL_STORAGE"};
                    requestPermissions(perms, PERMISSION_REQUEST_CODE);
                }

                break;

            default:
                ToastMaker.makeShortToast(getContext(),"No button pressed");

        }
    }

    //starts video capture intent
    public void startRecording() {

            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 60);
            startActivityForResult(intent, VIDEO_CAPTURED);

    }

    //checks if video has been captured on result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == VIDEO_CAPTURED){

            //RESULT_OK indicates successful video capture
            if(resultCode == RESULT_OK && data != null) {

                //get the uri for the video
                videoUri = data.getData();
                ToastMaker.makeLongToast(getContext(),"Video has been saved!");

                //get file path from the Uri
                filePath = Utils.getRealPathFromURI(getContext(),videoUri);
                statusText.setText("Video saved to "+filePath+" Click Send.");

                //after video has been saved, send can be clicked
                sendButton.setEnabled(true);
                sendButton.setAlpha(1f);

            } else if (resultCode == RESULT_CANCELED) {
                //video cancelled by user, let them know
                ToastMaker.makeShortToast(getContext(),"Video recording cancelled");
            } else {
                ToastMaker.makeShortToast(getContext(),"Failed to save");
            }

        }

    }


    //permission results, handle different cases
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {

            //permission granted by user, start sending video
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //start sending video
                startVideoTransfer();

            } else if(grantResults[0] == PackageManager.PERMISSION_DENIED){
                //user denied permission, explain why permission is needed
                ToastMaker.makeLongToast(getContext(),"Need permission to save video..Please grant permission");
            } else {
                ToastMaker.makeShortToast(getContext(),"Permission Denied");
            }
        }
    }


    // Thread to send video
    private class ClientAsyncTask extends AsyncTask<Void,Integer,Void> {

        //IP and port
        String dstAddress;
        int dstPort;

        //variable for get progress bar value
        long total = 0;

        //saved video file
        File videoFile;

        //AsyncTask constructor
        public ClientAsyncTask(String dstAddress,int dstPort) {
            this.dstAddress = dstAddress;
            this.dstPort = dstPort;
        }

        //method called before thread starts executing
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            //set initial progress bar value
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(0);

            //disable  buttons till upload is complete
            sendButton.setEnabled(false);
            sendButton.setAlpha(0.5f);
            captureButton.setEnabled(false);
            captureButton.setAlpha(0.5f);

            statusText.setText("Sending video....");

            //set percentage sent text to 0%
            percentText.setVisibility(View.VISIBLE);
            percentText.setText("0 %");

            //create new file with the saved video path
            videoFile = new File(filePath);

        }

        //Thread runs in background
        @Override
        protected Void doInBackground(Void... params) {

            //////
            //client socket
            Socket socket = null;

            try {

                //create InetAddress from String IP
                InetAddress serverAddr =
                        InetAddress.getByName(dstAddress);
                socket = new Socket(serverAddr,dstPort); //create socket from IP and port

                //Get OutputStream from socket to write video data
                OutputStream outputStream = socket.getOutputStream();

                //Get FileInputStream from saved video
                FileInputStream in = new FileInputStream(videoFile);

                byte[] buffer = new byte[1024]; // 1KB buffer size

                //get dataoutputstream to send file size to host app
                DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream());
                dataOutputStream.writeLong(videoFile.length());
                dataOutputStream.flush();   //clear dataoutputstream after writing

                //read data from file into buffer
                int length = 0;
                while ( (length = in.read(buffer, 0, buffer.length)) != -1 ){
                    total += length;
                    //calculate percentage completed value
                    int val = (int) ((total*100)/videoFile.length());

                    //send progress value to OnProgressUpdate method
                    publishProgress(val);

                    //write video data to outputstream
                    outputStream.write(buffer, 0, length);
                }

                outputStream.flush(); //flush output stream
                in.close(); //close input stream


            } catch (IOException e) {
                e.printStackTrace();
            }
            catch (NullPointerException npe){
                npe.printStackTrace();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ToastMaker.makeLongToast(getContext(),"Please record video again, Something went wrong!");
                    }
                });
            }finally {
                //close socket that was open
                if(socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            //////
            return null;
        }

        //recieves progress from doInBackground method
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            //set the progress value
            progressBar.setProgress(values[0]);
            percentText.setText(values[0]+" %");
        }

        //UI updates in this method
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            ToastMaker.makeShortToast(getContext(),"Done Uploading");
            statusText.setText("Video Sent!");

            //re-enable buttons after upload
            sendButton.setEnabled(true);
            sendButton.setAlpha(1f);
            captureButton.setEnabled(true);
            captureButton.setAlpha(1f);
        }

    }

    //starts new async task
    public void startVideoTransfer(){
        if(clientAsyncTask != null){
            clientAsyncTask.cancel(true);
        }
        clientAsyncTask = new ClientAsyncTask(ip,Integer.parseInt(port));
        clientAsyncTask.execute();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //saved any parameters before orientation change
        outState.putString(FILE_PATH,filePath);
        outState.putInt(PROGRESS,progressBar.getProgress());
    }
}

