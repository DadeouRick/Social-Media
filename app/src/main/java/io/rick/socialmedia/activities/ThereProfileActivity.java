package io.rick.socialmedia.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

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

import java.util.ArrayList;
import java.util.List;

import io.rick.socialmedia.R;
import io.rick.socialmedia.adapters.PostsAdapter;
import io.rick.socialmedia.models.ModelPost;

public class ThereProfileActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    //Views
    RoundedImageView imageProfileView;
    ImageView coverIV;
    TextView txtName;
    TextView txtEmail;
    RecyclerView postsRecyclerView;
    List<ModelPost> postList;
    PostsAdapter postsAdapter;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //init views
        imageProfileView = findViewById(R.id.profileImage);
        coverIV = findViewById(R.id.coverIV);
        txtName = findViewById(R.id.userName);
        txtEmail = findViewById(R.id.userEmail);

        postsRecyclerView = findViewById(R.id.recyclerView_posts);
        firebaseAuth = FirebaseAuth.getInstance();
        //get uid of clicked user to retrieve his posts
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");

        Query query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("uid").equalTo(uid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check until required data get
                for (DataSnapshot ds : snapshot.getChildren()){
                    //get data
                    String name = "" + ds.child("name").getValue();
                    String email = "" + ds.child("email").getValue();
                    String image = "" + ds.child("image").getValue();
                    String cover = "" + ds.child("cover").getValue();

                    //set data
                    txtName.setText(name);
                    txtEmail.setText(email);
                    txtName.setText(name);
                    try {
                        Picasso.get().load(image).into(imageProfileView);
                    } catch (Exception e){
                        Picasso.get().load(R.drawable.ic_face).into(imageProfileView);
                    }
                    try {
                        Picasso.get().load(cover).into(coverIV);
                    } catch (Exception e){
                        Picasso.get().load(R.drawable.ic_face).into(coverIV);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        postList = new ArrayList<>();
        checkUserStatus();
        loadHisPosts();
    }

    private void loadHisPosts() {
        //linear layout manager for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //show newest post first
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);
        //init post list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load posts
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPost = ds.getValue(ModelPost.class);
                    //add to list
                    postList.add(myPost);
                    //adapter
                    postsAdapter = new PostsAdapter(ThereProfileActivity.this, postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(postsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("" + error.getMessage());
            }
        });
    }

    private void searchHisPosts(final String searchQuery){
        //linear layout manager for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        //show newest post first
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);
        //init post list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load posts
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPost = ds.getValue(ModelPost.class);
                    if (myPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            myPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())){
                        //add to list
                        postList.add(myPost);
                    }

                    //adapter
                    postsAdapter = new PostsAdapter(ThereProfileActivity.this, postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(postsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("" + error.getMessage());
            }
        });
    }

    private void showToast(String message){
        Toast.makeText(ThereProfileActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            //user is signed in stay here // set email of logged user
        } else {
            //user is not signed go to signUp activity
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_menu).setVisible(false);
        menu.findItem(R.id.action_addPost).setVisible(false);
        menu.findItem(R.id.action_layout).setVisible(false);
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called when user press search button
                if (!TextUtils.isEmpty(s)){
                    //search
                    searchHisPosts(s);
                } else {
                    loadHisPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //ca
                if (!TextUtils.isEmpty(s)){
                    //search
                    searchHisPosts(s);
                } else {
                    loadHisPosts();
                }
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

}