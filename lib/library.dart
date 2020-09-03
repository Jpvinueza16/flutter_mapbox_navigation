library navigation;

import 'dart:async';
import 'dart:convert';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';


part "flutter_mapbox_navigation.dart";
part "models/wayPoint.dart";
part 'models/route.dart';
part 'models/routeLeg.dart';
part 'models/routeStep.dart';
part 'models/routeEvent.dart';

bool IsNullOrZero(dynamic val)
{
    return val == 0.0 || val == null;
}


