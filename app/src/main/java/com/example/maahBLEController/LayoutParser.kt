package com.example.maahBLEController

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LayoutParser(
    val fileName: String,
    val context: Context
) {
    val result = mutableListOf<ButtonConfig>()
    fun readJSON() {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val root = JSONObject(jsonString)
        val cellsArray = root.getJSONArray("cells")
        for (i in 0..<cellsArray.length()) {
            when (val cell = cellsArray.get(i)) {
                is JSONArray -> {result.add(
                    ButtonConfig(
                        text = cell.getString(0),
                        width = cell.getInt(1),
                        height = cell.getInt(2),
                        xOffsetRel = cell.getDouble(3).toFloat(),
                        yOffsetRel = cell.getDouble(4).toFloat(),
                    )
                )
                }
            }
        }
    }
}