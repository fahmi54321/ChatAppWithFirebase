package com.android.chatappwithfirebase.Common;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.android.chatappwithfirebase.Model.UserModel;
import com.firebase.ui.auth.data.model.User;

import java.util.Random;

public class Common {
    public static final String USER_REFERENCE = "People";
    public static final String CHAT_LIST_REFERENCE = "ChatList";
    public static final String CHAT_REFERENCE = "Chat";
    public static final String CHAT_DETAIL_REFERENCE = "Detail";
    public static UserModel currentUser = new UserModel();
    public static UserModel chatuser = new UserModel();

    public static String generateChatRoomId(String a, String b) {
        if (a.compareTo(b)>0){
            return new StringBuilder(a).append(b).toString();
        }else if (a.compareTo(b)<0){
            return new StringBuilder(b).append(a).toString();
        }else{
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
        if (fileUri.getScheme().equals("content")){
            Cursor cursor = contentResolver.query(fileUri,null,null,null);
            try {
                if (cursor !=null && cursor.moveToFirst()){
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }finally {
                cursor.close();
            }
        }

        if (result == null){
            result = fileUri.getPath();
            int cut = result.lastIndexOf("/");
            if (cut != -1){
                result = result.substring(cut+1);
            }
        }

        return result;
    }
}
