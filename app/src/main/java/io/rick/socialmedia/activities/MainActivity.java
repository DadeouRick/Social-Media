package io.rick.socialmedia.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import io.rick.socialmedia.R;

public class MainActivity extends AppCompatActivity {

    Button buttonSignUp;
    Button buttonSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonSignUp = findViewById(R.id.signUpBotn);
        buttonSignIn = findViewById(R.id.signInBotn);

        buttonSignUp.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));

        buttonSignIn.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), SignInActivity.class)));
    }
}