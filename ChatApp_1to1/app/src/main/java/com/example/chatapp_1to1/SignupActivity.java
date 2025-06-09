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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class SignupActivity extends AppCompatActivity {

    private EditText signupTextId;
    private EditText signupPassword;
    private Button btnSignup;
    private TextView loginText;
    private ImageView passwordToggle;
    private boolean isPasswordVisible = false;

    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        signupTextId = findViewById(R.id.SignupTextId);
        signupPassword = findViewById(R.id.SignupPassword);
        btnSignup = findViewById(R.id.btnSignup);
        loginText = findViewById(R.id.login_text);
        passwordToggle = findViewById(R.id.passwordToggle);

        mAuth = FirebaseAuth.getInstance();
        usersRef = FirebaseDatabase.getInstance().getReference("users");

        // 초기 비밀번호 마스킹: ♡
        signupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        signupPassword.setTransformationMethod(new HeartPasswordTransformationMethod());

        // 비밀번호 표시/숨김 토글
        passwordToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    // 비밀번호 숨기기
                    signupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    signupPassword.setTransformationMethod(new HeartPasswordTransformationMethod());
                    passwordToggle.setImageResource(R.drawable.ic_visibility_off);
                } else {
                    // 비밀번호 보이기
                    signupPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    signupPassword.setTransformationMethod(null);
                    passwordToggle.setImageResource(R.drawable.ic_visibility);
                }
                isPasswordVisible = !isPasswordVisible;
                signupPassword.setSelection(signupPassword.getText().length());
            }
        });

        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 회원가입 처리
                String id = signupTextId.getText().toString();
                String password = signupPassword.getText().toString();
                
                if (id.isEmpty() || password.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                mAuth.createUserWithEmailAndPassword(id, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String uid = mAuth.getCurrentUser().getUid();
                                String code = generateRandomCode(12);
                                usersRef.child(uid).setValue(new UserModel(id, password, "캡스톤베이스", code));
                                Toast.makeText(SignupActivity.this, "회원가입 성공!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                finish();
                            } else {
                                Toast.makeText(SignupActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });

        loginText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            }
        });
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randIndex = (int) (Math.random() * chars.length());
            sb.append(chars.charAt(randIndex));
        }
        return sb.toString();
    }
}
