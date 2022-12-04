package io.rick.socialmedia.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

import io.rick.socialmedia.R;

public class SignUpActivity extends AppCompatActivity {

    private EditText mNameET;
    private EditText mEmailET;
    private EditText mPasswordET;
    private EditText mConfirmPasswordET;
    private TextView signInTXT;
    private MaterialButton mSignUpBtn;
    private ProgressDialog progressDialog;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Create New Account");
        //enable back button
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        mNameET = findViewById(R.id.nameEt);
        mEmailET = findViewById(R.id.emailEt);
        mPasswordET = findViewById(R.id.passwordET);
        mConfirmPasswordET = findViewById(R.id.confirmPasswordEt);
        signInTXT = findViewById(R.id.textSignIn);
        mSignUpBtn = findViewById(R.id.buttonSignUp);

        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering User...");

        setListeners();
    }

    private void setListeners() {
        mSignUpBtn.setOnClickListener(view -> {
            String name = mNameET.getText().toString().trim();
            String email = mEmailET.getText().toString().trim();
            String password = mPasswordET.getText().toString().trim();
            String confirmPassword = mConfirmPasswordET.getText().toString().trim();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                mEmailET.setError("Invalid Email");
                mEmailET.setFocusable(true);
            } else if (password.length() < 6){
                mPasswordET.setError("Password length at least 6 characters");
                mPasswordET.setFocusable(true);
            } else if (name.isEmpty() && password.isEmpty() && confirmPassword.isEmpty() && email.isEmpty()){
                mNameET.setError("Enter Name");
                mNameET.setFocusable(true);
                mEmailET.setError("Enter Email");
                mEmailET.setFocusable(true);
                mPasswordET.setError("Enter Password");
                mPasswordET.setFocusable(true);
                mConfirmPasswordET.setError("Enter Confirm Password");
                mConfirmPasswordET.setFocusable(true);
            }else if (password.length() < 6 && confirmPassword.length() < 6){
                mPasswordET.setError("Password length at least 6 characters");
                mPasswordET.setFocusable(true);
                mConfirmPasswordET.setError("Password length at least 6 characters");
                mConfirmPasswordET.setFocusable(true);
            } else if (!password.equals(confirmPassword)){
                showToast("Password & Confirm Password must be same");
            } else {
                registerUser(email, password, name);
            }
        });

        signInTXT.setOnClickListener(view ->  {
            startActivity(new Intent(getApplicationContext(), SignInActivity.class));
            finish();
        });

    }

    private void registerUser(String email, String password, String name){
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            progressDialog.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();
                            String email = user.getEmail();
                            String uid = user.getUid();
                            //storing info to realtime database using HashMap
                            HashMap<Object, String> hashMap = new HashMap<>();
                            hashMap.put("email", email);
                            hashMap.put("uid", uid);
                            hashMap.put("name", name);//will be add later edit profile
                            hashMap.put("onlineStatus", "online");//will be add later edit profile
                            hashMap.put("typingTo", "noOne");//will be add later edit profile
                            hashMap.put("phone", "");//will be add later edit profile
                            hashMap.put("image", "");//will be add later edit profile
                            hashMap.put("cover", "");//will be add later edit profile

                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                            //Path to store user data named "Users"
                            DatabaseReference reference = database.getReference("Users");
                            //put data written in hashMap in database
                            reference.child(uid).setValue(hashMap);

                            showToast("Registered...\n" + user.getEmail());
                            Intent intent = new Intent(SignUpActivity.this, DashboardActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            progressDialog.dismiss();
                            showToast("Failed");
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        showToast("" + e.getMessage());
                    }
                });
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

}