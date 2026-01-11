import android.content.Context
import android.util.Log
import java.io.ByteArrayOutputStream

class FileReceiver(
    private val appContext: Context,
){
    private val buffer = ByteArrayOutputStream()
    private lateinit var filename: String
    private var count = 0
    fun handleMessage(value: ByteArray?): String{
        if(value == null) return ""
        val message = String(value,Charsets.UTF_8)
        when {
            message.startsWith("START") -> {
                buffer.reset()
                count = 0
                filename = message.substringAfter(":")
                Log.d("FTP","File transfer started $filename")
                return ""
            }
            message.contains("END") -> {
                appContext.openFileOutput(filename, Context.MODE_PRIVATE).use {
                    it.write(buffer.toByteArray())
                }
                Log.d("FTP","$filename received")
                return filename


            }
            else -> {
                value.let {buffer.write(value)}
                return ""
            }

        }
    }
}