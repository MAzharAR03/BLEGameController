package com.example.maahBLEController

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


data class ButtonConfig(
    val text: String = "Button",
    val width: Int = 100,
    val height: Int = 100,
    val xOffset: Int,
    val yOffset: Int
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
            .size(
                width = config.width.dp,
                height = config.height.dp
            )
            .offset(x = config.xOffset.dp,y = config.yOffset.dp)
    ) {
        Text(config.text)
    }
}

@Composable
fun PixelLayout(
    buttonCfgList: List<ButtonConfig>,
    sendButton: (String) -> Unit
){
    Box(
        modifier = Modifier.fillMaxSize()
    ){
        for (button in buttonCfgList){
            CustomButton(button,
                sendButton = sendButton)
        }
    }
}


@Preview(
    showBackground = true,
    device = "spec:width = 411dp, height = 891dp, orientation = landscape, dpi = 420"
)
@Composable
fun WeightedScreen() {
    val buttonsList = listOf(
        ButtonConfig("Fire",100,100,50,50),
        ButtonConfig("Pause",200,200,400,100))
    PixelLayout(buttonsList,{})
}