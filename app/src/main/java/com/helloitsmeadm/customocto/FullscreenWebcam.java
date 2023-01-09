package com.helloitsmeadm.customocto;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;

public class FullscreenWebcam extends AppCompatActivity {
    CountDownTimer timer;
    static Bitmap webcamImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fullscreen_webcam);

        ImageView webcam = findViewById(R.id.webcam);
        TextView loadingWebcam = findViewById(R.id.loadingWebcam);

        timer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                getWebcamImage();
                Thread thread = new Thread(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (webcamImage != null) {
                        runOnUiThread(() -> {
                            webcam.setImageBitmap(webcamImage);
                            loadingWebcam.setVisibility(View.GONE);
                        });
                    }
                });
                thread.start();
                start();
            }
        };
        timer.start();

        ImageButton refreshWebcamButton = findViewById(R.id.refreshWebcamButton);
        refreshWebcamButton.setOnClickListener(v -> {
            JSONArray commandList = new JSONArray();
            commandList.put("refreshWebcam");
            MainActivity.sendCommand(commandList, this);
            Toast.makeText(this, "Refresh of webcam requested!", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timer.cancel();
    }

    public static void getWebcamImage() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference islandRef = storageRef.child("webcam.jpg");
        final long ONE_MEGABYTE = 1024 * 1024;
        islandRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
            webcamImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        });
    }
}
