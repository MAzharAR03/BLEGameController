package com.example.maahBLEController

import FileReceiver
import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
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
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.maahBLEController.ui.theme.BLEAdvertiseTheme
import java.io.File
import java.lang.System


import java.util.UUID
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

private const val PERMISSION_REQUEST_CODE = 1
// Example UUIDs — generate your own unique ones if needed
private val phoneIOServiceUuid: UUID = UUID.fromString("0000feed-0000-1000-8000-00805f9b34fb")

private val buttonCharUuid: UUID = UUID.fromString("0000beef-0000-1000-8000-00805f9b34fb")
private val tiltCharUUID: UUID = UUID.fromString("446be5b0-93b7-4911-abbe-e4e18d545640")
private val stepCharUUID: UUID = UUID.fromString("36d942a6-9e79-4812-8a8f-84a275f6b176")
private val controlCharUUID: UUID = UUID.fromString("4a55006e-990a-4737-9634-133466ef8e35")
private val CCCDUUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

private val fileTransferCharUUID: UUID = UUID.fromString("efcdbf7b-fee2-489b-8f79-b649aa50619b")
@SuppressLint("MissingPermission")
class MainActivity : ComponentActivity() {
    private var buttonCharacteristic: BluetoothGattCharacteristic? = null
    private var tiltCharacteristic: BluetoothGattCharacteristic? = null
    private var stepCharacteristic: BluetoothGattCharacteristic? = null
    private var messageCharacteristic: BluetoothGattCharacteristic? = null
    private var layoutCharacteristic: BluetoothGattCharacteristic? = null
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGattServer: BluetoothGattServer? = null
    private var connectedDevice: BluetoothDevice? = null

    private val TILT_UPDATE_INTERVAL_MS = 0L
    private var lastTiltSpendTime = 0L
    private var lastSentTilt : Double = 0.0
    private val TILT_THRESHOLD = 0.01
    private var isGattServerSetup = false
    private lateinit var appContext: Context

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
    var z = mutableStateOf(0f)
    lateinit var uiLayout : UIConfig
    var stepSensor: MeasurableSensor? = null
    var accelerometer: MeasurableSensor? = null
    private lateinit var fileReceiver: FileReceiver
    private fun copyDefaultLayout(){
        val targetFile = File(filesDir,"Test.json")
        assets.open("Test.json").use {
            input -> targetFile.outputStream().use {
                output -> input.copyTo(output)
        }
        }
    }

