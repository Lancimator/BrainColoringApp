package com.example.braincoloringapp

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class AchievementsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)
        val rewiredCount = intent.getIntExtra("rewiredCount", 0)

        fun updateAchievement(id: Int, unlocked: Boolean) {
            val view = findViewById<TextView>(id)
            if (unlocked) {
                view.setTextColor(Color.parseColor("#4CAF50")) // green
                view.text = view.text.toString() + " ✅"
            } else {
                view.setTextColor(Color.parseColor("#888888"))
                view.text = view.text.toString().replace(" ✅", "")
            }
        }

        updateAchievement(R.id.achievement5, rewiredCount >= 5)
        updateAchievement(R.id.achievement10, rewiredCount >= 10)
        updateAchievement(R.id.achievement20, rewiredCount >= 20)

        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            finish()  // closes this activity and returns to MainActivity
        }

    }
}