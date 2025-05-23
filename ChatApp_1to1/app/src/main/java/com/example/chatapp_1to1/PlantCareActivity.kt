package com.example.chatapp_1to1

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction

class PlantCareActivity : AppCompatActivity() {

    enum class ItemType { WATER, LIGHT, HEALTH }

    data class ItemEffect(
        val field: String,
        val amount: Int = -1,
        val expGain: Int = 1
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_care)

        val menuButton = findViewById<ImageButton>(R.id.btnMenu)
        val waterButton = findViewById<ImageButton>(R.id.btnWater)
        val sunlightButton = findViewById<ImageButton>(R.id.btnSunlight)
        val nutrientButton = findViewById<ImageButton>(R.id.btnNutrient)
        val moreButton = findViewById<ImageButton>(R.id.btnMore)
        val speechBubble = findViewById<ImageView>(R.id.ivSpeechBubble)

        val roomId = "ABCD1234" // TODO: 실제 방 ID로 대체하거나 Intent 등으로 받아오기

        waterButton.setOnClickListener {
            applyItem(ItemType.WATER, roomId)
            Toast.makeText(this, getString(R.string.water_message), Toast.LENGTH_SHORT).show()
        }

        sunlightButton.setOnClickListener {
            applyItem(ItemType.LIGHT, roomId)
            Toast.makeText(this, getString(R.string.sunlight_message), Toast.LENGTH_SHORT).show()
        }

        nutrientButton.setOnClickListener {
            applyItem(ItemType.HEALTH, roomId)
            Toast.makeText(this, getString(R.string.nutrient_message), Toast.LENGTH_SHORT).show()
        }

        moreButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.more_message), Toast.LENGTH_SHORT).show()
        }

        menuButton.setOnClickListener {
            Toast.makeText(this, getString(R.string.menu), Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyItem(type: ItemType, roomId: String) {
        val db = FirebaseFirestore.getInstance()
        val roomRef = db.collection("rooms").document(roomId)

        val effects = mapOf(
            ItemType.WATER to ItemEffect("wateritem", -1, 1),
            ItemType.LIGHT to ItemEffect("lightitem", -1, 1),
            ItemType.HEALTH to ItemEffect("healthitem", -1, 2)
        )

        val effect = effects[type] ?: return

        db.runTransaction { tx: Transaction ->
            val snapshot = tx.get(roomRef)
            val currentItem = (snapshot.getLong("item.${effect.field}") ?: 0).toInt()
            val currentExp = (snapshot.getLong("plant.experience") ?: 0).toInt()
            val nowLevel = (snapshot.getLong("plant.nowlevel") ?: 1).toInt()
            val nextLevel = (snapshot.getLong("plant.nextlevel") ?: 10).toInt()

            if (currentItem <= 0) {
                throw Exception("아이템 없음")
            }

            val newExp = currentExp + effect.expGain
            var newLevel = nowLevel
            var newNext = nextLevel

            if (newExp >= nextLevel) {
                newLevel += 1
                newNext += 10 // 예: 다음 레벨업까지 10씩 증가
            }

            tx.update(roomRef, mapOf(
                "item.${effect.field}" to currentItem + effect.amount,
                "plant.experience" to newExp,
                "plant.nowlevel" to newLevel,
                "plant.nextlevel" to newNext
            ))
        }.addOnSuccessListener {
            Log.d("Firestore", "${type.name} 사용 성공 및 경험치 적용 완료")
        }.addOnFailureListener { e ->
            Log.e("Firestore", "아이템 적용 실패", e)
        }
    }
}