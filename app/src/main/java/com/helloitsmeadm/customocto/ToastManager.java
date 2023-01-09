package com.helloitsmeadm.customocto;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class ToastManager extends Dialog {
    public Activity activity;
    public int duration, image;
    public String message;
    ImageView imageW;
    TextView messageW;
    RelativeLayout toast_mainW;

    public ToastManager(Activity activity, int duration, int imageId, String message) {
        super(activity);
        this.activity = activity;
        this.duration = duration;
        this.image = imageId;
        this.message = message;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_toast);

        messageW = findViewById(R.id.message);
        imageW = findViewById(R.id.image);
        toast_mainW = findViewById(R.id.toast_main);

        messageW.setText(message);
        imageW.setImageResource(image);
        toast_mainW.setOnClickListener(v -> dismiss());

        new Handler().postDelayed(() -> {
            dismiss();
        }, duration * 1000);
    }

    public static void run(Activity activity, int duration, int imageId, String message) {
        ToastManager toastManager = new ToastManager(activity, duration, imageId, message);
        toastManager.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        toastManager.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        toastManager.getWindow().getAttributes().windowAnimations = R.style.ToastAnimation;
        Window window = toastManager.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.gravity = Gravity.BOTTOM;
        wlp.verticalMargin = 0.05f;
        window.setAttributes(wlp);
        toastManager.show();
    }
}