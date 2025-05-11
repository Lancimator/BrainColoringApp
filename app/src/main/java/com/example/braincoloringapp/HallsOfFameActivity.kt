package com.example.braincoloringapp

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HallsOfFameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_halls_of_fame)

        // pull stored stats
        val prefs = getSharedPreferences("HallsOfFamePrefs", Context.MODE_PRIVATE)
        val lastFills = prefs.getInt("last_fill_count", 0)
        val lastEndDate = prefs.getString("last_end_date", "") ?: ""

        // if we have real data, show it
        if (lastFills > 0 && lastEndDate.isNotEmpty()) {
            val message = getString(
                R.string.halls_of_fame_message,
                lastFills,
                lastEndDate
            )
            findViewById<TextView>(R.id.hallsOfFameMessage).text = message
        }
    }
}
