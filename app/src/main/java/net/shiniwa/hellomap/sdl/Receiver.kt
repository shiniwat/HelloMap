package net.shiniwa.hellomap.sdl

import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartdevicelink.transport.SdlBroadcastReceiver
import com.smartdevicelink.transport.SdlRouterService
import com.smartdevicelink.transport.TransportConstants
import android.os.Build

import android.app.PendingIntent
import android.app.PendingIntent.CanceledException


class Receiver: SdlBroadcastReceiver() {
    private val TAG = Receiver::class.java.simpleName

    override fun defineLocalSdlRouterClass(): Class<out SdlRouterService> {
        return net.shiniwa.hellomap.sdl.RouterService::class.java
    }

    override fun onReceive(context: Context, intent: Intent?) {
        super.onReceive(context, intent) // Required if overriding this method

        if (intent != null) {
            val action = intent.action
            if (action != null) {
                if (action.equals(TransportConstants.START_ROUTER_SERVICE_ACTION, ignoreCase = true)) {
                    onSdlEnabled(context, intent)
                }
            }
        }
    }

    override fun onSdlEnabled(context: Context?, intent: Intent?) {
        Log.d(TAG, "onSdlEnabled")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent?.getParcelableExtra<PendingIntent>(TransportConstants.PENDING_INTENT_EXTRA)?.let {
                try {
                    Log.d(TAG, "onSdlEnabled, pendingIntent=" + it)
                    //it.send(context, 0, intent)
                    // we still need ACTION_STARTPROXY
                    ServiceBridge.sendBroadcast(context!!, Intent().setAction(ServiceBridge.SupportedActions.ACTION_STARTPROXY.name))
                } catch(e: CanceledException) {
                    e.printStackTrace()
                }
            }
        } else {
            // SdlService needs to be foregrounded in Android O and above
            // This will prevent apps in the background from crashing when they try to start SdlService
            // Because Android O doesn't allow background apps to start background services
            //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //    context!!.startForegroundService(intent)
            //} else {
            //    context!!.startService(intent)
            //}
            ServiceBridge.sendBroadcast(context!!, Intent().setAction(ServiceBridge.SupportedActions.ACTION_STARTPROXY.name))
        }
    }
}