package com.example.maahBLEController

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import coil.compose.AsyncImage


/*
transfer file over BLE
try sending json file over godot and see if everything still works fine
potentially change image inputs to scale sizing (maybe also buttons)


allow contentScale option ?
* */

interface ElementConfig{
    val width: Int
    val height: Int
    val xOffsetRel: Float
    val yOffsetRel: Float
}
data class ButtonConfig (
    val text: String,
    val fontSize: Int,
    override val width: Int,
    override val height: Int,
    override val xOffsetRel: Float,
    override val yOffsetRel: Float,
    val shape: Shape,
    val color: String,
    val imageURL: String,
    val textColor: String,
    val padding: Int,
): ElementConfig


data class ImageConfig(
    override val width: Int,
    override val height: Int,
    override val xOffsetRel: Float,
    override val yOffsetRel: Float,
    val imageURL: String,
    val contentScale: ContentScale,
) : ElementConfig

data class UIConfig(
    val buttons: List<ButtonConfig>,
    val images: List<ImageConfig>,
    val backgroundImage: String
)

//make reusable AsyncImage clas

@Composable
fun CustomButton(
    config: ButtonConfig,
    sendButton: (String) -> Unit,
    parentHeight: Dp,
    parentWidth: Dp,
) {
    Button(
        onClick = { sendButton(config.text) },
        shape = config.shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = when(config.color){
                "transparent" -> Color.Transparent
                else -> Color(android.graphics.Color.parseColor(config.color))
            }),
        contentPadding = PaddingValues(config.padding.dp), //add padding to json
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
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ){
            AsyncImage(
                model = when{
                    !config.imageURL.startsWith("http") -> File(
                        LocalContext.current.filesDir,
                        config.imageURL
                    )
                    else -> config.imageURL
                },
                contentDescription = "Button Image",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(config.shape)
            )
            Text(
                text = config.text,
                fontSize = config.fontSize.sp,
                color = Color(android.graphics.Color.parseColor(config.textColor)))
        }

    }
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PixelLayout(
    uiConfig: UIConfig,
    sendButton: (String) -> Unit,
){
    val context = LocalContext.current
    val bgImageModel = when{
        !uiConfig.backgroundImage.startsWith("http") -> File(
            context.filesDir,
            uiConfig.backgroundImage
        )
        else -> uiConfig.backgroundImage
    }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ){
        AsyncImage(
            model = bgImageModel,
            contentDescription = "Background Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        for (button in uiConfig.buttons){
            CustomButton(button,
                sendButton = sendButton,
                parentWidth = maxWidth,
                parentHeight = maxHeight
            )
        }
        for (image in uiConfig.images){
            AsyncImage(
                model = image.imageURL,
                contentDescription = null,
                contentScale = image.contentScale,
                modifier = Modifier
                    .size(
                        width = image.width.dp,
                        height = image.height.dp
                    )
                    .offset(
                        x = maxWidth * image.xOffsetRel - image.width.dp/2,
                        y = maxHeight * image.yOffsetRel - image.width.dp/2)
                    )
        }
    }
}


//@Preview(
//    showBackground = true,
//    device = "spec:width=411dp,height=891dp,orientation=landscape,dpi=420"
//)
//@Composable
//fun WeightedScreen() {
//    val buttonsList = listOf(
//        ButtonConfig("Fire", 50, 50, 0.25f, 0.1f,),
//        ButtonConfig("Pause", 200, 200, 0.5f, 0.5f,))
//    PixelLayout(buttonsList,{})
//}