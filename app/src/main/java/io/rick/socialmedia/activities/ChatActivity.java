package io.rick.socialmedia.activities;

import static com.google.android.gms.common.internal.ImagesContract.URL;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.rick.socialmedia.R;
import io.rick.socialmedia.adapters.ChatAdapter;
import io.rick.socialmedia.models.ModelChat;
import io.rick.socialmedia.models.ModelUser;
import io.rick.socialmedia.notifications.Data;
import io.rick.socialmedia.notifications.Sender;
import io.rick.socialmedia.notifications.Token;

public class ChatActivity extends AppCompatActivity {

    //views
    Toolbar toolbar;
    RecyclerView recyclerView;
    RoundedImageView profileIV;
    ImageView onlineStatusIv;
    ImageView blockIv;
    ImageButton back;
    TextView nameTV;
    TextView userStatusTV;
    EditText messageET;
    ImageButton sendBtn;

    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDbRef;
    String hisUid;
    String myUid;
    String hisImage;
    boolean isBlocked = false;
    //for checking if user has seen message or not
    ValueEventListener seenListener;
    DatabaseReference userRefForSeen;
    //String timestamp = String.valueOf(System.currentTimeMillis());

    List<ModelChat> chatList;
    ChatAdapter chatAdapter;

    //volley request queue for notification
    private RequestQueue requestQueue;
    private boolean notify = false;
    String FCM_KEY = "AAAAo7P1cP0:APA91bEIr9h8fvmzpAx9_MJy4B7ph3Z6AWQncKwGNcH1K-NX52QTQj-ax7N-4pAyIaPGG8m5dFDa6HkqCEYE_-cTCQvChQEt74xndqO32jSw9yAhROvjzb6O_ibkJCr7jWssMulK6q92";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //init views
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        recyclerView = findViewById(R.id.chatRecyclerView);
        back = findViewById(R.id.back);
        profileIV = findViewById(R.id.profileImg);
        onlineStatusIv = findViewById(R.id.onlineStatusIv);
        blockIv = findViewById(R.id.blockIv);
        nameTV = findViewById(R.id.nameTV);
        userStatusTV = findViewById(R.id.userStatusTV);
        messageET = findViewById(R.id.inputMessage);
        sendBtn = findViewById(R.id.send);
        blockIv.setVisibility(View.GONE);

        requestQueue = Volley.newRequestQueue(getApplicationContext());

        //LinearLayout for recycler view
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //recyclerview properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);



        Intent intent = getIntent();
        hisUid = intent.getStringExtra("hisUid");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef = firebaseDatabase.getReference("Users");
        //search user to get that user's info
        Query userQuery = usersDbRef.orderByChild("uid").equalTo(hisUid);
        //get user picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check until required inf is received
                for (DataSnapshot ds : snapshot.getChildren()){
                    //get data
                    String name = "" + ds.child("name").getValue();
                    hisImage = "" + ds.child("image").getValue();
                    String typingStatus = "" + ds.child("typingTo").getValue();
                    //check typing status
                    if (typingStatus.equals(myUid)){

                        userStatusTV.setText("typing...");
                    } else {
                        //get value of online status
                        String onlineStatus = "" + ds.child("onlineStatus").getValue();
                        if (onlineStatus.equals("online")){
                            onlineStatusIv.setImageResource(R.drawable.circle_online);
                            userStatusTV.setText(onlineStatus);
                        } else {
                            onlineStatusIv.setImageResource(R.drawable.circle_offline);
                            //convert timestamp to proper time date
                            String pattern = "dd/MM/yyyy hh:mm aa";
                            Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime = DateFormat.format(pattern, cal).toString();
                            userStatusTV.setText("Last seen at " + dateTime);//TODO review the last date and time of users sign in
                        }
                    }

                    //set values
                    nameTV.setText(name);
                    try {
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_user_default).into(profileIV);
                    } catch (Exception e){
                        Picasso.get().load(R.drawable.ic_user_default).into(profileIV);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        setListeners();
        readMessage();
        checkIsBlocked();
        seenMessage();
    }

    private void checkIsBlocked() {
        //check if each user is blocked or not
        //if user's uid exists in "BlockedUsers" then that the user is blocked
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            if (ds.exists()){
                                blockIv.setImageResource(R.drawable.ic_block);
                                isBlocked = true;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void blockUser() {
        //block user by adding uid to current user's node
        //
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", hisUid);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUid).setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    //blocked successfully
                    showToast("Blocked");
                    blockIv.setImageResource(R.drawable.ic_block);
                })
                .addOnFailureListener(e -> {
                    //failed to block
                    showToast("Failed: " + e.getMessage());
                });
    }

