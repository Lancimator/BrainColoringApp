package com.example.braincoloringapp

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.widget.TextView
import com.airbnb.lottie.LottieAnimationView
import android.view.View
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import android.content.Intent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    private fun vibratePhone() {
        val vibrator = getSystemService(Vibrator::class.java)
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createOneShot(
                    500, // milliseconds
                    VibrationEffect.DEFAULT_AMPLITUDE // or use 255 for max
                )
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }
    }



    private fun showFireworks() {
        val fireworkView = findViewById<LottieAnimationView>(R.id.fireworkView)
        fireworkView.visibility = View.VISIBLE
        fireworkView.playAnimation()

        vibratePhone() // ✅ Add this line

        fireworkView.postDelayed({
            fireworkView.cancelAnimation()
            fireworkView.visibility = View.GONE
        }, 3000)
    }

    private lateinit var brainView: BrainView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        brainView = findViewById(R.id.brainView)

        val fireworkView = findViewById<LottieAnimationView>(R.id.fireworkView)

        brainView.setCelebrationListener {
            runOnUiThread {
                showFireworks()
            }
        }
        val hallsOfFameButton = findViewById<Button>(R.id.hallsOfFameButton)
        hallsOfFameButton.setOnClickListener {
            // quick sanity check:
           // Toast.makeText(this, "Halls of Fame clicked!", Toast.LENGTH_SHORT).show()

            // then launch the activity
            val intent = Intent(this, HallsOfFameActivity::class.java)
            startActivity(intent)
        }
        findViewById<Button>(R.id.achievementsButton).setOnClickListener {
            val intent = Intent(this, AchievementsActivity::class.java)
            intent.putExtra("rewiredCount", brainView.getRewiredCount())
            startActivity(intent)
        }



        val fillCounter = findViewById<TextView>(R.id.fillCounter)
        val fillTimer = findViewById<TextView>(R.id.fillTimer)

        brainView.setFillListener { fills, secondsLeft ->
            runOnUiThread {
                fillCounter.text = "Available fills: $fills"
                fillTimer.text = "Next fill in: ${secondsLeft}s"
            }
        }
        val rewiredStatus = findViewById<TextView>(R.id.rewiredStatus)

        brainView.setRewiredListener { count ->
            runOnUiThread {
                rewiredStatus.text = "Brain cells rewired: $count out of 90"
            }
        }


        val resetButton = findViewById<Button>(R.id.resetButton)

        resetButton.setOnClickListener {
            // 1) Grab the final fill count and date
            val fills = brainView.getRewiredCount()
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val endDate = dateFormat.format(Date())

            // 2) Build the new entry
            val newEntry = getString(
                R.string.halls_of_fame_message,
                fills,
                endDate
            )

            // 3) Prepend it to the existing log
            val hofPrefs = getSharedPreferences("HallsOfFamePrefs", Context.MODE_PRIVATE)
            val oldLog = hofPrefs.getString("hof_log", "") ?: ""
            val updatedLog = if (oldLog.isNotEmpty()) {
                "$newEntry\n\n$oldLog"
            } else {
                newEntry
            }

            // 4) Save the combined log
            hofPrefs.edit()
                .putString("hof_log", updatedLog)
                .apply()

            // 5) Finally, clear the brain view
            brainView.resetImage()
        }


    }

    override fun onPause() {
        super.onPause()
        brainView.saveFillsOnExit()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_choose_color -> {
                showColorPicker()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showColorPicker() {
        val colors = arrayOf(
            "Red", "Blue", "Green", "Yellow", "Purple",
            "Orange", "Pink", "Cyan", "Gray", "Brown", "Black"
        )

        val colorValues = arrayOf(
            Color.RED,
            Color.BLUE,
            Color.GREEN,
            Color.YELLOW,
            Color.rgb(128, 0, 128), // Purple
            Color.rgb(255, 165, 0), // Orange
            Color.rgb(255, 105, 180), // Pink
            Color.CYAN,
            Color.GRAY,
            Color.rgb(165, 42, 42), // Brown
            Color.BLACK
        )

        AlertDialog.Builder(this)
            .setTitle("Choose a Color")
            .setItems(colors) { _, which ->
                brainView.setSelectedColor(colorValues[which])
            }
            .show()
    }

}
