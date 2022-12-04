package io.rick.socialmedia.adapters;

import android.content.Context;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.rick.socialmedia.R;
import io.rick.socialmedia.models.ModelComment;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.MyHolder> {

    Context context;
    List<ModelComment> commentList;
    String myUid;
    String postId;

    public CommentAdapter(Context context, List<ModelComment> commentList, String myUid, String postId) {
        this.context = context;
        this.commentList = commentList;
        this.myUid = myUid;
        this.postId = postId;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //bind the comment_row.xml layout
        View view = LayoutInflater.from(context).inflate(R.layout.comments_row, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        //get data
        String uid = commentList.get(position).getUid();
        String name = commentList.get(position).getuName();
        String email = commentList.get(position).getuEmail();
        String image = commentList.get(position).getuDp();
        String cid = commentList.get(position).getcId();
        String comment = commentList.get(position).getComment();
        String timestamp = commentList.get(position).getTimestamp();
        //convert timestamp to proper time date
        String pattern = "dd/MM/yyyy hh:mm aa";
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(timestamp));
        String cTime = DateFormat.format(pattern, cal).toString();
        //set data
        holder.nameTv.setText(name);
        holder.commentTv.setText(comment);
        holder.timeTv.setText(cTime);
        //set user dp
        try {
            Picasso.get().load(image).placeholder(R.drawable.ic_face_24).into(holder.avatarIv);
        }catch (Exception e){
            Picasso.get().load(R.drawable.ic_face_24).into(holder.avatarIv);
        }
        //comment LongClickListener
        holder.itemView.setOnLongClickListener(view -> {
            //check if this comment is by currently signed in user or not
            if (myUid.equals(uid)){
                //my comment
                //show delete dialog
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(view.getRootView().getContext());
                builder.setTitle("Delete");
                builder.setMessage("Are you sure you want to delete this comment ?");
                builder.setPositiveButton("YES", (dialogInterface, i) -> {
                    //delete comment
                    deleteComment(cid);
                    showToast("Deleted successfully...");
                });
                builder.setNegativeButton("NO", (dialogInterface, i) -> {
                    //dismiss dialog
                    dialogInterface.dismiss();
                });
                //show dialog
                builder.create().show();
            } else {
                //not my comment
                showToast("Can't delete other comment...");
            }
            return false;
        });
    }

    private void showToast(String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void deleteComment(String cid) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.child("Comments").child(cid).removeValue();//it will delete the comment
        //now update the comments count
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String comments = "" + snapshot.child("pComments").getValue();
                int newCommentVal = (Integer.parseInt(comments) - 1);
                ref.child("pComments").setValue("" + newCommentVal);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    class MyHolder extends RecyclerView.ViewHolder{

        //comment_row.xml views
        ImageView avatarIv;
        TextView nameTv;
        TextView commentTv;
        TextView timeTv;


        public MyHolder(@NonNull View itemView) {
            super(itemView);
            avatarIv = itemView.findViewById(R.id.avatarIv);
            nameTv = itemView.findViewById(R.id.nameTv);
            commentTv = itemView.findViewById(R.id.commentTv);
            timeTv = itemView.findViewById(R.id.timeTv);
        }
    }

}
