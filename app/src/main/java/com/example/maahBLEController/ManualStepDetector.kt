package com.example.maahBLEController

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class ManualStepDetector(
    linearAccelerometer: LinearAccelerometer,
    gravity: Gravity
) {
    // minimum threshold for Linear Acceleration to pass to count as a step. Filters out noise like small bumps
    private val NOISE_THRESHOLD = 1.0
    //Coefficient that the adaptive threshold is multiplied by every update. Adaptive threshold is determined by running average of past PEAK_BUFFER_SIZE peaks(not steps)
    private val ADAPTIVE_COEFFICIENT = 0.7
    //Upper and lower bound to which adaptive threshold is clamped. Prevents threshold from reaching unattainable heights
    private val ADAPTIVE_UPPER_BOUND = 15.0
    private val ADAPTIVE_LOWER_BOUND = 3.0
    //Buffer size to hold the past x peaks
    private val PEAK_BUFFER_SIZE = 20
    //Initial threshold that adaptiveThreshold is set to
    private val INITIAL_THRESHOLD = 6.0
    private val STEP_INTERVAL = 250L //250ms
    private var lastSentTime = 0L
    private var adaptiveThreshold = INITIAL_THRESHOLD
    var stepCounter = mutableStateOf(0)
    private var wasRising = false
    private var peaks = DoubleArray(PEAK_BUFFER_SIZE)
    private var peakCount = 0
    private var gravityValues = FloatArray(3)
    private var linearAccelValues = FloatArray(3)
    private var verticalAccel = 0.0
    private var previousVertAccel = 0.0
    private val STEP_TIMEOUT_MS = 1000L
    private var lastStepTime = 0L

    public fun updateValues(gravity: List<Float>, linearAccel: List<Float>){
        gravityValues = gravity.toFloatArray()
        linearAccelValues = linearAccel.toFloatArray()
        detectStep()
    }

    public val isCurrentlyStepping: Boolean
        get() = System.currentTimeMillis() - lastStepTime < STEP_TIMEOUT_MS
    fun detectStep() {
        // Calculate the magnitude of gravitational acceleration
        val gravityMag = sqrt(
            gravityValues[0].pow(2) +
                    gravityValues[1].pow(2) +
                    gravityValues[2].pow(2)
        )
        if(gravityMag == 0f) return //if magnitude is 0, gravity values have not been set yet

        //Divide each axis of direction by gravity magnitude
        val gravityUnit = floatArrayOf(
            gravityValues[0] / gravityMag,
            gravityValues[1] / gravityMag,
            gravityValues[2] / gravityMag
        )
        //calculate gravity-weighted linear acceleration, helps us determine if acceleration is in vertical axis
        verticalAccel = (
                linearAccelValues[0] * gravityUnit[0] +
                        linearAccelValues[1] * gravityUnit[1] +
                        linearAccelValues[2] * gravityUnit[2]
                ).toDouble()
        peakDetection()

    }

    private fun peakDetection() {
        /*
        Algorithm is inspired/adapted from
        A Step Counter Service for Java-enabled Devices Using a Built-in Accelerometer (Mladenov and Mock ,2009)
        */
        if(verticalAccel <= NOISE_THRESHOLD) return
        if (verticalAccel > previousVertAccel) {
            wasRising = true
        } else if (wasRising && verticalAccel < previousVertAccel) {
            val peak = previousVertAccel
            if (peak > (adaptiveThreshold) && System.currentTimeMillis() - lastSentTime > STEP_INTERVAL) {
                lastSentTime = System.currentTimeMillis()
                lastStepTime = System.currentTimeMillis()
                stepCounter.value++
                Log.d("STEP","Step detected! Peak: $peak, Threshold: $adaptiveThreshold")
            }
            peaks[peakCount % PEAK_BUFFER_SIZE] = peak
            peakCount++
            val validPeaks = peaks.take(min(peakCount, PEAK_BUFFER_SIZE))
            val newThreshold = validPeaks.average() * ADAPTIVE_COEFFICIENT
            adaptiveThreshold = newThreshold.coerceIn(ADAPTIVE_LOWER_BOUND,ADAPTIVE_UPPER_BOUND)
            wasRising = false
        }
        previousVertAccel = verticalAccel
    }
}