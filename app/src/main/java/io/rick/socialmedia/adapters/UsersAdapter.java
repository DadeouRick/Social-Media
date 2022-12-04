package io.rick.socialmedia.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import io.rick.socialmedia.R;
import io.rick.socialmedia.activities.ChatActivity;
import io.rick.socialmedia.activities.ThereProfileActivity;
import io.rick.socialmedia.models.ModelUser;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.MyHolder>{

    Context context;
    List<ModelUser> usersList;
    FirebaseAuth firebaseAuth;
    String myUid;


    public UsersAdapter(Context context, List<ModelUser> usersList) {
        this.context = context;
        this.usersList = usersList;
        firebaseAuth = FirebaseAuth.getInstance();
        myUid = firebaseAuth.getUid();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout user_row.xml
        View view = LayoutInflater.from(context).inflate(R.layout.users_row, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        //get data
        String hisUID = usersList.get(position).getUid();
        String userImage = usersList.get(position).getImage();
        String userName = usersList.get(position).getName();
        final String userEmail = usersList.get(position).getEmail();
        //set data
        holder.mNameTV.setText(userName);
        holder.mEmailTV.setText(userEmail);
        try {
            Picasso.get().load(userImage)
                    .placeholder(R.drawable.ic_face_24)
                    .into(holder.mAvatar);
        } catch (Exception e){
            Picasso.get().load(R.drawable.ic_face_24)
                    .into(holder.mAvatar);
        }

        //holder.blockIv.setImageResource(R.drawable.ic_unblocked);
        holder.blockIv.setVisibility(View.GONE);
        //check if each user is blocked or not
        //checkIsBlocked(hisUID, holder, position);

        //handle item click
        holder.itemView.setOnClickListener(view -> {
            //show dialog
            //MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            //builder.setItems(new String[]{"Profile", "Chat"}, (dialogInterface, i) -> {
            //    if (i == 0){
                    //profile clicked
            //        Intent intent = new Intent(context, ThereProfileActivity.class);
            //        intent.putExtra("uid", hisUID);
            //        context.startActivity(intent);
            //    } else if (i == 1){
                    //chat clicked
                    //isBlockedOrNot(hisUID);
                    Intent chatIntent = new Intent(context, ChatActivity.class);
                    chatIntent.putExtra("hisUid", hisUID);
                    context.startActivity(chatIntent);
            //    }
            //});
            //builder.create().show();
        });
        holder.blockIv.setOnClickListener(v -> {
            if (usersList.get(position).isBlocked()){
               // unBlockUser(hisUID);
            } else {
               // blockUser(hisUID);
            }
        });
    }

    private void isBlockedOrNot(String hisUID){
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(myUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            if (ds.exists()){
                                showToast("You are blocked by that user can't send send message");
                                //blocked, don't proceed further;
                            } else {
                                //not blocked, start activity
                                Intent chatIntent = new Intent(context, ChatActivity.class);
                                chatIntent.putExtra("hisUid", hisUID);
                                context.startActivity(chatIntent);
                            }
                        }

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void checkIsBlocked(String hisUID, MyHolder holder, int position) {
        //check if each user is blocked or not
        //if user's uid exists in "BlockedUsers" then that the user is blocked
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            if (ds.exists()){
                                holder.blockIv.setImageResource(R.drawable.ic_block);
                                usersList.get(position).setBlocked(true);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    private void blockUser(String hisUID) {
        //block user by adding uid to current user's node
        //
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("uid", hisUID);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").child(hisUID).setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    //blocked successfully
                    showToast("Blocked");
                })
                .addOnFailureListener(e -> {
                    //failed to block
                    showToast("Failed: " + e.getMessage());
                });
    }

    private void unBlockUser(String hisUID) {
        //unblock user by removing uid to current user's node
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(myUid).child("BlockedUsers").orderByChild("uid").equalTo(hisUID)
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
                                        })
                                        .addOnFailureListener(e -> {
                                            //failed to unblock
                                            showToast("Failed : " + e.getMessage());
                                        });
                            } else {

                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    private void showToast(String message){
        Toast.makeText(context.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        ImageView mAvatar;
        ImageView blockIv;
        TextView mNameTV;
        TextView mEmailTV;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            mAvatar = itemView.findViewById(R.id.userImageProfile);
            blockIv = itemView.findViewById(R.id.blockIv);
            mNameTV = itemView.findViewById(R.id.userNameTV);
            mEmailTV = itemView.findViewById(R.id.userEmailTV);
        }
    }

}
