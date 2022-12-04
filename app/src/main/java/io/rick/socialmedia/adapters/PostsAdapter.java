package io.rick.socialmedia.adapters;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.rick.socialmedia.R;
import io.rick.socialmedia.activities.AddPostImageActivity;
import io.rick.socialmedia.activities.PostDetailsActivity;
import io.rick.socialmedia.activities.PostLikedByActivity;
import io.rick.socialmedia.activities.ThereProfileActivity;
import io.rick.socialmedia.models.ModelPost;

public class PostsAdapter extends RecyclerView.Adapter<PostsAdapter.MyHolder>{

    Context context;
    List<ModelPost> postList;
    String myUid;
    String pLikes;
    String pComments;
    String pShares;
    private DatabaseReference likesRef;//for likes database node
    private DatabaseReference postsRef;//reference of posts
    boolean mProcessLike = false;
    int plikes;

    public PostsAdapter(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflate layout post_row.xml
        View view = LayoutInflater.from(context).inflate(R.layout.posts_row, parent, false);
        return new MyHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int position) {
        //get data
        String uid = postList.get(position).getUid();
        String uEmail = postList.get(position).getuEmail();
        String uName = postList.get(position).getuName();
        String uDP = postList.get(position).getuDp();
        String pId = postList.get(position).getpId();
        String pType = postList.get(position).getpType();
        String pTitle = postList.get(position).getpTitle();
        String pDescription = postList.get(position).getpDescription();
        String pImage = postList.get(position).getpImage();
        String pTimeStamp = postList.get(position).getpTime();
        pLikes = postList.get(position).getpLikes();//contains total number of likes for a post
        pComments = postList.get(position).getpComments();//contains total number of comments for a post
        pShares = postList.get(position).getpShares();//contains total number of shares for a post
        String lk = " Like";
        String lks = " Likes";
        //convert timestamp to proper time date
        String pattern = "dd/MM/yyyy hh:mm aa";
        Calendar cal = Calendar.getInstance(Locale.ENGLISH);
        cal.setTimeInMillis(Long.parseLong(pTimeStamp));
        String pTime = DateFormat.format(pattern, cal).toString();
        //set data
        holder.uNameTv.setText(uName);
        holder.pTimeTv.setText(pTime);
        holder.pTitleTv.setText(pTitle);
        holder.pDescriptionTv.setText(pDescription);
        if (Integer.parseInt(pLikes) > 1){
            holder.pLikesTv.setText(pLikes + lks);
        } else if (Integer.parseInt(pLikes) <= 1){
            holder.pLikesTv.setText(pLikes + lk);
        }
        holder.likeBtn.setText("" + pLikes);
        holder.commentBtn.setText("" + pComments);
        holder.shareBtn.setText("" + pShares);
        //set likes for each post
        setLikes(holder, pId);
        //set user dp
        try {
            Picasso.get().load(uDP).placeholder(R.drawable.ic_face_24).into(holder.imageProfile);
        }catch (Exception e){
            Picasso.get().load(R.drawable.ic_face_24).into(holder.imageProfile);
        }
        //set post image
        //if there is no image then hide imageview
        switch (pType) {
            case "text":
                //hide imageview
                holder.pImageIv.setVisibility(View.GONE);
                holder.pVideoVv.setVisibility(View.GONE);
                break;
            case "image":
                //show imageview
                holder.pImageIv.setVisibility(View.VISIBLE);
                holder.pVideoVv.setVisibility(View.GONE);
                try {
                    Picasso.get().load(pImage).into(holder.pImageIv);
                } catch (Exception e) {
                    Picasso.get().load(R.drawable.ic_image).into(holder.pImageIv);
                }
                break;
            case "video":
                /*
                holder.pImageIv.setVisibility(View.GONE);
                Uri uri = Uri.parse(pImage);
                holder.pVideoVv.setVideoURI(uri);

                holder.pVideoVv.clearFocus();
                holder.pVideoVv.setVideoURI(uri);
                MediaController controller = new MediaController(context);
                controller.setAnchorView(holder.pVideoVv);
                holder.pVideoVv.setMediaController(controller);
                controller.setAnchorView(holder.pVideoVv);*/

                holder.pImageIv.setVisibility(View.GONE);
                Uri uri = Uri.parse(pImage);
                holder.pVideoVv.setVideoURI(uri);
                holder.pVideoVv.start();
                holder.pVideoVv.setOnClickListener(v -> {
                    if (!holder.pVideoVv.isPlaying()) {
                        holder.pVideoVv.start();
                    } else {
                        holder.pVideoVv.pause();
                    }
                });
                break;
        }

        //handle button click
        holder.moreBtn.setOnClickListener(view -> {
            showMoreOptions(holder.moreBtn, uid, myUid, pId, pImage);
        });
        holder.likeBtn.setOnClickListener(view -> {
            //get total number of likes for the post, whose like button clicked
            //if currently signed in user has not liked it before
            //increase value by 1, otherwise decrease value by 1
            plikes = Integer.parseInt(postList.get(position).getpLikes());
            mProcessLike = true;
            //get id of the post clicked
            String postId = postList.get(position).getpId();
            likesRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (mProcessLike){
                        if (snapshot.child(postId).hasChild(myUid)){
                            //already liked, so remove like
                            postsRef.child(postId).child("pLikes").setValue("" + (plikes-1));
                            likesRef.child(postId).child(myUid).removeValue();
                            mProcessLike = false;
                        }else {
                            //not liked
                            postsRef.child(postId).child("pLikes").setValue("" + (plikes+1));
                            likesRef.child(postId).child(myUid).setValue("liked");
                            mProcessLike = false;
                            addToHisNotifications(uid, pId, "Liked your post");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        });
        holder.commentBtn.setOnClickListener(view -> {
            //start PostDetailsActivity
            Intent intent = new Intent(context, PostDetailsActivity.class);
            intent.putExtra("postId", pId);
            context.startActivity(intent);
        });
        holder.shareBtn.setOnClickListener(view -> {
            //get from imageview
            if (pType.equals("image") || pType.equals("text")){
                BitmapDrawable bitmapDrawable = (BitmapDrawable) holder.pImageIv.getDrawable();
                if (bitmapDrawable == null){
                    //post without image
                    shareTextOnly(pTitle, pDescription);
                } else {
                    //post with image
                    //convert image to bitmap
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle, pDescription, bitmap);
                }
            }
        });
        holder.uNameTv.setOnClickListener(view -> {
            Intent intent = new Intent(context, ThereProfileActivity.class);
            intent.putExtra("uid", uid);
            context.startActivity(intent);
        });
        holder.imageProfile.setOnClickListener(view -> {
            Intent intent = new Intent(context, ThereProfileActivity.class);
            intent.putExtra("uid", uid);
            context.startActivity(intent);
        });

        holder.pLikesTv.setOnClickListener(v -> {
            Intent intent = new Intent(context, PostLikedByActivity.class);
            intent.putExtra("postId", pId);
            context.startActivity(intent);
        });

    }

    private void addToHisNotifications(String hisUid, String pId, String notification){
        String timestamp = String.valueOf(System.currentTimeMillis());
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("pId", pId);
        hashMap.put("timestamp", timestamp);
        hashMap.put("pUid", hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid", myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Users");
        ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    //added successfully

                })
                .addOnFailureListener(e -> {
                    //failed
                });
    }

    private void shareTextOnly(String pTitle, String pDescription) {
        //concatenate title and description to share
        String shareBody = pTitle + "\n" + pDescription;
        //share intent
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here");//in case share is made via an email app
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        context.startActivity(Intent.createChooser(shareIntent, "Share via"));//message to show in share dialog
    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {
        //concatenate title and description to share
        String shareBody = pTitle + "\n" + pDescription;
        //first save this image in cache, get the saved image uri
        //Uri uri = saveImageToShare(bitmap);
        Uri uri = saveImage(bitmap);
        //share intent
        Intent shIntent = new Intent(Intent.ACTION_SEND);
        shIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        shIntent.putExtra(Intent.EXTRA_SUBJECT, "Share Here");
        shIntent.setType("image/*");
        context.startActivity(Intent.createChooser(shIntent, "Share Via"));
    }

    private Uri saveImage(Bitmap image) {
        //TODO - Should be processed in another thread
        File imagesFolder = new File(context.getCacheDir(), "images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context, "io.rick.socialmedia.fileprovider", file);

        } catch (IOException e) {
            showToast("IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }

    private void setLikes(MyHolder holder, String postKey) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postKey).hasChild(myUid)){
                    //user has liked this post
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                    //holder.likeBtn.setText(pLikes);
                } else {
                    //user has not liked this post
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                    //holder.likeBtn.setText("" + (Integer.parseInt(pLikes)));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showMoreOptions(ImageButton moreBtn, String uid, String myUid, String pId, String pImage) {
        //creating popup menu currently having option Delete for now
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);
        //show delete option in only post of currently signed in user
        if (uid.equals(myUid)){
            //add items in menu
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");
        }
        popupMenu.getMenu().add(Menu.NONE, 2, 0, "View Detail");
        //item click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == 0){
                    //delete is clicked
                    beginDelete(pId, pImage);
                } else if (id == 1){
                    //Edit is checked
                    //Start AddPostActivity with key "editPost" and the id of the post clicked
                    Intent intent = new Intent(context, AddPostImageActivity.class);
                    intent.putExtra("key", "editPost");
                    intent.putExtra("editPostId", pId);
                    context.startActivity(intent);
                } else if (id == 2){
                    //start PostDetailsActivity
                    Intent intent = new Intent(context, PostDetailsActivity.class);
                    intent.putExtra("postId", pId);
                    context.startActivity(intent);
                }
                return false;
            }
        });
        //show menu
        popupMenu.show();
    }

    private void  beginDelete(String pId, String pImage) {
        //post can be with or without image
        if (pImage.equals("noImage")){
            //post without image
            deleteWithoutImage(pId);
        } else {
            //post with image
            deleteWithImage(pId, pImage);
        }
    }

    private void deleteWithoutImage(String pId) {
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting Post...");
        Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    ds.getRef().removeValue();//remove values from firebase where pid matches
                }
                showToast("Deleted successfully");
                pd.dismiss();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void deleteWithImage(String pId, String pImage) {
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting Post...");
        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete()
                .addOnSuccessListener(unused -> {
                    //image deleted now delete database
                    Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()){
                                ds.getRef().removeValue();//remove values from firebase where pid matches
                            }
                            showToast("Deleted successfully");
                            pd.dismiss();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                })
                .addOnFailureListener(e -> {
                    //failed, can't
                    pd.dismiss();
                    showToast("" + e.getMessage());
                });
    }

    private void showToast(String message){
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    //view holder
    class MyHolder extends RecyclerView.ViewHolder{

        //views from post_row.xml
        ImageView imageProfile;
        ImageView pImageIv;
        VideoView pVideoVv;
        TextView uNameTv;
        TextView pTimeTv;
        TextView pTitleTv;
        TextView pDescriptionTv;
        TextView pLikesTv;
        ImageButton moreBtn;
        Button likeBtn;
        Button commentBtn;
        Button shareBtn;
        LinearLayout profileLayout;

        public MyHolder(@NonNull View itemView) {
            super(itemView);
            //init views
            imageProfile = itemView.findViewById(R.id.profImage);
            pImageIv = itemView.findViewById(R.id.pimage);
            pVideoVv = itemView.findViewById(R.id.pvideo);
            uNameTv = itemView.findViewById(R.id.uNameTV);
            pTimeTv = itemView.findViewById(R.id.pTimeTV);
            pTitleTv = itemView.findViewById(R.id.pTitleTv);
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            profileLayout = itemView.findViewById(R.id.profileLayout);
        }
    }

}
