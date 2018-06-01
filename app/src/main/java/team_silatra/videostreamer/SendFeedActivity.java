package team_silatra.videostreamer;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class SendFeedActivity extends AppCompatActivity implements Runnable{
    Button startSendFeed,toggleFlash;
    EditText IPAddrEditText,PortAddrEditText;
    TextView socketRecvrTextView,gestureTextView;
    Socket senderSocket;
    BufferedWriter out;
    BufferedReader in;
    String IPAddr;
    ImgWriter imgWriter;
    private Camera mCamera;
    private CameraPreview mPreview;
    int commonPort,imagePort;
    Socket imgSenderSocket;
    NetworkInfo mWifi;
    TextToSpeech textToSpeech;
    SharedPreferences sharedPreferences;
    boolean NetworkConnected;
    Thread NetworkCheckerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        networkChecker();
        //To enable fullscreen mode -
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_send_feed);
        textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });
        startSendFeed=findViewById(R.id.sendFeedStartButton);
        //toggleFlash=(Button)findViewById(R.id.flashBtn);
        IPAddrEditText=findViewById(R.id.IPAddrText);
        PortAddrEditText = findViewById(R.id.PortAddrText);
        socketRecvrTextView = findViewById(R.id.socketRecvrTextView);
        //gestureTextView = (TextView)findViewById(R.id.gestureText);

        //sharedPreferences =

        startSendFeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(IPAddrEditText.isEnabled()){
                    IPAddrEditText.setEnabled(false);
                    PortAddrEditText.setEnabled(false);

                    Log.d("VS123","Sender Thread - detected click");

                    IPAddr=IPAddrEditText.getText().toString();
                    imagePort=Integer.parseInt(PortAddrEditText.getText().toString());

                    Log.d("VS123","Sender Thread - found IP Address to be "+IPAddr);
                    if(IPAddr!=null && IPAddr.length()!=0){
                        Log.d("VS123","Sender Thread - created");
//                    new Thread(SendFeedActivity.this).start();
                        imgWriter=new ImgWriter();
                        imgWriter.start();
                    }

                    startSendFeed.setBackgroundColor(Color.RED);
                    startSendFeed.setText("Stop Feed");
                }
                else{
                    IPAddrEditText.setEnabled(true);
                    PortAddrEditText.setEnabled(true);

                    Log.d("VS123","Sender Thread - detected click");


                    try{
                        mCamera.setPreviewCallback(null);
                        Log.d("VS123","iuhiuguygi");
                        imgWriter.imgOutput.writeInt(0);
                        imgWriter.join();
                        imgWriter.signReader.join();

                        imgSenderSocket.close();
                    }
                    catch(IOException e){
                        Log.d("VS123","Sender Thread - IOException from onStop() in SendFeed"+e.toString());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    startSendFeed.setBackgroundColor(Color.BLUE);
                    startSendFeed.setText("Start Feed");
                }


            }
        });

        /*toggleFlash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });*/

