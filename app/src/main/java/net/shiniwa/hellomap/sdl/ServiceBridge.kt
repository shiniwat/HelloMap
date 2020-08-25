package net.shiniwa.hellomap.sdl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ServiceBridge(context: Context, callback: Callback) : BroadcastReceiver() {
    companion object {
        public fun sendBroadcast(context: Context, intent: Intent) {
            val manager = LocalBroadcastManager.getInstance(context.applicationContext)
            manager.sendBroadcast(intent)
        }
    }
    enum class SupportedActions {
        ACTION_STARTPROXY {
            override fun action(cb: Callback, intent: Intent) {
                cb.onRequestStartProxy()
            }
        },
        ACTION_RESTART {
            override fun action(cb: Callback, intent: Intent) {
                cb.onRestartApp()
            }
        },;

        abstract fun action(cb: Callback, intent: Intent)
    }

    interface Callback {
        abstract fun onRequestStartProxy()
        abstract fun onRestartApp()
    }

    val mCallback: Callback
    val mManager: LocalBroadcastManager
    val mContext: Context

    init {
        mContext = context
        mCallback = callback
        mManager = LocalBroadcastManager.getInstance(mContext)
        val filter = IntentFilter()
        for (n in SupportedActions.values()) {
            filter.addAction(n.name)
        }
        mManager.registerReceiver(this, filter)
    }

    fun unregister() {
        mManager.unregisterReceiver(this)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent != null) {
            val action = intent.action
            if (action != null) {
                for (n in SupportedActions.values()) {
                    if (action.equals(n.name, ignoreCase = true)) {
                        n.action(mCallback, intent)
                        break
                    }
                }
            }
        }
    }
}