package io.rick.socialmedia.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.Locale;

import io.rick.socialmedia.R;

public class SettActivity extends AppCompatActivity {

    ActionBar actionBar;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference usersDbRef;
    LinearLayout profileLayout;
    LinearLayout messageLayout;
    LinearLayout postLayout;
    LinearLayout logoutLayout;
    LinearLayout aboutLayout;
    private String myImage;
    TextView userNameTV;
    RoundedImageView userImageProfile;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Settings");
        //enable back button
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        firebaseAuth = FirebaseAuth.getInstance();
        profileLayout = findViewById(R.id.profileLayout);
        messageLayout = findViewById(R.id.messageLayout);
        postLayout = findViewById(R.id.postLayout);
        logoutLayout = findViewById(R.id.logoutLayout);
        aboutLayout = findViewById(R.id.AboutLayout);
        userNameTV = findViewById(R.id.userNameTV);
        userImageProfile = findViewById(R.id.userImageProfile);

        firebaseDatabase = FirebaseDatabase.getInstance();
        usersDbRef = firebaseDatabase.getReference("Users");
        //search user to get that user's info
        Query userQuery = usersDbRef.orderByChild("uid").equalTo("" + firebaseAuth.getUid());
        //get user picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check until required inf is received
                for (DataSnapshot ds : snapshot.getChildren()){
                    //get data
                    String name = "" + ds.child("name").getValue();
                    myImage = "" + ds.child("image").getValue();
                    String typingStatus = "" + ds.child("typingTo").getValue();
                    //set values
                    userNameTV.setText(name);
                    try {
                        Picasso.get().load(myImage).placeholder(R.drawable.ic_user_default).into(userImageProfile);
                    } catch (Exception e){
                        Picasso.get().load(R.drawable.ic_user_default).into(userImageProfile);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        setListeners();
    }

    private void setListeners() {
        profileLayout.setOnClickListener(v -> {
            Intent profileIntent = new Intent(SettActivity.this, DashboardActivity.class);
            profileIntent.putExtra("layout", "profile");
            startActivity(profileIntent);
        });
        messageLayout.setOnClickListener(v -> {
            Intent profileIntent = new Intent(SettActivity.this, DashboardActivity.class);
            profileIntent.putExtra("layout", "chatList");
            startActivity(profileIntent);
        });
        postLayout.setOnClickListener(v -> {
            Intent profileIntent = new Intent(SettActivity.this, DashboardActivity.class);
            profileIntent.putExtra("layout", "home");
            startActivity(profileIntent);
        });
        logoutLayout.setOnClickListener(v -> {
            firebaseAuth.signOut();
            checkUserStatus();
        });
        aboutLayout.setOnClickListener(v -> {

        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();//go to previous activity
        return super.onSupportNavigateUp();
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            //user is signed in stay here // set email of logged user
            //mProfileTv.setText(user.getEmail());
        } else {
            //user is not signed go to signUp activity
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }

}