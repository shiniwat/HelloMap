package net.shiniwa.hellomap.logging

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import net.shiniwa.hellomap.R

class LogListAdapter(context: Context,
                     private var mListView: ListView?,
                     private var mItems: MutableList<LogListItem>?) :
    ArrayAdapter<LogListAdapter.LogListItem>(context, R.layout.main_list_item, mItems!!) {
    private val mInfrater: LayoutInflater
    private var mScrollLockManager: ScrollLockManager? = null

    init {
        mInfrater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        mScrollLockManager = ScrollLockManager()
    }

    class LogListItem(time: String, text: String) {
        val mTime: String
        val mMessage: String
        init {
            mTime = time
            mMessage = text
        }
    }

    private class ViewHolder {
        var tvMessage: TextView? = null
        var tvTime: TextView? = null
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val holder: ViewHolder

        if (convertView == null) {
            convertView = mInfrater.inflate(R.layout.main_list_item, null)
            holder = ViewHolder()
            holder.tvMessage = convertView!!.findViewById(R.id.main_list_item_content)
            holder.tvTime = convertView.findViewById(R.id.main_list_item_time)
            convertView.tag = holder
        } else {
            holder = convertView.tag as ViewHolder
        }

        val item = mItems!![position]
        if (item != null) {
            holder.tvMessage!!.text = item.mMessage
            holder.tvTime!!.text = item.mTime
        }
        return convertView
    }

    fun destroy() {
        mItems!!.clear()
        mItems = null
        mScrollLockManager!!.destroy()
        mScrollLockManager = null
        mListView = null
    }

    override fun clear() {
        mItems!!.clear()
        notifyDataSetChanged()
        mScrollLockManager!!.clear()
        super.clear()
    }

    @Synchronized
    fun addLog(items: List<LogListItem>) {
        mItems!!.addAll(items)
        excludeTooManyItems()
        notifyDataSetChanged()
        mScrollLockManager!!.adjustCursor()
    }

    @Synchronized
    fun addLog(item: LogListItem) {
        mItems!!.add(item)
        excludeTooManyItems()
        notifyDataSetChanged()
        mScrollLockManager!!.adjustCursor()
    }

    private fun excludeTooManyItems() {
        var sub = mItems!!.size - LOG_MAX_LINES
        if (sub > 0) {
            // memo : In Java 8 you can use removeIf().
            do {
                mItems!!.removeAt(0)
                sub--
            } while (sub > 0)
        }
    }

    private inner class ScrollLockManager : AbsListView.OnScrollListener {

        private var isLock: Boolean = false

        init {
            mListView!!.setOnScrollListener(this)
            clear()
        }


        fun clear() {
            isLock = true
        }

        fun adjustCursor() {
            if (isLock) {
                mListView!!.setSelection(count)
            }
        }

        fun destroy() {
            mListView!!.setOnScrollListener(null)
        }

        override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

        override fun onScroll(
            view: AbsListView,
            firstVisibleItem: Int,
            visibleItemCount: Int,
            totalItemCount: Int
        ) {
            if (firstVisibleItem + visibleItemCount >= totalItemCount) {
                isLock = true
            } else {
                isLock = false
            }
        }
    }

    companion object {

        private val TAG = LogListAdapter::class.java.simpleName

        private val LOG_MAX_LINES = 500
    }
}