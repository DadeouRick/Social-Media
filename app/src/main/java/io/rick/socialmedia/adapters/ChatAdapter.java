package io.rick.socialmedia.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.rick.socialmedia.R;
import io.rick.socialmedia.models.ModelChat;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MyHolder> {

    private static final int MSG_TYPE_LEFT = 0;
    private static final int MSG_TYPE_RIGHT = 1;
    Context context;
    List<ModelChat> chatList;
    String imageUri;
    String msgTimeStamp;
    FirebaseUser fUser;

    public ChatAdapter(Context context, List<ModelChat> chatList, String imageUri) {
        this.context = context;
        this.chatList = chatList;
        this.imageUri = imageUri;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layouts: chat_left_row.xml for receiver, chat_right_row.xml for sender
        View view;
        if (viewType == MSG_TYPE_RIGHT){
            view = LayoutInflater.from(context).inflate(R.layout.chat_right_row, parent, false);
        } else {
            view = LayoutInflater.from(context).inflate(R.layout.chat_left_row, parent, false);
        }
        return new MyHolder(view);
    }

    private String getTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm aa", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("Africa/Porto-Novo"));
        return sdf.format(new Date());
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        //get data
        String message = chatList.get(position).getMessage();
        String timeStamp = chatList.get(position).getTimeStamp();
        //convert time stamp to dd//mm//yy hh:mm am/pm
        String pattern = "dd/MM/yyyy hh:mm aa";
        //Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        //cal.setTimeInMillis(Long.parseLong(timeStamp));
        //String dateTime = DateFormat.format(pattern, cal).toString();
        //set data
        holder.messageTV.setText(message);
        holder.timeTV.setText(getTimestamp());
        try {
            Picasso.get().load(imageUri).into(holder.profileIV);
        } catch (Exception e){

        }
        //click to show delete dialog
        holder.messageLayout.setOnLongClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Delete");
            builder.setMessage("Do you want to delete this message ?");
            //delete button
            builder.setPositiveButton("YES", (dialog, which) -> {
                //deleteMessage(which);
                //TODO À REVOIR
            });
            //Cancel button
            builder.setNegativeButton("NO", (dialog, which) -> {
                //dismiss dialog
                dialog.dismiss();
            });
            //create and show dialog
            builder.create().show();
            return false;
        });
        //set seen/delivered status of message
        if (position == chatList.size() - 1){
            if (chatList.get(position).isSeen()){
                holder.isSeenTV.setText("Seen");
            } else {
                holder.isSeenTV.setText("Delivered");
            }
        }else {
            holder.isSeenTV.setVisibility(View.GONE);
        }

    }

    private void deleteMessage(int position) {
        String myUID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        msgTimeStamp = chatList.get(position).getTimeStamp();
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("Chats");
        Query query = dbRef.orderByChild("timestamp").equalTo(msgTimeStamp);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    if (ds.child("sender").getValue().equals(myUID)){
                        //1 remove the message from Chats
                        ds.getRef().removeValue();

                        //2 set the value of message "You remove a message"
                        /*HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("message", "Message removed");
                        ds.getRef().updateChildren(hashMap);*/
                        showToast("Message deleted...");
                    } else {
                        showToast("You can only delete your message...");
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
        return chatList.size();
    }

    private void showToast(String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemViewType(int position) {
        fUser = FirebaseAuth.getInstance().getCurrentUser();
        if (chatList.get(position).getSender().equals(fUser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        //views
        ImageView profileIV;
        TextView messageTV;
        TextView timeTV;
        TextView isSeenTV;
        LinearLayout messageLayout;//for click listener to show delete

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            //init views
            profileIV = itemView.findViewById(R.id.profileChatIV);
            messageTV = itemView.findViewById(R.id.messageTV);
            timeTV = itemView.findViewById(R.id.timeTV);
            isSeenTV = itemView.findViewById(R.id.isSeenTV);
            messageLayout = itemView.findViewById(R.id.messageLayout);
        }
    }

}
