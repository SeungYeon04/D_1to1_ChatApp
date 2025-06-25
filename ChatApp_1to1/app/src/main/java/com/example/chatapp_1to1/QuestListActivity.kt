package com.example.chatapp_1to1

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class QuestListActivity : AppCompatActivity() {

    private lateinit var btnclose: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quest_list)

        btnclose = findViewById(R.id.closequest_button)

        btnclose.setOnClickListener {
            var Intent = Intent(applicationContext, PlantCareActivity::class.java)
            startActivity(Intent)
            finish()
        }
    }

}