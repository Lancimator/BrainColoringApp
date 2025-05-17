package com.example.stopaddiction

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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
import android.content.SharedPreferences
import android.widget.EditText
import android.view.inputmethod.EditorInfo
import android.text.method.KeyListener
import android.graphics.Rect
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import com.android.billingclient.api.*
import android.widget.NumberPicker
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.view.Gravity
import android.widget.PopupWindow
import android.view.WindowManager
import android.graphics.drawable.ColorDrawable
import android.util.Log
import com.android.billingclient.api.BillingClient.*
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams

private lateinit var supportTab: TextView
private lateinit var supportPanel: View
private lateinit var supportOverlay: View
private lateinit var supportScrim: View
private lateinit var donateButton: Button
private var isBillingReady = false

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
    private lateinit var userNote: EditText
    private var noteKeyListener: KeyListener? = null

    companion object {
        private val DEFAULT_BRAIN_RES_ID = R.drawable.brain_90
        private const val LAST_BRAIN_KEY = "last_brain_res_id"
        private const val USER_NOTE_SUFFIX = "user_note"
    }
    // Billing
    private lateinit var billingClient: BillingClient
    private val skuMap = mapOf(
        1  to "support_1",
        5  to "support_5",
        10  to "support_10",
        100 to "support_100"
    )

    private fun refreshUserNoteField() {
        val noteKey   = keyForImage(USER_NOTE_SUFFIX)
        val savedNote = brainPrefs.getString(noteKey, null)

        if (::userNote.isInitialized) {                      // field exists
            if (savedNote != null) {
                userNote.setText(savedNote)
                userNote.isFocusable = false
            } else {        // ——— FIRST TIME FOR THIS BRAIN ———
                userNote.apply {
                    setText("")                   // clear any previous brain’s text
                    keyListener = noteKeyListener // make editable
                    isFocusable = true
                    isFocusableInTouchMode = true
                }

                // 1) Save & lock when user presses the IME “Done”
                userNote.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        lockAndSaveNote()
                        true
                    } else false
                }

                // 2) …or when they simply tap elsewhere and the field loses focus
                userNote.setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) lockAndSaveNote()
                }
            }

        }
    }

    private fun lockAndSaveNote() {
        val text = userNote.text.toString().trim()
        if (text.isNotEmpty()) {
            brainPrefs.edit()
                .putString(keyForImage(USER_NOTE_SUFFIX), text)
                .apply()
            userNote.apply {              // lock field
                keyListener = null
                isFocusable = false
                isFocusableInTouchMode = false
                clearFocus()
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        // Only handle PURCHASED state
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        // For consumables, consume them so user can repurchase
        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.consumeAsync(consumeParams) { billingResult, outToken ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                // Grant entitlement to the user (e.g. add “support” credit)
                Toast.makeText(this, "Thanks for your support!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(
                    this,
                    "Consumption failed: ${billingResult.debugMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
    
    // ① Declare it here:
    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                Toast.makeText(this, "Purchase canceled", Toast.LENGTH_SHORT).show()
            }
            else -> {
                Toast.makeText(
                    this,
                    "Error ${billingResult.responseCode}: ${billingResult.debugMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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
        fillCounter.text     = "Available rewirings: 0"
        fillTimer.text       = "Next: ${FILL_INTERVAL_SECONDS}s"
        unlockedThresholds.clear()
        updateRank()
        refreshUserNoteField()

    }
    private fun showAmountPicker() {
        val picker = NumberPicker(this).apply {
            minValue = 1
            maxValue = 100          // allow €1-100; change as you like
            wrapSelectorWheel = false
        }

        AlertDialog.Builder(this)
            .setTitle("Choose amount")
            .setView(picker)
            .setPositiveButton("Donate") { _, _ ->
                launchPurchase(picker.value)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun launchPurchase(amount: Int) {
        if (!::billingClient.isInitialized) {
            Toast.makeText(this, "Billing not ready yet", Toast.LENGTH_SHORT).show()
            return
        }
        val sku = skuMap.entries.filter { it.key <= amount }.maxByOrNull { it.key }?.value
        if (sku == null) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(sku)
                        .setProductType(ProductType.INAPP)
                        .build()
                )
            )
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val productDetails = productDetailsList[0]
                val productDetailsParamsList = listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .build()
                )
                val billingFlowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(productDetailsParamsList)
                    .build()
                billingClient.launchBillingFlow(this, billingFlowParams)
            } else {
                Toast.makeText(this, "Failed to fetch donation product", Toast.LENGTH_SHORT).show()
            }
        }
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
    private fun showCongratsPopup() {
        val popupView = layoutInflater.inflate(R.layout.congratulations_popup, null)
        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            true
        )
        popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // 🎆 Start firework animation
        val fireworkView = findViewById<LottieAnimationView>(R.id.fireworkView)
        fireworkView.visibility = View.VISIBLE
        fireworkView.playAnimation()

        // 🎆 Stop firework when popup is dismissed
        popupWindow.setOnDismissListener {
            fireworkView.cancelAnimation()
            fireworkView.visibility = View.GONE
        }

        // 👆 Dismiss popup when background is tapped
        popupView.findViewById<View>(R.id.congratsOverlay).setOnClickListener {
            popupWindow.dismiss()
        }

        val rootView = findViewById<View>(android.R.id.content)
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)
    }



    private fun showInfoPopup() {
        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.info_popup, null)
// --- Get version info dynamically ---
        val pInfo = packageManager.getPackageInfo(packageName, 0)
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            pInfo.versionCode.toLong()
        }
        val versionName = pInfo.versionName

        val versionTextView = popupView.findViewById<TextView>(R.id.versionTextView)
        versionTextView.text = "v$versionName (code $versionCode)"
        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true // focusable = true allows outside touches to dismiss
        )

        // Optional: center popup
        val rootView = findViewById<View>(android.R.id.content)
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)
    }


    /** Hides keyboard if the current touch is outside any EditText. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            currentFocus?.let { current ->
                if (current is EditText) {
                    val outRect = Rect()
                    current.getGlobalVisibleRect(outRect)
                    // touch was outside the EditText?
                    if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                        current.clearFocus()
                        (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                            .hideSoftInputFromWindow(current.windowToken, 0)
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportTab     = findViewById(R.id.supportTab)
        supportOverlay = findViewById(R.id.supportOverlay)
        supportPanel   = findViewById(R.id.supportPanel)
        supportScrim   = findViewById(R.id.supportScrim)

        /* bring overlay to topmost */
        supportOverlay.bringToFront()
