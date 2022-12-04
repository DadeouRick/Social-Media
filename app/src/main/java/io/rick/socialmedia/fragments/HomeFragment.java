package io.rick.socialmedia.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import io.rick.socialmedia.R;
import io.rick.socialmedia.activities.AddPostImageActivity;
import io.rick.socialmedia.activities.AddPostTextActivity;
import io.rick.socialmedia.activities.AddPostVideoActivity;
import io.rick.socialmedia.activities.MainActivity;
import io.rick.socialmedia.activities.SettActivity;
import io.rick.socialmedia.adapters.PostsAdapter;
import io.rick.socialmedia.models.ModelPost;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    RecyclerView recyclerView;
    FloatingActionButton fab;
    List<ModelPost> postList;
    PostsAdapter postsAdapter;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        //init
        firebaseAuth = FirebaseAuth.getInstance();
        recyclerView = view.findViewById(R.id.postsRecyclerView);
        //init user list
        postList = new ArrayList<>();
        //get all users
        loadPosts();
        return view;
    }

    private void loadPosts() {
        //linear layout manager for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to recyclerview
        recyclerView.setLayoutManager(layoutManager);
        //path of all posts
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //get all data from this ref
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost modelPost = ds.getValue(ModelPost.class);
                    postList.add(modelPost);
                    //adapter
                    postsAdapter = new PostsAdapter(getActivity(), postList);
                    //set adapter to recyclerview
                    recyclerView.setAdapter(postsAdapter);
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //in case of error
                showToast("" + error.getMessage());
            }
        });
    }

    private void searchPosts(String searchQuery){
        //path of all posts
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //get all data from this ref
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost modelPost = ds.getValue(ModelPost.class);
                    if (modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            modelPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())){
                        postList.add(modelPost);
                    }
                    //adapter
                    postsAdapter = new PostsAdapter(getActivity(), postList);
                    //set adapter to recyclerview
                    recyclerView.setAdapter(postsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //in case of error
                showToast("" + error.getMessage());
            }
        });
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            //user is signed in stay here // set email of logged user
        } else {
            //user is not signed go to signUp activity
            startActivity(new Intent(getActivity(), MainActivity.class));
            getActivity().finish();
        }
    }

    private void showToast(String message){
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);//to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    //inflate option menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.action_search);
        MenuItem item1 = menu.findItem(R.id.action_addPost);
        MenuItem layout = menu.findItem(R.id.action_layout);
        MenuItem menuItem = menu.findItem(R.id.action_menu);
        layout.setVisible(false);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        //set search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //
                if (!TextUtils.isEmpty(query)){
                    searchPosts(query);
                } else {
                    loadPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)){
                    searchPosts(newText);
                } else {
                    loadPosts();
                }
                return false;
            }
        });
        item1.setOnMenuItemClickListener(item2 -> {
            showDialog();
            return false;
        });
        menuItem.setOnMenuItemClickListener(item2 -> {
            Intent intent = new Intent(getActivity(), SettActivity.class);
            startActivity(intent);
            return false;
        });

        super.onCreateOptionsMenu(menu, inflater);
    }

    private void showDialog() {
        //BottomSheetDialog dialog = new BottomSheetDialog(this);
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout postTextLayout = dialog.findViewById(R.id.layoutPostText);
        LinearLayout postImageLayout = dialog.findViewById(R.id.layoutPostImage);
        LinearLayout postVideoLayout = dialog.findViewById(R.id.layoutPostVideo);

        postTextLayout.setOnClickListener(view -> {
            //showToast("Text post is clicked !");
            startActivity(new Intent(getActivity(), AddPostTextActivity.class));
        });

        postImageLayout.setOnClickListener(view -> {
            //showToast("Image post is clicked !");
            startActivity(new Intent(getActivity(), AddPostImageActivity.class));
        });

        postVideoLayout.setOnClickListener(view -> {
            startActivity(new Intent(getActivity(), AddPostVideoActivity.class));
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

}