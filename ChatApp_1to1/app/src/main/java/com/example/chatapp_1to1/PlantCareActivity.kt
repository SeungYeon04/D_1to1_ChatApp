package com.example.chatapp_1to1

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class PlantCareActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_care)

        // firebace의 임시 데이터 사용 나중엔 유동적으로 받아오기
        val roomId = "ABCD1234"

        // 💧 물 버튼 (이미지 : R.drawable.water_item.png)
        findViewById<ImageButton>(R.id.btnWater).setOnClickListener {
            showItemModal(roomId, R.drawable.water_item, "item.wateritem", isCody = false)
        }

        // ☀️ 햇빛 버튼
        findViewById<ImageButton>(R.id.btnSunlight).setOnClickListener {
            showItemModal(roomId, R.drawable.sun_item, "item.lightitem", isCody = false)
        }

        // 🌿 영양제 버튼
        findViewById<ImageButton>(R.id.btnNutrient).setOnClickListener {
            showItemModal(roomId, R.drawable.nutrient_item, "item.healthitem", isCody = false)
        }

        // 👕 코디 버튼 (예: 모자)
        findViewById<ImageButton>(R.id.btnMore).setOnClickListener {
            showItemModal(roomId, R.drawable.nutrient_item, "item.codyitem", isCody = true)
        }
    }

    private fun showItemModal(roomId: String, iconRes: Int, firebasePath: String, isCody: Boolean) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.md_item_modal)
        dialog.setCancelable(true)

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val itemImage = dialog.findViewById<ImageButton>(R.id.itemimg)
        val itemText = dialog.findViewById<TextView>(R.id.textView)
        val closeBtn = dialog.findViewById<Button>(R.id.backbtn)

        itemImage.setImageResource(iconRes)
        itemText.text = "불러오는 중..."
        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.show()

        // 파이어베이스 데이터 불러오기
        val db = FirebaseFirestore.getInstance()
        val roomRef = db.collection("rooms").document(roomId)

        roomRef.get()
            .addOnSuccessListener { snapshot ->
                if (isCody) {
                    val codyMap = snapshot.get(firebasePath) as? Map<*, *>
                    if (codyMap != null) {
                        val myitem = codyMap["myitem"] as? Boolean ?: false
                        val price = (codyMap["price"] as? Long)?.toInt() ?: 0
                        val wearing = codyMap["wearing"] as? Boolean ?: false

                        itemText.text = when {
                            myitem && wearing -> "착용 중"
                            myitem -> "보유 중"
                            else -> "가격: $price"
                        }
                    } else {
                        itemText.text = "코디 아이템 없음"
                    }
                } else {
                    val count = (snapshot.getLong(firebasePath) ?: 0).toInt()
                    itemText.text = "x $count"
                }
            }
            .addOnFailureListener {
                itemText.text = "파이어베이스 오류 발생"
            }
    }
}
