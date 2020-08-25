package net.shiniwa.hellomap

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.smartdevicelink.debugext.DebugExtension
import net.shiniwa.hellomap.logging.Log
import net.shiniwa.hellomap.sdl.ProxyStateManager
import net.shiniwa.hellomap.sdl.SdlService

class MyApplication : Application(){
    val TAG = MyApplication::class.java.simpleName

    val TAGPREFIX = "[HelloMap]"
    companion object {
        var mApp: MyApplication? = null
        var mStateManager: ProxyStateManager? = null

        public fun getInstance(): MyApplication? {
            return mApp
        }

        public fun setConnectionState(state: ProxyStateManager.ProxyConnectionState) {
            mStateManager?.setConnectionState(state)
        }
    }

    var mService: SdlService? = null

    val mConnection = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            mService = (service as SdlService.LocalBinder).service
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mService = null
        }
    }

    override fun onCreate() {
        super.onCreate()
        mApp = this
        Log.d(TAG, "MyApplication#onCreate")
        DebugExtension.setTagPrefix(TAGPREFIX)
        DebugExtension.setListener(object : DebugExtension.Listener {
            override fun onLog(time: Long, tag: String, msg: String) {
                MainListActivity.addLogLine(time, tag, msg)
                Log.d(TAG, "onLog: " + msg)
            }

            override fun onTotalDataSize(size: Long) {
            }

            override fun onConnectRouter(name: String) {
                Log.d(TAG, "onConnectRouter: " + name)
                MainListActivity.onConnectRouter(name)
            }

            override fun onNotifyVersion(versionInfo: String?) {
                MainListActivity.onNotifyVersion(versionInfo)
            }
        })
        mStateManager = ProxyStateManager(object: ProxyStateManager.OnChangeStateListener {
            override fun onConnectionStateChanged(
                state: ProxyStateManager.ProxyConnectionState,
                oldState: ProxyStateManager.ProxyConnectionState
            ) {
                MainListActivity.onConnectionStateChanged(state)
                val msg = "onConnectionStateChanged " + (oldState?.name ?: "none") + "->" + state.name;
                MainListActivity.addLogLine(TAG, msg)
            }
        })
    }

    public fun getService(): SdlService? {
        return mService
    }

    public fun bindSdlService(context: Context?) {
        val handler = Handler(Looper.getMainLooper())
        handler.post(object: Runnable {
            override fun run() {
                Log.d(TAG, "bindSdlService")
                val intent = Intent(context, SdlService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context?.startForegroundService(intent)
                } else {
                    context?.startService(intent)
                }
                context?.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
            }
        })
    }
}