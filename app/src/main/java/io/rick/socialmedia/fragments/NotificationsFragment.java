package io.rick.socialmedia.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import io.rick.socialmedia.R;
import io.rick.socialmedia.adapters.NotificationAdapter;
import io.rick.socialmedia.models.ModelNotification;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class NotificationsFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    RecyclerView notificationRecyclerView;
    ArrayList<ModelNotification> notificationList;
    NotificationAdapter notificationAdapter;

    public NotificationsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);
        firebaseAuth = FirebaseAuth.getInstance();
        //recyclerview and it's properties
        notificationRecyclerView = view.findViewById(R.id.notificationRecyclerView);
        //init post list
        notificationList = new ArrayList<>();
        getAllNotifications();
        setListener();
        return view;
    }

    private void showToast(String message){
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    private void getAllNotifications() {
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show post first
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to recyclerview
        notificationRecyclerView.setLayoutManager(layoutManager);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(firebaseAuth.getUid()).child("Notifications")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        notificationList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()){
                            //get data
                            ModelNotification model = ds.getValue(ModelNotification.class);
                            //add to list
                            notificationList.add(model);
                            //adapter
                            notificationAdapter = new NotificationAdapter(getActivity(), notificationList);
                            //set adapter to recyclerview
                            notificationRecyclerView.setAdapter(notificationAdapter);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void setListener() {

    }

}