package com.example.chatapp_1to1

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth

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

        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser
                    Log.d("Firebase", "Login success: ${user?.uid}")
                } else {
                    Log.e("Firebase", "Login failed", task.exception)
                }
            }


        // 버튼 클릭 이벤트 설정
        waterButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.water_message), Toast.LENGTH_SHORT).show()
            // 물 주기 기능 구현
            val dialog = BottomSheetDialog(this)
            val view = LayoutInflater.from(this).inflate(R.layout.bottom_water_dialog, null)

            // 이미지 박스들
            val img1 = view.findViewById<ImageView>(R.id.img1)
            val img2 = view.findViewById<ImageView>(R.id.img2)
            val img3 = view.findViewById<ImageView>(R.id.img3)
            val img4 = view.findViewById<ImageView>(R.id.img4)
            val img5 = view.findViewById<ImageView>(R.id.img5)
            val img6 = view.findViewById<ImageView>(R.id.img6)

//            // 임시로 drawable 리소스 설정
//            val sampleImage = R.drawable.sample_plant // drawable에 있는 이미지 사용
//
//            img1.setImageResource(sampleImage)
//            img2.setImageResource(sampleImage)
//            img3.setImageResource(sampleImage)
//            img4.setImageResource(sampleImage)
//            img5.setImageResource(sampleImage)
//            img6.setImageResource(sampleImage)

            // 닫기 버튼
            val btnClose = view.findViewById<ImageView>(R.id.btnClose)
            btnClose.setOnClickListener {
                dialog.dismiss()
            }

            dialog.setContentView(view)
            dialog.show()
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
        speechBubble.setOnClickListener {
            val intent = Intent(this, CodeInputActivity::class.java)
            startActivity(intent)
        }

    }
} 