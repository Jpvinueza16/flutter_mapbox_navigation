package com.dormmom.flutter_mapbox_navigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.NonNull
import com.dormmom.flutter_mapbox_navigation.activity.MyNavigationLauncher
import com.dormmom.flutter_mapbox_navigation.activity.NavigationActivity
import com.dormmom.flutter_mapbox_navigation.activity.WayPointsNavigationActivity
import com.dormmom.flutter_mapbox_navigation.utilities.PluginUtilities
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.Serializable
import java.util.*
import kotlin.collections.HashMap

class FlutterMapboxNavigation(context: Context, activity: Activity, messenger: BinaryMessenger) : MethodChannel.MethodCallHandler, EventChannel.StreamHandler
{
    private var currentActivity: Activity? = activity
    private var currentContext: Context = context
    private var binaryMessenger: BinaryMessenger = messenger


    var _distanceRemaining: Double? = null
    var _durationRemaining: Double? = null

    lateinit var routes : List<DirectionsRoute>
    val EXTRA_ROUTES = "com.example.myfirstapp.MESSAGE"

    private var locationEngine: LocationEngine? = null
    private var mapView: MapView
    private var mapBoxMap: MapboxMap? = null
    private var navigation: MapboxNavigation
    private var currentRoute: DirectionsRoute? = null

    companion object{

        var eventSink:EventChannel.EventSink? = null

        var PERMISSION_REQUEST_CODE: Int = 367

        var origin: Point? = null
        var destination: Point? = null
        var navigationMode =  DirectionsCriteria.PROFILE_DRIVING_TRAFFIC
        var simulateRoute = false
        var navigationLanguage = Locale("en")
        var navigationVoiceUnits = DirectionsCriteria.IMPERIAL
        var zoom = 15.0
        var bearing = 0.0
        var tilt = 0.0

    }

    init {
        val options: MapboxMapOptions = MapboxMapOptions.createFromAttributes(context)
                .compassEnabled(false)
                .logoEnabled(true)
        var accessToken = PluginUtilities.getResourceFromContext(currentContext, "mapbox_access_token")
        Mapbox.getInstance(currentContext, accessToken)
        mapView = MapView(context, options)
        val navigationOptions = MapboxNavigationOptions.Builder()
                .build()
        navigation = MapboxNavigation(
                context,
                Mapbox.getAccessToken()!!,
                navigationOptions
        )
    }


    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result) {

        when(call.method)
        {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "getDistanceRemaining" -> {
                result.success(_distanceRemaining);
            }
            "getDurationRemaining" -> {
                result.success(_durationRemaining);
            }
            "startNavigation" -> {
                startNavigation(call, result, false)
            }
            "startNavigationWithWayPoints" -> {
                startNavigation(call, result, true)

            }
            "startEmbeddedNavigation" -> {
                //startEmbeddedNavigation(call, result)
            }
            "finishNavigation" -> {
                MyNavigationLauncher.stopNavigation(currentActivity)
            }
            else -> result.notImplemented()
        }

    }

    private fun startNavigation(@NonNull call: MethodCall, @NonNull result: MethodChannel.Result, isMultiStop: Boolean)
    {
        var arguments = call.arguments as? Map<String, Any>

        val navMode = arguments?.get("mode") as? String
        if(navMode != null)
        {
            if(navMode == "walking")
                navigationMode = DirectionsCriteria.PROFILE_WALKING;
            else if(navMode == "cycling")
                navigationMode = DirectionsCriteria.PROFILE_CYCLING;
            else if(navMode == "driving")
                navigationMode = DirectionsCriteria.PROFILE_DRIVING;
        }

        val simulated = arguments?.get("simulateRoute") as? Boolean
        if (simulated != null) {
            simulateRoute = simulated
        }

        var language = arguments?.get("language") as? String
        if(language != null)
            navigationLanguage = Locale(language)

        var units = arguments?.get("units") as? String

        if(units != null)
        {
            if(units == "imperial")
                navigationVoiceUnits = DirectionsCriteria.IMPERIAL
            else if(units == "metric")
                navigationVoiceUnits = DirectionsCriteria.METRIC
        }

        if(isMultiStop)
        {

            var wayPoints = arguments?.get("wayPoints") as HashMap<Int, Any>

            val points: MutableList<Point> = mutableListOf()
            for (item in wayPoints)
            {
                val point = item.value as HashMap<*, *>
                val latitude = point["Latitude"] as Double
                val longitude = point["Longitude"] as Double
                points.add(Point.fromLngLat(latitude, longitude))
            }

            val navigationActivity = Intent(currentContext, WayPointsNavigationActivity::class.java)
            navigationActivity.putExtra("waypoints", points as Serializable)
            currentActivity?.startActivity(navigationActivity)//.putExtra("route", currentRoute))
        }
        else
        {
            var originName = arguments?.get("originName") as? String
            val originLatitude = arguments?.get("originLatitude") as? Double
            val originLongitude = arguments?.get("originLongitude") as? Double

            val destinationName = arguments?.get("destinationName") as? String
            val destinationLatitude = arguments?.get("destinationLatitude") as? Double
            val destinationLongitude = arguments?.get("destinationLongitude") as? Double

            if(originLatitude != null && originLongitude != null && destinationLatitude != null && destinationLongitude != null)
            {

                val o = Point.fromLngLat(originLongitude, originLatitude)
                val d = Point.fromLngLat(destinationLongitude, destinationLatitude)

                origin = o
                destination = d

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    var haspermission = currentActivity?.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    if(haspermission != PackageManager.PERMISSION_GRANTED) {
                        //_activity.onRequestPermissionsResult((a,b,c) => onRequestPermissionsResult)
                        currentActivity?.requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSION_REQUEST_CODE)
                        startNavigation(o, d)
                    }
                    else
                        startNavigation(o, d)
                }
                else
                    startNavigation(o, d)


            }
        }

    }

    private fun startNavigation(origin: Point, destination: Point)
    {

        var opt = NavigationRoute.builder(currentContext)
                .accessToken(Mapbox.getAccessToken()!!)
                .origin(origin)
                .destination(destination)
                .profile(navigationMode)
                .language(navigationLanguage)
                .voiceUnits(navigationVoiceUnits)
                .build()
                .getRoute(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {

                        if (response.body() != null) {
                            if (!response.body()!!.routes().isEmpty()) {
                                // Route fetched from NavigationRoute
                                routes = response.body()!!.routes()
                                currentRoute = routes.get(0)

                                // Create a NavigationLauncherOptions object to package everything together
                                val options = NavigationLauncherOptions.builder()
                                        .directionsRoute(currentRoute)
                                        .shouldSimulateRoute(simulateRoute)
                                        .build()

                                // Call this method with Context from within an Activity
                                val navigationIntent = Intent(currentContext, NavigationActivity::class.java)
                                navigationIntent.putExtra("route", currentRoute as Serializable)
                                currentActivity?.startActivity(navigationIntent)
                            }
                        }


                    }

                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {

                    }
                })


    }