    private fun loadLayout(filename: String): UIConfig{
        return LayoutParser(filename,this).apply {readJSON()}.uiConfig
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appContext = applicationContext
        fileReceiver = FileReceiver(applicationContext)
        copyDefaultLayout()
        uiLayout = loadLayout("Test.json")
        if (checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.ACTIVITY_RECOGNITION), 1)
        }
        //for android 10 and below - BLE requires location perms
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PERMISSION_REQUEST_CODE
                )
            }
        }

        stepSensor = StepDetector(this,"FASTEST")
        accelerometer = Accelerometer(this,"GAME")
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        startAdvertising()
        enableEdgeToEdge()
        setContent {
            BLEAdvertiseTheme {
                AdvertiseScreen(
                    sendPressed = ::sendPressed,
                    uiLayout
                )
            }
        }
    }
    override fun onResume() {
        super.onResume()
        if (!bluetoothAdapter.isEnabled) {
            promptEnableBluetooth()
        }
        startAdvertising()
        stepSensor?.startListening()
        stepSensor?.setOnSensorValuesChangedListener {
            writeToChar("Step:", stepCharUUID,confirm = false)
        }
        accelerometer?.startListening()
        accelerometer?.setOnSensorValuesChangedListener { values ->
            x.value = values[0]
            y.value = values[1]
            z.value = values[2]
            val tilt = calculatePitch(x.value,y.value,z.value)
            val currentTime = System.currentTimeMillis()
            if(currentTime - lastTiltSpendTime >= TILT_UPDATE_INTERVAL_MS
                && abs(tilt - lastSentTilt) >= TILT_THRESHOLD)
            {
                writeToChar("Tilt:$tilt", tiltCharUUID,confirm = false)
                lastTiltSpendTime = currentTime
                lastSentTilt = tilt
            }
        }
    }


    private fun calculatePitch(x: Float, y: Float, z: Float): Double {
        return  atan2(-y.toDouble(), sqrt(x * x + z * z).toDouble())
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
            .setPositiveButton(R.string.ok) { _, _ ->
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
                    .setPositiveButton(R.string.ok, null)
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
            controlCharUUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                    BluetoothGattCharacteristic.PROPERTY_READ,
            BluetoothGattCharacteristic.PERMISSION_WRITE or
                    BluetoothGattCharacteristic.PERMISSION_READ
        )
        messageCharacteristic?.addDescriptor(BluetoothGattDescriptor(
            CCCDUUID,
            BluetoothGattDescriptor.PERMISSION_WRITE or BluetoothGattDescriptor.PERMISSION_READ)
        )

        layoutCharacteristic = BluetoothGattCharacteristic(
            fileTransferCharUUID,
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
        phoneIOService.addCharacteristic(layoutCharacteristic)
        bluetoothGattServer?.addService(phoneIOService)

        isGattServerSetup = true
    }
    private fun restartGattServer(){
        if(isGattServerSetup){
            bluetoothGattServer?.close()
            bluetoothGattServer = null
            isGattServerSetup = false
            bluetoothAdapter.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            setupGattServer()
        }
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                Log.i("GATT", "Device connected: ${device.address}")
            } else {
                connectedDevice = null
                restartGattServer()
                Log.i("GATT", "Device disconnected: ${device.address}")
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            bluetoothGattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                data
                )
        }

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

            var status = BluetoothGatt.GATT_SUCCESS
            try {
                when(characteristic?.uuid){
                    controlCharUUID -> {
                        val message = String(value!!, Charsets.UTF_8)
                        when{
                            message.startsWith("START") -> {
                                fileReceiver.handleStart(message = message)
                            }
                            message.startsWith("CHECKSUM") -> {
                                val receivedChecksum = message.substringAfter(":").toLong()
                                val acknowledgement = fileReceiver.handleCRC(receivedChecksum = receivedChecksum)
                                writeToChar(acknowledgement, controlCharUUID,confirm = true)
                                Log.d("FTP","Notifying: $acknowledgement")
                            }
                            message.startsWith("END") -> {
                                fileReceiver.handleEnd()
                                if(fileReceiver.isTransferComplete()){
                                    if(fileReceiver.getFilename().endsWith(".json")){
                                        runOnUiThread {
                                            loadLayout(filename = fileReceiver.getFilename())
                                        }
                                    }
                                }
                            }
                            else -> {
                                Log.d("FTP","Error processing control message.")
                            }
                        }
                    }
                    fileTransferCharUUID -> {
                        fileReceiver.handleFileTransfer(value)
                    }
                    else -> {
                        Log.d("BLE", "Write to unknown characteristic")
                    }
                }
            } catch (e: Exception) {
                status = BluetoothGatt.GATT_FAILURE
                Log.d("BLE","Error writing to characteristic: $e")
            }
            if (responseNeeded) {
                bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            }
        }
    }
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


    private fun writeToChar(text: String, uuid: UUID, confirm: Boolean){
        val data = text.toByteArray()
        val characteristic = when(uuid){
            buttonCharUuid -> buttonCharacteristic
            tiltCharUUID -> tiltCharacteristic
            stepCharUUID -> stepCharacteristic
            controlCharUUID -> messageCharacteristic
            else -> null
        }
        connectedDevice?.let {device ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGattServer?.notifyCharacteristicChanged(
                    device,
                    characteristic!!,
                    confirm,
                    data
                )
            } else {
                buttonCharacteristic?.value = data
                bluetoothGattServer?.notifyCharacteristicChanged(
                    device,
                    characteristic,
                    false
                )
            }
        }
    }

    private fun sendPressed(text:String){
        writeToChar(text = text, uuid = buttonCharUuid,confirm = false)
    }
    fun hideSystemUI(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            val controller = WindowInsetsControllerCompat(window,window.decorView)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }else{
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus) hideSystemUI()
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
    sendPressed: (String) -> Unit,
    uiLayout: UIConfig,
) {
    PixelLayout(uiLayout,sendPressed)
}

//@Preview(
//    showBackground = true,
//    device = "spec:width=411dp,height=891dp,orientation=landscape,dpi=420"
//)
//@Composable
//fun AdvertiseApp(){
//    val buttonsList = listOf(
//    ButtonConfig("Fire", 50, 50, 0.25f, 0.1f,),
//    ButtonConfig("Pause", 200, 200, 0.5f, 0.5f,))
//
//    AdvertiseScreen(
//        sendPressed = {},
//        buttonsList
//    )
//}
