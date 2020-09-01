package net.shiniwa.hellomap

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import net.shiniwa.hellomap.logging.Log

class TransportConfigActivity : AppCompatActivity() {
    private val TAG = TransportConfigActivity::class.java.simpleName

    companion object {
        val prefName = "TransportConfig"
        val transportKey = "transportMultiplex"
        val addrKey = "ipadddr"
        val portKey = "port"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.transport_config)
        loadSettings()

        val applyButton = findViewById<Button>(R.id.button_apply)
        applyButton.setOnClickListener {
            Log.d(TAG, "ApplyButton pressed")
            saveSettings()
            finish()
        }
        val radio = findViewById<RadioGroup>(R.id.radio_transports)
        radio.setOnCheckedChangeListener { _, checkedId: Int ->
            val addrEdit = findViewById<EditText>(R.id.addr_text)
            val portEdit = findViewById<EditText>(R.id.port_text)
            when(checkedId) {
                R.id.radioMultiplex -> {
                    addrEdit.isEnabled = false
                    portEdit.isEnabled = false
                }
                R.id.radioTCP -> {
                    addrEdit.isEnabled = true
                    portEdit.isEnabled = true
                }
            }
        }
    }

    private fun loadSettings() {
        val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val multiplexMode = pref.getBoolean(transportKey, true)
        val multiplexButton = findViewById<RadioButton>(R.id.radioMultiplex)
        val tcpButton = findViewById<RadioButton>(R.id.radioTCP)
        val addrEdit = findViewById<EditText>(R.id.addr_text)
        val portEdit = findViewById<EditText>(R.id.port_text)

        if (multiplexMode) {
            multiplexButton.isChecked = true
            addrEdit.isEnabled = false
            portEdit.isEnabled = false
        } else {
            tcpButton.isChecked = true
            addrEdit.isEnabled = true
            portEdit.isEnabled = true
        }

        val addr = pref.getString(addrKey, "127.0.0.1")
        addrEdit.setText(addr)

        val port = pref.getInt(portKey, 12345)
        portEdit.setText(String.format("%d", port))
    }

    private fun saveSettings() {
        val pref = getSharedPreferences(prefName, Context.MODE_PRIVATE)
        val edit = pref.edit()

        val multiplexButton = findViewById<RadioButton>(R.id.radioMultiplex)
        val tcpButton = findViewById<RadioButton>(R.id.radioTCP)
        val addrEdit = findViewById<EditText>(R.id.addr_text)
        val portEdit = findViewById<EditText>(R.id.port_text)

        edit.putBoolean(transportKey, multiplexButton.isChecked)
        edit.putString(addrKey, addrEdit.text.toString())
        edit.putInt(portKey, portEdit.text.toString().toInt())

        edit.commit()
    }
}