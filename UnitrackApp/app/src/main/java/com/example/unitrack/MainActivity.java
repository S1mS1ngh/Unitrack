package com.example.unitrack;

//import com.squareup.picasso.Picasso;

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
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity {

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
    private Uri videoUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ImageView imageView = findViewById(R.id.Unitrackimage);
        //String url = "https://www.dropbox.com/s/1l9ttno6nt2zgqb/Capture.PNG?raw=1"; //Change with different Host
        //Picasso.with(this).invalidate(url);
        //Picasso.with(this).load(url).into(imageView);
        //Todo MQTT

        btnLoadImage = findViewById(R.id.loadimage);
        textSource = findViewById(R.id.sourceuri);
        imageResult = findViewById(R.id.result);
        imageDrawingPane = findViewById(R.id.drawingpane);

        btnLoadImage.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, RQS_IMAGE1);
            }
        });


        imageResult.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getAction();
                int x = (int) event.getX();
                int y = (int) event.getY();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        textSource.setText("ACTION_DOWN- " + x + " : " + y);
                        startPt = projectXY((ImageView) v, bitmapMaster, x, y);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        textSource.setText("ACTION_MOVE- " + x + " : " + y);
                        drawOnRectProjectedBitMap((ImageView) v, bitmapMaster, x, y);
                        break;
                    case MotionEvent.ACTION_UP:
                        textSource.setText("ACTION_UP- " + x + " : " + y);
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

        if(requestCode == VIDEO_REQUEST && resultCode == RESULT_OK){
            videoUri = data.getData();
        }

        super.onActivityResult(requestCode, resultCode, data);

        Bitmap tempBitmap;

        if (resultCode == RESULT_OK) {
            if (requestCode == RQS_IMAGE1) {
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
            }
        }
    }

    public void Record(View view){
        Intent videoIntent = new Intent (MediaStore.ACTION_VIDEO_CAPTURE);
        if(videoIntent.resolveActivity (getPackageManager())!=null){
            startActivityForResult(videoIntent, VIDEO_REQUEST);
        }

    }

    public void Watch(View view){
        Intent playVideo = new Intent(this,playVideo.class);
        playVideo.putExtra("videoUri",videoUri.toString());
        startActivity(playVideo);
    }

}