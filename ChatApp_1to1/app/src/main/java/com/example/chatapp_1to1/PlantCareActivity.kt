package com.example.chatapp_1to1

import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class PlantCareActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_care)

        // 버튼 연결하기
        val menuButton = findViewById<ImageButton>(R.id.btnMenu)
        val waterButton = findViewById<ImageButton>(R.id.btnWater)
        val sunlightButton = findViewById<ImageButton>(R.id.btnSunlight)
        val nutrientButton = findViewById<ImageButton>(R.id.btnNutrient)
        val moreButton = findViewById<ImageButton>(R.id.btnMore)
        val speechBubble = findViewById<ImageView>(R.id.ivSpeechBubble)

        // 버튼 클릭 이벤트 설정
        waterButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.water_message), Toast.LENGTH_SHORT).show()
            // 물 주기 기능 구현
        }

        sunlightButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.sunlight_message), Toast.LENGTH_SHORT).show()
            // 햇빛 기능 구현
        }

        nutrientButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.nutrient_message), Toast.LENGTH_SHORT).show()
            // 영양제 기능 구현
        }

        moreButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.more_message), Toast.LENGTH_SHORT).show()
            // 추가 기능 구현
        }

        menuButton.setOnClickListener {
            // 메뉴 기능 구현
            Toast.makeText(this, getString(R.string.menu), Toast.LENGTH_SHORT).show()
        }
    }
} 