// ─── set up Google Play Billing ───
        // 1. Build the BillingClient
        // Disable the Donate button until billing is ready
        val donateButton = findViewById<Button>(R.id.donateButton)
        donateButton.isEnabled = false

        billingClient = BillingClient.newBuilder(this)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    // Mark billing as ready
                    isBillingReady = true
                    // Now re-enable the Donate button
                    donateButton.isEnabled = true
                    Toast.makeText(this@MainActivity, "Billing ready", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Billing setup failed: ${billingResult.debugMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            override fun onBillingServiceDisconnected() {
                isBillingReady = false
                donateButton.isEnabled = false
                Toast.makeText(this@MainActivity, "Billing disconnected", Toast.LENGTH_SHORT).show()
            }
        })

// Wire the click listener (don’t need to re-set it later)
        donateButton.setOnClickListener {
            showAmountPicker()  // inside that, launchPurchase()…
        }


        Toast.makeText(this, "Reached billing logic!", Toast.LENGTH_SHORT).show()

        fun openSupport() {
            supportOverlay.visibility = View.VISIBLE
            supportPanel.scaleX = 0f
            supportPanel.scaleY = 0f
            supportPanel.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(350)
                .start()
        }

        fun closeSupport() {
            supportPanel.animate()
                .scaleX(0f)
                .scaleY(0f)
                .setDuration(250)
                .withEndAction { supportOverlay.visibility = View.GONE }
                .start()
        }

        supportTab.setOnClickListener { openSupport() }
        supportScrim.setOnClickListener { closeSupport() }   // outside-tap closes


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
// --- one-time note field ---
        userNote = findViewById(R.id.userNoteEditText)
        noteKeyListener = userNote.keyListener
        refreshUserNoteField()

        val savedNote = brainPrefs.getString(USER_NOTE_SUFFIX, null)

        if (savedNote != null) {              // NOTE ALREADY SAVED  → lock field
            userNote.apply {
                setText(savedNote)
                keyListener = null            // disables editing
                isFocusable = false
                isFocusableInTouchMode = false
            }
        } else {                              // FIRST TIME FOR THIS BRAIN → editable
            userNote.apply {
                setText("")                   // clear any previous brain’s text
                keyListener = noteKeyListener // restore editing
                isFocusable = true
                isFocusableInTouchMode = true
            }

            userNote.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val text = userNote.text.toString().trim()
                    if (text.isNotEmpty()) {
                        brainPrefs.edit()
                            .putString(keyForImage(USER_NOTE_SUFFIX), text)
                            .apply()

                        // lock immediately after saving
                        userNote.apply {
                            keyListener = null
                            isFocusable = false
                            isFocusableInTouchMode = false
                        }
                    }
                    true
                } else false
            }
        }

