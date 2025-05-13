package com.example.braincoloringapp

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val brainResId = intent.getIntExtra("brain_res_id", R.drawable.brain_90)
        val title = when (brainResId) {
            R.drawable.brain_90 -> getString(R.string.title_brain_90)
            R.drawable.brain_45 -> getString(R.string.title_brain_45)
            else                -> getString(R.string.app_name)
        }
        supportActionBar?.title = title

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


    }
    override fun onSupportNavigateUp(): Boolean {
        finish()            // closes this screen
        return true
    }

}