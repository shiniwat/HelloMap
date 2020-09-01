package net.shiniwa.hellomap

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View
import com.smartdevicelink.proxy.rpc.*
import com.smartdevicelink.proxy.rpc.enums.TouchType
import java.util.*

// memo: This class does not need to be analyzed
class OnTouchScreenView : View {
    private val mPaint = Paint()
    private val mPaintLine = Paint()
    private val mPaintText = Paint()
    private var mData: HashMap<Int, MutableList<TouchCoordEx>>? = null
    private var mScreenParams: ScreenParams? = null
    private var mViewWidth = 0.0
    private var mViewHeight = 0.0
    private var isInit = false

    inner class TouchCoordEx(var tc: TouchCoord, var time: Long, var id: Int, var type: TouchType) {
        private val timeString: String
            private get() {
                val cal = Calendar.getInstance()
                val divide = cal.timeInMillis / max_ts_value
                cal.timeInMillis = time + divide * max_ts_value
                return getTimeString(cal, true, true)
            }

        private fun getTimeString(cal: Calendar, isSimple: Boolean, isDelimiter: Boolean): String {
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

        fun toAllString(): String {
            return String.format(
                "%s:indx=%d (%d,%d) ts=%s",
                if (type == TouchType.BEGIN) "BEG " else if (type == TouchType.END) "END " else if (type == TouchType.MOVE) "MOV " else if (type == TouchType.CANCEL) "CNL " else "??? ",
                id,
                tc.x, tc.y,
                timeString
            )
        }
    }

    interface OnTouchLogListener {
        fun onTouchLog(tc: List<TouchCoordEx>)
    }

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init()
    }

    private fun init() {
        if (isInit) return
        isInit = true
        mData = HashMap()
        setWillNotDraw(false)
        mPaint.color = Color.argb(0xff, 0xff, 0xff, 0xff)
        mPaint.strokeWidth = 0.5f
        mPaint.style = Paint.Style.STROKE
        mPaintText.style = Paint.Style.FILL_AND_STROKE
        mPaintText.strokeWidth = 1f
        mPaintText.textSize = 15f
        mPaintText.color = Color.argb(0xff, 0x00, 0x00, 0x00)
        mPaintText.isAntiAlias = true
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val DELETE_POINT_MAX = 30
        if (mData!!.size <= 0) {
            Log.e(TAG, "onDraw called with size zero.");
        } else if (mScreenParams == null) {
            Log.e(TAG, "onDraw called, but screenparam is zero");
        } else {

            // Delete too much data
            for (e: Map.Entry<Int, MutableList<TouchCoordEx>> in mData!!.entries) {
                var v: MutableList<TouchCoordEx> = e.value
                var del_count =
                    if (v.size <= DELETE_POINT_MAX) 0 else v.size - DELETE_POINT_MAX
                while (del_count > 0) {
                    v.removeAt(0)
                    del_count--
                }
            }
            for (e: Map.Entry<Int, MutableList<TouchCoordEx>> in mData!!.entries) {
                val v = e.value
                /*
                // Leave the trajectory of points.(optional)
                for (TouchCoordEx te : val) {
                    int x = te.tc.getX();
                    int y = te.tc.getY();
                    canvas.drawCircle(calcScale(x), calcScale(y), 1, mPaint);
                }
                */
                // draw cross cursor.
                Log.d(TAG, "point size=${v.size}")
                val lastPoint = v[v.size - 1]
                run {
                    mPaintLine.setColor(
                        if (lastPoint.type == TouchType.BEGIN) CROSSCURSOR_BEGIN else if (lastPoint.type == TouchType.MOVE) CROSSCURSOR_MOVE else if (lastPoint.type == TouchType.END) CROSSCURSOR_END else if (lastPoint.type == TouchType.CANCEL) CROSSCURSOR_CANCEL else Color.argb(
                            0xff,
                            0xff,
                            0xff,
                            0xff
                        )
                    )
                    val x: Float = java.lang.Float.valueOf(lastPoint.tc.getX().toFloat())
                    if (0.0f <= x && x <= mViewWidth) {
                        canvas.drawLine(x, 0.0f, x, mViewHeight.toFloat(), mPaintLine)
                    }
                    val y: Float = java.lang.Float.valueOf(lastPoint.tc.getY().toFloat())
                    Log.d(TAG, "drawLine: x=$x, y=$y")
                    if (0.0f <= y && y <= mViewHeight) {
                        canvas.drawLine(0.0f, y, mViewWidth.toFloat(), y, mPaintLine)
                    }
                }
            }

            // mPaintText is displayed at the front.
            for (e: Map.Entry<Int, MutableList<TouchCoordEx>> in mData!!.entries) {
                val v = e.value
                val lastPoint = v[v.size - 1]
                run {
                    val x: Float = java.lang.Float.valueOf(lastPoint.tc.getX().toFloat())
                    val y: Float = java.lang.Float.valueOf(lastPoint.tc.getY().toFloat())
                    val pointTxt: String = lastPoint.toAllString()
                    val metrics: Paint.FontMetrics = mPaintText.getFontMetrics()
                    var writeY: Float = y - (metrics.descent + CROSSCURSOR_TEXT_OFFSET)
                    var writeX: Float = x + CROSSCURSOR_TEXT_OFFSET
                    val pointTxtWidth: Float = mPaintText.measureText(pointTxt)
                    val pointTxtHeight: Float = mPaintText.getFontMetrics(null)
                    if (writeX + pointTxtWidth + CROSSCURSOR_TEXT_OFFSET > mViewWidth) {
                        writeX = mViewWidth.toFloat() - (pointTxtWidth + CROSSCURSOR_TEXT_OFFSET)
                    } else if (writeX < 0.0f) {
                        writeX = 0.0f
                    }
                    if (writeY - (pointTxtHeight + CROSSCURSOR_TEXT_OFFSET) < 0.0f) {
                        writeY = y + pointTxtHeight + CROSSCURSOR_TEXT_OFFSET
                    }
                    canvas.drawText(pointTxt, writeX, writeY, mPaintText)
                }
            }
        }
    }

