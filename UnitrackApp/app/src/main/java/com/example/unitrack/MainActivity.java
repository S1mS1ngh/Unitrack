package com.example.unitrack;

import com.squareup.picasso.Picasso;

import java.io.FileNotFoundException;

import android.net.Uri;
import android.provider.MediaStore;
import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Regions;

public class MainActivity extends Activity {

    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    // --- Constants to modify per your configuration ---

    // Customer specific IoT endpoint
    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_ENDPOINT = "a1t8x9a9l0hxhj-ats.iot.us-east-2.amazonaws.com";

    // Cognito pool ID. For this app, pool needs to be unauthenticated pool with
    // AWS IoT permissions.
    private static final String COGNITO_POOL_ID = "us-east-2:7c15273b-bcc7-495f-a583-0b82cbc2311a";

    // Region of AWS IoT
    private static final Regions MY_REGION = Regions.US_EAST_2;

    //EditText txtSubscribe;
    //EditText txtTopic;
    EditText txtMessage;

    //TextView tvLastMessage;
    TextView tvClientId;
    TextView tvStatus;

    Button btnConnect;
    //Button btnSubscribe;
    Button btnPublish;
    Button btnDisconnect;

    AWSIotMqttManager mqttManager;
    String clientId;

    CognitoCachingCredentialsProvider credentialsProvider;

    Button btnLoadImage;
    TextView textSource;
    ImageView imageResult, imageDrawingPane;

    final int RQS_IMAGE1 = 1;

    Uri source;
    Bitmap bitmapMaster;
    Canvas canvasMaster;
    Bitmap bitmapDrawingPane;
    Canvas canvasDrawingPane;
    projectPt startPt;

    private static int VIDEO_REQUEST =101;
    //private Uri videoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Todo MQTT

        btnLoadImage = findViewById(R.id.loadimage);
        textSource = findViewById(R.id.sourceuri);
        imageResult = findViewById(R.id.result);
        imageDrawingPane = findViewById(R.id.drawingpane);

        //txtSubscribe = (EditText) findViewById(R.id.txtSubscribe);
        //txtTopic = (EditText) findViewById(R.id.txtTopic);
        //txtMessage = (EditText) findViewById(R.id.txtMessage);

        //tvLastMessage = (TextView) findViewById(R.id.tvLastMessage);
        tvClientId = (TextView) findViewById(R.id.tvClientId);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        btnConnect = (Button) findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(connectClick);
        btnConnect.setEnabled(false);

        //btnSubscribe = (Button) findViewById(R.id.btnSubscribe);
        //btnSubscribe.setOnClickListener(subscribeClick);

        btnPublish = (Button) findViewById(R.id.btnPublish);
        btnPublish.setOnClickListener(publishClick);

        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnDisconnect.setOnClickListener(disconnectClick);

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        clientId = UUID.randomUUID().toString();
        tvClientId.setText(clientId);

