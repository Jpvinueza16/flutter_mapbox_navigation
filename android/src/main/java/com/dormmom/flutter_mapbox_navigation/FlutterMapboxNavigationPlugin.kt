package com.dormmom.flutter_mapbox_navigation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dormmom.flutter_mapbox_navigation.factory.FlutterMapViewFactory
import com.dormmom.flutter_mapbox_navigation.factory.MapViewFactory
import io.flutter.embedding.engine.FlutterEngine

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.*
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.platform.PlatformViewsController
import timber.log.Timber

/** FlutterMapboxNavigationPlugin */
public class FlutterMapboxNavigationPlugin: FlutterPlugin, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android

  companion object {

      private lateinit var binaryMessenger: BinaryMessenger

      private var _methodChannel : MethodChannel? = null
      private var _eventChannel: EventChannel? = null

      private var currentActivity: Activity? = null
      private lateinit var _context: Context

      @JvmStatic
      var view_name = "dormmom.mapbox_navigation/MapboxMapView"

    @JvmStatic
    var viewController: PlatformViewsController? = null

      @JvmStatic
      fun registerWith(engine: FlutterEngine) {
          viewController = engine.platformViewsController
          currentActivity?.let { activity ->
              viewController?.registry?.registerViewFactory(
                      view_name, MapViewFactory(engine.dartExecutor.binaryMessenger, activity))
          }
      }

    @JvmStatic
    fun registerWith(registrar: Registrar) {

        val instance = FlutterMapboxNavigationPlugin()
        setUpPluginMethods(registrar.activity(), registrar.messenger())

    }

      @JvmStatic
      fun registerWith(messenger: BinaryMessenger, context: Context) {
          val instance = FlutterMapboxNavigationPlugin()
          setUpPluginMethods(context, messenger)
      }

      @JvmStatic
      private fun setUpPluginMethods(context: Context, messenger: BinaryMessenger) {
          Timber.plant(Timber.DebugTree())
          binaryMessenger = messenger
          _methodChannel = MethodChannel(messenger, "flutter_mapbox_navigation")
          _eventChannel = EventChannel(messenger, "flutter_mapbox_navigation/arrival")
      }



  }

  override fun onAttachedToEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
      setUpPluginMethods(binding.applicationContext, binding.binaryMessenger)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        viewController?.detachFromView()
        currentActivity = null
        _methodChannel!!.setMethodCallHandler(null)
        _methodChannel = null
        _eventChannel!!.setStreamHandler(null)
        _eventChannel = null

    }
    
    override fun onDetachedFromActivity() {
        currentActivity!!.finish()
        currentActivity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {

        currentActivity = binding.activity
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {

        currentActivity = binding.activity
        _context = binding.activity.applicationContext

        var instance = FlutterMapboxNavigation(binding.activity.applicationContext, binding.activity, binaryMessenger)
        _methodChannel?.setMethodCallHandler(instance)
        _eventChannel?.setStreamHandler(instance)

    }

    override fun onDetachedFromActivityForConfigChanges() {
        //To change body of created functions use File | Settings | File Templates.
    }

}
