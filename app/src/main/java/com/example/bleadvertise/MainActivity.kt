package com.example.bleadvertise

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothCodecType
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bleadvertise.ui.theme.BLEAdvertiseTheme
import com.example.bleadvertise.ui.theme.Orientation
import com.example.sensorguide.Accelerometer
import com.example.sensorguide.MeasurableSensor
import com.example.sensorguide.StepDetector
import java.lang.System


import java.util.UUID
import kotlin.math.abs

private const val PERMISSION_REQUEST_CODE = 1
// Example UUIDs — generate your own unique ones if needed
private val phoneIOServiceUuid: UUID = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb")
private val buttonCharUuid: UUID = UUID.fromString("0000beef-0000-1000-8000-00805f9b34fb")
private val tiltCharUUID: UUID = UUID.fromString("446be5b0-93b7-4911-abbe-e4e18d545640")
private val stepCharUUID: UUID = UUID.fromString("36d942a6-9e79-4812-8a8f-84a275f6b176")
private val messageCharUUID: UUID = UUID.fromString("4a55006e-990a-4737-9634-133466ef8e35")
private val CCCDUUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {
    private var buttonCharacteristic: BluetoothGattCharacteristic? = null
    private var tiltCharacteristic: BluetoothGattCharacteristic? = null
    private var stepCharacteristic: BluetoothGattCharacteristic? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var connectedDevice: BluetoothDevice? = null

    private val TILT_UPDATE_INTERVAL_MS = 0L
    private var lastTiltSpendTime = 0L
    private var lastSentTilt = 0f
    private val TILT_THRESHOLD = 1.0f
    private var isGattServerSetup = false

    private val bluetoothEnablingResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
            result ->
        if(result.resultCode == RESULT_OK){

        } else{
            promptEnableBluetooth()
        }
    }

    var x = mutableStateOf(0f)
    var y = mutableStateOf(0f)
    var stepSensor: MeasurableSensor? = null
    var accelerometer: MeasurableSensor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION)
            != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACTIVITY_RECOGNITION), 1)
        }

        stepSensor = StepDetector(this)
        accelerometer = Accelerometer(this)
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        startAdvertising()
        enableEdgeToEdge()
        setContent {
            BLEAdvertiseTheme {
                AdvertiseScreen(
                    sendPressed = ::sendPressed,
                    modifier = Modifier.fillMaxSize())

                }
            }
        }

    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
        stepSensor?.startListening()
        stepSensor?.setOnSensorValuesChangedListener {sendStep()}
        accelerometer?.startListening()
        accelerometer?.setOnSensorValuesChangedListener { values ->
            x.value = values[0]
            y.value = values[1]
            var orientation = calculateTilt(x.value,y.value)
            val currentTime = System.currentTimeMillis()
            if(currentTime - lastTiltSpendTime >= TILT_UPDATE_INTERVAL_MS
                && abs(orientation.tilt - lastSentTilt) > TILT_THRESHOLD)
            {
                sendTilt(orientation)
                lastTiltSpendTime = currentTime
                lastSentTilt = orientation.tilt
            }
}
    }


    private fun calculateTilt(x: Float, y: Float): Orientation {
        var tilt = 0f
        var orientation = "Center"
        var rightSideUp = x > 0
        when {
            x > 9 -> rightSideUp = true
            x < -9 -> rightSideUp = false
        }
        when {
            y < 0.5 && y > -0.5 -> orientation = "Center"
            y < -0.5 -> {
                if (rightSideUp) {
                    orientation = "Tilt Left"
                    tilt = (y + 0.5f) * 10
                } else {
                    orientation = "Tilt Right"
                    tilt = -(y + 0.5f) * 10
                }
            }

            y > 0.5 -> {
                if (rightSideUp) {
                    orientation = "Tilt Right"
                    tilt = (y - 0.5f) * 10
                } else {
                    orientation = "Tilt Left"
                    tilt = -(y - 0.5f) * 10
                }
            }

        }
        return Orientation(orientation = orientation, tilt = tilt)
    }

    private fun Activity.requestRelevantRuntimePermissions() {
        if(hasRequiredBluetoothPermissions()) {return}
        when{
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                requestBluetoothPermissions()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun requestBluetoothPermissions() = runOnUiThread {
        AlertDialog.Builder(this)
            .setTitle("Bluetooth permission required")
            .setMessage("Starting from Android 12, the system requires apps to be granted " +
                    "Bluetooth access in order to scan for and connect to BLE devices.")
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) {_, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_ADVERTISE
                    ),
                    PERMISSION_REQUEST_CODE
                )
            }
            .show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST_CODE) return

        val containsPermanentDenial = permissions.zip(grantResults.toTypedArray()).any {
            it.second == PackageManager.PERMISSION_DENIED &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, it.first)
        }

        val containsDenial = grantResults.any {it == PackageManager.PERMISSION_DENIED}
        val allGranted = grantResults.all {it == PackageManager.PERMISSION_GRANTED }
        when {
            containsPermanentDenial -> {
                AlertDialog.Builder(this)
                    .setTitle("Permission Denied")
                    .setMessage("Cannot scan for BLE devices without the required permissions.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
            containsDenial -> {
                requestRelevantRuntimePermissions()
            }
            allGranted && hasRequiredBluetoothPermissions() -> {
                startAdvertising()
            }
            else -> {
                recreate()
            }
        }
    }

    private fun promptEnableBluetooth(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !hasPermission(Manifest.permission.BLUETOOTH_CONNECT))
        {
            return
        }
        if(!bluetoothAdapter.isEnabled){
            Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE).apply {
                bluetoothEnablingResult.launch(this)
            }
        }
    }

    private fun setupGattServer(){
        if (isGattServerSetup) {
            Log.w("BLE", "GATT server already set up")
            return
        }
        bluetoothGattServer = bluetoothManager.openGattServer(this, gattServerCallback)
        buttonCharacteristic = BluetoothGattCharacteristic(
            buttonCharUuid,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        buttonCharacteristic?.addDescriptor(BluetoothGattDescriptor(
            CCCDUUID,
            BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ))

        tiltCharacteristic = BluetoothGattCharacteristic(
            tiltCharUUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        tiltCharacteristic?.addDescriptor(BluetoothGattDescriptor(
            CCCDUUID,
            BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ))

        stepCharacteristic = BluetoothGattCharacteristic(
            stepCharUUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_READ
        )

        stepCharacteristic?.addDescriptor(BluetoothGattDescriptor(
            CCCDUUID,
            BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ))

        messageCharacteristic = BluetoothGattCharacteristic(
            messageCharUUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        


        val phoneIOService = BluetoothGattService(
            phoneIOServiceUuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
        //add the characteristic to the service
        phoneIOService.addCharacteristic(buttonCharacteristic)
        phoneIOService.addCharacteristic(tiltCharacteristic)
        phoneIOService.addCharacteristic(stepCharacteristic)
        phoneIOService.addCharacteristic(messageCharacteristic)
        bluetoothGattServer?.addService(phoneIOService)

        isGattServerSetup = true
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                Log.i("GATT", "Device connected: ${device.address}")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevice = null
                Log.i("GATT", "Device disconnected: ${device.address}")
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            if (characteristic.uuid == buttonCharUuid) {
                val buttonByte = 1.toByte()// Example value
                bluetoothGattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, byteArrayOf(buttonByte))
            }
        }

        //Double check and learn this function
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            Log.i("GATT", "onDescriptorWriteRequest: ${descriptor.uuid}")

            if (descriptor.uuid == CCCDUUID) {
                // This is the CCCD (Client Characteristic Configuration Descriptor)
                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        value
                    )
                    Log.i("GATT", "Notification subscription confirmed")
                }
            } else {
                if (responseNeeded) {
                    bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null
                    )
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            }
            when(characteristic?.uuid){
                messageCharUUID -> {
                    Log.d("BLE", "Message Received: ${System.currentTimeMillis()}")
                }
                else -> {
                    Log.d("BLE", "Write to unknown characteristic")
                }
            }
        }
    }

    // ADD THIS METHOD - This is what's missing!


    private var isAdvertising by mutableStateOf(false)
    private fun startAdvertising() {
        if (!hasRequiredBluetoothPermissions()) {
            requestRelevantRuntimePermissions()
        } else {
            if (!bluetoothAdapter.isMultipleAdvertisementSupported) return
            setupGattServer()
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .addServiceUuid(ParcelUuid(phoneIOServiceUuid))
                .build()

            bluetoothAdapter.bluetoothLeAdvertiser.startAdvertising(
                settings,
                data,
                advertiseCallback
            )
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            Log.i("BLE", "Advertising started")
            isAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e("BLE", "Advertising failed: $errorCode")
            isAdvertising = false
        }
    }

    private fun sendPressed(){
        connectedDevice?.let {device ->
            val currentTime = System.currentTimeMillis()
            Log.d("BLE", "Message Sent: ${System.currentTimeMillis()}")
            buttonCharacteristic?.value = "Button Pressed: $currentTime".toByteArray()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGattServer?.notifyCharacteristicChanged(
                    device,
                    buttonCharacteristic!!,
                    false,
                buttonCharacteristic!!.value
                )
            } else {
                bluetoothGattServer?.notifyCharacteristicChanged(
                    device,
                    buttonCharacteristic,
                    false
                )
            }
        } ?: Log.w("BLE", "No device connected")

    }
    private fun sendStep(){
        connectedDevice?.let {device ->
            val currentTime = System.currentTimeMillis()
            stepCharacteristic?.value = "Step: $currentTime".toByteArray()
            bluetoothGattServer?.notifyCharacteristicChanged(
                device,
                stepCharacteristic,
                false
            )
        } ?: Log.w("BLE", "No device connected")

    }

    private fun sendTilt(orientation: Orientation){
        connectedDevice?.let {device ->
            val currentTime = System.currentTimeMillis()
            val string = "${orientation.orientation} : ${orientation.tilt} : $currentTime"
            tiltCharacteristic?.value = string.toByteArray()
            bluetoothGattServer?.notifyCharacteristicChanged(
                device,
                tiltCharacteristic,
                false
            )
        } ?: Log.w("BLE", "No device connected")

    }

}

fun Context.hasPermission(permissionType: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permissionType) ==
            PackageManager.PERMISSION_GRANTED
}

fun Context.hasRequiredBluetoothPermissions(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ){
        hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                hasPermission(Manifest.permission.BLUETOOTH_CONNECT) &&
                hasPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    } else {
        hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

@Composable
fun AdvertiseScreen(
    modifier: Modifier = Modifier,
    sendPressed:() -> Unit, ) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Button(onClick = {sendPressed()  }) {
            Text(stringResource(R.string.SendNotification))

        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdvertiseApp(){
    AdvertiseScreen(
        sendPressed = {},
        modifier = Modifier.fillMaxSize()
    )
}

