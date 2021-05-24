package com.android.chatappwithfirebase.Fragments;

import androidx.lifecycle.ViewModelProvider;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.android.chatappwithfirebase.ChatActivity;
import com.android.chatappwithfirebase.Common.Common;
import com.android.chatappwithfirebase.Model.ChatInfoModel;
import com.android.chatappwithfirebase.Model.UserModel;
import com.android.chatappwithfirebase.R;
import com.android.chatappwithfirebase.ViewHolder.ChatInfoHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;

import java.text.SimpleDateFormat;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

public class ChatFragment extends Fragment {

//    todo 2 (chat history)
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy");
    FirebaseRecyclerAdapter adapter;
    @BindView(R.id.rv_chat)
    RecyclerView recycler_chat;
    private Unbinder unbinder;


    private ChatViewModel mViewModel;

    static ChatFragment instance;
    public static ChatFragment getInstance(){
        return instance == null ? new ChatFragment():instance;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

//        todo 3 (chat history)
        View itemView =  inflater.inflate(R.layout.chat_fragment, container, false);
        initView(itemView);
        loadChatList();
        return itemView;
    }

    private void loadChatList() {
        Query query = FirebaseDatabase.getInstance()
                .getReference()
                .child(Common.CHAT_LIST_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        FirebaseRecyclerOptions<ChatInfoModel>options = new FirebaseRecyclerOptions
                .Builder<ChatInfoModel>()
                .setQuery(query,ChatInfoModel.class)
                .build();

        adapter = new FirebaseRecyclerAdapter<ChatInfoModel, ChatInfoHolder>(options) {

            @NonNull
            @Override
            public ChatInfoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View itemView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.layout_chat_item,parent,false);

                return new ChatInfoHolder(itemView);
            }

            @Override
            protected void onBindViewHolder(@NonNull ChatInfoHolder holder, int position, @NonNull ChatInfoModel model) {
                if (!adapter.getRef(position).getKey().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){
                    ColorGenerator generator = ColorGenerator.MATERIAL;
                    int color = generator.getColor(FirebaseAuth.getInstance().getCurrentUser().getUid());
                    TextDrawable.IBuilder builder = TextDrawable.builder()
                            .beginConfig()
                            .withBorder(4)
                            .endConfig()
                            .round();
                    String displayName = FirebaseAuth.getInstance().getCurrentUser().getUid()
                            .equals(model.getCreatedId()) ? model.getFriendName() : model.getCreateName();
                    TextDrawable drawable = builder.build(displayName.substring(0,1),color);
                    holder.img_avatar.setImageDrawable(drawable);

                    holder.txt_name.setText(displayName);
                    holder.txt_last_message.setText(model.getLastMessage());
                    holder.txt_time.setText(simpleDateFormat.format(model.getLastUpdate()));

                    //event
                    holder.itemView.setOnClickListener(view -> {
                        // go to chat detail
                        FirebaseDatabase.getInstance()
                                .getReference(Common.USER_REFERENCE)
                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid()
                                        .equals(model.getCreatedId()) ? model.getFriendId() : model.getCreatedId())
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()){
                                            UserModel userModel = snapshot.getValue(UserModel.class);
                                            Common.chatuser = userModel;
                                            Common.chatuser.setUid(snapshot.getKey());

                                            //todo 4 receive notification (next people fragment)
                                            String roomId = Common.generateChatRoomId(
                                                    FirebaseAuth.getInstance().getCurrentUser().getUid(),
                                                    Common.chatuser.getUid());
                                            Common.roomSelected = roomId;

                                            Log.d("ROOMID",roomId);

                                            //Register topic
                                            FirebaseMessaging.getInstance()
                                                    .subscribeToTopic(roomId)
                                                    .addOnSuccessListener(aVoid -> {
                                                        startActivity(new Intent(getContext(),ChatActivity.class));
                                                    });

                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });

                }else{
                    holder.itemView.setVisibility(View.GONE);
                    holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0,0));
                }
            }
        };
        adapter.startListening();
        recycler_chat.setAdapter(adapter);
    }

    private void initView(View itemView) {
        unbinder = ButterKnife.bind(this,itemView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recycler_chat.setLayoutManager(layoutManager);
        recycler_chat.addItemDecoration(new DividerItemDecoration(getContext(),layoutManager.getOrientation()));
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        // TODO: Use the ViewModel
    }

    //todo 4 (chat history) finish
    @Override
    public void onStart() {
        super.onStart();
        if (adapter!=null){
            adapter.startListening();
        }
    }
    @Override
    public void onStop() {
        if (adapter!=null){
            adapter.stopListening();
        }
        super.onStop();
    }

}