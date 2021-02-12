package net.shiniwa.hellomap.sdl

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.util.Log
import com.smartdevicelink.managers.IRPCMessageListener
import com.smartdevicelink.managers.SdlManager
import com.smartdevicelink.managers.SdlManagerListener
import com.smartdevicelink.managers.file.filetypes.SdlArtwork
import com.smartdevicelink.managers.lifecycle.LifecycleConfigurationUpdate
import com.smartdevicelink.protocol.enums.FunctionID
import com.smartdevicelink.proxy.RPCNotification
import com.smartdevicelink.proxy.RPCRequest
import com.smartdevicelink.managers.lifecycle.OnSystemCapabilityListener
import com.smartdevicelink.proxy.RPCMessage
import com.smartdevicelink.proxy.RPCResponse
import com.smartdevicelink.proxy.rpc.*
import com.smartdevicelink.proxy.rpc.enums.*
import com.smartdevicelink.proxy.rpc.listeners.OnRPCNotificationListener
import com.smartdevicelink.proxy.rpc.listeners.OnRPCRequestListener
import com.smartdevicelink.session.ISdlSessionListener
import com.smartdevicelink.streaming.video.SdlRemoteDisplay
import com.smartdevicelink.streaming.video.VideoStreamingParameters
import com.smartdevicelink.transport.BaseTransportConfig
import com.smartdevicelink.transport.MultiplexTransportConfig
import com.smartdevicelink.transport.TCPTransportConfig
import com.smartdevicelink.transport.enums.TransportType
import com.smartdevicelink.util.Version
import net.shiniwa.hellomap.*
import java.util.*

class SdlService : Service() {

