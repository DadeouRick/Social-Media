package io.rick.socialmedia.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

import io.rick.socialmedia.R;
import io.rick.socialmedia.fragments.ChatListFragment;
import io.rick.socialmedia.fragments.HomeFragment;
import io.rick.socialmedia.fragments.NotificationsFragment;
import io.rick.socialmedia.fragments.ProfileFragment;
import io.rick.socialmedia.fragments.UsersFragment;
import io.rick.socialmedia.notifications.Token;

public class DashboardActivity extends AppCompatActivity {

    //firebase auth
    FirebaseAuth firebaseAuth;
    ActionBar actionBar;
    BottomNavigationView navigationView;
    String mUID;
    String layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Home");
        //init
        firebaseAuth = FirebaseAuth.getInstance();
        //Bottom navigation view
        navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        Intent intent = getIntent();
        layout = "" + intent.getStringExtra("layout");

        switch (layout) {
            case "home":
                HomeFragment parentFragment = new HomeFragment();
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.Content, parentFragment, "");
                fragmentTransaction.commit();
                checkUserStatus();
                break;
            case "profile":
                ProfileFragment profileFragment = new ProfileFragment();
                FragmentTransaction fragmentTransaction1 = getSupportFragmentManager().beginTransaction();
                fragmentTransaction1.replace(R.id.Content, profileFragment);
                fragmentTransaction1.commit();
                break;
            case "chatList":
                actionBar.setTitle("ChatList");
                ChatListFragment chatListFragment = new ChatListFragment();
                FragmentTransaction fragmentTransaction3 = getSupportFragmentManager().beginTransaction();
                fragmentTransaction3.replace(R.id.Content, chatListFragment, "");
                fragmentTransaction3.commit();
                break;

        }


    }

    @Override
    protected void onResume() {
        checkUserStatus();
        super.onResume();
    }

    public void updateToken(String token){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Tokens");
        Token mToken = new Token(token);
        ref.child(mUID).setValue(mToken);
    }

    private final BottomNavigationView.OnNavigationItemSelectedListener selectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @SuppressLint("NonConstantResourceId")
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()){
                        case R.id.nav_home:
                            //Home fragment
                            actionBar.setTitle("Home");
                            HomeFragment homeFragment = new HomeFragment();
                            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                            fragmentTransaction.replace(R.id.Content, homeFragment, "");
                            fragmentTransaction.commit();
                            return true;
                        case R.id.nav_users:
                            //Users fragment
                            actionBar.setTitle("Users");
                            UsersFragment usersFragment = new UsersFragment();
                            FragmentTransaction fragmentTransaction2 = getSupportFragmentManager().beginTransaction();
                            fragmentTransaction2.replace(R.id.Content, usersFragment, "");
                            fragmentTransaction2.commit();
                            return true;
                        case R.id.nav_notifications:
                            //AddBlogs fragment
                            actionBar.setTitle("Notifications");
                            NotificationsFragment notificationsFragment = new NotificationsFragment();
                            FragmentTransaction fragmentTransaction4 = getSupportFragmentManager().beginTransaction();
                            fragmentTransaction4.replace(R.id.Content, notificationsFragment, "");
                            fragmentTransaction4.commit();
                            return true;
                        case R.id.nav_chat:
                            //Chat fragment
                            actionBar.setTitle("ChatList");
                            ChatListFragment chatListFragment = new ChatListFragment();
                            FragmentTransaction fragmentTransaction3 = getSupportFragmentManager().beginTransaction();
                            fragmentTransaction3.replace(R.id.Content, chatListFragment, "");
                            fragmentTransaction3.commit();
                            return true;
                        case R.id.nav_profile:
                            //Profile fragment
                            actionBar.setTitle("Profile");
                            ProfileFragment profileFragment = new ProfileFragment();
                            FragmentTransaction fragmentTransaction1 = getSupportFragmentManager().beginTransaction();
                            fragmentTransaction1.replace(R.id.Content, profileFragment);
                            fragmentTransaction1.commit();
                            return true;

                    }

                    return false;
                }
            };

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            //user is signed in stay here // set email of logged user
            //mProfileTv.setText(user.getEmail());
            mUID = user.getUid();
            //save uid of current signed in user in shared preferences
            SharedPreferences sp = getSharedPreferences("SP_USER", MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            editor.putString("Current_USERID", mUID);
            editor.apply();
            //update token
            updateToken(String.valueOf(FirebaseMessaging.getInstance().getToken()));
        } else {
            //user is not signed go to signUp activity
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    protected void onStart() {
        //check on start of app
        checkUserStatus();
        super.onStart();
    }

}