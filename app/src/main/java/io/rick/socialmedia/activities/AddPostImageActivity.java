package io.rick.socialmedia.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
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

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

import io.rick.socialmedia.R;

public class AddPostImageActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    DatabaseReference userDbRef;
    ActionBar actionBar;
    TextInputEditText titleEt;
    TextInputEditText descriptionEt;
    ImageView imageIv;
    MaterialButton upload;
    private static final int CAMERA_REQUEST_CODE = 101;
    private static final int STORAGE_REQUEST_CODE = 201;
    private static final int IMAGE_PICK_GALLERY_CODE = 301;
    private static final int IMAGE_PICK_CAMERA_CODE = 401;
    //array of permissions to be requested
    String[] cameraPermission;
    String[] storagePermission;
    //user info
    String name;
    String email;
    String uid;
    String dp;
    String isUpdateKey;
    String editPostId;
    //info of post to be edited
    String editTitle;
    String editDescription;
    String editImage;
    //uri of picked image
    Uri image_uri = null;
    //progressbar
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post_image);

        actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setTitle("Add New post");
        //enable back button
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        pd = new ProgressDialog(this);
        //init
        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();
        //init views
        titleEt = findViewById(R.id.pTitleET);
        descriptionEt = findViewById(R.id.pDescriptionEt);
        imageIv = findViewById(R.id.pImageIv);
        upload = findViewById(R.id.uploadBtn);
        //get data through intent from previous activity's adapter
        Intent intent = getIntent();
        isUpdateKey = "" + intent.getStringExtra("key");
        editPostId = "" + intent.getStringExtra("editPostId");
        //validate if we came here to update post
        if (isUpdateKey.equals("editPost")){
            //update
            actionBar.setTitle("Edit Post");
            upload.setText("SAVE CHANGES");
            loadPostData(editPostId);
        } else {
            actionBar.setTitle("Add New post");
            upload.setText("UPLOAD");
        }
        actionBar.setSubtitle(email);
        //get some info of current user to include in post
        userDbRef = FirebaseDatabase.getInstance().getReference("Users");
        Query query = userDbRef.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    name = "" + ds.child("name").getValue();
                    email = "" + ds.child("email").getValue();
                    dp = "" + ds.child("image").getValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        //init arrays of permissions
        cameraPermission = new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        setListeners();
    }

    private void loadPostData(String editPostId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        //get details of post using id of post
        Query query = reference.orderByChild("pId").equalTo(editPostId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    //get data
                    editTitle = "" + ds.child("pTitle").getValue();
                    editDescription = "" + ds.child("pDescription").getValue();
                    editImage = "" + ds.child("pImage").getValue();
                    //set data to views
                    titleEt.setText(editTitle);
                    descriptionEt.setText(editDescription);
                    if (!editImage.equals("noImage")){
                        try {
                            Picasso.get().load(editImage).into(imageIv);
                        } catch (Exception e){

                        }
                    }

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setListeners() {
        upload.setOnClickListener(view -> {
            //get data title , description from EditTexts
            String title = titleEt.getText().toString().trim();
            String description = descriptionEt.getText().toString().trim();
            if (TextUtils.isEmpty(title)){
                showToast("Enter title...");
                return;
            }
            if (TextUtils.isEmpty(description)){
                showToast("Enter description...");
                return;
            }

            if (isUpdateKey.equals("editPost")){
                beginUpdate(title, description, editPostId);
            } else {
                uploadData(title, description);
            }

        });
        //get image from camera or gallery on imageview click
        imageIv.setOnClickListener(view -> showImagePickDialog());
    }

    private void beginUpdate(String title, String description, String editPostId) {
        pd.setMessage("Applying Post Changes...");
        pd.show();
        if (!editImage.equals("noImage")){
            //with image
            updateWasWithImage(title, description, editPostId);
        } else if (imageIv.getDrawable() != null){
            //with image
            updateWithNowImage(title, description, editPostId);
        } else {
            //without image
            updateWithoutImage(title, description, editPostId);
        }
    }

    private void updateWithoutImage(String title, String description, String editPostId) {
        HashMap<String, Object> hashMap = new HashMap<>();
        //put post info
        hashMap.put("uid", uid);
        hashMap.put("uName", name);
        hashMap.put("uEmail", email);
        hashMap.put("uDp", dp);
        hashMap.put("pTitle", title);
        hashMap.put("pDescription", description);
        hashMap.put("pImage", "noImage");
        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Posts");
        ref1.child(editPostId)
                .updateChildren(hashMap)
                .addOnSuccessListener(unused -> {
                    pd.dismiss();
                    showToast("Updated...");
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    showToast("" + e.getMessage());
                });
    }

    private void updateWithNowImage(final String title, final String description, final String editPostId) {
        //
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_" + timeStamp;

        //get image from imageview
        Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
        /*

         */
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //image compress
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] data = baos.toByteArray();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putBytes(data)
                .addOnSuccessListener(taskSnapshot -> {
                    //image upload get its url
                    Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                    while (!uriTask.isSuccessful());

                    String downloadUri = uriTask.getResult().toString();
                    if (uriTask.isSuccessful()){
                        //url is received, upload firebase database
                        HashMap<String, Object> hashMap = new HashMap<>();
                        //put post info
                        hashMap.put("uid", uid);
                        hashMap.put("uName", name);
                        hashMap.put("uEmail", email);
                        hashMap.put("uDp", dp);
                        hashMap.put("pTitle", title);
                        hashMap.put("pDescription", description);
                        hashMap.put("pImage", downloadUri);
                        DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Posts");
                        ref1.child(editPostId)
                                .updateChildren(hashMap)
                                .addOnSuccessListener(unused -> {
                                    pd.dismiss();
                                    showToast("Updated...");
                                })
                                .addOnFailureListener(e -> {
                                    pd.dismiss();
                                    showToast("" + e.getMessage());
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    //image not uploaded
                    pd.dismiss();
                    showToast("" + e.getMessage());
                });
    }

    private void updateWasWithImage(String title, String description, String editPostId) {
        //post is with image, delete previous image first
        StorageReference mPictureRef = FirebaseStorage.getInstance().getReferenceFromUrl(editImage);
        mPictureRef.delete()
                .addOnSuccessListener(unused -> {
                    //image deleted, upload new image
                    //for post-image name, post-id, publish-time
                    String timeStamp = String.valueOf(System.currentTimeMillis());
                    String filePathAndName = "Posts/" + "post_" + timeStamp;

                    //get image from imageview
                    Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    //image compress
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                    byte[] data = baos.toByteArray();
                    StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                    ref.putBytes(data)
                            .addOnSuccessListener(taskSnapshot -> {
                                //image upload get its url
                                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                while (!uriTask.isSuccessful());

                                String downloadUri = uriTask.getResult().toString();
                                if (uriTask.isSuccessful()){
                                    //url is received, upload firebase database
                                    HashMap<String, Object> hashMap = new HashMap<>();
                                    //put post info
                                    hashMap.put("uid", uid);
                                    hashMap.put("uName", name);
                                    hashMap.put("uEmail", email);
                                    hashMap.put("uDp", dp);
                                    hashMap.put("pTitle", title);
                                    hashMap.put("pDescription", description);
                                    hashMap.put("pImage", downloadUri);
                                    DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Posts");
                                    ref1.child(editPostId)
                                            .updateChildren(hashMap)
                                            .addOnSuccessListener(unused1 -> {
                                                pd.dismiss();
                                                showToast("Updated...");
                                            })
                                            .addOnFailureListener(e -> {
                                                pd.dismiss();
                                                showToast("" + e.getMessage());
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                //image not uploaded
                                pd.dismiss();
                                showToast("" + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    pd.dismiss();
                    showToast("" + e.getMessage());
                });
    }

    private void uploadData(String title, String description) {
        pd.setMessage("Publishing post...");
        pd.show();
        //for post-image, post-id, post-publish-time
        final String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_" + timeStamp;
        if (imageIv.getDrawable() != null){
            //get image from imageview
            Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //image compress
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] data = baos.toByteArray();

            //post with image
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data)
                    .addOnSuccessListener(taskSnapshot -> {
                        //image is uploaded to firebase storage, now get it's uri
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        String downloadUri = uriTask.getResult().toString();
                        if (uriTask.isSuccessful()){
                            //url is received upload post to firebase database
                            HashMap<String, Object> hashMap = new HashMap<>();
                            //put post info
                            hashMap.put("uid", uid);
                            hashMap.put("pType", "image");
                            hashMap.put("uName", name);
                            hashMap.put("uEmail", email);
                            hashMap.put("uDp", dp);
                            hashMap.put("pId", timeStamp);
                            hashMap.put("pTitle", title);
                            hashMap.put("pDescription", description);
                            hashMap.put("pImage", downloadUri);
                            hashMap.put("pTime", timeStamp);
                            hashMap.put("pLikes", "0");
                            hashMap.put("pComments", "0");
                            hashMap.put("pShares", "0");
                            //path to store post data
                            DatabaseReference ref1 = FirebaseDatabase.getInstance().getReference("Posts");
                            //put data in this ref
                            ref1.child(timeStamp).setValue(hashMap)
                                    .addOnSuccessListener(unused -> {
                                        //added in database
                                        pd.dismiss();
                                        showToast("Post published");
                                        //reset views
                                        titleEt.setText("");
                                        descriptionEt.setText("");
                                        imageIv.setImageURI(null);
                                        image_uri = null;
                                    })
                                    .addOnFailureListener(e -> {
                                        //Failed adding post in database
                                        pd.dismiss();
                                        showToast("" + e.getMessage());
                                    });
                        }

                    })
                    .addOnFailureListener(e -> {
                        //failed uploading image
                        pd.dismiss();
                        showToast("" + e.getMessage());
                    });
        } else {
            //post without image
            HashMap<String, Object> hashMap = new HashMap<>();
            //put post info
            hashMap.put("uid", uid);
            hashMap.put("uName", name);
            hashMap.put("uEmail", email);
            hashMap.put("uDp", dp);
            hashMap.put("pId", timeStamp);
            hashMap.put("pTitle", title);
            hashMap.put("pDescription", description);
            hashMap.put("pImage", "noImage");
            hashMap.put("pTime", timeStamp);
            hashMap.put("pLikes", "0");
            hashMap.put("pComments", "0");
            hashMap.put("pShares", "0");
            //path to store post data
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
            //put data in this ref
            ref.child(timeStamp).setValue(hashMap)
                    .addOnSuccessListener(unused -> {
                        //added in database
                        pd.dismiss();
                        showToast("Post published");
                        //reset views
                        titleEt.setText("");
                        descriptionEt.setText("");
                        imageIv.setImageURI(null);
                        image_uri = null;
                    })
                    .addOnFailureListener(e -> {
                        //Failed adding post in database
                        pd.dismiss();
                        showToast("" + e.getMessage());
                    });
        }
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestStoragePermission(){
        //request runtime permission
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        //Check if storage permission is enable or not
        //return true if enable
        //return false if not enable
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void requestCameraPermission(){
        //request runtime permission
        ActivityCompat.requestPermissions(this, cameraPermission, PackageManager.PERMISSION_GRANTED);
    }

    @SuppressLint("NewApi")
    private void showImagePickDialog() {
        String[] options = {"Camera", "Gallery"};
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("Choose Image From");
        //set item options
        builder.setItems(options, (dialogInterface, i) -> {
            if (i == 0){
                //Camera clicked
                if (!checkCameraPermission()){
                    requestCameraPermission();
                } else {
                    pickFromCamera();
                }
            } else if (i == 1){
                //Gallery clicked
                if (!checkStoragePermission()){
                    requestStoragePermission();
                } else {
                    pickFromGallery();
                }
            }
        });
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //handle permission allow or denied when user press on Allow or Deny from permission request dialog
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                //picking from camera, first check if camera and storage permission allowed or not
                if (grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean StorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && StorageAccepted){
                        pickFromCamera();
                    } else {
                        showToast("Enable Camera & storage permissions");
                    }
                }else {

                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                //picking from gallery, first check if storage permission allowed or not
                if (grantResults.length > 0){
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        pickFromGallery();
                    } else {
                        showToast("Enable storage permission");
                    }
                }
            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                //image is picked from gallery, get uri of image
                image_uri = data != null ? data.getData() : null;
                //set to image
                imageIv.setImageURI(image_uri);
            }else if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from camera, get uri of image
                imageIv.setImageURI(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();//go to previous activity
        return super.onSupportNavigateUp();
    }

    private void pickFromCamera() {
        //Intent of picked image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pick");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        //put image uri
        image_uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private void pickFromGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, IMAGE_PICK_GALLERY_CODE);
    }

    private void checkUserStatus(){
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null){
            //user is signed in stay here // set email of logged user
            email = user.getEmail();
            uid = user.getUid();
        } else {
            //user is not signed go to signUp activity
            startActivity(new Intent(getApplicationContext(), MainActivity.class));
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.findItem(R.id.action_menu).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_addPost).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

}