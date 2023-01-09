package com.helloitsmeadm.customocto;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.webkit.WebView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Objects;

public class DatabaseActivity extends AppCompatActivity {
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    CountDownTimer cnt;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.database_view);

        TextView mainTitle = findViewById(R.id.mainTitle);
        mainTitle.setText(Html.fromHtml("Custom<b>Octo</b>"));
        TextView content = findViewById(R.id.content);
        content.setText("Logging in to database...");

        FirebaseApp.initializeApp(this);
        FirebaseAuth.getInstance().signInWithEmailAndPassword(getString(R.string.firestore_email), getString(R.string.firestore_password)).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                content.setText("Getting data from database...");
                cnt = new CountDownTimer(2000, 1000) {
                    @Override
                    public void onTick(long l) {

                    }

                    @Override
                    public void onFinish() {
                        db.collection("json").get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                StringBuilder sb = new StringBuilder();
                                String spacer = "\n\n-----------------------------------------------------\n\n";
                                for (DocumentSnapshot document : task.getResult()) {
                                    for (String key : Objects.requireNonNull(document.getData()).keySet()) {
                                        String value = Objects.requireNonNull(document.getData().get(key)).toString().replaceAll(",", ",\n");
                                        sb.append(key).append(":\n").append(value).append(spacer);
                                    }
                                }
                                sb.delete(sb.length() - spacer.length(), sb.length());
                                content.setText(sb.toString());
                            }
                        });
                        start();
                    }
                };
                cnt.start();
            } else {
                content.setText("Error: " + task.getException().getMessage());
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cnt.cancel();
    }
}
