package com.example.mapapp

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View

import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONObject

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

import com.example.mapapp.decode
import kotlinx.android.synthetic.main.activity_maps.*
import java.math.BigDecimal
import java.math.RoundingMode

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var start: LatLng
    private lateinit var end: LatLng

    private var isHided = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        button.setOnClickListener{
            if (editText.text.isNotEmpty())
            {
                val location = editText.text.toString()
                val url = getURLGeoCoding(location)
                Log.d("TESTURL", url)

                val requestQueue = Volley.newRequestQueue(this)

                val directionsRequest = object : StringRequest(Request.Method.GET, url, Response.Listener<String> {
                        response ->
                    val jsonResponse = JSONObject(response)

                    val result = jsonResponse.getJSONArray("results").getJSONObject(0)
                    val loc = result.getJSONArray("locations").getJSONObject(0)
                    val latLng = loc.getJSONObject("latLng")

                    val lat = latLng.getDouble("lat")
                    val lng = latLng.getDouble("lng")

                    start = LatLng(lat, lng)
                    Log.d("TESTSTART", start.toString())
                    drawMap()
                }, Response.ErrorListener {
                    Log.e("ERROR","That didn't work!")
                    Log.e("ERROR",it.toString())
                }){}
                requestQueue.add(directionsRequest)
            }
        }

        button3.setOnClickListener {
            if (editText2.text.isNotEmpty()) {
                val location = editText2.text.toString()
                val url = getURLGeoCoding(location)
                Log.d("TESTURL", url)

                val requestQueue = Volley.newRequestQueue(this)

                val directionsRequest = object :
                    StringRequest(Request.Method.GET, url, Response.Listener<String> { response ->
                        val jsonResponse = JSONObject(response)

                        val result = jsonResponse.getJSONArray("results").getJSONObject(0)
                        val loc = result.getJSONArray("locations").getJSONObject(0)
                        val latLng = loc.getJSONObject("latLng")

                        val lat = latLng.getDouble("lat")
                        val lng = latLng.getDouble("lng")

                        end = LatLng(lat, lng)
                        Log.d("TESTEND", end.toString())
                        drawMap()
                    }, Response.ErrorListener {
                        Log.e("ERROR", "That didn't work!")
                    }) {}
                requestQueue.add(directionsRequest)
            }
        }

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun getURL(from : LatLng, to : LatLng) : String {
        val start = "start=" + from.longitude + "," + from.latitude
        val end = "end=" + to.longitude + "," + to.latitude
        val key = "api_key=" + getString(R.string.openroute_apikey)
        return "https://api.openrouteservice.org/v2/directions/driving-car?$key&$start&$end"
    }

    private fun getURLGeoCoding(adress: String) : String {
        var location = "location=" + adress.replace(", ", ",")
        val key = "key=" + getString(R.string.mapquest_apikey)
        return "https://open.mapquestapi.com/geocoding/v1/address?$key&$location"
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        drawMap()
    }

    fun drawMap(){
        mMap.clear()
        if(this::start.isInitialized || this::end.isInitialized)
        {
            drawMarkers()
        }
        if(this::start.isInitialized && this::end.isInitialized)
        {
            drawRoute()
        }
    }

    fun drawMarkers()
    {
        if(this::start.isInitialized)
        {
            mMap.addMarker(MarkerOptions().position(start).title("Start"))
        }
        if(this::end.isInitialized)
        {
            mMap.addMarker(MarkerOptions().position(end).title("End"))
        }
    }

    fun drawRoute()
    {
        val options = PolylineOptions()
        options.color(Color.RED)
        options.width(5f)

        val urlDirections = getURL(start, end)

        val requestQueue = Volley.newRequestQueue(this)

        val directionsRequest = object : StringRequest(Request.Method.GET, urlDirections, Response.Listener<String> {
                response ->
            val jsonResponse = JSONObject(response)
            val feature = jsonResponse.getJSONArray("features").getJSONObject(0)
            val summary = feature.getJSONObject("properties").getJSONObject("summary")

            val distance = summary.getDouble("distance")
            if (distance > 1000)
            {
                textView4.text = "Расстояние: " + BigDecimal(distance/1000).setScale(2, RoundingMode.HALF_EVEN).toString() + " км"
            }
            else
            {
                textView4.text = "Расстояние: " + BigDecimal(distance).setScale(2, RoundingMode.HALF_EVEN).toString() + " м"
            }

            val duration = summary.getDouble("duration")
            if (duration > 3600)
            {
                textView5.text = "Время: " + BigDecimal((duration/3600)).setScale(1, RoundingMode.HALF_EVEN).toString() + " ч"
            }
            else
            {
                textView5.text = "Время: " + BigDecimal(duration/60).setScale(1, RoundingMode.HALF_EVEN).toString() + " м"
            }
            val geometry = feature.getJSONObject("geometry")
            val coords = geometry.getJSONArray("coordinates")
            if (coords.length() != 0)
            {
                options.add(start)
                for (i in 0 until coords.length()) {
                    val point = coords.getJSONArray(i)
                    val latLng = LatLng(point[1] as Double, point[0] as Double)

                    options.add(latLng)
                }
                options.add(end)
                mMap.addPolyline(options)
            }
        }, Response.ErrorListener {
            Log.e("ERROR","That didn't work!")
            Log.e("ERROR",it.toString())
        }){}
        requestQueue.add(directionsRequest)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(start))
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_clear -> {
                if(isHided)
                {
                    item.title = "Скрыть"
                    editText.visibility = View.VISIBLE
                    button.visibility = View.VISIBLE
                    editText2.visibility = View.VISIBLE
                    button3.visibility = View.VISIBLE
                    isHided = false
                }
                else
                {
                    item.title = "Показать"
                    editText.visibility = View.GONE
                    button.visibility = View.GONE
                    editText2.visibility = View.GONE
                    button3.visibility = View.GONE
                    isHided = true
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
