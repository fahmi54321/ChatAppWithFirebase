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

public class UserViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.img_avatar)
    public ImageView imgAvatar;
    @BindView(R.id.txt_name)
    public TextView txt_name;
    @BindView(R.id.txt_bio)
    public TextView txt_bio;

    private Unbinder unbinder;

    public UserViewHolder(@NonNull View itemView) {
        super(itemView);
        unbinder= ButterKnife.bind(this,itemView);
    }
}