    @Synchronized
    fun onTouchEvent(
        notification: OnTouchEvent?,
        dc: DisplayCapabilities?,
        listener: OnTouchLogListener?
    ) {
        //Log.e(TAG, "onTouchEvent called.");
        val sp = dc?.screenParams
        if (sp != null) {
            if (mScreenParams == null) {
                // first time
                mViewWidth = java.lang.Double.valueOf(measuredWidth.toDouble())
                mViewHeight = java.lang.Double.valueOf(measuredHeight.toDouble())

                /*--
                Log.e(TAG, "onTouchEvent firsttime" +
                        " /mViewWidth:" + mViewWidth +
                        " /mViewHeight:" + mViewHeight +
                        " /hmiWidth:" + Double.valueOf(sp.getImageResolution().getResolutionWidth()) +
                        " /hmiHeight:" + Double.valueOf(sp.getImageResolution().getResolutionHeight()));--*/
            }
            mScreenParams = sp
        }
        if (notification == null) {
            Log.e(TAG, "onTouchEvent ignored 1");
            return
        }
        val type = notification.type
        val events = notification.event
        //Log.d(TAG, "onTouchEvent type=$type")
        if (type == TouchType.BEGIN) {
            val logList: MutableList<TouchCoordEx> = ArrayList()
            for (te: TouchEvent in events) {
                var dlist: MutableList<TouchCoordEx>? = mData?.get(te.id)
                if (dlist == null) {
                    dlist = ArrayList()
                    mData!![te.id] = dlist
                }
                val tcex = TouchCoordEx(
                    te.touchCoordinates[0],
                    te.timestamps[0],
                    te.id, TouchType.BEGIN
                )
                dlist.add(tcex)
                logList.add(tcex)
                mData!![te.id] = dlist
            }
            listener?.onTouchLog(logList)
        } else if (type == TouchType.END) {
            val logList: MutableList<TouchCoordEx> = ArrayList()
            for (te: TouchEvent in events) {
                val dlist: MutableList<TouchCoordEx>? = mData?.get(te.id)
                if (dlist != null) {
                    val tcex = TouchCoordEx(
                        te.touchCoordinates[0],
                        te.timestamps[0],
                        te.id, TouchType.END
                    )
                    dlist.add(tcex)
                    logList.add(tcex)
                    mData!![te.id] = dlist
                }
            }
            listener?.onTouchLog(logList)
        } else if (type == TouchType.MOVE) {
            val logList: MutableList<TouchCoordEx> = ArrayList()
            for (te: TouchEvent in events) {
                var dlist: MutableList<TouchCoordEx>? = mData?.get(te.id)
                if (dlist == null) {
                    dlist = ArrayList()
                    mData!![te.id] = dlist
                }
                val tcSrc = te.touchCoordinates
                val timestamps = te.timestamps
                var i = 0
                for (tc: TouchCoord in tcSrc) {
                    val timestamp = timestamps[i]
                    val tcex = TouchCoordEx(tc, timestamp, te.id, TouchType.MOVE)
                    dlist.add(tcex)
                    logList.add(tcex)
                    i++
                }
                mData!![te.id] = dlist
            }
            listener?.onTouchLog(logList)
        } else if (type == TouchType.CANCEL) {
            val logList: MutableList<TouchCoordEx> = ArrayList()
            for (te: TouchEvent in events) {
                val dlist: MutableList<TouchCoordEx>? = mData?.get(te.id)
                if (dlist != null) {
                    val tcex = TouchCoordEx(
                        te.touchCoordinates[0],
                        te.timestamps[0],
                        te.id, TouchType.CANCEL
                    )
                    dlist.add(tcex)
                    logList.add(tcex)
                    mData!![te.id] = dlist
                }
            }
            listener?.onTouchLog(logList)
        }
        invalidate()
    }

    @Synchronized
    fun release() {
        mData!!.clear()
    }

    companion object {
        private val TAG = OnTouchScreenView::class.java.simpleName
        private val CROSSCURSOR_BEGIN = Color.argb(0xff, 0xff, 0x00, 0x00)
        private val CROSSCURSOR_MOVE = Color.argb(0xff, 0x00, 0xb0, 0xf0)
        private val CROSSCURSOR_CANCEL = Color.argb(0xff, 0xff, 0xff, 0x00)
        private val CROSSCURSOR_END = Color.argb(0xff, 0xa9, 0xd1, 0x8e)
        private val CROSSCURSOR_TEXT_OFFSET = 10.0f
        private val max_ts_value = 2147483647
    }
}