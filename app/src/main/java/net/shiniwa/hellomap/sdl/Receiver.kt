package net.shiniwa.hellomap.sdl

import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartdevicelink.transport.SdlBroadcastReceiver
import com.smartdevicelink.transport.SdlRouterService
import com.smartdevicelink.transport.TransportConstants

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
        ServiceBridge.sendBroadcast(context!!, Intent().setAction(ServiceBridge.SupportedActions.ACTION_STARTPROXY.name))
    }
}