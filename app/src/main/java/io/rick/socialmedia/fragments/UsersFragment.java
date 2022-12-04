package io.rick.socialmedia.fragments;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

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
import io.rick.socialmedia.activities.MainActivity;
import io.rick.socialmedia.adapters.UsersAdapter;
import io.rick.socialmedia.models.ModelUser;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class UsersFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    RecyclerView recyclerView;
    UsersAdapter usersAdapter;
    List<ModelUser> usersList;

    public UsersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_users, container, false);
        //init
        firebaseAuth = FirebaseAuth.getInstance();
        //init recyclerview
        recyclerView = view.findViewById(R.id.usersRecyclerView);
        //set recyclerview properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        //init user list
        usersList = new ArrayList<>();
        //get all users
        getAllUsers();
        return view;
    }

    private void getAllUsers() {
        //get current user
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //get path of database named "Users" containing users info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        //get all data from path
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelUser modelUsers = ds.getValue(ModelUser.class);
                    //get all users except currently signed in user
                    if (!modelUsers.getUid().equals(fUser.getUid())){
                        usersList.add(modelUsers);
                    }
                    //adapter
                    usersAdapter = new UsersAdapter(getActivity(), usersList);
                    //set adapter to recycler view
                    recyclerView.setAdapter(usersAdapter);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void searchUsers(String query) {
        //get current user
        FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //get path of database named "Users" containing users info
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        //get all data from path
        ref.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelUser modelUsers = ds.getValue(ModelUser.class);
                    //get all users searched except currently signed in user
                    if (!modelUsers.getUid().equals(fUser.getUid())){
                        if (modelUsers.getName().toLowerCase().contains(query.toLowerCase()) ||
                                modelUsers.getEmail().toLowerCase().contains(query.toLowerCase())){
                            usersList.add(modelUsers);
                        }
                    }
                    //adapter
                    usersAdapter = new UsersAdapter(getActivity(), usersList);
                    //refresh adapter
                    usersAdapter.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(usersAdapter);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);//to show menu option in fragment
        super.onCreate(savedInstanceState);
    }

    //inflate option menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main, menu);
        //searchView
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //Called when user press search button from keyboard
                //if search query is not empty then search
                if (!TextUtils.isEmpty(s.trim())){
                    //search text contains text, search it
                    searchUsers(s);
                } else {
                    //search text empty, get all users
                    getAllUsers();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called when user press any single letter
                //if search query is not empty then search
                if (!TextUtils.isEmpty(s.trim())){
                    //search text contains text, search it
                    searchUsers(s);
                } else {
                    //search text empty, get all users
                    getAllUsers();
                }
                return false;
            }
        });
        //hide search view
        menu.findItem(R.id.action_addPost).setVisible(false);
        menu.findItem(R.id.action_layout).setVisible(false);
        menu.findItem(R.id.action_menu).setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }


    //handle menu item click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_menu){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }

}