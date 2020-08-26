package net.shiniwa.hellomap

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.smartdevicelink.transport.SdlBroadcastReceiver
import net.shiniwa.hellomap.logging.Log
import java.util.*
import net.shiniwa.hellomap.logging.LogListAdapter
import net.shiniwa.hellomap.sdl.ProxyStateManager
import net.shiniwa.hellomap.sdl.ServiceBridge

class MainListActivity: AppCompatActivity() {
    val TAG = MainListActivity::class.java.simpleName
    val mHandler: Handler
    var mListAdapter: LogListAdapter? = null
    var mHeader1: TextView? = null
    var mHeader2: TextView? = null
    var mHeader3: TextView? = null

    companion object {
        var sThis: MainListActivity? = null

        fun addLogLine(tag: String, msg: String) {
            addLogLine(0, tag, msg)
        }

        fun addLogLine(time: Long, tag: String, msg: String) {
            sThis?.addLogLineToMainActivity(time, tag, msg)
            Log.e(tag, msg)
        }

        fun onConnectionStateChanged(state: ProxyStateManager.ProxyConnectionState) {
            sThis?.onConnectionStateChangedToMainActivity(state)
        }

        fun onConnectRouter(name: String?) {
            sThis?.onConnectRouter(name)
        }

        fun onNotifyVersion(version: String?) {
            sThis?.onNotifyVersion(version)
        }

        fun getNowTimeString(): String {
            return getTimeString(Calendar.getInstance(), true, true)
        }
        fun getTimeString(cal: Calendar, isSimple: Boolean, isDelimiter: Boolean): String {
            return if (isSimple) {
                String.format(
                    if (isDelimiter) "%02d:%02d:%02d.%03d" else "%02d%02d%02d%03d",
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND),
                    cal.get(Calendar.MILLISECOND)
                )
            } else {
                String.format(
                    if (isDelimiter) "%04d/%02d/%02d %02d:%02d:%02d.%03d" else "%04d%02d%02d%02d%02d%02d%03d",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    cal.get(Calendar.SECOND),
                    cal.get(Calendar.MILLISECOND)
                )
            }
        }
    }

    init {
        mHandler = Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sThis = this
        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) !== PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }
        Log.initiate(true, "/mnt/sdcard/" + getPackageName(),  "logs", "DebugLog_")

        setContentView(R.layout.main_list)

        val lv = findViewById(R.id.main_listview) as ListView
        mListAdapter = LogListAdapter(this, lv, ArrayList())
        lv.setAdapter(mListAdapter)

        mHeader1 = findViewById(R.id.main_header_line1) as TextView
        mHeader2 = findViewById(R.id.main_header_line2) as TextView
        mHeader3 = findViewById(R.id.main_header_line3) as TextView

        SdlBroadcastReceiver.queryForConnectedService(this)
        // also start service
        MyApplication.getInstance()?.bindSdlService(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.my_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_restart) {
            ServiceBridge.sendBroadcast(this, Intent().setAction(ServiceBridge.SupportedActions.ACTION_RESTART.name))
        } else if (item.itemId == R.id.action_config) {
            startActivity(Intent(this, TransportConfigActivity::class.java))
        } else if (item.itemId == R.id.action_vde_config) {
            startActivity(Intent(this, VdeConfigActivity::class.java))
        }
        return super.onOptionsItemSelected(item)
    }

    fun addLogLineToMainActivity(time: Long, tag: String, msg: String) {
        mHandler.post {
            mListAdapter?.addLog(LogListAdapter.LogListItem(getNowTimeString(), msg))
        }
    }

    fun onConnectionStateChangedToMainActivity(state: ProxyStateManager.ProxyConnectionState) {
        mHandler.post { mHeader1?.setText("CONNECTION STATE:  " + state.name) }
    }

    fun onConnectRouter(name: String?) {
        mHeader3?.text = "ROUTER: " + name
    }

    fun onNotifyVersion(version: String?) {
        mHeader2?.text = "SDL Proxy Version = $version"
    }
}