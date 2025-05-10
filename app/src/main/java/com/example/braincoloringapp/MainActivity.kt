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

class MainActivity : AppCompatActivity() {

    private lateinit var brainView: BrainView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the toolbar for the menu
        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        brainView = findViewById(R.id.brainView)
        val fillCounter = findViewById<TextView>(R.id.fillCounter)
        val fillTimer = findViewById<TextView>(R.id.fillTimer)

        brainView.setFillListener { fills, secondsLeft ->
            runOnUiThread {
                fillCounter.text = "Fills: $fills"
                fillTimer.text = "Next fill in: ${secondsLeft}s"
            }
        }
        val resetButton = findViewById<Button>(R.id.resetButton)

        resetButton.setOnClickListener {
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
