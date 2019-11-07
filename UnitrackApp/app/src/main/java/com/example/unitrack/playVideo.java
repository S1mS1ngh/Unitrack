package com.example.unitrack;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import java.util.Objects;

public class playVideo extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_video);
        VideoView mVideoView = findViewById(R.id.VideoVIew);

        Uri videoUri= Uri.parse(Objects.requireNonNull(getIntent().getExtras()).getString("videoUri"));
        mVideoView.setVideoURI(videoUri);
        mVideoView.start();

    }
}