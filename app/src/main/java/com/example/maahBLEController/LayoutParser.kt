package com.example.maahBLEController

import android.content.Context
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import org.json.JSONArray
import org.json.JSONObject
import java.io.File



class LayoutParser(
    val fileName: String,
    val context: Context
) {
    val buttonList = mutableListOf<ButtonConfig>()
    val imageList = mutableListOf<ImageConfig>()
    lateinit var uiConfig: UIConfig
    fun readJSON() {
        val jsonString = try {
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.readText()
            } else {
                context.assets.open(fileName).bufferedReader().use {
                    it.readText()
                }
            }
        } catch (e: Exception) {
            throw e
        }
        val root = JSONObject(jsonString)
        val backgroundImage = root.getString("background image")
        val buttons = if(root.has("buttons")){
            root.getJSONArray("buttons")
        } else {
            JSONArray()
        }
        for (i in 0..<buttons.length()) {
            val cell = buttons.getJSONObject(i)
            buttonList.add(
                ButtonConfig(
                    text = cell.getString("text"),
                    textColor = cell.getString("textColor"),
                    width = cell.getInt("width"),
                    height = cell.getInt("height"),
                    xOffsetRel = cell.getDouble("xOffset").toFloat(),
                    yOffsetRel = cell.getDouble("yOffset").toFloat(),
                    shape = when (cell.getString("shape")) {
                        "circle" -> CircleShape
                        "rectangle" -> RectangleShape
                        "rounded rectangle" -> RoundedCornerShape(cell.getInt("rounding"))
                        else -> CircleShape //default is circleShape
                    },
                    color = cell.getString("color"),
                    imageURL = cell.getString("imageURL"),
                    padding = cell.getInt("padding")
                )
            )
        }
        val images = if(root.has("images")){
            root.getJSONArray("images")
        } else {
            JSONArray()
        }
        for (i  in 0..<images.length()){
            val cell = images.getJSONObject(i)
            imageList.add(
                ImageConfig(
                    imageURL = cell.getString("imageURL"),
                    width = cell.getInt("width"),
                    height = cell.getInt("height"),
                    xOffsetRel = cell.getDouble("xOffset").toFloat(),
                    yOffsetRel = cell.getDouble("yOffset").toFloat()
                )
            )
        }
        uiConfig = UIConfig(
            backgroundImage = backgroundImage,
            buttons = buttonList,
            images = imageList
        )
    }


}
