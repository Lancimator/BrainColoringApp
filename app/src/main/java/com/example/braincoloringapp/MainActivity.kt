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
import android.content.SharedPreferences


class MainActivity : AppCompatActivity() {
    // keep track of which thresholds we’ve celebrated
    private val unlockedThresholds = mutableSetOf<Int>()
    // these need to be class properties so performHardReset() can see them:
    private lateinit var rewiredStatus: TextView
    private lateinit var fillCounter: TextView
    private lateinit var fillTimer: TextView
    private lateinit var rankTitle: TextView
    private lateinit var rankDesc: TextView
    private lateinit var brainPrefs: SharedPreferences
    private val UNLOCKED_THRESHOLDS_KEY = "unlocked_thresholds"

    companion object {
        private val DEFAULT_BRAIN_RES_ID = R.drawable.brain_90
        private const val LAST_BRAIN_KEY = "last_brain_res_id"
    }


    private fun titleFor(resId: Int): String = when (resId) {
        R.drawable.brain_90 -> getString(R.string.title_brain_90)
        R.drawable.brain_45 -> getString(R.string.title_brain_45)
        else                -> getString(R.string.app_name)
    }

    private fun updateActionBarTitle() {
        val resId = if (::brainView.isInitialized)
            brainView.getCurrentResId()
        else
            DEFAULT_BRAIN_RES_ID

        val title = titleFor(resId)

        supportActionBar?.customView
            ?.findViewById<TextView>(R.id.actionbarTitle)
            ?.text = title

        supportActionBar?.title = title
    }




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

        // Bring to front & fade-in
        fireworkView.apply {
            bringToFront()
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(200).start()
            playAnimation()
        }

        vibratePhone()  // keep the haptic feedback

        // Fade-out and hide after 3 s
        fireworkView.postDelayed({
            fireworkView.cancelAnimation()
            fireworkView.animate().alpha(0f).setDuration(300)
                .withEndAction { fireworkView.visibility = View.GONE }
                .start()
        }, 2000)
    }


    private lateinit var brainView: BrainView

    /** namespace any BrainPrefs key by the current brain image */
    private fun keyForImage(suffix: String): String =
        "${brainView.getCurrentResId()}_$suffix"

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
        brainPrefs = getSharedPreferences("BrainPrefs", MODE_PRIVATE)   // NEW
        rankTitle = findViewById(R.id.rankTitle)
        rankDesc  = findViewById(R.id.rankDesc)
        // existing binding
        brainView = findViewById(R.id.brainView)
// -------- RESTORE LAST BRAIN --------
        val savedResId = brainPrefs.getInt(LAST_BRAIN_KEY, brainView.getCurrentResId())

        if (savedResId != brainView.getCurrentResId()) {
            brainView.setBaseImageResource(savedResId)   // reloads fills & rewired count
        }
// update toolbar title now that we know the brain
        updateActionBarTitle()
