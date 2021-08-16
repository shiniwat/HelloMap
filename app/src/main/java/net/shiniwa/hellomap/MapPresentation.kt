package net.shiniwa.hellomap

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Display
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.smartdevicelink.streaming.video.SdlRemoteDisplay

class MapPresentation(mContext: Context, mDisplay: Display) : SdlRemoteDisplay(mContext, mDisplay) {
    private val TAG = MapPresentation::class.java.simpleName
    private lateinit var mMap: GoogleMap
    var mMapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.map_view)
        mMapView = findViewById(R.id.mapview) as MapView
        mMapView?.onCreate(savedInstanceState)
        mMapView?.onResume()
        mMapView?.getMapAsync(object: OnMapReadyCallback {
            override fun onMapReady(p0: GoogleMap?) {
                mMap = p0!!
                /*---
                val sydney = LatLng(-34.0, 151.0)
                mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
                mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney)) --*/
                // Let's move to Tokyo
                val tokyo = LatLng(35.652832, 139.839478)
                mMap.addMarker(MarkerOptions().position(tokyo).title("Marker in Tokyo"))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(tokyo, 10.0f))
            }
        })

        Log.d(TAG, "MapPresentation created")
    }

    override fun onViewResized(width: Int, height: Int) {
        //super.onViewResized(width, height)
    }
}