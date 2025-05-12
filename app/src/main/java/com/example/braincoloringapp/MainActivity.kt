package com.example.braincoloringapp

import com.example.braincoloringapp.ACHIEVEMENTS
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
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import com.example.braincoloringapp.FILL_INTERVAL_SECONDS

class MainActivity : AppCompatActivity() {
    // keep track of which thresholds we’ve celebrated
    private val unlockedThresholds = mutableSetOf<Int>()
    // these need to be class properties so performHardReset() can see them:
    private lateinit var rewiredStatus: TextView
    private lateinit var fillCounter: TextView
    private lateinit var fillTimer: TextView
    private lateinit var rankTitle: TextView
    private lateinit var rankDesc: TextView

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

    private fun performHardReset() {
        // a) Clear all BrainPrefs (fills, rewired count, timers, saved colors, etc)
        getSharedPreferences("BrainPrefs", Context.MODE_PRIVATE)
            .edit().clear().apply()

        // b) Clear Halls of Fame log
        getSharedPreferences("HallsOfFamePrefs", Context.MODE_PRIVATE)
            .edit().clear().apply()

        // c) Clear any in-memory milestones so fireworks can re-fire
        unlockedThresholds.clear()

        // d) Reset the view and UI
        brainView.resetImage()
        rewiredStatus.text = "Brain cells rewired: 0"
        fillCounter.text     = "Available fills: 0"
        fillTimer.text       = "Next fill in: ${FILL_INTERVAL_SECONDS}s"
        unlockedThresholds.clear()
        updateRank()
    }

    /**
     * Chooses a rank string based on how many achievements we’ve unlocked
     * (0→Initiate, 1→Apprentice, …, 7→Deity; if >7, stays at Deity).
     */
    private fun updateRank() {
        // load the full “Rank – Description” strings
        val ranks = resources.getStringArray(R.array.ranks)
        val count = unlockedThresholds.size
        val index = count.coerceIn(0, ranks.size - 1)
        val fullText = ranks[index]

        // split on the dash “–”
        val parts = fullText.split("–", limit = 2)
        val titleText = parts[0].trim()         // e.g. “Initiate”
        val descText  = if (parts.size > 1)
            parts[1].trim()    // e.g. “Just getting started…”
        else
            ""

        rankTitle.text = titleText
        rankDesc.text  = descText
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rankTitle = findViewById(R.id.rankTitle)
        rankDesc  = findViewById(R.id.rankDesc)
        // existing binding
        brainView = findViewById(R.id.brainView)

// new bindings
        rewiredStatus = findViewById(R.id.rewiredStatus)
        fillCounter    = findViewById(R.id.fillCounter)
        fillTimer      = findViewById(R.id.fillTimer)

        brainView = findViewById(R.id.brainView)

        val fireworkView = findViewById<LottieAnimationView>(R.id.fireworkView)

        brainView.setCelebrationListener {
            runOnUiThread {
                showFireworks()
            }
        }
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)
            setDisplayShowCustomEnabled(true)
            setCustomView(R.layout.actionbar_custom_title)
        }
        // 2) Find the menu icon and attach a PopupMenu
        val menuIcon = supportActionBar?.customView
            ?.findViewById<ImageView>(R.id.menuIcon)
        menuIcon?.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menuInflater.inflate(R.menu.toolbar_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_settings -> {
                        // TODO: launch settings screen
                        true
                    }
                    R.id.brain90 -> {
                        // load the 90-day brain image
                        brainView.setBaseImageResource(R.drawable.brain_90)
                        true
                    }
                    R.id.brain45 -> {
                        // load the 45-day brain image
                        brainView.setBaseImageResource(R.drawable.brain_45)
                        true
                    }
                    R.id.action_hard_reset -> {
                        // 1) Show confirmation dialog
                        AlertDialog.Builder(this)
                            .setTitle("Hard Reset")
                            .setMessage("Are you sure you want to reset everything? It resets Halls of Fame, Achievements, current Status")
                            .setPositiveButton("Yes") { dialog, which ->
                                performHardReset()
                            }
                            .setNegativeButton("No", null)
                            .show()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
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
            rewiredStatus.text = "Brain cells rewired: $count"

            // For each Achievement object…
            ACHIEVEMENTS.forEach { achievement ->
                // Compare against its threshold, and if newly unlocked, show fireworks
                if (count >= achievement.threshold && unlockedThresholds.add(achievement.threshold)) {
                    showFireworks()
                }
            }

            // Update your rank or whatever next…
            updateRank()
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
            brainView.resetCurrentBrain()

            // 6) Clear in-memory achievements so we go back to “Initiate”
            unlockedThresholds.clear()

            // 7) Force the rank TextViews to re-compute (calls updateRank())
            updateRank()

        }

        unlockedThresholds.clear()
        updateRank()

// show the initial rank (probably “Initiate”)
        updateRank()

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
