package net.shiniwa.hellomap.sdl

import android.content.Context
import com.smartdevicelink.marshal.JsonRPCMarshaller
import com.smartdevicelink.proxy.rpc.DisplayCapabilities
import org.json.JSONObject

class DisplayCapabilityManager {
    companion object {
        private val PREF_DISPLAY_CAPABILITY = "display_capability_manager"
        private val DISPLAY_CAPABILITY_KEY = "DisplayCapabilities"

        fun setDisplayCapabilities(context: Context, dc: DisplayCapabilities) {
            val pref = context.getSharedPreferences(PREF_DISPLAY_CAPABILITY, Context.MODE_PRIVATE)
            val edit = pref.edit()
            edit.putString(DISPLAY_CAPABILITY_KEY, dc.serializeJSON().toString(4))
            edit.commit()
        }

        fun getDisplayCapabilities(context: Context) : DisplayCapabilities {
            val pref = context.getSharedPreferences(PREF_DISPLAY_CAPABILITY, Context.MODE_PRIVATE)
            val str = pref.getString(DISPLAY_CAPABILITY_KEY, null)
            val json = JSONObject(str)
            val hash = JsonRPCMarshaller.deserializeJSONObject(json)
            return DisplayCapabilities(hash)
        }
     }
}