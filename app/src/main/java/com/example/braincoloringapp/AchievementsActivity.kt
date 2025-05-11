package com.example.braincoloringapp

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.view.View

// 1) A simple data class:
data class Achievement(val threshold: Int, val iconViewId: Int, val noteViewId: Int)

// 2) A public, companion‐less list:
val ACHIEVEMENTS = listOf(
    Achievement( 5, R.id.achievement5,  R.id.achievement5_note),
    Achievement(10, R.id.achievement10, R.id.achievement10_note),
    Achievement(20, R.id.achievement20, R.id.achievement20_note),
    Achievement(45, R.id.achievement45, R.id.achievement45_note),
    Achievement(60, R.id.achievement60, R.id.achievement60_note),
    Achievement(80, R.id.achievement80, R.id.achievement80_note),
    Achievement(89, R.id.achievement89, R.id.achievement89_note),
    Achievement(90, R.id.achievement90, R.id.achievement90_note)
)


class AchievementsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)
        val rewiredCount = intent.getIntExtra("rewiredCount", 0)

        fun updateAchievement(id: Int, noteId: Int, unlocked: Boolean) {
            val view = findViewById<TextView>(id)
            val noteView = findViewById<TextView>(noteId)

            if (unlocked) {
                view.setTextColor(Color.parseColor("#4CAF50"))  // Main line green
                noteView.setTextColor(Color.parseColor("#4CAF50"))  // Extra line green
                if (!view.text.contains("✅")) {
                    view.text = view.text.toString() + " ✅"
                }
                noteView.visibility = View.VISIBLE
            } else {
                view.setTextColor(Color.parseColor("#888888"))
                noteView.setTextColor(Color.parseColor("#888888"))
                view.text = view.text.toString().replace(" ✅", "")
                noteView.visibility = View.GONE
            }
        }



        updateAchievement(R.id.achievement5, R.id.achievement5_note, rewiredCount >= 5)
        updateAchievement(R.id.achievement10, R.id.achievement10_note, rewiredCount >= 10)
        updateAchievement(R.id.achievement20, R.id.achievement20_note, rewiredCount >= 20)
        updateAchievement(R.id.achievement45, R.id.achievement45_note, rewiredCount >= 30)
        updateAchievement(R.id.achievement60, R.id.achievement60_note, rewiredCount >= 30)
        updateAchievement(R.id.achievement80, R.id.achievement80_note, rewiredCount >= 30)
        updateAchievement(R.id.achievement89, R.id.achievement89_note, rewiredCount >= 30)
        updateAchievement(R.id.achievement90, R.id.achievement90_note, rewiredCount >= 30)


        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish()  // closes this activity and returns to MainActivity
        }

    }
}