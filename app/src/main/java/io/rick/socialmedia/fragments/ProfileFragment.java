package io.rick.socialmedia.fragments;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
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
import com.google.firebase.storage.UploadTask;
import com.makeramen.roundedimageview.RoundedImageView;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.rick.socialmedia.R;
import io.rick.socialmedia.activities.AddPostImageActivity;
import io.rick.socialmedia.activities.AddPostTextActivity;
import io.rick.socialmedia.activities.AddPostVideoActivity;
import io.rick.socialmedia.activities.MainActivity;
import io.rick.socialmedia.activities.SettActivity;
import io.rick.socialmedia.adapters.PostsAdapter;
import io.rick.socialmedia.models.ModelPost;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class ProfileFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase database;
    DatabaseReference databaseReference;
    //storage
    StorageReference reference;
    //path where image of user profile and cover will be stored
    String storagePath = "Users_Profile_Cover_Images/";

    //Views
    RoundedImageView imageProfileView;
    ImageView coverIV;
    TextView txtName;
    TextView txtEmail;
    FloatingActionButton fab;
    RecyclerView postsRecyclerView;
    //ProgressDialog
    ProgressDialog pd;
    //permissions constants
    private static final int CAMERA_REQUEST_CODE = 1001;
    private static final int STORAGE_REQUEST_CODE = 2001;
    private static final int IMAGE_PICK_GALLERY_CODE = 3001;
    private static final int IMAGE_PICK_CAMERA_CODE = 4001;
    //array of permissions to be requested
    String[] cameraPermission;
    String[] storagePermission;
    List<ModelPost> postList;
    PostsAdapter postsAdapter;
    String uid;
    //uri of picked image
    Uri image_uri;
    //check profile or cover image
    String profileOrCoverImage;

    public ProfileFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        //init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference("Users");
        reference = FirebaseStorage.getInstance().getReference();

        //init arrays of permissions
        cameraPermission = new String[] {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermission = new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE};
        //init views
        imageProfileView = view.findViewById(R.id.profileImage);
        coverIV = view.findViewById(R.id.coverIV);
        txtName = view.findViewById(R.id.userName);
        txtEmail = view.findViewById(R.id.userEmail);
        fab = view.findViewById(R.id.editFab);
        postsRecyclerView = view.findViewById(R.id.profileRecyclerView_posts);

        pd = new ProgressDialog(getActivity());

        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //check until required data get
                for (DataSnapshot ds : snapshot.getChildren()){
                    //get data
                    String name = "" + ds.child("name").getValue();
                    String email = "" + ds.child("email").getValue();
                    String image = "" + ds.child("image").getValue();
                    String cover = "" + ds.child("cover").getValue();

                    //set data
                    txtName.setText(name);
                    txtEmail.setText(email);
                    txtName.setText(name);
                    try {
                        Picasso.get().load(image).into(imageProfileView);
                    } catch (Exception e){
                        Picasso.get().load(R.drawable.ic_face).into(imageProfileView);
                    }
                    try {
                        Picasso.get().load(cover).into(coverIV);
                    } catch (Exception e){
                        Picasso.get().load(R.drawable.ic_face).into(coverIV);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        setListeners();
        postList = new ArrayList<>();
        checkUserStatus();
        loadMyPosts();
        return view;
    }

    private void loadMyPosts() {
        //linear layout manager for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);
        //init post list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load posts
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPost = ds.getValue(ModelPost.class);
                    //add to list
                    postList.add(myPost);
                    //adapter
                    postsAdapter = new PostsAdapter(getActivity(), postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(postsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("" + error.getMessage());
            }
        });
    }

    private void searchMyPosts(String searchQuery) {
        //linear layout manager for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);
        //init post list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load posts
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPost = ds.getValue(ModelPost.class);
                    if (myPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) ||
                            myPost.getpDescription().toLowerCase().contains(searchQuery.toLowerCase())){
                        //add to list
                        postList.add(myPost);
                    }

                    //adapter
                    postsAdapter = new PostsAdapter(getActivity(), postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(postsAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showToast("" + error.getMessage());
            }
        });
    }

    private void setListeners() {
        fab.setOnClickListener(view -> {
            showEditProfileDialog();
        });
    }

    private void showToast(String message){
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }

    private void requestStoragePermission(){
        //request runtime permission
        requestPermissions(storagePermission, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        //Check if storage permission is enable or not
        //return true if enable
        //return false if not enable
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result && result1;
    }

    private void requestCameraPermission(){
        //request runtime permission
        requestPermissions(cameraPermission, PackageManager.PERMISSION_GRANTED);
    }

    private void showEditProfileDialog() {
        String[] options = {"Edit Name", "Edit Profile Picture", "Edit Cover Image ", "Change Password"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Action");
        //set item
        builder.setItems(options, (dialogInterface, i) -> {
            if (i == 0){
                //Edit Name
                pd.setMessage("Updating Name");
                showNameUpdateDialog("name");
            } else if (i == 1){
                //Edit Profile picture
                pd.setMessage("Updating Profile Picture");
                profileOrCoverImage = "image";
                showEditImagePicDialog();
            } else if (i == 2){
                //Edit cover image
                pd.setMessage("Updating Cover Image");
                profileOrCoverImage = "cover";
                showEditImagePicDialog();
            } else if (i == 3){
                //Edit cover image
                pd.setMessage("Changing Password");
                profileOrCoverImage = "cover";
                showChangePasswordDialog();
            }
        });
        builder.create().show();
    }

    private void showChangePasswordDialog() {
        //password change dialog with custom layout having currentPassword, newPassword and update button
        //inflate layout for dialog
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_update_password, null);
        final EditText OldPasswordET = view.findViewById(R.id.OldPasswordET);
        final EditText passwordET = view.findViewById(R.id.passwordET);
        final EditText confirmPasswordEt = view.findViewById(R.id.ConfirmPasswordEt);
        MaterialButton updatePwdBtn = view.findViewById(R.id.updatePassword);
        final MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity());
        builder.setView(view);//set view on dialog
        final androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        updatePwdBtn.setOnClickListener(v -> {
            String oldPassword = OldPasswordET.getText().toString().trim();
            String newPassword = passwordET.getText().toString().trim();
            String confirmNewPassword = confirmPasswordEt.getText().toString().trim();
            if (TextUtils.isEmpty(oldPassword)){
                showToast("Enter your current password...");
                return;
            }
            if ((newPassword.length() < 6) || (confirmNewPassword.length() < 6)){
                showToast("New password length must at least 6 characters");
                return;
            }
            if (!newPassword.equals(confirmNewPassword)){
                showToast("Password and Confirm must be the same");
            }
            dialog.dismiss();
            updatePassword(oldPassword, newPassword);
        });
        //builder.create().show();
    }

    private void updatePassword(String oldPassword, String newPassword) {
        pd.show();
        //get current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        //before changing password re-authenticate
        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);
        user.reauthenticate(authCredential)
                .addOnSuccessListener(unused -> {
                    //successfully authenticated, begin update
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(unused1 -> {
                                pd.dismiss();
                                showToast("Password Updated Successfully..");
                            })
                            .addOnFailureListener(e -> {
                                pd.dismiss();
                                showToast("" + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    //authenticated failed
                    pd.dismiss();
                    showToast("" + e.getMessage());
                });
    }

    private void showNameUpdateDialog(final String key) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Update" + key);
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10, 10, 10, 10);
        EditText editText = new EditText(getActivity());
        editText.setHint("Enter " + key);
        linearLayout.addView(editText);
        builder.setView(linearLayout);

        builder.setPositiveButton("Update", (dialogInterface, i) -> {
            String value = editText.getText().toString().trim();
            if (!TextUtils.isEmpty(value)){
                pd.show();
                HashMap<String, Object> result = new HashMap<>();
                result.put(key, value);
                databaseReference.child(user.getUid()).updateChildren(result)
                        .addOnSuccessListener(unused -> {
                            pd.dismiss();
                            showToast("Updated...");
                        })
                        .addOnFailureListener(e -> {
                            pd.dismiss();
                            showToast("" + e.getMessage());
                        });
                //if user edit his name, also change it from his posts
                if (key.equals("name")){
                    DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                    Query query = ref.orderByChild("uid").equalTo(uid);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()){
                                String child = ds.getKey();
                                snapshot.getRef().child(child).child("uName").setValue(value);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    //update name in current comments on posts
                    ref.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot ds : snapshot.getChildren()){
                                String child = ds.getKey();
                                if (snapshot.child(child).hasChild("Comments")){
                                    String child1 = "" + snapshot.child(child).getKey();
                                    Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                    child2.addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            for (DataSnapshot ds : snapshot.getChildren()){
                                                String child = ds.getKey();
                                                snapshot.getRef().child(child).child("uName").setValue(value);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {

                                        }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                }
            } else {
                showToast("Enter " + key);
            }
        });
        builder.setNegativeButton("Cancel", (dialogInterface, i) -> {

        });
        builder.show();
    }

    private void showEditImagePicDialog() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Pick Image From");
        //set item
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
                    boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted && writeStorageAccepted){
                        pickFromCamera();
                    } else {
                        showToast("Enable storage permission");
                    }
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
                        showToast("Enable camera & storage permission");
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
                image_uri = data.getData();
                uploadProfileCoverImage(image_uri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                //image is picked from camera, get uri of image
                uploadProfileCoverImage(image_uri);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfileCoverImage(Uri uri) {
        pd.show();
        //path and name of image to be stored in firebase storage
        String filePathAndName = storagePath + "" + profileOrCoverImage + "" + user.getUid();
        StorageReference storageReference = reference.child(filePathAndName);
        storageReference.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //image is uploaded to storage, get it's uri and store in user's database
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isSuccessful());
                        Uri downloadUri = uriTask.getResult();
                        //check if image is uploaded or not and url is received
                        if (uriTask.isSuccessful()){
                            //image uploaded
                            //add/update uri in user's database
                            HashMap<String, Object> results = new HashMap<>();
                            results.put(profileOrCoverImage, downloadUri.toString());
                            databaseReference.child(user.getUid()).updateChildren(results)
                                    .addOnSuccessListener(unused -> {
                                        //url in database of user is added successfully
                                        //dismiss progress bar
                                        pd.dismiss();
                                        showToast("Image Updated...");
                                    })
                                    .addOnFailureListener(e -> {
                                        //Error when adding ulr in database of user
                                        pd.dismiss();
                                        showToast("Error Updating Image...");
                                    });

                            //if user edit his name, also change it from his posts
                            if (profileOrCoverImage.equals("image")){
                                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                Query query = ref.orderByChild("uid").equalTo(uid);
                                query.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot ds : snapshot.getChildren()){
                                            String child = ds.getKey();
                                            snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                                //update user image in current users comments on post
                                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        for (DataSnapshot ds : snapshot.getChildren()){
                                            String child = ds.getKey();
                                            if (snapshot.child(child).hasChild("Comments")){
                                                String child1 = "" + snapshot.child(child).getKey();
                                                Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments").orderByChild("uid").equalTo(uid);
                                                child2.addValueEventListener(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                        for (DataSnapshot ds : snapshot.getChildren()){
                                                            String child = ds.getKey();
                                                            snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());
                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                    }
                                                });
                                            }
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }

                        } else {
                            pd.dismiss();
                            showToast("An Error Occurred");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    //Error occurred, get and show error message ,dismiss progress dialog
                    pd.dismiss();
                    showToast("" + e.getMessage());
                });
    }

    private void pickFromCamera() {
        //Intent of picked image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pick");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        //put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
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
            uid = user.getUid();
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
        MenuItem item = menu.findItem(R.id.action_search);
        MenuItem item1 = menu.findItem(R.id.action_addPost);
        MenuItem menuItem = menu.findItem(R.id.action_menu);
        menu.findItem(R.id.action_layout).setVisible(false);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called when user press search button
                if (!TextUtils.isEmpty(s)){
                    //search
                    searchMyPosts(s);
                } else {
                    loadMyPosts();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //ca
                if (!TextUtils.isEmpty(s)){
                    //search
                    searchMyPosts(s);
                } else {
                    loadMyPosts();
                }
                return false;
            }
        });
        item1.setOnMenuItemClickListener(item2 -> {
            showDialog();
            return false;
        });
        menuItem.setOnMenuItemClickListener(item2 -> {
            Intent intent = new Intent(getActivity(), SettActivity.class);
            startActivity(intent);
            return false;
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void showDialog() {
        //BottomSheetDialog dialog = new BottomSheetDialog(this);
        Dialog dialog = new Dialog(getActivity());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout postTextLayout = dialog.findViewById(R.id.layoutPostText);
        LinearLayout postImageLayout = dialog.findViewById(R.id.layoutPostImage);
        LinearLayout postVideoLayout = dialog.findViewById(R.id.layoutPostVideo);

        postTextLayout.setOnClickListener(view -> {
            //showToast("Text post is clicked !");
            startActivity(new Intent(getActivity(), AddPostTextActivity.class));
        });

        postImageLayout.setOnClickListener(view -> {
            //showToast("Image post is clicked !");
            startActivity(new Intent(getActivity(), AddPostImageActivity.class));
        });

        postVideoLayout.setOnClickListener(view -> {
            startActivity(new Intent(getActivity(), AddPostVideoActivity.class));
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }

}