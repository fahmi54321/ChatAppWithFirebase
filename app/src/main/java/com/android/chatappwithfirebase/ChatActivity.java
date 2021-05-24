package com.android.chatappwithfirebase;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.android.chatappwithfirebase.Common.Common;
import com.android.chatappwithfirebase.Listener.IFirebaseLoadFailed;
import com.android.chatappwithfirebase.Listener.ILoadTimeFromFirebaseListener;
import com.android.chatappwithfirebase.Model.ChatInfoModel;
import com.android.chatappwithfirebase.Model.ChatMessageModel;
import com.android.chatappwithfirebase.Model.FCMSendData;
import com.android.chatappwithfirebase.Remote.IFCMService;
import com.android.chatappwithfirebase.Remote.RetrofitFCMClient;
import com.android.chatappwithfirebase.ViewHolder.ChatPictureHolder;
import com.android.chatappwithfirebase.ViewHolder.ChatPictureReceiveHolder;
import com.android.chatappwithfirebase.ViewHolder.ChatTextHolder;
import com.android.chatappwithfirebase.ViewHolder.ChatTextReceiveHolder;
import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class ChatActivity extends AppCompatActivity implements ILoadTimeFromFirebaseListener, IFirebaseLoadFailed {

    //    todo 3 (create conversation)
    private static final int MY_CAMERA_REQUEST_CODE = 7171;
    private static final int MY_REQUEST_LOAD_IMAGE = 7172;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.img_camera)
    ImageView img_camera;
    @BindView(R.id.img_image)
    ImageView img_image;
    @BindView(R.id.edt_chat)
    AppCompatEditText edt_chat;
    @BindView(R.id.img_send)
    ImageView img_send;
    @BindView(R.id.rv_chat)
    RecyclerView rv_chat;
    @BindView(R.id.img_preview)
    ImageView img_preview;
    @BindView(R.id.img_avatar)
    ImageView img_avatar;
    @BindView(R.id.txt_name)
    TextView txt_name;

    FirebaseDatabase database;
    DatabaseReference chatRef, offsetRef;
    ILoadTimeFromFirebaseListener listener;
    IFirebaseLoadFailed errorListener;
    FirebaseRecyclerAdapter<ChatMessageModel, RecyclerView.ViewHolder> adapter;
    FirebaseRecyclerOptions<ChatMessageModel> options;
    Uri fileUri;
    LinearLayoutManager layoutManager;

    IFCMService ifcmService;
    CompositeDisposable compositeDisposable = new CompositeDisposable();

    //todo 2 send pictures
    StorageReference storageReference;
    @OnClick(R.id.img_image)
    void onSelectImageClick(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/");
        startActivityForResult(intent,MY_REQUEST_LOAD_IMAGE);
    }
    @OnClick(R.id.img_camera)
    void onCaptureImageClick(){
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE,"New Picture");
        values.put(MediaStore.Images.Media.DESCRIPTION,"From your camera");
        fileUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values
        );
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,fileUri);
        startActivityForResult(intent,MY_CAMERA_REQUEST_CODE);
    }

    //todo 5(create conversation)
    @Override
    protected void onStart() {
        super.onStart();
        if (adapter!=null){
            adapter.startListening();
        }
    }
    @Override
    protected void onStop() {
        if (adapter!=null){
            adapter.stopListening();
        }
        super.onStop();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (adapter!=null){
            adapter.startListening();
        }
    }
    @OnClick(R.id.img_send)
    void onSubmitChatClick(){
        offsetRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long offset = snapshot.getValue(Long.class);
                long estimatedServerTimeInMs = System.currentTimeMillis() + offset;
                listener.onLoadOnlyTimeSuccess(estimatedServerTimeInMs);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                errorListener.onError(error.getMessage());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //todo 4(create conversation)
        initViews();
        loadChatContent();
    }

    private void loadChatContent() {
        String receiverId = FirebaseAuth
                .getInstance()
                .getCurrentUser()
                .getUid();
        adapter = new FirebaseRecyclerAdapter<ChatMessageModel, RecyclerView.ViewHolder>(options) {

            @Override
            public int getItemViewType(int position) {
                if (adapter.getItem(position).getSenderId().equals(receiverId)){ //  if message is own
                    return !adapter.getItem(position).isPicture()?0:1;
                }else{
                    return !adapter.getItem(position).isPicture()?2:3;
                }
            }

            @Override
            protected void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull ChatMessageModel model) {
                if (holder instanceof ChatTextHolder){
                    ChatTextHolder chatTextHolder = (ChatTextHolder) holder;
                    chatTextHolder.txt_chat_message.setText(model.getContent());
                    chatTextHolder.txt_time.setText(DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                            Calendar.getInstance().getTimeInMillis(),0).toString());
                }else if (holder instanceof ChatTextReceiveHolder){
                    ChatTextReceiveHolder chatTextReceiveHolder = (ChatTextReceiveHolder) holder;
                    chatTextReceiveHolder.txt_chat_message.setText(model.getContent());
                    chatTextReceiveHolder.txt_time.setText(DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                            Calendar.getInstance().getTimeInMillis(),0).toString());
                }//todo 8 send pictures ()
                else if (holder instanceof ChatPictureHolder){
                    ChatPictureHolder chatPictureHolder = (ChatPictureHolder) holder;
                    chatPictureHolder.txt_chat_message.setText(model.getContent());
                    chatPictureHolder.txt_time.setText(DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                            Calendar.getInstance().getTimeInMillis(),0).toString());
                    Glide.with(ChatActivity.this)
                            .load(model.getPictureLink())
                            .into(chatPictureHolder.img_preview);
                }
                else if (holder instanceof ChatPictureReceiveHolder){
                    ChatPictureReceiveHolder chatPictureReceiveHolder = (ChatPictureReceiveHolder) holder;
                    chatPictureReceiveHolder.txt_chat_message.setText(model.getContent());
                    chatPictureReceiveHolder.txt_time.setText(DateUtils.getRelativeTimeSpanString(model.getTimeStamp(),
                            Calendar.getInstance().getTimeInMillis(),0).toString());
                    Glide.with(ChatActivity.this)
                            .load(model.getPictureLink())
                            .into(chatPictureReceiveHolder.img_preview);
                }
            }

            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view;
                if (viewType == 0) { // text message of user "own message"
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_message_text_own, parent, false);
                    return new ChatTextReceiveHolder(view);

                    //todo 7 send pictures (next chat activity)
                }else if(viewType==1){ // picture message of user
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_message_picture_friend, parent, false);
                    return new ChatPictureReceiveHolder(view);
                }
                else if(viewType==2){ // text message of friend
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_message_text_friend, parent, false);
                    return new ChatTextHolder(view);
                }else{ // picture message of friend
                    view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.layout_message_picture_own, parent, false);
                    return new ChatPictureHolder(view);
                }
            }
        };

        //auto scroll when receive new message
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeChanged(int positionStart, int itemCount) {
                super.onItemRangeChanged(positionStart, itemCount);

                int friendlyMessageCount = adapter.getItemCount();
                int lastVisiblePositions = layoutManager.findLastVisibleItemPosition();
                if (lastVisiblePositions == -1 ||
                        (positionStart >= (friendlyMessageCount - 1)
                                && lastVisiblePositions == (positionStart - 1))) {

                    rv_chat.scrollToPosition(positionStart);
                }
            }
        });

        rv_chat.setAdapter(adapter);

    }

    @Override
    protected void onDestroy() {
        //todo 8 send notification (finish)
        compositeDisposable.clear();
        Common.roomSelected = "";
        super.onDestroy();
    }

    private void initViews() {

        //todo 7 send notification
        ifcmService = RetrofitFCMClient.getInstance().create(IFCMService.class);

        listener = this;
        errorListener = this;
        database = FirebaseDatabase.getInstance();
        chatRef = database.getReference(Common.CHAT_REFERENCE);
        offsetRef = database.getReference(".info/serverTimeOffset");
        Query query = chatRef.child(Common.generateChatRoomId(
                Common.chatuser.getUid(),
                FirebaseAuth.getInstance().getCurrentUser().getUid()
        )).child(Common.CHAT_DETAIL_REFERENCE);

        options = new FirebaseRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(query, ChatMessageModel.class)
                .build();
        ButterKnife.bind(this);
        layoutManager = new LinearLayoutManager(this);
        rv_chat.setLayoutManager(layoutManager);

        ColorGenerator generator = ColorGenerator.MATERIAL;
        int color = generator.getColor(Common.chatuser.getUid());
        TextDrawable.IBuilder builder = TextDrawable.builder()
                .beginConfig()
                .withBorder(4)
                .endConfig()
                .round();

        TextDrawable drawable = builder.build(Common.chatuser.getFirstName().substring(0, 1), color);
        img_avatar.setImageDrawable(drawable);
        txt_name.setText(Common.getName(Common.chatuser));

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> {
            finish();
        });
    }

    //todo 5(create conversation)
    @Override
    public void onLoadOnlyTimeSuccess(long estimmateTimeInMs) {
        ChatMessageModel chatMessageModel = new ChatMessageModel();
        chatMessageModel.setName(Common.getName(Common.currentUser));
        chatMessageModel.setContent(edt_chat.getText().toString());
        chatMessageModel.setTimeStamp(estimmateTimeInMs);
        chatMessageModel.setSenderId(FirebaseAuth.getInstance().getCurrentUser().getUid());

        //todo 4 send pictures
        if (fileUri==null) {
            chatMessageModel.setPicture(false);
            submitChatToFirebase(chatMessageModel, chatMessageModel.isPicture(), estimmateTimeInMs);
        }else{
            uploadPicture(fileUri,chatMessageModel,estimmateTimeInMs);
        }
    }

    private void uploadPicture(Uri fileUri, ChatMessageModel chatMessageModel, long estimmateTimeInMs) {
        AlertDialog dialog = new AlertDialog.Builder(ChatActivity.this)
                .setCancelable(false)
                .setMessage("Please wait...")
                .create();
        dialog.show();
        String fileName = Common.getFileName(getContentResolver(),fileUri);
        String path = new StringBuilder(Common.chatuser.getUid())
                .append("/")
                .append(fileName)
                .toString();
        storageReference = FirebaseStorage.getInstance()
                .getReference()
                .child(path);

        UploadTask uploadTask = storageReference.putFile(fileUri);

        //Create Task
        Task<Uri> task = uploadTask.continueWithTask(task1 -> {
            if (!task1.isSuccessful()){
                Toast.makeText(this, "Failed to upload", Toast.LENGTH_SHORT).show();
            }
            return storageReference.getDownloadUrl();
        }).addOnCompleteListener(task1 -> {
            if (task1.isSuccessful()){
                String url = task1.getResult().toString();
                dialog.dismiss();

                chatMessageModel.setPicture(true);
                chatMessageModel.setPictureLink(url);

                submitChatToFirebase(chatMessageModel,chatMessageModel.isPicture(),estimmateTimeInMs);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void submitChatToFirebase(ChatMessageModel chatMessageModel, boolean picture, long estimmateTimeInMs) {
        chatRef.child(Common.generateChatRoomId(Common.chatuser.getUid(),
                FirebaseAuth.getInstance().getCurrentUser().getUid()))
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            appendChat(chatMessageModel, picture, estimmateTimeInMs);
                        } else {
                            createChat(chatMessageModel, picture, estimmateTimeInMs);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(ChatActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void appendChat(ChatMessageModel chatMessageModel, boolean isPicture, long estimmateTimeInMs) {
        Map<String, Object> update_data = new HashMap<>();
        update_data.put("lastIpdate", estimmateTimeInMs);

        //todo 5 send pictures
        if (isPicture){
            update_data.put("lastMessage","<Image>");
        }else {
            update_data.put("lastMessage", chatMessageModel.getContent());
        }

        //update
        //update on user list
        FirebaseDatabase.getInstance()
                .getReference(Common.CHAT_LIST_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(Common.chatuser.getUid())
                .updateChildren(update_data)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(aVoid -> {
                    //Submit success for ChatInfo
                    //Copy to Friend Chat List
                    FirebaseDatabase.getInstance()
                            .getReference(Common.CHAT_LIST_REFERENCE)
                            .child(Common.chatuser.getUid())
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .updateChildren(update_data)
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnSuccessListener(aVoid1 -> {
                                // add on chat ref
                                String roomId = Common.generateChatRoomId(Common.chatuser.getUid(),
                                        FirebaseAuth.getInstance().getCurrentUser().getUid());
                                chatRef.child(roomId)
                                        .child(Common.CHAT_DETAIL_REFERENCE)
                                        .push()
                                        .setValue(chatMessageModel)
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                //clear
                                                edt_chat.setText("");
                                                edt_chat.requestFocus();
                                                if (adapter != null) {
                                                    adapter.notifyDataSetChanged();
                                                }

                                                //todo 10 send pictures ()
                                                // clear preview thumbnail
                                                if (isPicture){
                                                    fileUri = null;
                                                    img_preview.setVisibility(View.GONE);
                                                }

                                                //todo 5 send notification
                                                sendNotificationToFriend(chatMessageModel,roomId);
                                            }
                                        });
                            });
                });
    }

    private void sendNotificationToFriend(ChatMessageModel chatMessageModel, String roomId) {
        Map<String,String> notifData = new HashMap<>();
        notifData.put(Common.NOTI_TITLE,"Message from"+chatMessageModel.getName());
        notifData.put(Common.NOTI_CONTENT,chatMessageModel.getContent());
        notifData.put(Common.NOTI_SENDER,FirebaseAuth.getInstance().getCurrentUser().getUid());
        notifData.put(Common.NOTI_ROOM_ID,roomId);

        FCMSendData sendData = new FCMSendData("/topics/"+roomId,notifData);

        compositeDisposable.add(
                ifcmService.sendNotification(sendData)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(fcmResponse -> {

                },throwable->{
                    Toast.makeText(this, ""+throwable.getMessage(), Toast.LENGTH_SHORT).show();
                })
        );
    }

    private void createChat(ChatMessageModel chatMessageModel, boolean isPicture, long estimmateTimeInMs) {
        ChatInfoModel chatInfoModel = new ChatInfoModel();
        chatInfoModel.setCreatedId(FirebaseAuth.getInstance().getCurrentUser().getUid());
        chatInfoModel.setFriendName(Common.getName(Common.chatuser));
        chatInfoModel.setFriendId(Common.chatuser.getUid());
        chatInfoModel.setCreateName(Common.getName(Common.currentUser));

        //todo 6 send pictures (next layout message picture friend && layout message picture own)
        if (isPicture){
            chatInfoModel.setLastMessage("<Image>");
        }else {
            chatInfoModel.setLastMessage(chatMessageModel.getContent());
        }

        chatInfoModel.setLastUpdate(estimmateTimeInMs);
        chatInfoModel.setCreateDate(estimmateTimeInMs);

        //submit on firebase
        // add on user chat list
        FirebaseDatabase.getInstance()
                .getReference(Common.CHAT_LIST_REFERENCE)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child(Common.chatuser.getUid())
                .setValue(chatInfoModel)
                .addOnFailureListener(e -> {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnSuccessListener(aVoid -> {
                    //Submit success for ChatInfo
                    //Copy to Friend Chat List
                    FirebaseDatabase.getInstance()
                            .getReference(Common.CHAT_LIST_REFERENCE)
                            .child(Common.chatuser.getUid())
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .setValue(chatInfoModel)
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            })
                            .addOnSuccessListener(aVoid1 -> {
                                // add on chat ref
                                String roomId = Common.generateChatRoomId(Common.chatuser.getUid(),
                                        FirebaseAuth.getInstance().getCurrentUser().getUid());
                                chatRef.child(roomId)
                                        .child(Common.CHAT_DETAIL_REFERENCE)
                                        .push()
                                        .setValue(chatMessageModel)
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                //clear
                                                edt_chat.setText("");
                                                edt_chat.requestFocus();
                                                if (adapter != null) {
                                                    adapter.notifyDataSetChanged();
                                                }

                                                //todo 9 send pictures ()
                                                // clear preview thumbnail
                                                if (isPicture){
                                                    fileUri = null;
                                                    img_preview.setVisibility(View.GONE);
                                                }

                                                // sennd notofication
                                                //todo 6 send notification
                                                sendNotificationToFriend(chatMessageModel,roomId);
                                            }
                                        });
                            });
                });
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    //todo 3 send pictures
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MY_CAMERA_REQUEST_CODE){
            if (resultCode == RESULT_OK){
                try {
                    Bitmap thumbnail = MediaStore.Images.Media.getBitmap(
                            getContentResolver(),
                            fileUri
                    );
                    img_preview.setImageBitmap(thumbnail);
                    img_preview.setVisibility(View.VISIBLE);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else if (requestCode == MY_REQUEST_LOAD_IMAGE){
            if (requestCode == RESULT_OK){
                try {
                    final Uri uri = data.getData();
                    InputStream inputStream = getContentResolver()
                            .openInputStream(uri);
                    Bitmap selectImage = BitmapFactory.decodeStream(inputStream);
                    img_preview.setImageBitmap(selectImage);
                    img_preview.setVisibility(View.VISIBLE);
                }catch (FileNotFoundException e){
                    e.printStackTrace();
                }
            }
        }else{
            Toast.makeText(this, "Please Choose Image", Toast.LENGTH_SHORT).show();
        }

    }
}