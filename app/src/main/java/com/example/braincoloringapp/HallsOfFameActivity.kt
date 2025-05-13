package com.example.braincoloringapp

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HallsOfFameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_halls_of_fame)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // 1) which brain are we showing?
        val resId = intent.getIntExtra("brain_res_id", R.drawable.brain_90)
        val title = when (resId) {
            R.drawable.brain_90 -> getString(R.string.title_brain_90)
            R.drawable.brain_45 -> getString(R.string.title_brain_45)
            else                -> getString(R.string.app_name)
        }
        supportActionBar?.title = title

        // 2) pull only that brain’s log
        val prefs = getSharedPreferences("HallsOfFamePrefs", Context.MODE_PRIVATE)
        val hofKey = "${resId}_hof_log"
        val hofLog = prefs.getString(hofKey, "") ?: ""

        // 3) display
        findViewById<TextView>(R.id.hallsOfFameMessage).text = hofLog
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
