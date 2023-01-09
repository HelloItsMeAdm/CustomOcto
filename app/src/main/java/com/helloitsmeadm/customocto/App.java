package com.helloitsmeadm.customocto;

import android.app.Application;
import android.content.Intent;
import android.widget.Toast;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        startService(new Intent(this, KeepAlive.class));
    }
}
