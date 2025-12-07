package com.example.sensorguide

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor

class RotationVectorSensor(
    context: Context
): AndroidSensor(
    context = context,
    sensorFeature = "android.hardware.SensorManager.sensor.rotation_vector",
    sensorType = Sensor.TYPE_ROTATION_VECTOR
)

class StepDetector(
    context: Context
): AndroidSensor(
    context = context,
    sensorFeature = PackageManager.FEATURE_SENSOR_STEP_DETECTOR,
    sensorType = Sensor.TYPE_STEP_DETECTOR
)

class Accelerometer(
    context: Context
): AndroidSensor(
    context = context,
    sensorFeature = PackageManager.FEATURE_SENSOR_ACCELEROMETER,
    sensorType = Sensor.TYPE_ACCELEROMETER
)