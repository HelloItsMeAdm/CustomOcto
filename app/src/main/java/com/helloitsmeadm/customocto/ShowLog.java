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

import org.json.JSONObject;

import java.util.Objects;

public class ShowLog extends AppCompatActivity {
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
                        db.collection("json").document("log").get().addOnCompleteListener(task1 -> {
                            if (task1.isSuccessful()) {
                                DocumentSnapshot document = task1.getResult();
                                if (document.exists()) {
                                    try {
                                        JSONObject historyJson = new JSONObject(Objects.requireNonNull(document.getData()).get("history").toString());

                                        StringBuilder history = new StringBuilder();
                                        for (int i = 0; i < historyJson.length(); i++) {
                                            history.append(historyJson.getString(String.valueOf(i))).append("<br>");
                                        }

                                        content.setText(Html.fromHtml(history.toString()));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    content.setText("No data found");
                                }
                            } else {
                                content.setText("Error getting data from database");
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
