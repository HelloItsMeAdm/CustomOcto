package com.helloitsmeadm.customocto;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Random;

public class Notifications {
    public static void showNotification(Context context, String title, String message, int icon, Bitmap image, long when) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "printer";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Printer", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("All notifications received from the printer");
            notificationChannel.enableLights(true);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(when)
                .setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(image).bigLargeIcon(null))
                .setContentIntent(pendingIntent);

        assert notificationManager != null;
        notificationManager.notify(new Random().nextInt(), notificationBuilder.build());
    }

    public static void showProgressNotification(Context context, String title, int progress, Bitmap image, String name, String willEndAt) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String NOTIFICATION_CHANNEL_ID = "printer";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "Print progress", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Shows the progress of the print");
            notificationChannel.enableLights(true);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(notificationChannel);
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, new Intent(context, MainActivity.class), 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        notificationBuilder.setAutoCancel(true)
                .setDefaults(Notification.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.logo_max_white)
                .setContentTitle(title)
                .setContentText("End at " + willEndAt + " - " + name)
                .setProgress(100, progress, false)
                .setOngoing(true)
                .setContentIntent(pendingIntent);

        if (image != null) {
            notificationBuilder.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(image).bigLargeIcon(null));
        }

        assert notificationManager != null;
        notificationManager.notify(1, notificationBuilder.build());
    }

    public static void removeProgressNotification(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.cancel(1);
    }
}