    private void unBlockUser() {
        //unblock user by removing uid to current user's node
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            if (ds.exists()){
                                //remove blocked user data from current user's BlockedUsers list
                                ds.getRef().removeValue()
                                        .addOnSuccessListener(unused -> {
                                            //unblocked successfully
                                            showToast("unblocked successfully");
                                            blockIv.setImageResource(R.drawable.ic_unblocked);
                                        })
                                        .addOnFailureListener(e -> {
                                            //failed to unblock
                                            showToast("Failed : " + e.getMessage());
                                        });
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void setListeners() {
        sendBtn.setOnClickListener(view -> {
            notify = true;
            //get text from inputMessage
            String message = messageET.getText().toString().trim();
            //check if text is empty or not
            if (TextUtils.isEmpty(message)){
                //text empty
                showToast("Can not send the empty message");
            } else {
                //text not empty
                sendMessage(message);
            }
            //reset edittext after sending message
            messageET.setText("");
        });

        //Check edit text change listener
        messageET.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() == 0){
                    checkOnTypingStatus("noOne");
                } else {
                    checkOnTypingStatus(hisUid);//uid of receiver
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        blockIv.setOnClickListener(v -> {
            if (isBlocked){
                unBlockUser();
            } else {
                blockUser();
            }
        });

        back.setOnClickListener(v -> {
            onBackPressed();
        });

    }

    private void seenMessage() {
        userRefForSeen = FirebaseDatabase.getInstance().getReference("Chats");
        seenListener = userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid)){
                        HashMap<String, Object> hasSeenHashMap = new HashMap<>();
                        hasSeenHashMap.put("isSeen", true);
                        ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void readMessage() {
        chatList = new ArrayList<>();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid) ||
                            chat.getReceiver().equals(hisUid) && chat.getSender().equals(myUid)){
                        chatList.add(chat);
                    }
                    //adapter
                    chatAdapter = new ChatAdapter(ChatActivity.this, chatList, hisImage);
                    chatAdapter.notifyDataSetChanged();
                    //set adapter to recyclerview
                    recyclerView.setAdapter(chatAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    //new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    private void sendMessage(String message) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
        String timeStamp = String.valueOf(System.currentTimeMillis());
        //String dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault()).format(new Date());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("sender", myUid);
        hashMap.put("receiver", hisUid);
        hashMap.put("message", message);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("isSeen", false);
        databaseReference.child("Chats").push().setValue(hashMap);

        String msg = message;
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ModelUser user = snapshot.getValue(ModelUser.class);
                if (notify){
                    //sendNotification(hisUid, user.getName(), msg);
                    send(hisUid, user.getName(), msg);
                }
                notify = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //create chatList in firebase database
        DatabaseReference chatRef1 = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(myUid)
                .child(hisUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        DatabaseReference chatRef2 = FirebaseDatabase.getInstance().getReference("ChatList")
                .child(hisUid)
                .child(myUid);
        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()){
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void sendNotification(String hisUid, String name, String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid, name + " : " + message, "New Message", hisUid, R.drawable.ic_face_24);
                    Sender sender = new Sender(data, token.getToken());
                    //fcm json object request
                    try {
                        JSONObject senderJsonObject = new JSONObject(new Gson().toJson(sender));
                        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest("https://fcm.googleapis.com/fcm/send", senderJsonObject,
                                response -> {
                                    //response of the request
                                    Log.d("JSON_RESPONSE_SUCCESS", "onResponse: " + response.toString());
                                }, error -> {
                            Log.d("JSON_RESPONSE_FAILED", "onResponse: " + error.toString());
                        }){
                            @Override
                            public Map<String, String> getHeaders() throws AuthFailureError {
                                //put params
                                Map<String, String> headers = new HashMap<>();
                                headers.put("Content-Type", "application/json");
                                headers.put("Authorization", "key=" + FCM_KEY);
                                return headers;
                            }
                        };
                        //add  this to queue
                        requestQueue.add(jsonObjectRequest);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public void send(String hisUid, String title, String body) {
        Log.i("Check", "Sending: \n"+hisUid + "\n" + title + "\n" + body);
        final JSONObject notification = new JSONObject();
        JSONObject notificationBody = new JSONObject();
        JSONObject data = new JSONObject();

        try {
            Log.i("Check", "Enter TRY Block");
            data.put("key", FCM_KEY);
            notificationBody.put("title", title);
            notificationBody.put("body", body);

            notification.put("to", hisUid);
            notification.put("notification", notificationBody);
            notification.put("data", data);

        } catch (Exception e) {
            Log.i("Check", "Enter CATCH Block:" + e.getMessage());
            e.printStackTrace();
        }
        StringRequest req = new StringRequest(Request.Method.POST, "https://fcm.googleapis.com/fcm/send", response -> {
            Log.i("Check", "Response: " + response);
            Toast.makeText(ChatActivity.this, "Response: " + response, Toast.LENGTH_LONG).show();
        }, error -> {
            Log.i("Check", "Error: " + error);
            Toast.makeText(ChatActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
        }) {


            @Override
            public byte[] getBody() {
                try {
                    return notification.toString().getBytes(StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return null;
                }
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "key=" + FCM_KEY);
                params.put("Content-Type", "application/json");
                return params;
            }
        };
        requestQueue.add(req);
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            //user is signed in stay here // set email of logged user
            myUid = user.getUid();//currently signed user's uid
        } else {
            //user is not signed go to signUp activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void checkOnlineStatus(String status){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("onlineStatus", status);
        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }

    private void checkOnTypingStatus(String typing){
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("typingTo", typing);
        //update value of onlineStatus of current user
        dbRef.updateChildren(hashMap);
    }

    @Override
    protected void onStart() {
        checkUserStatus();
        //set online
        onlineStatusIv.setImageResource(R.drawable.circle_online);
        checkOnlineStatus("online");
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //get timeStamp
        String timeStamp = String.valueOf(System.currentTimeMillis());
        onlineStatusIv.setImageResource(R.drawable.circle_offline);
        //set offline with last seen time stamp
        checkOnlineStatus(timeStamp);
        checkOnTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        //set online
        onlineStatusIv.setImageResource(R.drawable.circle_online);
        checkOnlineStatus("online");
        //onlineStatusIv.setSr;//.get().load().into();
        super.onResume();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //hide search view
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_addPost).setVisible(false);
        menu.findItem(R.id.action_menu).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_layout){
            Intent intent = new Intent(ChatActivity.this, ThereProfileActivity.class);
            intent.putExtra("uid", hisUid);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

}