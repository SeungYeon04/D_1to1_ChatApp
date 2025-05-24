
package com.example.chatapp_1to1

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class PlantCareActivity : AppCompatActivity() {

    enum class ItemType {
        WATER, LIGHT, HEALTH, CODY
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_care)

        val roomId = "ABCD1234"

        findViewById<ImageButton>(R.id.btnWater).setOnClickListener {
            showItemModal(roomId, R.drawable.water_item, "item.wateritem", false, ItemType.WATER)
        }

        findViewById<ImageButton>(R.id.btnSunlight).setOnClickListener {
            showItemModal(roomId, R.drawable.sun_item, "item.lightitem", false, ItemType.LIGHT)
        }

        findViewById<ImageButton>(R.id.btnNutrient).setOnClickListener {
            showItemModal(roomId, R.drawable.nutrient_item, "item.healthitem", false, ItemType.HEALTH)
        }

        findViewById<ImageButton>(R.id.btnMore).setOnClickListener {
            showItemModal(roomId, R.drawable.nutrient_item, "item.codyitem", true, ItemType.CODY)
        }
    }

    fun showItemModal(roomId: String, iconRes: Int, firebasePath: String, isCody: Boolean, type: ItemType) {
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
        val itemCountText = dialog.findViewById<TextView>(R.id.itemCountText)
        val closeBtn = dialog.findViewById<Button>(R.id.backbtn)

        itemImage.setImageResource(iconRes)
        itemCountText.text = "불러오는 중..."
        closeBtn.setOnClickListener { dialog.dismiss() }

        dialog.show()

        val db = FirebaseFirestore.getInstance()
        val roomRef = db.collection("rooms").document(roomId)

        roomRef.get().addOnSuccessListener { snapshot ->
            if (!isCody) {
                val count = (snapshot.getLong(firebasePath) ?: 0).toInt()
                itemCountText.text = "x $count"

                itemImage.setOnClickListener {
                    if (count > 0) {
                        when (type) {
                            ItemType.WATER -> playWaterAnimation(iconRes)
                            ItemType.LIGHT -> playSunlightAnimation(iconRes)
                            ItemType.HEALTH -> playNutrientAnimation(iconRes)
                            else -> {}
                        }
                        applyItem(type, roomId)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "아이템이 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                val codyMap = snapshot.get(firebasePath) as? Map<*, *>
                val myitem = codyMap?.get("myitem") as? Boolean ?: false
                val price = (codyMap?.get("price") as? Long)?.toInt() ?: 0
                val wearing = codyMap?.get("wearing") as? Boolean ?: false

                itemCountText.text = when {
                    myitem && wearing -> "착용 중"
                    myitem -> "보유 중"
                    else -> "가격: $price"
                }

                itemImage.setOnClickListener {
                    if (myitem) {
                        Toast.makeText(this, "코디 아이템 사용!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "보유 중인 아이템이 없습니다", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.addOnFailureListener {
            itemCountText.text = "파이어베이스 오류 발생"
        }
    }

    fun playWaterAnimation(iconRes: Int) {
        val view = findViewById<ImageView>(R.id.ivEffect)
        view.setImageResource(iconRes)
        view.rotation = -30f
        view.translationX = -50f
        view.translationY = -150f
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.animate()
            .alpha(1f)
            .translationY(0f)
            .translationX(0f)
            .rotation(0f)
            .setDuration(800)
            .withEndAction {
                view.animate().alpha(0f).setDuration(300).withEndAction {
                    view.visibility = View.GONE
                }
            }
    }

    fun playSunlightAnimation(iconRes: Int) {
        val view = findViewById<ImageView>(R.id.ivEffect)
        view.setImageResource(iconRes)
        view.alpha = 0f
        view.scaleX = 0.5f
        view.scaleY = 0.5f
        view.visibility = View.VISIBLE

        view.animate()
            .alpha(1f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(600)
            .withEndAction {
                view.animate().alpha(0f).setDuration(300).withEndAction {
                    view.visibility = View.GONE
                }
            }
    }

    fun playNutrientAnimation(iconRes: Int) {
        val view = findViewById<ImageView>(R.id.ivEffect)
        view.setImageResource(iconRes)
        view.translationY = 100f
        view.alpha = 0f
        view.visibility = View.VISIBLE

        view.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(500)
            .withEndAction {
                view.animate().alpha(0f).setDuration(300).withEndAction {
                    view.visibility = View.GONE
                }
            }
    }

    fun applyItem(type: ItemType, roomId: String) {
        val db = FirebaseFirestore.getInstance()
        val roomRef = db.collection("rooms").document(roomId)

        val (itemKey, expGain) = when (type) {
            ItemType.WATER -> "item.wateritem" to 1
            ItemType.LIGHT -> "item.lightitem" to 1
            ItemType.HEALTH -> "item.healthitem" to 2
            else -> return
        }

        db.runTransaction { transaction ->
            val snapshot = transaction.get(roomRef)
            val currentCount = (snapshot.getLong(itemKey) ?: 0).toInt()
            val currentExp = (snapshot.getLong("plant.experience") ?: 0).toInt()
            val newExp = currentExp + expGain

            if (currentCount <= 0) throw Exception("아이템 없음")

            transaction.update(roomRef, itemKey, currentCount - 1)
            transaction.update(roomRef, "plant.experience", newExp)
        }.addOnSuccessListener {
            Toast.makeText(this, "아이템 사용 완료!", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(this, "아이템 사용 실패", Toast.LENGTH_SHORT).show()
        }
    }
}
