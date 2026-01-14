package com.example.maahBLEController

import android.content.Context
import android.util.Log
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
const val DEBUG = true
class FileReceiver(
    private val appContext: Context,
){
    private val buffer = ByteArrayOutputStream()
    private lateinit var filename: String
    private var transferComplete = false
    private val crc = CRC32()

    fun handleStart(message: String){
        buffer.reset()
        filename = message.substringAfter(":")
        Log.d("FTP","File transfer started $filename")
        transferComplete = false
        if(DEBUG == true)
        Log.d("FTP", "We did a little corruption")
        buffer.write("corruption!!".toByteArray())
    }

    fun handleCRC(receivedChecksum: Long): String{
        crc.reset()
        val data = buffer.toByteArray() //crc.update() takes ByteArray not ByteArrayOutputStream
        crc.update(data)
        val localChecksum = crc.value
        if(receivedChecksum == localChecksum){
            return "OK"
        } else{
            buffer.reset()
            return "RESEND"
        }
    }

    fun handleEnd(){
        appContext.openFileOutput(filename, Context.MODE_PRIVATE).use{
            it.write(buffer.toByteArray())
        }
        Log.d("FTP","$filename received")
        transferComplete = true
    }
    fun handleFileTransfer(value: ByteArray?){
        if(value == null) return
        else
            value.let {buffer.write(value)}

    }
    fun isTransferComplete(): Boolean{
        return transferComplete
    }

    fun getFilename(): String{
        return filename
    }
}