        // Initialize the AWS Cognito credentials provider
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(), // context
                COGNITO_POOL_ID, // Identity Pool ID
                MY_REGION // Region
        );

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_ENDPOINT);

        // The following block uses a Cognito credentials provider for authentication with AWS IoT.
        new Thread(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnConnect.setEnabled(true);
                    }
                });
            }
        }).start();

        //Picasso.get().load("https://www.dropbox.com/s/rgbr1vwh613a422/image.jpg?raw=1").into(imageResult);

        btnLoadImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, RQS_IMAGE1);
            }
        });

        Picasso.get().load("https://www.dropbox.com/s/rgbr1vwh613a422/image.jpg?raw=1").fit().into(imageResult);

        imageResult.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getAction();
                int x = (int) event.getX();
                int y = (int) event.getY();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        textSource.setText(x + " : " + y);
                        //textSource.setText("ACTION_DOWN- " + x + " : " + y);
                        startPt = projectXY((ImageView) v, bitmapMaster, x, y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        textSource.setText(x + " : " + y);
                        //textSource.setText("ACTION_MOVE- " + x + " : " + y);
                        drawOnRectProjectedBitMap((ImageView) v, bitmapMaster, x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        textSource.setText(x + " : " + y);
                        //textSource.setText("ACTION_UP- " + x + " : " + y);
                        drawOnRectProjectedBitMap((ImageView) v, bitmapMaster, x, y);
                        finalizeDrawing();
                        break;
                }
                /*
                 * Return 'true' to indicate that the event have been consumed.
                 * If auto-generated 'false', your code can detect ACTION_DOWN only,
                 * cannot detect ACTION_MOVE and ACTION_UP.
                 */
                return true;
            }
        });

    }

    View.OnClickListener connectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Log.d(LOG_TAG, "clientId = " + clientId);

            try {
                mqttManager.connect(credentialsProvider, new AWSIotMqttClientStatusCallback() {
                    @Override
                    public void onStatusChanged(final AWSIotMqttClientStatus status,
                                                final Throwable throwable) {
                        Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (status == AWSIotMqttClientStatus.Connecting) {
                                    tvStatus.setText("Connecting...");

                                } else if (status == AWSIotMqttClientStatus.Connected) {
                                    tvStatus.setText("Connected");

                                } else if (status == AWSIotMqttClientStatus.Reconnecting) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                    }
                                    tvStatus.setText("Reconnecting");
                                } else if (status == AWSIotMqttClientStatus.ConnectionLost) {
                                    if (throwable != null) {
                                        Log.e(LOG_TAG, "Connection error.", throwable);
                                        throwable.printStackTrace();
                                    }
                                    tvStatus.setText("Disconnected");
                                } else {
                                    tvStatus.setText("Disconnected");

                                }
                            }
                        });
                    }
                });
            } catch (final Exception e) {
                Log.e(LOG_TAG, "Connection error.", e);
                tvStatus.setText("Error! " + e.getMessage());
            }
        }
    };

    /*

    View.OnClickListener subscribeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final String topic = txtSubscribe.getText().toString();

            Log.d(LOG_TAG, "topic = " + topic);

            try {
                mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
                        new AWSIotMqttNewMessageCallback() {
                            @Override
                            public void onMessageArrived(final String topic, final byte[] data) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String message = new String(data, "UTF-8");
                                            Log.d(LOG_TAG, "Message arrived:");
                                            Log.d(LOG_TAG, "   Topic: " + topic);
                                            Log.d(LOG_TAG, " Message: " + message);

                                            tvLastMessage.setText(message);

                                        } catch (UnsupportedEncodingException e) {
                                            Log.e(LOG_TAG, "Message encoding error.", e);
                                        }
                                    }
                                });
                            }
                        });
            } catch (Exception e) {
                Log.e(LOG_TAG, "Subscription error.", e);
            }
        }
    };

     */

    View.OnClickListener publishClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            final String topic = "demo-topic";
            final String msg = "{" +
                    " \"Coordinates\": " +
                    " \"10,20,30,40\" " +
                    "}";

            try {
                mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Publish error.", e);
            }

        }
    };

    View.OnClickListener disconnectClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            try {
                mqttManager.disconnect();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Disconnect error.", e);
            }

        }
    };

    class projectPt {
        int x;
        int y;

        projectPt(int tx, int ty) {
            x = tx;
            y = ty;
        }
    }

    private projectPt projectXY(ImageView iv, Bitmap bm, int x, int y) {
        if (x < 0 || y < 0 || x > iv.getWidth() || y > iv.getHeight()) {
            //outside ImageView
            return null;
        } else {
            int projectedX = (int) ((double) x * ((double) bm.getWidth() / (double) iv.getWidth()));
            int projectedY = (int) ((double) y * ((double) bm.getHeight() / (double) iv.getHeight()));

            return new projectPt(projectedX, projectedY);
        }
    }

    private void drawOnRectProjectedBitMap(ImageView iv, Bitmap bm, int x, int y) {
        if (x < 0 || y < 0 || x > iv.getWidth() || y > iv.getHeight()) {
            //outside ImageView
            return;
        } else {
            int projectedX = (int) ((double) x * ((double) bm.getWidth() / (double) iv.getWidth()));
            int projectedY = (int) ((double) y * ((double) bm.getHeight() / (double) iv.getHeight()));

            //clear canvasDrawingPane
            canvasDrawingPane.drawColor(Color.TRANSPARENT, Mode.CLEAR);

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(10);
            canvasDrawingPane.drawRect(startPt.x, startPt.y, projectedX, projectedY, paint);
            imageDrawingPane.invalidate();


            textSource.setText(x + ":" + y + "/" + iv.getWidth() + " : " + iv.getHeight() + "\n" +
                    projectedX + " : " + projectedY + "/" + bm.getWidth() + " : " + bm.getHeight()
            );
        }
    }

    private void finalizeDrawing() {
        canvasMaster.drawBitmap(bitmapDrawingPane, 0, 0, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        //if(requestCode == VIDEO_REQUEST && resultCode == RESULT_OK){
        //    videoUri = data.getData();
        //}

        super.onActivityResult(requestCode, resultCode, data);

        Bitmap tempBitmap;

        //if (resultCode == RESULT_OK) {
            //if (requestCode == RQS_IMAGE1) {
                //case RQS_IMAGE1:
                    source = data.getData();
                assert source != null;
                textSource.setText(source.toString());

                    try {
                        //tempBitmap is Immutable bitmap,
                        //cannot be passed to Canvas constructor
                        tempBitmap = BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(source));

                        Config config;
                        if (tempBitmap.getConfig() != null) {
                            config = tempBitmap.getConfig();
                        } else {
                            config = Config.ARGB_8888;
                        }

                        //bitmapMaster is Mutable bitmap
                        bitmapMaster = Bitmap.createBitmap(
                                tempBitmap.getWidth(),
                                tempBitmap.getHeight(),
                                config);

                        canvasMaster = new Canvas(bitmapMaster);
                        canvasMaster.drawBitmap(tempBitmap, 0, 0, null);

                        imageResult.setImageBitmap(bitmapMaster);

                        //Create bitmap of same size for drawing
                        bitmapDrawingPane = Bitmap.createBitmap(
                                tempBitmap.getWidth(),
                                tempBitmap.getHeight(),
                                config);
                        canvasDrawingPane = new Canvas(bitmapDrawingPane);
                        imageDrawingPane.setImageBitmap(bitmapDrawingPane);


                    } catch (FileNotFoundException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    //break;
            //}
        //}
    }

    public void Record(View view){
        Intent videoIntent = new Intent (MediaStore.ACTION_VIDEO_CAPTURE);
        if(videoIntent.resolveActivity (getPackageManager())!=null){
            startActivityForResult(videoIntent, VIDEO_REQUEST);
        }

    }

    public void Watch(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivity(intent);
    }

/*    public void Watch(View view){
        Intent playVideo = new Intent(this,playVideo.class);
        playVideo.putExtra("videoUri",videoUri.toString());
        startActivity(playVideo);
    }*/

}