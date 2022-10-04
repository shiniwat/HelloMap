package net.shiniwa.hellomap

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.widget.ArrayAdapter
import android.widget.ListView
import com.smartdevicelink.streaming.video.SdlRemoteDisplay

class ListPresentation(context: Context, display: Display) : SdlRemoteDisplay(context, display) {
    private val TAG = ListPresentation::class.java.simpleName
    private val mContext = context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu_list)

        val adapter = ArrayAdapter<String>(mContext, R.layout.menu_list_item, R.id.text_view)
        adapter.add("Tap")
        adapter.add("Double Tap")
        adapter.add("Long Press")
        adapter.add("Drag")
        val listView = findViewById<ListView>(R.id.list_view)
        listView.setOnItemClickListener { parent, view, position, id ->
            Log.d(TAG, "onItemClick: position=$position")
        }
        listView.adapter = adapter
    }

    override fun onViewResized(width: Int, height: Int) {
        Log.d(TAG, "onViewResized($width, $height)")
    }
}