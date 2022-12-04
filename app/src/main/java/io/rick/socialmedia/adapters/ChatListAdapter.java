package io.rick.socialmedia.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;

import java.util.HashMap;
import java.util.List;

import io.rick.socialmedia.R;
import io.rick.socialmedia.activities.ChatActivity;
import io.rick.socialmedia.models.ModelUser;

public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.MyHolder> {

    private Context context;
    private List<ModelUser> userList;
    private HashMap<String, String> lastMessageMap;

    public ChatListAdapter(Context context, List<ModelUser> userList) {
        this.context = context;
        this.userList = userList;
        lastMessageMap = new HashMap<>();
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate la
        View view = LayoutInflater.from(context).inflate(R.layout.chatlist_row, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        //get data
        String hisUid = userList.get(position).getUid();
        String userImage = userList.get(position).getImage();
        String userName = userList.get(position).getName();
        String lastMessage = lastMessageMap.get(hisUid);

        //set data
        holder.nameTv.setText(userName);
        holder.lastMessageTv.setText(lastMessage);
        //if (lastMessage != null || lastMessage.equals("default")){
        //  holder.lastMessageTv.setVisibility(View.GONE);
        //} else {
        //    holder.lastMessageTv.setVisibility(View.VISIBLE);

        //}

        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_face_24).into(holder.profileIv);
        } catch (Exception e){
            Picasso.get().load(R.drawable.ic_face_24).into(holder.profileIv);
        }
        //set online status of other users in chatList
        if (userList.get(position).getOnlineStatus().equals("online")){
            //online
            holder.onlineStatusIv.setImageResource(R.drawable.circle_online);
        } else {
            //offline
            holder.onlineStatusIv.setImageResource(R.drawable.circle_offline);
        }
        //
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("hisUid", hisUid);
            context.startActivity(intent);
        });
    }

    public void setLastMessageMap(String userId, String lastMessage){
        lastMessageMap.put(userId, lastMessage);
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }


    class MyHolder extends RecyclerView.ViewHolder{

        //views
        RoundedImageView profileIv;
        ImageView onlineStatusIv;
        TextView nameTv;
        TextView lastMessageTv;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            profileIv = itemView.findViewById(R.id.profileIv);
            onlineStatusIv = itemView.findViewById(R.id.onlineStatusIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            lastMessageTv = itemView.findViewById(R.id.lastMessageTv);
        }
    }

}
