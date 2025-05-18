package com.example.chatapp_1to1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class StartActivity : AppCompatActivity() {

    lateinit var startButton : Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start); // start 화면 xml 필요
        enableEdgeToEdge();

        startButton = findViewById<Button>(R.id.startButton);

        startButton.setOnClickListener {

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                Toast.makeText(this, "자동로그인 성공", Toast.LENGTH_SHORT).show();
                var Intent = Intent(applicationContext, PlantCareActivity::class.java)
                startActivity(Intent)
                finish();
            } else {
                var Intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(Intent)
                finish();
            }

        }

    }

}