package com.example.chatapp_1to1;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextId, editTextPassword;
    private Button loginBtn;
    private TextView signupMoveText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        editTextId = findViewById(R.id.LoginTextId);
        editTextPassword = findViewById(R.id.LoginTextPassword);
        loginBtn = findViewById(R.id.Loginbtn);
        signupMoveText = findViewById(R.id.signupMove_text);

        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(view -> {
            String email = editTextId.getText().toString().trim();
            String password = editTextPassword.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, StartActivity.class));
                            finish();
                        } else {
                            Toast.makeText(this, "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        signupMoveText.setOnClickListener(v -> {
            startActivity(new Intent(this, SignupActivity.class));
            finish();
        });
    }
}
