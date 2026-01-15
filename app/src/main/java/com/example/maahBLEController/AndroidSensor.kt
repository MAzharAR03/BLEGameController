package com.example.maahBLEController

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

enum class SensorDelay{
    UI,
    GAME,
    FASTEST,
    NORMAL
}
abstract class AndroidSensor(
    private val context: Context,
    private val sensorFeature: String,
    sensorType: Int,
    val sensorDelay: SensorDelay
): MeasurableSensor(sensorType), SensorEventListener{

    override val doesSensorExist: Boolean
        get() = context.packageManager.hasSystemFeature(sensorFeature)

    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null

    override fun startListening() {
        if(!doesSensorExist){
            return
        }
        if(!::sensorManager.isInitialized && sensor == null){
            sensorManager = context.getSystemService(SensorManager::class.java) as SensorManager
            sensor = sensorManager.getDefaultSensor(sensorType)
        }
        sensor?.let{
            when(sensorDelay){
                SensorDelay.UI -> sensorManager.registerListener(this,it,SensorManager.SENSOR_DELAY_UI)
                SensorDelay.GAME -> sensorManager.registerListener(this,it,SensorManager.SENSOR_DELAY_GAME)
                SensorDelay.FASTEST -> sensorManager.registerListener(this,it, SensorManager.SENSOR_DELAY_FASTEST)
                else -> sensorManager.registerListener(this,it, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
    }

    override fun stopListening() {
        if(!doesSensorExist || !::sensorManager.isInitialized){
            return
        }
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if(!doesSensorExist){
            return
        }
        if(event?.sensor?.type == sensorType){
            onSensorValuesChanged?.invoke(event.values.toList())
        }
    }
}