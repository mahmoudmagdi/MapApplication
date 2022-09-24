package com.khlafawi.mapsapplication.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import com.khlafawi.mapsapplication.MapsActivity.Companion.REQUEST_ACTIVITY_RECOGNITION_PERMISSION

class StepsCounter(private val activity: AppCompatActivity) : SensorEventListener {

    val liveSteps = MutableLiveData<Int>()

    private val sensorManager by lazy {
        activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private val stepCounterSensor: Sensor? by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }

    private var initialSteps = -1

    fun setupStepsCounter() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                startCounting()
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    REQUEST_ACTIVITY_RECOGNITION_PERMISSION
                )
            }
        } else {
            startCounting()
        }
    }

    private fun startCounting() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(
                this, stepCounterSensor,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        event.values.firstOrNull()?.toInt()?.let { newSteps ->
            if (initialSteps == -1) {
                initialSteps = newSteps
            }

            val currentSteps = newSteps - initialSteps

            liveSteps.value = currentSteps
        }
    }

    fun unloadStepCounter() {
        if (stepCounterSensor != null) {
            sensorManager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
}