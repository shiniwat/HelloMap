package net.shiniwa.hellomap.sdl

class ProxyStateManager(listener: OnChangeStateListener) {
    enum class ProxyConnectionState {
        NONE,
        PROXY_OPENING,
        READY_NONE,
        READY_LIMITED,
        READY_BACKGROUND,
        READY_FULL,
        DISPOSING
    }

    interface OnChangeStateListener {
        fun onConnectionStateChanged(state: ProxyConnectionState, oldState: ProxyConnectionState)
    }

    private var mOnChangeStateListener: OnChangeStateListener? = null
    private var mConnectionState: ProxyConnectionState? = null

    init {
        mOnChangeStateListener = listener
        mConnectionState = ProxyConnectionState.NONE
    }

    fun destroy() {
        mOnChangeStateListener = null
    }

    @Synchronized
    fun setConnectionState(state: ProxyConnectionState) {
        if (mConnectionState != state) {
            if (mOnChangeStateListener != null) {
                mOnChangeStateListener?.onConnectionStateChanged(state, mConnectionState!!)
            }
            mConnectionState = state
        }
    }

    @Synchronized
    fun getConnectionState(): ProxyConnectionState? {
        return mConnectionState
    }
}