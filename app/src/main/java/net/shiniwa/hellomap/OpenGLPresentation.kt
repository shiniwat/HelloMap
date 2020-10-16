package net.shiniwa.hellomap

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.view.Display
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.smartdevicelink.marshal.JsonRPCMarshaller.deserializeJSONObject
import com.smartdevicelink.proxy.rpc.DisplayCapabilities
import com.smartdevicelink.proxy.rpc.OnKeyboardInput
import com.smartdevicelink.proxy.rpc.OnTouchEvent
import com.smartdevicelink.proxy.rpc.PerformInteractionResponse
import com.smartdevicelink.streaming.video.SdlRemoteDisplay
import net.shiniwa.hellomap.OnTouchScreenView.OnTouchLogListener
import net.shiniwa.hellomap.OnTouchScreenView.TouchCoordEx
import net.shiniwa.hellomap.logging.Log
import net.shiniwa.hellomap.sdl.DisplayCapabilityManager
import net.shiniwa.hellomap.sdl.ProxyStateManager
import org.json.JSONException
import org.json.JSONObject
import java.lang.Boolean
import java.util.*

class OpenGLPresentation(context: Context?, display: Display?) : SdlRemoteDisplay(context, display), OnTouchLogListener {
    private var mFrameCounter: Long = 0
    private var mOnTouchScreenViewHU: OnTouchScreenView? = null
    private var mTopLayout: LinearLayout? = null
    private var mTopLine1: TextView? = null
    //private var mHapticLayout: FrameLayout? = null
    private var mTopLine1Text: String? = null
    private var mCenterLine: TextView? = null
    //private var mGetwaypointsLine: TextView? = null
    //private var mGetwaypointsText: String? = null
    //private var mDriverDistractionLine: TextView? = null
    //private var mDriverDistractionText: String? = null
    private var mDisplayCapabilities: DisplayCapabilities? = null
    private var mIsInvalidateFirstTime = false
    private var mStartTime: Long = 0
    private var mVrText: String? = null
    //private var mHapticManager: HapticManager? = null
    //private var mKeyboardManager: KeyboardManager? = null
    private var mBps: Long = 0
    val CAPABILITY_PREFERENCE = "capability_preference"
    val DISPLAY_CAPABILITY_KEY = "display_capabilities"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sThis = this
        mFrameCounter = 0
        mIsInvalidateFirstTime = true
        mStartTime = System.currentTimeMillis()
        super.onCreate(savedInstanceState)
        //Log.d(TAG, "onCreate")
        setContentView(R.layout.presentation_opengl)
        mTopLayout = findViewById(R.id.presentation_opengl_top)
        mTopLine1 = findViewById(R.id.presentation_opengl_toptext_line1)
        mCenterLine = findViewById(R.id.presentation_opengl_centertext)
        mOnTouchScreenViewHU = findViewById(R.id.presentation_opengl_ontouchview)
        //mHapticLayout = findViewById(R.id.presentation_opengl_fr_haptic)
        //mGetwaypointsLine = findViewById(R.id.presentation_opengl_getwaypoints_line)
        //mDriverDistractionLine = findViewById(R.id.presentation_opengl_driver_distraction_line)
        mTopLine1Text = "frame ID "
        //BuildConfig.APP_NAME.toString() + " VERSION " + BuildConfig.VERSION_NAME + "." + BuildConfig.BAMBOO_BUILD_NO + BR +
        //        Util.getPreferenceValueString(context) + " frameID:"
        sIntervalDataSize = 0
    }

    protected fun onInvalidate() {
        ++mFrameCounter
        val durationTime = System.currentTimeMillis() - mStartTime
        val measuredStr: String
        if (durationTime < 1000) {
            measuredStr = "[Measured] FPS:N/A bytes/sec:N/A"
        } else {
            var fps = mFrameCounter.toDouble() / (durationTime.toDouble() / 1000)
            fps = (fps * 100).toInt().toDouble() / 100
            val intervalDiffTime = sIntervalEndTIme - sIntervalStartTIme
            //sIntervalDataSize
            if (intervalDiffTime > 1000 && sIntervalDataSize != 0L) {
                mBps = sIntervalDataSize / (intervalDiffTime / 1000)
                sIntervalDataSize = 0
            }
            var sBps = "N/A"
            if (mBps != 0L) {
                sBps = mBps.toString()
            }
            measuredStr = String.format("[Measured] FPS:%.2f bytes/sec:%s", fps, sBps)
            //Calculate BPS from sTotalDataSize
            //mBps = sTotalDataSize / (durationTime / 1000);
            //measuredStr = String.format("[Measured] FPS:%.2f BPS:%d", fps, mBps);
        }
        var txt = mTopLine1Text + mFrameCounter.toString() + BR + measuredStr
        if (mVrText != null) {
            txt += BR + mVrText
        }
        mTopLine1!!.text = txt
        val timestamp = "HS-TS " + MainListActivity.getNowTimeString()
        //MainListActivity.setTimeStamp(timestamp)
        mCenterLine!!.text = timestamp
        if (mIsInvalidateFirstTime) {
            //MylApplication.setVDEState(ProxyStateManager.ProxyVDEState.PLAYING)
            mIsInvalidateFirstTime = false
        }
    }

    override fun dismiss() {
        //Log.d(TAG, "dismiss")
        sThis = null
        super.dismiss()
        mTopLayout!!.keepScreenOn = false
        if (mOnTouchScreenViewHU != null) {
            mOnTouchScreenViewHU!!.release()
            mOnTouchScreenViewHU = null
        }
        /*--
        if (mHapticManager != null) {
            mHapticManager.destroy()
            mHapticManager = null
        }
        if (mKeyboardManager != null) {
            mKeyboardManager.destroy()
            mKeyboardManager = null
        }
        mHapticLayout!!.removeAllViews()
        --*/
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart()")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            // This is a work-around for an issue found in Android P devices where the top activity
            // is not receiving the keyboard or the BACK_KEY event (AS-2090).  Apparently the
            // android.app.Presentation is being given the "focus" even though it is not on the HS
            // display.
            Log.v(TAG, "Bringing app task to front")
            val mgr = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val appTasks = mgr.appTasks
            if (!appTasks.isEmpty()) {
                val task0 = appTasks[0]
                task0.moveToFront()
            }
        }
    }

    fun setDisplayCapabilities(dc: DisplayCapabilities) {
        val pref = context.getSharedPreferences(CAPABILITY_PREFERENCE, Context.MODE_PRIVATE)
        val edit = pref.edit()
        edit.putString(DISPLAY_CAPABILITY_KEY, dc.serializeJSON().toString(4))
    }

    fun getDisplayCapabilityJSON() : String? {
        val pref = context.getSharedPreferences(CAPABILITY_PREFERENCE, Context.MODE_PRIVATE)
        return pref.getString(DISPLAY_CAPABILITY_KEY, "")
    }

    fun getDisplayCapability() : DisplayCapabilities? {
        val str = getDisplayCapabilityJSON()
        val hash = deserializeJSONObject(JSONObject(str))
        return DisplayCapabilities(hash)
    }

    fun handleTouchEventToPresentation(te: OnTouchEvent?) {
        Log.d(TAG, "handleTouchEventToPresentation")
        if (mDisplayCapabilities == null) {
            mDisplayCapabilities = DisplayCapabilityManager.getDisplayCapabilities(context)
        }
        if (mOnTouchScreenViewHU != null && mDisplayCapabilities != null) {
            mOnTouchScreenViewHU!!.onTouchEvent(te, mDisplayCapabilities, this)
        }
        //if (mKeyboardManager != null) {
        //    mKeyboardManager.handleTouchEvent(proxy, te)
        //}
    }

    fun startEncodingToPresentation() {
        Log.d(TAG, "startEncodingToPresentation")
        mTopLayout!!.keepScreenOn = true
        /*--
        if (mHapticManager == null) {
            // memo: In fact, you should check getIsHapticSpatialDataSupported() in VideoStreamingCapability here, but not for Debugging.
            val log = "Send HapticData."
            Toast.makeText(context, log, Toast.LENGTH_LONG).show()
            Log.d(TAG, log)
            val wh: Point = PreferenceVde.getVideoSize(context, true)
            mHapticManager = HapticManager(context, proxy, wh.x, wh.y)
            val haptic: HapticManager.HapticRectResult = mHapticManager.getHapticRectResult()
            if (haptic != null) {
                for (v in haptic.views) {
                    mHapticLayout!!.addView(v)
                }
            }
        } --*/
    }

    fun stopEncodingToPresentation() {
        Log.d(TAG, "stopEncodingToPresentation")
        stop()
    }

    fun showVrTextToPresentation(text: String) {
        Log.d(TAG, "showVrTextToPresentation /text:$text")
        mVrText = text
        onInvalidate()
    }

    fun sendHapticDynamicToPresentation(hapticNo: Int) {
        Log.d(TAG, "sendHapticDynamicToPresentation /hapticNo:$hapticNo")
        /*--
        if (mHapticManager != null) {
            mHapticManager.destroy()
            mHapticManager = null
        }
        mHapticLayout!!.removeAllViews()
        --*/
        startEncodingToPresentation()
        //if (mKeyboardManager != null) {
        //    mKeyboardManager.onChangeHaptic(context)
        //}
    }

    fun onPerformInteractionResponseToPresentation(response: PerformInteractionResponse?) {
        //if (mKeyboardManager != null) {
        //    mKeyboardManager.onPerformInteractionResponse(response)
        //}
    }

    /*--
    fun onOnKeyboardInputToPresentation(notification: OnKeyboardInput?) {
        //if (mKeyboardManager != null) {
        //    mKeyboardManager.onOnKeyboardInput(notification)
        //}
    } --*/

    /*--
    fun updateGetWayPointsOpenGLPresentation(response: String?) {
        if (response == null) {
            return
        }
        try {
            val json = JSONObject(response)
            if (!json.has("success")) {
                return
            }
            if (!Boolean.valueOf(json.getString("success"))) {
                mGetwaypointsText = "GetWayPoints returned an error"
                return
            } else {
                mGetwaypointsText = "GetWayPoints returned succeeded\n"
            }
            if (json.has("wayPoints")) {
                try {
                    val jArrayWayPoints = json.getJSONArray("wayPoints")
                    for (i in 0 until jArrayWayPoints.length()) {
                        var locationName = "none"
                        var latitude = "none"
                        var longitude = "none"
                        var addressLines = "none"
                        var locationDescription = "none"
                        val jGetwayPoints = jArrayWayPoints.getJSONObject(i)
                        if (jGetwayPoints.has("locationName")) {
                            locationName = jGetwayPoints.getString("locationName")
                        }
                        if (jGetwayPoints.has("coordinate")) {
                            val jCoordinate = jGetwayPoints.getJSONObject("coordinate")
                            if (jCoordinate.has("latitudeDegrees")) {
                                latitude = jCoordinate.getString("latitudeDegrees")
                            }
                            if (jCoordinate.has("longitudeDegrees")) {
                                longitude = jCoordinate.getString("longitudeDegrees")
                            }
                        }
                        if (jGetwayPoints.has("addressLines")) {
                            addressLines = jGetwayPoints.getString("addressLines")
                        }
                        if (jGetwayPoints.has("locationDescription")) {
                            locationDescription = jGetwayPoints.getString("locationDescription")
                        }
                        mGetwaypointsText += """locationName:$locationName
latitude:$latitude longitude:$longitude
addressLines:$addressLines
locationDescription:$locationDescription"""
                        if (i + 1 < jArrayWayPoints.length()) {
                            mGetwaypointsText += "\n"
                        }
                    }
                } catch (e: JSONException) {
                    Log.d(TAG, "updateGetWayPointsOpenGLPresentation /e:" + e.message)
                }
            } else {
                mGetwaypointsText += """
                    locationName:none
                    latitude:none longitude:none
                    addressLines:none
                    locationDescription:none
                    """.trimIndent()
            }
        } catch (e: JSONException) {
            mGetwaypointsText = null
        }
    }

    fun updateDriverDistractionOpenGLPresentation(
        ddStatus: ArrayList<Int>?,
        ddTimes: ArrayList<Double>?
    ) {
        if (ddStatus == null || ddTimes == null) {
            return
        }
        mDriverDistractionText = "Driver Distraction"
        for (i in ddStatus.size downTo 1) {
            var status: String
            status = if (ddStatus[i - 1] == 1) {
                "DD_ON"
            } else if (ddStatus[i - 1] == 0) {
                "DD_OFF"
            } else {
                ddStatus[i - 1].toString()
            }
            mDriverDistractionText += """
$status(${ddTimes[i - 1]} s)"""
        }
        mDriverDistractionLine!!.text = mDriverDistractionText
    }
    --*/

    override fun onTouchLog(tc: List<TouchCoordEx>) {
        MainListActivity.addLogLineOnTouch(tc)
    }

    private fun log(tag: String, msg: String) {
        MainListActivity.addLogLine(System.currentTimeMillis(), tag, msg)
    }

    companion object {
        private val TAG = OpenGLPresentation::class.java.simpleName

        /*
    static interfaces for accessing OpenGLPresentation.
     */
        private var sThis: OpenGLPresentation? = null
        private var sTotalDataSize: Long = 0
        private var sIntervalStartTIme: Long = 0
        private var sIntervalEndTIme: Long = 0
        private var sIntervalDataSize: Long = 0
        fun handleTouchEvent(te: OnTouchEvent?) {
            if (sThis != null) {
                sThis!!.handleTouchEventToPresentation(te)
            }
        }

        fun startEncoding() {
            if (sThis != null) {
                sThis!!.startEncodingToPresentation()
            }
        }

        fun stopEncoding() {
            if (sThis != null) {
                sThis!!.stopEncodingToPresentation()
            }
        }

        fun setTotalDataSize(size: Long) {
            sTotalDataSize = size
        }

        fun intervalVideoDataSize(startTime: Long, endTime: Long, sendDataSize: Long) {
            sIntervalStartTIme = startTime
            sIntervalDataSize = sendDataSize
            sIntervalEndTIme = endTime
        }

        fun showVrText(text: String) {
            if (sThis != null) {
                sThis!!.showVrTextToPresentation(text)
            }
        }

        /*--
        fun sendHapticDynamic(proxy: SdlProxyALM?, hapticNo: Int) {
            if (sThis != null) {
                sThis!!.sendHapticDynamicToPresentation(proxy, hapticNo)
            }
        }

        fun onPerformInteractionResponse(response: PerformInteractionResponse?) {
            if (sThis != null) {
                sThis!!.onPerformInteractionResponseToPresentation(response)
            }
        }

        fun onOnKeyboardInput(notification: OnKeyboardInput?) {
            if (sThis != null) {
                //sThis!!.onOnKeyboardInputToPresentation(notification)
            }
        }

        fun updateGetWayPoints(response: String?) {
            if (sThis != null) {
                //sThis!!.updateGetWayPointsOpenGLPresentation(response)
            }
        }

        fun updateDriverDistraction(ddStatus: ArrayList<Int>?, ddTimes: ArrayList<Double>?) {
            if (sThis != null) {
                //sThis!!.updateDriverDistractionOpenGLPresentation(ddStatus, ddTimes)
            }
        } --*/

        /*
    OpenGLPresentation implements.
     */
        private val BR = System.getProperty("line.separator")
    }
}