//        commonPort=Integer.parseInt(getResources().getString(R.string.common_socket));
//        imagePort=Integer.parseInt(getResources().getString(R.string.img_socket));

        // Create an instance of Camera
        mCamera = getCameraInstance();

        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        FrameLayout preview = findViewById(R.id.camPreview);
        preview.addView(mPreview);

    }


    @Override
    protected void onStop() {
        super.onStop();
        mCamera.release();
        if(textToSpeech !=null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        NetworkCheckerThread.stop();
//        try{
//
////            senderSocket.close();
////            imgSenderSocket.close();
//        }
//        catch(IOException e){
//            Log.d("VS123","Sender Thread - IOException from onStop() in SendFeed"+e.toString());
//        }

    }



    /**
     * This function establishes socket connection over which dates will be sent
     */
    void initializeSendFeedActivity(String ip,int PortNo){
        try{
            senderSocket=new Socket(ip,PortNo);
            Log.d("VS123","Sender Thread - created socket");
            out=new BufferedWriter(new OutputStreamWriter(senderSocket.getOutputStream()));


        }
        catch (IOException e){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),"Device not found!",Toast.LENGTH_LONG).show();
                }
            });

            Log.d("VS123","Sender Thread - IOException from initializeSendFeedActivity()"+e.toString());
        }

    }

    private void networkChecker() {
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkConnected = true;
        NetworkCheckerThread = new Thread() {
            public void run() {
                int i = 0;
                while (true) {
                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mWifi.isConnected()) {
                                    if(!NetworkConnected)
                                    {
                                        NetworkConnected = true;
                                        startSendFeed.setEnabled(true);
                                        Toast.makeText(getApplicationContext(), "WIFI connected", Toast.LENGTH_SHORT).show();
                                    }

                                }
                                else
                                {
                                    if(NetworkConnected)
                                    {
                                        NetworkConnected = false;
                                        startSendFeed.setEnabled(false);
                                        Toast.makeText(getApplicationContext(), "WIFI not connected", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        NetworkCheckerThread.start();
    }

    /**
     * This thread is used for sending dates over socket
     */
    @Override
    public void run() {

        initializeSendFeedActivity(IPAddr,commonPort);

        try{
            while(true){
                Log.d("VS123","Sender Thread - created date content to send ");
                Calendar c=Calendar.getInstance();
                String date=c.get(Calendar.HOUR_OF_DAY)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND);
                out.write(date+"\r\n");
                out.flush();
                Log.d("VS123","Sender Thread - wrote "+date+" via socket");

                Thread.sleep(1000);
            }
        }
        catch (Exception e){
            Log.d("VS123",e.toString());
        }
    }




    /**
     * Reference: https://developer.android.com/guide/topics/media/camera.html#access-camera
     **/
    /* A safe way to get an instance of the Camera object. */
    public static Camera getCameraInstance(){
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
            List<Camera.Size> ls= c.getParameters().getSupportedPreviewSizes();  //Reference: https://stackoverflow.com/a/8385634/5370202
            Log.d("VS123","Supported Resolutions:");
            boolean flagResoExists =false;
            for(Camera.Size s:ls){
                Log.d("VS123",s.width+"x"+s.height);
                if(s.width == 640 && s.height == 480){
                    flagResoExists = true;
                }
//                if(s.width == 320 && s.height == 240){
//                    flagResoExists = true;
//                }
            }

            if(flagResoExists){
                Camera.Parameters params = c.getParameters();
                params.setPreviewSize(640,480);

                List<Integer> ls1 = params.getSupportedPreviewFrameRates();
                Log.d("VS123",ls1.toString());

                Log.d("VS123","Previous preview frame rate"+params.getPreviewFrameRate());

//                params.setPreviewFrameRate(params.getSupportedPreviewFrameRates().get(0));

                Log.d("VS123","Current preview frame rate"+params.getPreviewFrameRate());


                params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//                c.setDisplayOrientation(90);
//                params.setRotation(90);
                c.setParameters(params);
            }

        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }
        return c; // returns null if camera is unavailable
    }


    /**
     * Inner class is used so that some variables defined in Activity can be used directly without thinking about passing them to this class instance
     */
    class ImgWriter extends Thread{

        DataOutputStream imgOutput;
        //String imgFolderPath="/storage/F074-706E/Demo/";  //Here saved images were present which were being sent over socket for testing

//        Calendar pastSentImageCal;

        int imgCtr = 0;
        int customFrameRate = 5;

        int frameNoThresh = 30 / customFrameRate;

        SignReader signReader;


        /**
         * This function establishes socket connection over which images will be sent
         */
        void initializeImgSend(String ip,int PortNo2){
//            pastSentImageCal=Calendar.getInstance();  //This will be useful in setPreviewCallback
            try{
                imgSenderSocket=new Socket(ip,PortNo2);
                imgOutput = new DataOutputStream(imgSenderSocket.getOutputStream());
//                in = new BufferedReader(new InputStreamReader(imgSenderSocket.getInputStream()));
                signReader = new SignReader();
                signReader.start();
                Log.d("VS123","Img Sender Thread - created socket");

            }
            catch (IOException e){
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Device not found!",Toast.LENGTH_LONG).show();
                    }
                });

                Log.d("VS123","Img Sender Thread - IOException from initializeImgSend()"+e.toString());
            }

        }


        /**
         * This thread is used for sending images over socket
         */
        @Override
        public void run() {
            initializeImgSend(IPAddr,imagePort);

            /**
             * Reference: http://stackoverflow.com/questions/16602736/android-send-an-image-through-socket-programming
             **/
            try{
//                while(true){
                    /**
                     * takePicture caused a lot of latency while refreshing preview. Hence this was not selected
                     **/
                    mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                        @Override
                        public void onPreviewFrame(byte[] imgBytes, Camera camera) {
                            try{
//                                Calendar currentTime=Calendar.getInstance();
//                                if(pastSentImageCal.get(Calendar.MILLISECOND)/100==currentTime.get(Calendar.MILLISECOND)/100){
//                                    /**
//                                     * Compare current time milliseconds and milliseconds at which last image frame was sent.
//                                     * Here, both are divided by 100 thus achieving a frame rate of 10 fps
//                                     */
//                                    return;
//                                }
//
//                                pastSentImageCal=currentTime;  //If new image frame in new millisecond segment, store current Calendar instance
                                imgCtr = (imgCtr + 1)%(frameNoThresh);
                                if((imgCtr) != 0){
                                    return;
                                }

                                Log.d("VS123",imgBytes.length+"");


                                /**
                                 * Initially YUV to Bitmap conversion was performed on receiver side which was found to be inefficient
                                 * Reference: http://stackoverflow.com/a/9330203/5370202
                                 */
                                Camera.Parameters parameters = camera.getParameters();
                                int width = parameters.getPreviewSize().width;
                                int height = parameters.getPreviewSize().height;
                                ByteArrayOutputStream out = new ByteArrayOutputStream();
                                YuvImage yuvImage = new YuvImage(imgBytes, ImageFormat.NV21, width, height, null);
                                yuvImage.compressToJpeg(new Rect(0, 0, width, height), 50, out);
                                byte[] imageBytes = out.toByteArray();

                                imgOutput.writeInt(imageBytes.length); //Send image size
                                imgOutput.write(imageBytes,0,imageBytes.length); //Send image
                                imgOutput.flush();

                                int len=imageBytes.length;


                                Log.d("VS123","Img Sender Thread - sent image of len "+len+" via socket");

                            }
                            catch (Exception e){
                                Log.d("VS123 Cam","Exception thrown from onPictureTaken in PictureCallback "+e.toString());
                                e.printStackTrace();
                            }
                        }
                    });


//                    Thread.sleep(1000);
//                }
                /*
                //Stored Images Send Code
                for(int imgNo=1;imgNo<=7;imgNo++){
                    Log.d("VS123","Img Sender Thread - created content ");

                    FileInputStream fis=new FileInputStream(new File(imgFolderPath+"DemoPic"+imgNo+".jpg"));

                    Bitmap bm= BitmapFactory.decodeStream(fis);
                    byte[] imgBytes=getBytesFromBitmap(bm);
                    Log.d("VS123",imgBytes.length+"");

                    imgOutput.writeInt(imgBytes.length);
                    imgOutput.write(imgBytes,0,imgBytes.length);
                    imgOutput.flush();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Sent Content to socket",Toast.LENGTH_SHORT).show();
                        }
                    });

                    Log.d("VS123","Img Sender Thread - wrote content via socket");

                    Thread.sleep(4000);
                }*/
            }
            catch (Exception e){
                Log.d("VS123","Exception in ImgWriter Thread "+e.toString());
            }
        }

        /*
        public byte[] getBytesFromBitmap(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
            return stream.toByteArray();
        }*/
    }

    class SignReader extends Thread{

        void initializeSignRecv(){

            try{
                in = new BufferedReader(new InputStreamReader(imgSenderSocket.getInputStream()));

                Log.d("VS123","SignReader Thread - connected to socket's input stream");

            }
            catch (IOException e){


                Log.d("VS123","SignReader Thread - IOException from initializeImgSend()"+e.toString());
            }

        }

        @Override
        public void run(){
            initializeSignRecv();
            while(true){
                try{
                    final String recvdText = in.readLine();
//                    if(recvdText.indexOf("GESTURE:")!=-1){
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                gestureTextView.setText(recvdText.substring(recvdText.indexOf("GESTURE:")+("GESTURE:").length()));
//                                Log.d("VS123","Received: "+recvdText);
//                            }
//                        });
//                    }
                    if(recvdText.equals("QUIT")){
                        return;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            socketRecvrTextView.setText(recvdText);
                            Log.d("VS123","Received: "+recvdText);
                            //Text-To-Speech part: Where the actual speech comes out.
                            if(textToSpeech.speak(recvdText, TextToSpeech.QUEUE_FLUSH, null, null) == TextToSpeech.ERROR)
                            {
                                Toast.makeText(getApplicationContext(), "TTS did not work.",Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
                catch (Exception e){
                    Log.d("VS123 Cam","Exception thrown from runonUiThread in onPictureTaken in PictureCallback "+e.toString());
                    return;
                }

            }

        }
    }
}


/**
 * Reference: https://developer.android.com/guide/topics/media/camera.html#camera-preview
 * Except for parameters part, code is AS IS mentioned by example given above
 */

/** A basic Camera preview class */
class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;


        /**
         * Parameters can be changed over here
         */
        Camera.Parameters parameters=mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); //This is for auto-focus
        mCamera.setParameters(parameters);
        mCamera.setDisplayOrientation(90);



        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d("VS123 Cam", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d("VS123 Cam", "Error starting camera preview: " + e.getMessage());
        }
    }
}
