package com.example.bleadvertise

import GridCell
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class LayoutParser(
    val fileName: String,
    val context: Context
) {
    var rows = 0
    var columns = 0
    val result = mutableListOf<GridCell>()

    fun readJSON() {
        val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
        val root = JSONObject(jsonString)
        val cellsArray = root.getJSONArray("cells")

        for (i in 0..<cellsArray.length()) {
            when (val cell = cellsArray.get(i)) {
                is String -> {
                    result.add(GridCell.Empty)
                }

                is JSONArray -> {
                    result.add(
                        GridCell.ButtonCell(
                            ButtonConfig(
                                text = cell.getString(1),
                                width = cell.getInt(2),
                                height = cell.getInt(3)
                            )
                        )
                    )
                }
            }
        }
        rows = root.getInt("rows")
        columns = root.getInt("columns")
    }
}