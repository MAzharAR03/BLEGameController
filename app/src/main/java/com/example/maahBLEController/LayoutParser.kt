package com.example.maahBLEController

import android.content.Context
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import org.json.JSONArray
import org.json.JSONObject
import java.io.File



class LayoutParser(
    val fileName: String,
    val context: Context
) {
    var buttonList: List<ButtonConfig> = listOf()
    var imageList: List<ImageConfig> = listOf()
    var backgroundImage: String = ""
    fun getNewUI(): UIConfig{
        return UIConfig(
            buttons = buttonList,
            images = imageList,
            backgroundImage = backgroundImage
        )
    }
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
        backgroundImage = root.getString("background image")
        val buttons = if(root.has("buttons")){
            root.getJSONArray("buttons")
        } else {
            JSONArray()
        }
        for (i in 0..<buttons.length()) {
            val cell = buttons.getJSONObject(i)
            buttonList = buttonList +
                ButtonConfig(
                    text = cell.getString("text"),
                    textColor = cell.getString("textColor"),
                    fontSize = cell.getInt("textFontSize"),
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
        }
        val images = if(root.has("images")){
            root.getJSONArray("images")
        } else {
            JSONArray()
        }
        for (i  in 0..<images.length()){
            val cell = images.getJSONObject(i)
            imageList = imageList +
                ImageConfig(
                    imageURL = cell.getString("imageURL"),
                    width = cell.getInt("width"),
                    height = cell.getInt("height"),
                    xOffsetRel = cell.getDouble("xOffset").toFloat(),
                    yOffsetRel = cell.getDouble("yOffset").toFloat(),
                    contentScale = when(cell.getString("contentScale")){
                        "Fit" -> ContentScale.Fit
                        "FillWidth" -> ContentScale.FillWidth
                        "FillHeight" -> ContentScale.FillWidth
                        "Inside" -> ContentScale.Inside
                        "Crop" -> ContentScale.Crop
                        "FillBounds" -> ContentScale.FillBounds
                        else -> ContentScale.None
                    }
                )
        }
    }


}
