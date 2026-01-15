package com.example.maahBLEController

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor

class RotationVectorSensor(
    context: Context,
    sensorDelay: SensorDelay
): AndroidSensor(
    context = context,
    sensorFeature = "android.hardware.SensorManager.sensor.rotation_vector",
    sensorType = Sensor.TYPE_ROTATION_VECTOR,
    sensorDelay = sensorDelay
)

class StepDetector(
    context: Context,
    sensorDelay: SensorDelay
): AndroidSensor(
    context = context,
    sensorFeature = PackageManager.FEATURE_SENSOR_STEP_DETECTOR,
    sensorType = Sensor.TYPE_STEP_DETECTOR,
    sensorDelay = sensorDelay
)

class Accelerometer(
    context: Context,
    sensorDelay: SensorDelay
): AndroidSensor(
    context = context,
    sensorFeature = PackageManager.FEATURE_SENSOR_ACCELEROMETER,
    sensorType = Sensor.TYPE_ACCELEROMETER,
    sensorDelay = sensorDelay
)