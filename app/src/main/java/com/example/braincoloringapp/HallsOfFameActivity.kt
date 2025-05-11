package com.example.braincoloringapp

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class HallsOfFameActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_halls_of_fame)

        // pull the full log
        val prefs = getSharedPreferences("HallsOfFamePrefs", Context.MODE_PRIVATE)
        val hofLog = prefs.getString("hof_log", "") ?: ""

        // dump it into the TextView
        findViewById<TextView>(R.id.hallsOfFameMessage).apply {
            text = hofLog
        }
    }
}
