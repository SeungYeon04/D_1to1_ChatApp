package com.example.chatapp_1to1;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.example.chatapp_1to1.HeartPasswordTransformationMethod;

public class LoginActivity extends AppCompatActivity {

    private EditText loginTextId;
    private EditText loginTextPassword;
    private Button loginBtn;
    private TextView signupMoveText;
    private ImageView passwordToggle;
    private boolean isPasswordVisible = false;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        loginTextId = findViewById(R.id.LoginTextId);
        loginTextPassword = findViewById(R.id.LoginTextPassword);
        loginBtn = findViewById(R.id.Loginbtn);
        signupMoveText = findViewById(R.id.signupMove_text);
        passwordToggle = findViewById(R.id.passwordToggle);

        mAuth = FirebaseAuth.getInstance();

        // 초기 비밀번호 마스킹: ♡
        loginTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        loginTextPassword.setTransformationMethod(new HeartPasswordTransformationMethod());

        // 비밀번호 표시/숨김 토글
        passwordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // 비밀번호 숨기기
                    loginTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    loginTextPassword.setTransformationMethod(new HeartPasswordTransformationMethod());
                    passwordToggle.setImageResource(R.drawable.ic_visibility_off);
                } else {
                    // 비밀번호 보이기
                    loginTextPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    loginTextPassword.setTransformationMethod(null);
                    passwordToggle.setImageResource(R.drawable.ic_visibility);
                }
                isPasswordVisible = !isPasswordVisible;
                loginTextPassword.setSelection(loginTextPassword.getText().length());
            }
        });

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String id = loginTextId.getText().toString();
                String password = loginTextPassword.getText().toString();

                if (id.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }

                mAuth.signInWithEmailAndPassword(id, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "로그인 성공", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, StartActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        signupMoveText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, SignupActivity.class));
            }
        });
    }
}
