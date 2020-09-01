package net.shiniwa.hellomap

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity

class VdeConfigActivity : AppCompatActivity() {
    private val TAG = VdeConfigActivity::class.java.simpleName

    companion object {
        val prefName = "VdeConfig"
        val useStableFrameRateKey = "useStableFrameRate"
        val frameRateKey = "frameRate"
        val isMapPresentationKey = "isMapPresentation"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vde_config)
        loadSettings()

        val applyButton = findViewById<Button>(R.id.button_apply)
        applyButton.setOnClickListener {
            Log.d(TAG, "about saveSettings")
            saveSettings()
            finish()
        }
    }

    private fun loadSettings() {
        val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val useStableFrameRate = pref.getBoolean(useStableFrameRateKey, true)
        val checkBox = findViewById<CheckBox>(R.id.useStableFramerate)
        val frameEdit = findViewById<EditText>(R.id.frame_rate)
        val isMapPresentation = pref.getBoolean(isMapPresentationKey, true)
        val mapButton = findViewById<RadioButton>(R.id.radioMap)
        val openGLButton = findViewById<RadioButton>(R.id.radioOpenGL)

        checkBox.isChecked = useStableFrameRate
        val frameRate = pref.getInt(frameRateKey, 30)
        frameEdit.setText(String.format("%d", frameRate))
        mapButton.isChecked = isMapPresentation
        openGLButton.isChecked = !isMapPresentation
    }

    private fun saveSettings() {
        val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val edit = pref.edit()

        val checkBox = findViewById<CheckBox>(R.id.useStableFramerate)
        val frameEdit = findViewById<EditText>(R.id.frame_rate)
        val mapButton = findViewById<RadioButton>(R.id.radioMap)
        edit.putBoolean(useStableFrameRateKey, checkBox.isChecked)
        edit.putInt(frameRateKey, frameEdit.text.toString().toInt())
        edit.putBoolean(isMapPresentationKey, mapButton.isChecked)
        edit.commit()
    }
}