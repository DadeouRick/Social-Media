package io.rick.socialmedia.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.rick.socialmedia.R;
import io.rick.socialmedia.adapters.CommentAdapter;
import io.rick.socialmedia.models.ModelComment;

public class PostDetailsActivity extends AppCompatActivity {

    //to get detail of user and post
    String myUid;
    String myEmail;
    String myName;
    String myDp;
    String postId;
    String pLikes;
    String commentCount;
    String shareCount;
    String hisDp;
    String hisName;
    String hisUid;
    String post;
    String pType;
    //views
    ImageView uPictureIv;
    ImageView pImageIv;
    VideoView pVideoVv;
    TextView uNameTv;
    TextView pTimeTv;
    TextView pTitleTv;
    TextView pDescriptionTv;
    TextView pLikesTv;
    ImageButton moreBtn;
    MaterialButton likeBtn;
    MaterialButton commentBtn;
    MaterialButton shareBtn;
    LinearLayout profileLayout;
    RecyclerView recyclerView;
    List<ModelComment> commentList;
    CommentAdapter commentAdapter;
    //add comment views
    EditText commentEt;
    ImageButton sendBtn;
    ImageView cAvatarIv;
    ProgressDialog pd;
    boolean mProcessShare = false;
    boolean mProcessComment = false;
    boolean mProcessLike = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_details);

        //Actionbar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        //get id of post using intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");
        //init views
        uPictureIv = findViewById(R.id.profImage);
        pImageIv = findViewById(R.id.pImage);
        pVideoVv = findViewById(R.id.pVideo);
        uNameTv = findViewById(R.id.uNameTV);
        pTimeTv = findViewById(R.id.pTimeTV);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        commentBtn = findViewById(R.id.commentBtn);
        shareBtn = findViewById(R.id.shareBtn);
        profileLayout = findViewById(R.id.profileLayout);
        recyclerView = findViewById(R.id.commentRecyclerView);
        commentEt = findViewById(R.id.inputComment);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);

        loadPostInfo();
        checkUserStatus();
        loadUserInfo();
        setLikes();
        //set subtitle of actionbar
        actionBar.setTitle("SignedIn as: ");
        actionBar.setSubtitle(myEmail);
        loadComments();
        setListeners();
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

    private void loadComments() {
        //linearlayoutManager for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        //set layout to recyclerview
        recyclerView.setLayoutManager(layoutManager);

        //init comments list
        commentList = new ArrayList<>();
        //path of the post, to get it's comments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelComment modelComment = ds.getValue(ModelComment.class);
                    commentList.add(modelComment);
                    //pass myUid and postId as parameter of constructor of Comment Adapter

                    //setup adapter
                    commentAdapter = new CommentAdapter(getApplicationContext(), commentList, myUid, postId );
                    //set adapter
                    recyclerView.setAdapter(commentAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setLikes() {
        //when the details of post is loading, also check if current uer has liked it or not
        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postId).hasChild(myUid)){
                    //user has liked this post
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked, 0, 0, 0);
                } else {
                    //user has not liked this post
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black, 0, 0, 0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadUserInfo() {
        //get current user info
        Query myRef = FirebaseDatabase.getInstance().getReference("Users");
        myRef.orderByChild("uid").equalTo(myUid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot ds : snapshot.getChildren()){
                            myName = "" + ds.child("name").getValue();
                            myDp = "" + ds.child("image").getValue();
                            //set data
                            try {
                                Picasso.get().load(myDp).placeholder(R.drawable.ic_face_24).into(cAvatarIv);
                            } catch (Exception e){
                                Picasso.get().load(R.drawable.ic_face_24).into(cAvatarIv);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

    }

    private void loadPostInfo() {
        //get post using the id of the post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //keep checking posts until get the required post
                for (DataSnapshot ds : snapshot.getChildren()){
                    String pTitle = "" + ds.child("pTitle").getValue();
                    String pDescription = "" + ds.child("pDescription").getValue();
                    pLikes = "" + ds.child("pLikes").getValue();
                    String pTimeStamp = "" + ds.child("pTime").getValue();
                    post = "" + ds.child("pImage").getValue();
                    pType = "" + ds.child("pType").getValue();
                    hisDp = "" + ds.child("uDp").getValue();
                    hisUid = "" + ds.child("uid").getValue();
                    String uEmail = "" + ds.child("uEmail").getValue();
                    hisName = "" + ds.child("uName").getValue();
                    String lk = " Like";
                    String lks = " Likes";
                    commentCount = "" + ds.child("pComments").getValue();
                    shareCount = "" + ds.child("pShares").getValue();
                    //convert timestamp to proper time date
                    String pattern = "dd/MM/yyyy hh:mm aa";
                    Calendar cal = Calendar.getInstance(Locale.ENGLISH);
                    cal.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime = DateFormat.format(pattern, cal).toString();
                    //set data
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescription);
                    if (Integer.parseInt(pLikes) > 1){
                        pLikesTv.setText(pLikes + lks);
                    } else if (Integer.parseInt(pLikes) <= 1){
                        pLikesTv.setText(pLikes + lk);
                    }
                    pTimeTv.setText(pTime);
                    uNameTv.setText(hisName);
                    likeBtn.setText("" + pLikes);
                    commentBtn.setText("" + commentCount);
                    shareBtn.setText("" + shareCount);
                    //set image of the user who posted
                    //if there is no image then hide imageview
                    if (pType.equals("text")){
                        //hide imageview
                        pImageIv.setVisibility(View.GONE);
                        pVideoVv.setVisibility(View.GONE);
                    } else if (pType.equals("image")){
                        //show imageview
                        pImageIv.setVisibility(View.VISIBLE);
                        pVideoVv.setVisibility(View.GONE);
                        try {
                            Picasso.get().load(post).into(pImageIv);
                        }catch (Exception e){
                            Picasso.get().load(R.drawable.ic_image).into(pImageIv);
                        }
                    } else if (pType.equals("video")){
                        pImageIv.setVisibility(View.GONE);
                        Uri uri=Uri.parse(post);
                        pVideoVv.setVideoURI(uri);

                        pVideoVv.clearFocus();
                        pVideoVv.setVideoURI(uri);
                        MediaController controller = new MediaController(PostDetailsActivity.this);
                        pVideoVv.setMediaController(controller);
                        controller.setAnchorView(pVideoVv);
                    }
                    //set user image in comment part
                    try {
                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_face_24).into(uPictureIv);
                    } catch (Exception e){
                        Picasso.get().load(R.drawable.ic_face_24).into(uPictureIv);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setListeners() {
        likeBtn.setOnClickListener(view -> {
            likePost();
        });
        sendBtn.setOnClickListener(view -> {
            postComment();
        });
        moreBtn.setOnClickListener(view -> {
            showMoreOptions();
        });
        shareBtn.setOnClickListener(view -> {
            String pTitle = pTitleTv.getText().toString().trim();
            String pDescription = pDescriptionTv.getText().toString().trim();
            //get from imageview
            if (pType.equals("image") || pType.equals("text")){
                BitmapDrawable bitmapDrawable = (BitmapDrawable) pImageIv.getDrawable();
                if (bitmapDrawable == null){
                    //post without image
                    shareTextOnly(pTitle, pDescription);
                    updateSharesCount();
                } else {
                    //post with image
                    //convert image to bitmap
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle, pDescription, bitmap);
                    updateSharesCount();
                }
            }
        });
        pLikesTv.setOnClickListener(v -> {
            Intent intent = new Intent(PostDetailsActivity.this, PostLikedByActivity.class);
            intent.putExtra("postId", postId);
            startActivity(intent);
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
        startActivity(Intent.createChooser(shareIntent, "Share via"));//message to show in share dialog
    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {
        //concatenate title and description to share
        String shareBody = pTitle + "\n" + pDescription;
        //first save this image in cache, get the saved image uri
        Uri uri = saveImage(bitmap);
        //share intent
        Intent shIntent = new Intent(Intent.ACTION_SEND);
        shIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
        shIntent.putExtra(Intent.EXTRA_SUBJECT, "Share Here");
        shIntent.setType("image/*");
        startActivity(Intent.createChooser(shIntent, "Share Via"));
    }

    private Uri saveImage(Bitmap image) {
        //TODO - Should be processed in another thread
        File imagesFolder = new File(getCacheDir(), "images");
        Uri uri = null;
        try {
            imagesFolder.mkdirs();
            File file = new File(imagesFolder, "shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.PNG, 90, stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(PostDetailsActivity.this, "io.rick.socialmedia.fileprovider", file);

        } catch (IOException e) {
            showToast("IOException while trying to write file for sharing: " + e.getMessage());
        }
        return uri;
    }

    private void showMoreOptions() {
        //creating popup menu currently having option Delete for now
        PopupMenu popupMenu = new PopupMenu(PostDetailsActivity.this, moreBtn, Gravity.END);
        //show delete option in only post of currently signed in user
        if (hisUid.equals(myUid)){
            //add items in menu
            popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            popupMenu.getMenu().add(Menu.NONE, 1, 0, "Edit");
        }
        //item click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == 0){
                    //delete is clicked
                    beginDelete();
                } else if (id == 1){
                    //Edit is checked
                    //Start AddPostActivity with key "editPost" and the id of the post clicked
                    Intent intent = new Intent(PostDetailsActivity.this, AddPostImageActivity.class);
                    intent.putExtra("key", "editPost");
                    intent.putExtra("editPostId", postId);
                    startActivity(intent);
                }
                return false;
            }
        });
        //show menu
        popupMenu.show();
    }

    private void beginDelete() {
        //post can be with or without image
        if (post.equals("noImage")){
            //post without image
            deleteWithoutImage();
        } else {
            //post with image
            deleteWithImage();
        }
    }

    private void deleteWithImage() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting Post...");
        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(post);
        picRef.delete()
                .addOnSuccessListener(unused -> {
                    //image deleted now delete database
                    Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
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

    private void deleteWithoutImage() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting Post...");
        Query query = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
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

    private void likePost() {
        //get total number of likes for the post, whose like button clicked
        //if currently signed in user has not liked it before
        //increase value by 1, otherwise decrease value by 1
        mProcessLike = true;
        //get id of the post clicked
        DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessLike){
                    if (snapshot.child(postId).hasChild(myUid)){
                        //already liked, so remove like
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes)-1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;
                    }else {
                        //not liked
                        postsRef.child(postId).child("pLikes").setValue("" + (Integer.parseInt(pLikes)+1));
                        likesRef.child(postId).child(myUid).setValue("liked");
                        mProcessLike = false;
                        addToHisNotifications(hisUid, postId, "Liked your post");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void postComment() {
        pd = new ProgressDialog(this);
        pd.setMessage("Adding comment...");
        //get data from comment ET
        String comment = commentEt.getText().toString().trim();
        //
        if (TextUtils.isEmpty(comment)){
            showToast("Comment is empty...");
            return;
        }
        String timeStamp = String.valueOf(System.currentTimeMillis());
        //each post will have a child "Comments" that will contain comments of that post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        HashMap<String, Object> hashMap = new HashMap<>();
        //put info in hashMap
        hashMap.put("cId", timeStamp);
        hashMap.put("comment", comment);
        hashMap.put("timestamp", timeStamp);
        hashMap.put("uid", myUid);
        hashMap.put("uEmail", myEmail);
        hashMap.put("uDp", myDp);
        hashMap.put("uName", myName);
        //put this data in db
        ref.child(timeStamp).setValue(hashMap)
                .addOnSuccessListener(unused -> {
                    //added
                    pd.dismiss();
                    showToast("Comment Added...");
                    commentEt.setText("");
                    updateCommentCount();
                    addToHisNotifications(hisUid, postId, "Commented your post");
                })
                .addOnFailureListener(e -> {
                    //failed
                    pd.dismiss();
                    showToast("" + e.getMessage());
                });
    }

    private void updateCommentCount() {
        //whenever user add comment increase the comment count as we did for like count
        mProcessComment = true;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessComment){
                    String comments = "" + snapshot.child("pComments").getValue();
                    int newCommentVal = Integer.parseInt(comments) + 1;
                    ref.child("pComments").setValue("" + newCommentVal);
                    mProcessComment = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void updateSharesCount() {
        //whenever user add comment increase the comment count as we did for like count
        mProcessShare = true;
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessComment){
                    String shares = "" + snapshot.child("pShares").getValue();
                    int newShareVal = Integer.parseInt(shares) + 1;
                    ref.child("pShares").setValue("" + newShareVal);
                    mProcessShare = false;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void checkUserStatus() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null){
            //user is signed in
            myEmail = user.getEmail();
            myUid = user.getUid();
        } else {
            //user not s
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

}