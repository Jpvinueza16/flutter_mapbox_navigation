import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_mapbox_navigation/library.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  final _origin = WayPoint(name: "My Home", latitude: 42.944719, longitude: -78.7931947);
  final _stop1 = WayPoint(name: "Brew Pub", latitude: 42.8925713, longitude: -78.6806405);
  final _stop2 = WayPoint(name: "Catalyst Fitness", latitude: 42.9328275, longitude: -78.7172975);
  final _stop3 = WayPoint(name: "Mini Mart", latitude: 42.81957, longitude: -78.8286187);
  final _cornell = WayPoint(name: "Cornell University", latitude: 42.4534492, longitude: -76.4756914);
  MapboxNavigation _directions;
  bool _arrived = false;
  double _distanceRemaining, _durationRemaining;

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    _directions = MapboxNavigation(onRouteProgress: (e) async {
      _distanceRemaining = await _directions.distanceRemaining;
      _durationRemaining = await _directions.durationRemaining;

      setState(() {
        _arrived = e.arrived;
      });
      if (e.arrived)
        {
          await Future.delayed(Duration(seconds: 3));
          await _directions.finishNavigation();
        }
    });

    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await _directions.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Column(children: <Widget>[
            SizedBox(
              height: 30,
            ),
            Text('Running on: $_platformVersion\n'),
            SizedBox(
              height: 60,
            ),
            RaisedButton(
              child: Text("Start  Navigation"),
              onPressed: () async {

                await _directions.startNavigation(origin: _origin, destination: _cornell,
                    mode: MapBoxNavigationMode.drivingWithTraffic,
                    simulateRoute: true, language: "en", units: VoiceUnits.metric);

              },
            ),
            SizedBox(height: 30,),
            RaisedButton(
              child: Text("Start Multi Stop Navigation"),
              onPressed: () async {
                
                var wayPoints = List<WayPoint>();
                wayPoints.add(_origin);
                wayPoints.add(_stop1);
                wayPoints.add(_stop2);
                wayPoints.add(_stop3);
                wayPoints.add(_origin);

                await _directions.startNavigationWithWayPoints(
                    wayPoints: wayPoints,
                    mode: MapBoxNavigationMode.drivingWithTraffic,
                    simulateRoute: true, language: "en", units: VoiceUnits.metric);
                
              },
            ),
            SizedBox(
              height: 60,
            ),
            Padding(
              padding: EdgeInsets.all(20.0),
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: <Widget>[
                  Row(
                    children: <Widget>[
                      Text("Distance Remaining: "),
                      Text(_distanceRemaining != null
                          ? "${(_distanceRemaining * 0.000621371).toStringAsFixed(1)} miles"
                          : "---")
                    ],
                  ),
                  Row(
                    children: <Widget>[
                      Text("Duration Remaining: "),
                      Text(_durationRemaining != null
                          ? "${(_durationRemaining / 60).toStringAsFixed(0)} minutes"
                          : "---")
                    ],
                  )
                ],
              ),
            ),

          ]),
        ),
      ),
    );
  }
}
