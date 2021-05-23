package com.android.chatappwithfirebase.ViewHolder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.android.chatappwithfirebase.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

//todo 7 send pictures (next chat activity)
public class ChatPictureReceiveHolder extends RecyclerView.ViewHolder {

    private Unbinder unbinder;
    @BindView(R.id.txt_time)
    public TextView txt_time;
    @BindView(R.id.txt_chat_message)
    public TextView txt_chat_message;
    @BindView(R.id.img_preview)
    public ImageView img_preview;

    public ChatPictureReceiveHolder(@NonNull View itemView) {
        super(itemView);
        unbinder = ButterKnife.bind(this,itemView);
    }
}
