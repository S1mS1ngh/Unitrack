package com.example.unitrack;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.ImageView;
import android.os.Bundle;
import com.squareup.picasso.Picasso;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageView imageView = findViewById(R.id.Unitrackimage);
        String url = "https://www.dropbox.com/s/1l9ttno6nt2zgqb/Capture.PNG?raw=1"; //Change with different Host
        Picasso.with(this).invalidate(url);
        Picasso.with(this).load(url).into(imageView);

    }
    //Todo Draw Rectangle Class with canvas, display rectangle coordinates, MQTT
}
