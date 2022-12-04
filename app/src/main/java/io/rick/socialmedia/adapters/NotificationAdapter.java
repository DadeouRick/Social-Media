package io.rick.socialmedia.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.format.DateFormat;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import io.rick.socialmedia.R;
import io.rick.socialmedia.activities.PostDetailsActivity;
import io.rick.socialmedia.models.ModelNotification;


public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.HolderNotification>{

    private final Context context;
    private final ArrayList<ModelNotification> notificationsList;
    private final FirebaseAuth firebaseAuth;

    public NotificationAdapter(Context context, ArrayList<ModelNotification> notificationsList) {
        this.context = context;
        this.notificationsList = notificationsList;
        firebaseAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public HolderNotification onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate view notification_row.xml
        View view = LayoutInflater.from(context).inflate(R.layout.notification_row, parent, false);

        return new HolderNotification(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderNotification holder, int position) {
        //get data
        ModelNotification model = notificationsList.get(position);
        String name = model.getsName();
        String notification = model.getNotification();
        String image = model.getsImage();
        String timestamp = model.getTimestamp();
        String senderUid = model.getsUid();
        String pId = model.getpId();
        //convert timestamp to proper time date
        String pattern = "dd/MM/yyyy hh:mm aa";
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(timestamp));
        String time = DateFormat.format(pattern, cal).toString();
        //
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Users");
        reference.orderByChild("uid").equalTo(senderUid)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            String name = "" + ds.child("name").getValue();
                            String image = "" + ds.child("image").getValue();
                            String email = "" + ds.child("email").getValue();

                            //add to model
                            model.setsName(name);
                            model.setsEmail(email);
                            model.setsImage(image);
                            //set to views
                            holder.nameTv.setText(name);
                            try {
                                Picasso.get().load(image).placeholder(R.drawable.ic_face_24).into(holder.avatarIv);
                            }catch (Exception e){
                                Picasso.get().load(R.drawable.ic_face_24).into(holder.avatarIv);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
        //set data

        holder.notificationTv.setText(notification);
        holder.timeTv.setText(time);
        holder.itemView.setOnClickListener(v -> {
            //start PostDetailsActivity
            Intent intent = new Intent(context, PostDetailsActivity.class);
            intent.putExtra("postId", pId);
            context.startActivity(intent);
        });
        //long press to show delete dialog
        holder.itemView.setOnLongClickListener(v -> {
            //Show confirmation dialog
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle("Delete");
            builder.setMessage("Do you want to delete this notification ?");
            builder.setPositiveButton("Yes", (dialog, which) -> {
                //delete notification
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
                ref.child(firebaseAuth.getUid()).child("Notifications").child("timestamp").removeValue()
                        .addOnSuccessListener(unused -> {
                            //delete

                            showToast("Notification Deleted...");
                        })
                        .addOnFailureListener(e -> {
                            //failed
                            showToast("" + e.getMessage());
                        });
            });
            builder.setNegativeButton("NO", (dialog, which) -> {

            });
            builder.create().show();
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return notificationsList.size();
    }

    private void showToast(String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    //holder class for views of notification_row.xml
    static class HolderNotification extends RecyclerView.ViewHolder{

        ImageView avatarIv;
        TextView nameTv;
        TextView notificationTv;
        TextView timeTv;

        public HolderNotification(@NonNull View itemView) {
            super(itemView);
            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            notificationTv = itemView.findViewById(R.id.notificationTv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }

}
