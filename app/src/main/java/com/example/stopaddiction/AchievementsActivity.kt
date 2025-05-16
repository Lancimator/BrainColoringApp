package com.example.stopaddiction

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.view.View

// 1) A simple data class:
data class Achievement(val threshold: Int, val iconViewId: Int, val noteViewId: Int)

// 2) A public, companion‐less list:
// file: AchievementsActivity.kt  (keep near the top, right after the existing data class)
val ACHIEVEMENTS_90 = listOf(
    Achievement( 5,  R.id.achievement5,  R.id.achievement5_note),
    Achievement(10,  R.id.achievement10, R.id.achievement10_note),
    Achievement(20,  R.id.achievement20, R.id.achievement20_note),
    Achievement(25,  R.id.achievement25, R.id.achievement25_note),
    Achievement(35,  R.id.achievement35, R.id.achievement35_note),
    Achievement(44,  R.id.achievement44, R.id.achievement44_note),
    Achievement(45,  R.id.achievement45, R.id.achievement45_note),
    Achievement(60,  R.id.achievement60, R.id.achievement60_note),
    Achievement(80,  R.id.achievement80, R.id.achievement80_note),
    Achievement(89,  R.id.achievement89, R.id.achievement89_note),
    Achievement(90,  R.id.achievement90, R.id.achievement90_note),
)

val ACHIEVEMENTS_45 = listOf(
    Achievement( 5,  R.id.achievement5,  R.id.achievement5_note),
    Achievement( 10,  R.id.achievement10, R.id.achievement10_note),
    Achievement(25,  R.id.achievement25, R.id.achievement25_note),
    Achievement(35,  R.id.achievement35, R.id.achievement35_note),
    Achievement(44,  R.id.achievement44, R.id.achievement44_note),
    Achievement(45,  R.id.achievement45, R.id.achievement45_note),
)



/** helper */
fun achievementsFor(resId:Int) = when (resId) {
    R.drawable.brain_45 -> ACHIEVEMENTS_45
    else                -> ACHIEVEMENTS_90   // 90-day default
}



class AchievementsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_achievements)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val brainResId   = intent.getIntExtra("brain_res_id", R.drawable.brain_90)
        val title = when (brainResId) {
            R.drawable.brain_90 -> getString(R.string.title_brain_90)
            R.drawable.brain_45 -> getString(R.string.title_brain_45)
            else                -> getString(R.string.app_name)
        }
        supportActionBar?.title = title

        val rewiredCount = intent.getIntExtra("rewiredCount", 0)

        val achievements = achievementsFor(brainResId)
// ---------- 2.1: build ACTIVE-ID set ----------
        val activeIds = achievements
            .flatMap { listOf(it.iconViewId, it.noteViewId) }
            .toSet()

        val allAchievementIds = (ACHIEVEMENTS_90 + ACHIEVEMENTS_45)
            .flatMap { listOf(it.iconViewId, it.noteViewId) }
            .toSet()

        allAchievementIds.forEach { id ->
            findViewById<View>(id).visibility =
                if (id in activeIds) View.VISIBLE else View.GONE
        }
// ----------------------------------------------
// hide everything, we will unhide only the active list
        // ---------- 2.2: hide only the OTHER brain’s icons ----------
        listOf(
            R.id.achievement5,  R.id.achievement10, R.id.achievement20, R.id.achievement45,
            R.id.achievement60, R.id.achievement80, R.id.achievement89, R.id.achievement90,
            R.id.achievement5_note, R.id.achievement10_note, R.id.achievement20_note,
            R.id.achievement45_note, R.id.achievement60_note, R.id.achievement80_note,
            R.id.achievement89_note, R.id.achievement90_note
        ).forEach { id ->
            findViewById<View>(id).visibility =
                if (id in activeIds) View.VISIBLE else View.GONE
        }
// ------------------------------------------------------------


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

        achievements.forEach { ach ->
            updateAchievement(
                ach.iconViewId,
                ach.noteViewId,
                rewiredCount >= ach.threshold
            )
        }



    }
    override fun onSupportNavigateUp(): Boolean {
        finish()            // closes this screen
        return true
    }

}