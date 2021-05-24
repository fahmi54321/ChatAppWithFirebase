package com.android.chatappwithfirebase.Common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;

import androidx.core.app.NotificationCompat;

import com.android.chatappwithfirebase.Model.UserModel;
import com.android.chatappwithfirebase.R;
import com.android.chatappwithfirebase.Services.MyFCMService;
import com.firebase.ui.auth.data.model.User;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Random;

public class Common {
    public static final String USER_REFERENCE = "People";
    public static final String CHAT_LIST_REFERENCE = "ChatList";
    public static final String CHAT_REFERENCE = "Chat";
    public static final String CHAT_DETAIL_REFERENCE = "Detail";
    public static final String NOTI_TITLE = "title";
    public static final String NOTI_CONTENT = "content";
    public static final String NOTI_SENDER = "sender";
    public static final String NOTI_ROOM_ID = "room_id";
    public static UserModel currentUser = new UserModel();
    public static UserModel chatuser = new UserModel();
    public static String roomSelected = "";

    public static String generateChatRoomId(String a, String b) {
        if (a.compareTo(b) > 0) {
            return new StringBuilder(a).append(b).toString();
        } else if (a.compareTo(b) < 0) {
            return new StringBuilder(b).append(a).toString();
        } else {
            return new StringBuilder("Chat_Your_Self_Error").append(new Random().nextInt()).toString();
        }
    }

    public static String getName(UserModel chatuser) {
        return new StringBuilder(chatuser.getFirstName())
                .append(" ")
                .append(chatuser.getLastName())
                .toString();
    }

    public static String getFileName(ContentResolver contentResolver, Uri fileUri) {
        String result = null;
        if (fileUri.getScheme().equals("content")) {
            Cursor cursor = contentResolver.query(fileUri, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }

        if (result == null) {
            result = fileUri.getPath();
            int cut = result.lastIndexOf("/");
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }

        return result;
    }

    public static void showNotification(
            Context context,
            int id,
            String title,
            String content,
            String sender,
            String roomId,
            Intent intent) {

        PendingIntent pendingIntent = null;
        if (intent != null) {
            pendingIntent = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        String NOTIFICATION_CHANNEL_ID = "com.android.chatappwithfirebase";

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //check version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Chat App",
                    NotificationManager.IMPORTANCE_DEFAULT);

            notificationChannel.setDescription("Chat App");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);

            notificationManager.createNotificationChannel(notificationChannel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID);
        builder.setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_baseline_image_24));

        if (pendingIntent != null) {
            builder.setContentIntent(pendingIntent);
        }


        Notification notification = builder.build();
        if (!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(sender) && !Common.roomSelected.equals(roomId)) {
            notificationManager.notify(id, notification);
        }

    }
}
