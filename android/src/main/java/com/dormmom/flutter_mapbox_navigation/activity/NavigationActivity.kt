package com.dormmom.flutter_mapbox_navigation.activity

import android.location.Location
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.dormmom.flutter_mapbox_navigation.FlutterMapboxNavigation

import com.dormmom.flutter_mapbox_navigation.R
import com.dormmom.flutter_mapbox_navigation.models.MapBoxEvents
import com.dormmom.flutter_mapbox_navigation.models.MapBoxLocation
import com.dormmom.flutter_mapbox_navigation.models.MapBoxMileStone
import com.dormmom.flutter_mapbox_navigation.models.MapBoxRouteProgressEvent
import com.dormmom.flutter_mapbox_navigation.utilities.PluginUtilities

import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.services.android.navigation.ui.v5.NavigationView
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback
import com.mapbox.services.android.navigation.ui.v5.listeners.BannerInstructionsListener
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement
import com.mapbox.services.android.navigation.v5.milestone.Milestone
import com.mapbox.services.android.navigation.v5.milestone.MilestoneEventListener
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener
import com.mapbox.services.android.navigation.v5.route.FasterRouteListener
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress


class NavigationActivity : AppCompatActivity(), OnNavigationReadyCallback,
        ProgressChangeListener,
        OffRouteListener,
        MilestoneEventListener,
        NavigationEventListener,
        NavigationListener,
        FasterRouteListener,
        SpeechAnnouncementListener,
        BannerInstructionsListener,
        RouteListener {

    private var navigationView: NavigationView? = null
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private val route by lazy { intent.getSerializableExtra("route") as DirectionsRoute }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AppCompat_NoActionBar)
        setContentView(R.layout.activity_navigation)
        navigationView = findViewById(R.id.navigationView)

        navigationView?.onCreate(savedInstanceState)
        navigationView?.initialize(
                this,
                getInitialCameraPosition()
        )
    }

    override fun onLowMemory() {
        super.onLowMemory()
        navigationView?.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        navigationView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        navigationView?.onResume()
    }

    override fun onStop() {
        super.onStop()
        navigationView?.onStop()
    }

    override fun onPause() {
        super.onPause()
        navigationView?.onPause()
    }

    override fun onDestroy() {
        navigationView?.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // If the navigation view didn't need to do anything, call super
        if (!navigationView?.onBackPressed()!!) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        navigationView?.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        navigationView?.onRestoreInstanceState(savedInstanceState)
    }

    override fun onNavigationReady(isRunning: Boolean) {
        if (!isRunning && !::navigationMapboxMap.isInitialized) {
            navigationView?.retrieveNavigationMapboxMap()?.let { navMapboxMap ->
                this.navigationMapboxMap = navMapboxMap
                this.navigationMapboxMap.updateLocationLayerRenderMode(RenderMode.NORMAL)
                navigationView?.retrieveMapboxNavigation()?.let {
                    this.mapboxNavigation = it

                    mapboxNavigation.addOffRouteListener(this)
                    mapboxNavigation.addFasterRouteListener(this)
                    mapboxNavigation.addNavigationEventListener(this)
                }

                val optionsBuilder = NavigationViewOptions.builder()
                optionsBuilder.progressChangeListener(this)
                optionsBuilder.milestoneEventListener(this)
                optionsBuilder.navigationListener(this)
                optionsBuilder.speechAnnouncementListener(this)
                optionsBuilder.bannerInstructionsListener(this)
                optionsBuilder.routeListener(this)
                optionsBuilder.directionsRoute(route)
                optionsBuilder.shouldSimulateRoute(true)

                navigationView?.startNavigation(optionsBuilder.build())

            }
        }
    }

    override fun onProgressChange(location: Location, routeProgress: RouteProgress) {
        val progressEvent = MapBoxRouteProgressEvent(routeProgress, location)
        PluginUtilities.sendEvent(progressEvent)
    }

    override fun userOffRoute(location: Location) {

        PluginUtilities.sendEvent(MapBoxEvents.USER_OFF_ROUTE,
                MapBoxLocation(
                        latitude = location.latitude,
                        longitude = location.longitude
                ).toString())
    }

    override fun onMilestoneEvent(routeProgress: RouteProgress, instruction: String, milestone: Milestone) {

        PluginUtilities.sendEvent(MapBoxEvents.MILESTONE_EVENT,
                MapBoxMileStone(
                        identifier = milestone.identifier,
                        distanceTraveled = routeProgress.distanceTraveled(),
                        legIndex = routeProgress.legIndex,
                        stepIndex = routeProgress.stepIndex
                ).toString())
    }

    override fun onRunning(running: Boolean) {

        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_RUNNING)
    }

    override fun onCancelNavigation() {
        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_CANCELLED)
        navigationView?.stopNavigation()
    }

    override fun onNavigationFinished() {
        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_FINISHED)
    }

    override fun onNavigationRunning() {
        PluginUtilities.sendEvent(MapBoxEvents.NAVIGATION_RUNNING)
    }

    override fun fasterRouteFound(directionsRoute: DirectionsRoute) {
        PluginUtilities.sendEvent(MapBoxEvents.FASTER_ROUTE_FOUND, directionsRoute.toJson())
    }

    override fun willVoice(announcement: SpeechAnnouncement?): SpeechAnnouncement? {
        PluginUtilities.sendEvent(MapBoxEvents.SPEECH_ANNOUNCEMENT,
                "{" +
                        "  \"data\": \"${announcement?.announcement()}\"" +
                        "}")
        return announcement
    }

    override fun willDisplay(instructions: BannerInstructions?): BannerInstructions? {
        PluginUtilities.sendEvent(MapBoxEvents.BANNER_INSTRUCTION,
                "{" +
                        "  \"data\": \"${instructions?.primary()?.text()}\"" +
                        "}")
        return instructions
    }

    override fun onArrival() {
        PluginUtilities.sendEvent(MapBoxEvents.ON_ARRIVAL)
    }

    override fun onFailedReroute(errorMessage: String?) {
        PluginUtilities.sendEvent(MapBoxEvents.FAILED_TO_REROUTE,
                "{" +
                        "  \"data\": \"${errorMessage}\"" +
                        "}")
    }

    override fun onOffRoute(offRoutePoint: Point?) {
        PluginUtilities.sendEvent(MapBoxEvents.USER_OFF_ROUTE,
                MapBoxLocation(
                        latitude = offRoutePoint?.latitude(),
                        longitude = offRoutePoint?.longitude()
                ).toString())
    }

    override fun onRerouteAlong(directionsRoute: DirectionsRoute?) {
        PluginUtilities.sendEvent(MapBoxEvents.REROUTE_ALONG, "${directionsRoute?.toJson()}")
    }

    override fun allowRerouteFrom(offRoutePoint: Point?): Boolean {
        return true
    }

    private fun getInitialCameraPosition(): CameraPosition {
        val originCoordinate = route.routeOptions()?.coordinates()?.get(0)
        return CameraPosition.Builder()
                .target(LatLng(originCoordinate!!.latitude(), originCoordinate.longitude()))
                .zoom(FlutterMapboxNavigation.zoom)
                .bearing(FlutterMapboxNavigation.bearing)
                .tilt(FlutterMapboxNavigation.tilt)
                .build()
    }

}