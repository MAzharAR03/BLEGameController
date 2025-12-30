package com.example.bleadvertise

import GridCell
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp


data class ButtonConfig(
    val text: String = "Button",
    val width: Int = 100,
    val height: Int = 100,
)

@Composable
fun CustomButton(
    config: ButtonConfig,
    sendButton: (String) -> Unit,
) {
    FloatingActionButton(
        onClick = { sendButton(config.text) },
        shape = CircleShape,
        modifier = Modifier
            .fillMaxSize()
            .size(
                width = config.width.dp,
                height = config.height.dp

            )
    ) {
        Text(config.text)
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun ButtonGrid(
    rows: Int, columns: Int, cells: List<GridCell>, sendButton: (String) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val cellWidth = maxWidth / columns
        val cellHeight = maxHeight / rows
        //for loop using repeat - similar to Battlefield for loop
        Column {
            repeat(rows) { row ->
                Row {
                    repeat(columns) { col ->
                        val index = row * columns + col //Y*X+x
                        Box(
                            modifier = Modifier
                                .width(cellWidth)
                                .height(cellHeight)
                                .clipToBounds(),
                            contentAlignment = Alignment.Center
                        ) {
                            when (val cell = cells[index]) {
                                GridCell.Empty -> Spacer(Modifier.fillMaxSize())
                                is GridCell.ButtonCell -> CustomButton(
                                    cell.config,
                                    { sendButton(cell.config.text) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeightedButtonGrid(
    rows: Int, columns: Int, cells: List<GridCell>, sendButton: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.weight(1f)
            ) {
                repeat(columns) { col ->
                    val index = row * columns + col
                    var weight = 1f
                    if(index == 4) {
                        weight = 4f
                    }
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (val cell = cells[index]) {
                            GridCell.Empty -> Spacer(Modifier.fillMaxSize())
                            is GridCell.ButtonCell -> CustomButton(
                                cell.config,
                                { sendButton(cell.config.text) })
                        }
                    }
                }
            }
        }
    }
}



@Preview(
    showBackground = true, device = "spec:width = 411dp, height = 891dp, orientation = landscape, dpi = 420"
)
@Composable
fun WeightedScreen() {
    val cells =
        listOf(GridCell.Empty,GridCell.Empty,GridCell.Empty,
            GridCell.Empty,GridCell.ButtonCell(ButtonConfig()), GridCell.Empty,
            GridCell.Empty,GridCell.Empty,GridCell.Empty)
    WeightedButtonGrid(3, 3, cells, sendButton = {})

}