// ------------------------------------

        updateActionBarTitle()
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
        updateActionBarTitle()            // ← now safe: uses fallback before brainView is init

        brainView = findViewById(R.id.brainView)   // later call will refresh again

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
                        // 1) snapshot current fills/timer/rewired
                        brainView.saveFillsOnExit()
                        // 2) snapshot current thresholds
                        getSharedPreferences("BrainPrefs", Context.MODE_PRIVATE)
                            .edit()
                            .putStringSet(
                                keyForImage(UNLOCKED_THRESHOLDS_KEY),
                                unlockedThresholds.map { it.toString() }.toSet()
                            )
                            .apply()
                        // 3) switch
                        brainView.setBaseImageResource(R.drawable.brain_90)
                        brainPrefs.edit().putInt(LAST_BRAIN_KEY, R.drawable.brain_90).apply()
                        rewiredStatus.text = "Brain cells rewired: ${brainView.getRewiredCount()}"   // NEW

                        updateActionBarTitle()
                        true
                        // 4) reload this new image’s thresholds
                        runOnUiThread {
                            val prefs = getSharedPreferences("BrainPrefs", Context.MODE_PRIVATE)
                            val saved = prefs.getStringSet(keyForImage(UNLOCKED_THRESHOLDS_KEY), emptySet())!!
                            unlockedThresholds.clear()
                            unlockedThresholds.addAll(saved.mapNotNull { it.toIntOrNull() })
                            updateRank()
                        }
                        true
                    }
                    R.id.brain45 -> {
                        // load the 45-day brain image
                        // 1) snapshot current fills/timer/rewired
                        brainView.saveFillsOnExit()
                        // 2) snapshot current thresholds
                        getSharedPreferences("BrainPrefs", Context.MODE_PRIVATE)
                            .edit()
                            .putStringSet(
                                keyForImage(UNLOCKED_THRESHOLDS_KEY),
                                unlockedThresholds.map { it.toString() }.toSet()
                            )
                            .apply()
                        // 3) switch
                        brainView.setBaseImageResource(R.drawable.brain_45)
                        brainPrefs.edit().putInt(LAST_BRAIN_KEY, R.drawable.brain_45).apply()
                        rewiredStatus.text = "Brain cells rewired: ${brainView.getRewiredCount()}"   // NEW

                        updateActionBarTitle()
                        true
                        updateActionBarTitle()

                        // 4) reload this new image’s thresholds
                        runOnUiThread {
                            val prefs = getSharedPreferences("BrainPrefs", Context.MODE_PRIVATE)
                            val saved = prefs.getStringSet(keyForImage(UNLOCKED_THRESHOLDS_KEY), emptySet())!!
                            unlockedThresholds.clear()
                            unlockedThresholds.addAll(saved.mapNotNull { it.toIntOrNull() })
                            updateRank()
                        }
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
            val intent = Intent(this, HallsOfFameActivity::class.java).apply {
                putExtra("brain_res_id", brainView.getCurrentResId())
            }
            startActivity(intent)
        }
        findViewById<Button>(R.id.achievementsButton).setOnClickListener {
            val intent = Intent(this, AchievementsActivity::class.java).apply {
                putExtra("rewiredCount", brainView.getRewiredCount())
                putExtra("brain_res_id",  brainView.getCurrentResId())   // NEW
            }
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
                    // save this updated set back to prefs, namespaced by image
                    getSharedPreferences("BrainPrefs", Context.MODE_PRIVATE)
                      .edit()
                      .putStringSet(keyForImage(UNLOCKED_THRESHOLDS_KEY),
                        unlockedThresholds.map { it.toString() }.toSet()
                      )
                      .apply()
                }
            }

            // Update your rank or whatever next…
            updateRank()
        }
        // show the value that was loaded from SharedPreferences
        rewiredStatus.text = "Brain cells rewired: ${brainView.getRewiredCount()}"   // NEW





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
            val key = "${brainView.getCurrentResId()}_hof_log"
            val oldLog = hofPrefs.getString(key, "") ?: ""
            val updatedLog = if (oldLog.isNotEmpty()) {
                "$newEntry\n\n$oldLog"
            } else {
                newEntry
            }

            // 4) Save the combined log
            hofPrefs.edit()
                .putString(key, updatedLog)
                .apply()

            // 5) Finally, clear the brain view
            brainView.resetCurrentBrain()

            // 6) Clear in-memory achievements so we go back to “Initiate”
            unlockedThresholds.clear()

            // 7) Force the rank TextViews to re-compute (calls updateRank())
            updateRank()

        }

        // pull in any thresholds we’d unlocked on THIS brain
        val prefs = getSharedPreferences("BrainPrefs", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(keyForImage(UNLOCKED_THRESHOLDS_KEY), emptySet())!!
        unlockedThresholds.clear()
        unlockedThresholds.addAll(saved.mapNotNull { it.toIntOrNull() })
        updateRank()

    }

    override fun onPause() {
        super.onPause()

        // 1) persist per-image data
        brainView.saveFillsOnExit()

        // 2) remember which brain was active
        brainPrefs.edit()
            .putInt(LAST_BRAIN_KEY, brainView.getCurrentResId())
            .putStringSet(
                keyForImage(UNLOCKED_THRESHOLDS_KEY),
                unlockedThresholds.map { it.toString() }.toSet()
            )
            .apply()

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