    var sdlManager: SdlManager? = null
    //var transportConfig: BaseTransportConfig? = null
    val FOREGROUND_SERVICE_ID = 111
    val APP_ID = "net.shiniwa.hellomap"
    val APP_NAME = "HelloMap"
    val SDL_IMAGE_FILENAME = "sdl_image.png"
    val ICON_FILENAME = "sdl_icon.png"
    val TAG = "SdlService"
    var hmiCapabilities: HMICapabilities? = null
    var videoStreamingTransportAvailable = false
    var mBridge: ServiceBridge? = null
    var mHandler = Handler(Looper.getMainLooper())
    var mRestarting = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate")
        mBridge = ServiceBridge(applicationContext, object : ServiceBridge.Callback {
            override fun onRequestStartProxy() {
                this@SdlService.startProxy()
            }

            override fun onRestartApp() {
                if (mRestarting) {
                    return
                }
                mRestarting = true
                Log.d(TAG, "onRestartApp")
                // try unregister first
                //val unrai = UnregisterAppInterface()
                //unrai.correlationID = 65530
                //sdlManager?.sendRPC(unrai)
                this@SdlService.disposeSyncProxy()
                //
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    sdlManager = null
                    Log.d(TAG, "about startProxy")
                    this@SdlService.startProxy()
                    mRestarting = false
                }, 2000)
                //
                // restart the service
                //MyApplication.getInstance()?.restartService(applicationContext)
            }
        })
        enterForeground()
    }

    fun enterForeground() {
        Log.d(TAG, "about enterForeground")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                APP_ID,
                "SdlService",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.areNotificationsEnabled()) {
                manager.createNotificationChannel(channel)
                val builder = Notification.Builder(this, channel.id)
                builder.setContentTitle("MyMap's SDL service is about starting...")
                builder.setSmallIcon(R.drawable.ic_sdl)
                val notification = builder.build()
                startForeground(FOREGROUND_SERVICE_ID, notification)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val transportConfig = getTransportConfig()
        if (transportConfig is TCPTransportConfig) {
            startProxy() // startProxy will be called from onSdlEnabled, but we need to do that for TCPTransport
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true)
        }
        sdlManager?.dispose()
        MyApplication.setConnectionState(ProxyStateManager.ProxyConnectionState.NONE)
        mBridge?.unregister()
    }

    fun startProxy() {
        Log.d(TAG, "startProxy got called")
        val transportConfig = getTransportConfig()
        if (transportConfig is MultiplexTransportConfig) {
            transportConfig.transportListener =
                MultiplexTransportConfig.TransportListener { connectedTransports, _, _ ->
                    Log.d(TAG, "onTransportEvent transports=$connectedTransports")
                    var containsUsb = false
                    for (record in connectedTransports) {
                        if (record.type == TransportType.USB) {
                            containsUsb = true
                            break
                        }
                    }
                    if (videoStreamingTransportAvailable != containsUsb) {
                        videoStreamingTransportAvailable = containsUsb
                        // need to restart proxy here.
                        Log.d(TAG, "about restarting app")
                        // BT -> USB case: we need to disposeProxy here,
                        // USB -> BT case: we disposeProxy on USB transport already. So we should NOT dispose proxy on BT transport.
                        if (videoStreamingTransportAvailable) {
                            invokeRestart(0)
                        } else {
                            ServiceBridge.sendBroadcast(
                                applicationContext,
                                Intent().setAction(ServiceBridge.SupportedActions.ACTION_STARTPROXY.name)
                            )
                        }
                    }
                }
        }

        val appTypes = Vector<AppHMIType>(2)
        appTypes.add(AppHMIType.MEDIA)
        appTypes.add(AppHMIType.NAVIGATION)
        val listener = object: SdlManagerListener {
            override fun onStart() {
                Log.d(TAG, "SdlManagerListener.onStart got called")

                // HMI Status Listener
                sdlManager?.addOnRPCNotificationListener(
                    FunctionID.ON_HMI_STATUS,
                    object : OnRPCNotificationListener() {
                        override fun onNotified(notification: RPCNotification) {
                            val onHMIStatus = notification as OnHMIStatus
                            if (onHMIStatus.windowID != null && onHMIStatus.windowID != PredefinedWindows.DEFAULT_WINDOW.value) {
                                return
                            }
                            hmiStatusHandler(notification?.hmiLevel)
                        }
                    })
                sdlManager?.systemCapabilityManager?.getCapability(
                    SystemCapabilityType.DISPLAY,
                    object : OnSystemCapabilityListener {
                        override fun onCapabilityRetrieved(capability: Any?) {
                            val displayCapability = capability as DisplayCapabilities
                            DisplayCapabilityManager.setDisplayCapabilities(
                                applicationContext,
                                displayCapability
                            )
                            val level = sdlManager?.currentHMIStatus?.hmiLevel
                            Log.d(TAG, "Got DisplayCapabilities level=$level")
                            if (level == HMILevel.HMI_NONE) {
                                MyApplication.setConnectionState(ProxyStateManager.ProxyConnectionState.HMI_NONE)
                            }
                        }

                        override fun onError(info: String?) {
                            Log.e(TAG, "onError: $info")
                        }
                    }, false)

                sdlManager?.addOnRPCNotificationListener(
                    FunctionID.ON_TOUCH_EVENT,
                    object : OnRPCNotificationListener() {
                        override fun onNotified(notification: RPCNotification?) {
                            //Log.d(TAG, "OnTouchEvent notified")
                            val touchEvent: OnTouchEvent? = notification as OnTouchEvent
                            //OpenGLPresentation.handleTouchEvent(touchEvent)
                        }
                    })
            }

            override fun onDestroy() {
                Log.d(TAG, "SdlManagerListener.onDestroy")
                MyApplication.setConnectionState(ProxyStateManager.ProxyConnectionState.NONE)
                hmiCapabilities = null
            }

            override fun onError(info: String?, e: Exception?) {
                Log.d(TAG, "SdlManagerListener.onError error=" + info)
            }

            override fun managerShouldUpdateLifecycle(
                language: Language?,
                hmiLanguage: Language?
            ): LifecycleConfigurationUpdate? {
                Log.d(TAG, "managerShouldUpdateLifecycle" + language)
                return null
            }
        }

        val appIcon = SdlArtwork(ICON_FILENAME, FileType.GRAPHIC_PNG, R.mipmap.ic_launcher, true)

        MyApplication.setConnectionState(ProxyStateManager.ProxyConnectionState.PROXY_OPENING)

        // The manager builder sets options for your session
        val builder = SdlManager.Builder(this, APP_ID, APP_NAME, listener)
        builder.setAppTypes(appTypes)
        builder.setTransportType(transportConfig)
        builder.setAppIcon(appIcon)
        sdlManager = builder.build()
        sdlManager?.start(object: IRPCMessageListener {
            override fun onRPCMessage(message: RPCMessage?) {
                message?.functionID.let {
                    when(it) {
                        FunctionID.ON_HMI_STATUS -> {
                            val hmiStatus = message!! as OnHMIStatus
                            Log.d(TAG, "OnHMIStatus level=${hmiStatus.hmiLevel.name}")
                            hmiStatusHandler(hmiStatus.hmiLevel)
                        }
                        FunctionID.REGISTER_APP_INTERFACE -> {
                            if (message?.messageType!!.equals(RPCMessage.KEY_RESPONSE)) {
                                val response = message!! as RPCResponse
                                if (response.success) {
                                } else {
                                    Log.e(TAG, "got RAI error response. result= ${response.resultCode.name}")
                                    //if (response.resultCode.name.contains("APPLICATION_REGISTERED_ALREADY")) {
                                        // @TODO: need to figure out how to get around from this situation.
                                    //}
                                }
                            } else {

                            }
                        }
                        FunctionID.ON_APP_INTERFACE_UNREGISTERED -> {
                            val response = message!! as OnAppInterfaceUnregistered
                            Log.e(TAG, "got ON_APP_INTERFACE_UNREGISTERED; reason = ${response.reason.name}")
                        }
                        else -> {
                            // @TBD
                        }
                    }
                }
            }
        })
        Log.d(TAG, "sdlManager#start() finished")
    }

    fun hmiStatusHandler(hmiLevel: HMILevel) {
        if (hmiCapabilities == null) {
            hmiCapabilities =
                    sdlManager?.registerAppInterfaceResponse?.hmiCapabilities
        }
        when (hmiLevel) {
            HMILevel.HMI_NONE -> MyApplication.setConnectionState(
                    ProxyStateManager.ProxyConnectionState.HMI_NONE
            )
            HMILevel.HMI_LIMITED -> MyApplication.setConnectionState(
                    ProxyStateManager.ProxyConnectionState.HMI_LIMITED
            )
            HMILevel.HMI_FULL -> {
                MyApplication.setConnectionState(ProxyStateManager.ProxyConnectionState.HMI_FULL)
                if (hmiCapabilities?.isVideoStreamingAvailable() == true) {
                    Log.d(TAG, "videoStreaming is available")
                    startVPM(applicationContext)
                } else {
                    Log.d(TAG, "videoStreaming is NOT available")
                    val display = SetDisplayLayout(PredefinedLayout.MEDIA.name)
                    sdlManager?.sendRPC(display)
                    //if (status.firstRun) {
                    sdlManager?.getScreenManager()?.beginTransaction()
                    sdlManager?.getScreenManager()?.textField1 = APP_NAME
                    sdlManager?.getScreenManager()?.textField2 =
                            "Please connect USB cable"
                    sdlManager?.getScreenManager()?.primaryGraphic =
                            SdlArtwork(
                                    SDL_IMAGE_FILENAME,
                                    FileType.GRAPHIC_PNG,
                                    R.drawable.ic_sdl,
                                    true
                            )
                    sdlManager?.getScreenManager()?.commit { success ->
                        if (success) {
                            Log.i(TAG, "welcome show successful")
                        }
                    }
                    //}
                }
            }
            else -> Log.d(TAG, "ON_HMI_STATUS: " + hmiLevel.name)
        }

    }
    fun getTransportConfig(): BaseTransportConfig {
        val pref = getSharedPreferences(TransportConfigActivity.prefName, Context.MODE_PRIVATE)
        val mode = pref.getBoolean(TransportConfigActivity.transportKey, true)
        if (mode) {
            return MultiplexTransportConfig(
                this,
                APP_ID,
                MultiplexTransportConfig.FLAG_MULTI_SECURITY_HIGH
            )
        } else {
            val ipAddr = pref.getString(TransportConfigActivity.addrKey, "127.0.0.1")
            val port = pref.getInt(TransportConfigActivity.portKey, 12345)
            return TCPTransportConfig(port, ipAddr, false)
        }
    }

    inner class LocalBinder : Binder() {
        val service: SdlService
            get() = this@SdlService
    }

    private val mBinder = LocalBinder()
    override fun onBind(intent: Intent?): IBinder? {
        return mBinder
    }

    public fun disposeSyncProxy() {
        sdlManager?.dispose()
    }

    fun startVPM(context: Context) {
        // SetDisplayLayout here.
        var display = SetDisplayLayout(PredefinedLayout.NAV_FULLSCREEN_MAP.name)
        sdlManager?.sendRPC(display)
        if (sdlManager?.videoStreamManager == null) {
            Log.e(TAG, "videoStreamManager is null")
        } else {
            val pref = getSharedPreferences(VdeConfigActivity.prefName, Context.MODE_PRIVATE)
            var parameter = VideoStreamingParameters()
            parameter.isStableFrameRate = pref.getBoolean(
                VdeConfigActivity.useStableFrameRateKey,
                true
            )
            if (parameter.isStableFrameRate) {
                parameter.frameRate = pref.getInt(VdeConfigActivity.frameRateKey, 30)
            }
            if (pref.getBoolean(VdeConfigActivity.isMapPresentationKey, true)) {
                sdlManager?.videoStreamManager?.startRemoteDisplayStream(
                    context,
                    MapPresentation::class.java,
                    parameter,
                    false
                )
            }
        }
    }

    protected fun invokeRestart(delay: Int) {
        Log.d(TAG, "invokeRestart in progress")
        mHandler.postDelayed(Runnable {
            // In this case, we have to manually restart
            ServiceBridge.sendBroadcast(
                this@SdlService,
                Intent().setAction(ServiceBridge.SupportedActions.ACTION_RESTART.name)
            )
        }, delay.toLong())
    }
}