/*
    private fun startEmbeddedNavigation(call: MethodCall, result: MethodChannel.Result) {

        var arguments = call.arguments as? Map<String, Any>

        var zoom = arguments?.get("zoom") as? Double
        if(zoom != null) _zoom = zoom

        var bearing = arguments?.get("bearing") as? Double
        if(bearing != null) _bearing = bearing

        var tilt = arguments?.get("tilt") as? Double
        if(tilt != null) _tilt = tilt

        var simulateRoute = arguments?.get("simulateRoute") as Boolean
        if(simulateRoute != null) _simulateRoute = simulateRoute

        startEmbeddedNavigation()

        if (currentRoute != null) {
            result.success("Embedded Navigation started.")
        } else {
            result.success("No route found. Unable to start navigation.")
        }
    }

    private fun startEmbeddedNavigation() {
        isNavigationCanceled = false

        if (currentRoute != null) {
            navigation.addOffRouteListener(this@FlutterMapViewFactory)
            navigation.addFasterRouteListener(this@FlutterMapViewFactory)
            navigation.addProgressChangeListener(this@FlutterMapViewFactory)
            navigation.addMilestoneEventListener(this@FlutterMapViewFactory)
            navigation.addNavigationEventListener(this@FlutterMapViewFactory)

            currentRoute?.let {
                if (_simulateRoute) {
                    (locationEngine as ReplayRouteLocationEngine).assign(it)
                    navigation?.locationEngine = locationEngine as ReplayRouteLocationEngine
                }
                isNavigationInProgress = true
                navigation.startNavigation(it)
            }
        }
    }
*/
    override fun onListen(args: Any?, events: EventChannel.EventSink?) {
        eventSink = events;
    }

    override fun onCancel(args: Any?) {
        eventSink = null;
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            367 -> {

                for (permission in permissions) {
                    if (permission == Manifest.permission.ACCESS_FINE_LOCATION) {
                        var haspermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            currentActivity?.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            TODO("VERSION.SDK_INT < M")
                        }
                        if (haspermission == PackageManager.PERMISSION_GRANTED) {
                            if (origin != null && destination != null)
                                startNavigation(origin!!, destination!!)
                        }
                        // Not all permissions granted. Show some message and return.
                        return
                    }
                }

                // All permissions are granted. Do the work accordingly.
            }
        }
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    }

    


}