// --------------------------------

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
                    R.id.action_info -> {
                        showInfoPopup()
                        true
                    }
                    R.id.brain90 -> {
                        val newResId = R.drawable.brain_90       // <- ❶ target brain

                        // 1) persist the thresholds of the brain we’re leaving
                        brainView.saveFillsOnExit()
                        brainPrefs.edit()
                            .putStringSet(
                                keyForImage(UNLOCKED_THRESHOLDS_KEY),
                                unlockedThresholds.map { it.toString() }.toSet()
                            )
                            .apply()

                        // 2) ***pre-load already-unlocked thresholds for the NEXT brain***
                        val saved = brainPrefs.getStringSet(
                            "${newResId}_${UNLOCKED_THRESHOLDS_KEY}", emptySet()
                        )!!
                        unlockedThresholds.clear()
                        unlockedThresholds.addAll(saved.mapNotNull { it.toIntOrNull() })

                        // 3) now switch bitmaps (this fires `rewiredListener` once)
                        brainView.setBaseImageResource(newResId)
                        brainPrefs.edit().putInt(LAST_BRAIN_KEY, newResId).apply()

                        // 4) tidy UI
                        rewiredStatus.text =
                            "Brain cells rewired: ${brainView.getRewiredCount()}"
                        refreshUserNoteField()
                        updateActionBarTitle()
                        updateRank()
                        true
                    }
                    R.id.brain45 -> {
                        val newResId = R.drawable.brain_45       // <- ❶ target brain

                        // 1) persist the thresholds of the brain we’re leaving
                        brainView.saveFillsOnExit()
                        brainPrefs.edit()
                            .putStringSet(
                                keyForImage(UNLOCKED_THRESHOLDS_KEY),
                                unlockedThresholds.map { it.toString() }.toSet()
                            )
                            .apply()

                        // 2) ***pre-load already-unlocked thresholds for the NEXT brain***
                        val saved = brainPrefs.getStringSet(
                            "${newResId}_${UNLOCKED_THRESHOLDS_KEY}", emptySet()
                        )!!
                        unlockedThresholds.clear()
                        unlockedThresholds.addAll(saved.mapNotNull { it.toIntOrNull() })

                        // 3) now switch bitmaps (this fires `rewiredListener` once)
                        brainView.setBaseImageResource(newResId)
                        brainPrefs.edit().putInt(LAST_BRAIN_KEY, newResId).apply()

                        // 4) tidy UI
                        rewiredStatus.text =
                            "Brain cells rewired: ${brainView.getRewiredCount()}"
                        refreshUserNoteField()
                        updateActionBarTitle()
                        updateRank()
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
                putExtra("brain_res_id",  brainView.getCurrentResId())   // ← ensure this line exists
            }
            startActivity(intent)
        }





        val fillCounter = findViewById<TextView>(R.id.fillCounter)
        val fillTimer = findViewById<TextView>(R.id.fillTimer)

        brainView.setFillListener { available, nextFormatted ->
            fillCounter.text = "Available rewirings: $available"
            fillTimer.text = "Next: $nextFormatted"
        }

        val rewiredStatus = findViewById<TextView>(R.id.rewiredStatus)

        brainView.setRewiredListener { count ->
            rewiredStatus.text = "Brain cells rewired: $count"

            // For each Achievement object…
            achievementsFor(brainView.getCurrentResId()).forEach { achievement ->
                if (count >= achievement.threshold &&
                    unlockedThresholds.add(achievement.threshold)) {

                    showFireworks()

                    // persist per-brain
                    brainPrefs.edit()
                        .putStringSet(
                            keyForImage(UNLOCKED_THRESHOLDS_KEY),
                            unlockedThresholds.map { it.toString() }.toSet()
                        )
                        .apply()
                }
            }
            val lastAchievement = brainView.getCurrentResId().let {
                if (it == R.drawable.brain_45) 45 else 90
            }
            if (count >= lastAchievement ) {
                unlockedThresholds.add(lastAchievement)
                showCongratsPopup()
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
            refreshUserNoteField()
            userNote.keyListener            = noteKeyListener
            userNote.isFocusable            = true
            userNote.isFocusableInTouchMode = true

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
            R.id.action_info -> {
                showInfoPopup()
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
