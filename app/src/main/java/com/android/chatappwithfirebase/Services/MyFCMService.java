package com.android.chatappwithfirebase.Services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.NonNull;

import com.android.chatappwithfirebase.ChatActivity;
import com.android.chatappwithfirebase.Common.Common;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;
import java.util.Random;

public class MyFCMService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        //todo 3 receive notification (next chat fragment)
        Intent intent = new Intent(this, ChatActivity.class);
        Map<String,String> dataRecv = remoteMessage.getData();
        if (dataRecv!=null){
            Common.showNotification(
                    this,
                    new Random().nextInt(),
                    dataRecv.get(Common.NOTI_TITLE),
                    dataRecv.get(Common.NOTI_CONTENT),
                    dataRecv.get(Common.NOTI_SENDER),
                    dataRecv.get(Common.NOTI_ROOM_ID),
                    null);
        }
    }
}