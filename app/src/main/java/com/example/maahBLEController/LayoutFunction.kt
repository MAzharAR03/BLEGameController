package com.example.maahBLEController

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

//switch to relative only positioning and change size to relative sizes
data class ButtonConfig(
    val text: String,
    val width: Int,
    val height: Int,
    val xOffsetRel: Float,
    val yOffsetRel: Float,
)


@Composable
fun CustomButton(
    config: ButtonConfig,
    sendButton: (String) -> Unit,
    parentHeight: Dp,
    parentWidth: Dp
) {
    FloatingActionButton(
        onClick = { sendButton(config.text) },
        shape = CircleShape,
        modifier = Modifier
            .size(
                width = config.width.dp,
                height = config.height.dp
            )
            .offset(
                //to offset the button relative to center rather than top left
                x = parentWidth * config.xOffsetRel - config.width.dp/2,
                y = parentHeight * config.yOffsetRel - config.width.dp/2)
    ) {
        Text(config.text)
    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PixelLayout(
    buttonCfgList: List<ButtonConfig>,
    sendButton: (String) -> Unit
){

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ){
        for (button in buttonCfgList){
            CustomButton(button,
                sendButton = sendButton,
                parentWidth = maxWidth,
                parentHeight = maxHeight)
        }
    }
}


@Preview(
    showBackground = true,
    device = "spec:width=411dp,height=891dp,orientation=landscape,dpi=420"
)
@Composable
fun WeightedScreen() {
    val buttonsList = listOf(
        ButtonConfig("Fire",50,50,0.25f,0.1f),
        ButtonConfig("Pause",200,200,0.5f,0.5f))
    PixelLayout(buttonsList,{})
}