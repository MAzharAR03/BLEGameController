package com.example.maahBLEController

import android.annotation.SuppressLint
import androidx.compose.foundation.background
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
import coil3.compose.AsyncImage
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.coerceAtMost

/*
transfer file over BLE
try sending json file over godot and see if everything still works fine
potentially change image inputs to scale sizing (maybe also buttons)


allow contentScale option ?
* */

interface ElementConfig{
    val width: Float
    val height: Float
    val xOffsetRel: Float
    val yOffsetRel: Float
}
data class ButtonConfig (
    val text: String,
    val fontSize: Int,
    override val width: Float,
    override val height: Float,
    override val xOffsetRel: Float,
    override val yOffsetRel: Float,
    val shape: Shape,
    val color: String,
    val imageURL: String,
    val textColor: String,
    val padding: Int,
): ElementConfig


data class ImageConfig(
    override val width: Float,
    override val height: Float,
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

fun Modifier.onPressRelease(
    onPress: () -> Unit,
    onRelease: () -> Unit,
    interactionSource: MutableInteractionSource
): Modifier = this.pointerInput(Unit) {
    awaitPointerEventScope{
        while(true){
            awaitFirstDown()
            val press = PressInteraction.Press(Offset.Zero)
            interactionSource.tryEmit(press)
            onPress()
            waitForUpOrCancellation()
            interactionSource.tryEmit(PressInteraction.Release(press))
            onRelease()
        }
    }
}

@Composable
fun CustomButton(
    config: ButtonConfig,
    onButtonStateChanged: (String, Boolean) -> Unit,
    parentHeight: Dp,
    parentWidth: Dp,
) {

    val interactionSource = remember { MutableInteractionSource() } // For button ripple affect
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(
                width = parentWidth * config.width,
                height = parentHeight * config.height
            )
            .offset(
//                to offset the button relative to center rather than top left
//                x = parentWidth * config.xOffsetRel - config.width.dp/2,
//                y = parentHeight * config.yOffsetRel - config.height.dp/2
                x = (parentWidth * config.xOffsetRel).coerceAtMost(parentWidth - parentWidth * config.width),
                y = (parentHeight * config.yOffsetRel).coerceAtMost( parentHeight - parentHeight * config.height)
            )
            .clip(config.shape)
            .background(
                when (config.color) {
                    "transparent" -> Color.Transparent
                    else -> Color(android.graphics.Color.parseColor(config.color))
                }
            )
            .indication(interactionSource, ripple())
            .onPressRelease(
                onPress = { onButtonStateChanged (config.text, true) },
                onRelease = { onButtonStateChanged (config.text, false)},
                interactionSource = interactionSource
            )
            .padding(config.padding.dp)

) {
            AsyncImage(
                model = when {
                    !config.imageURL.startsWith("http") -> File(
                        LocalContext.current.filesDir,
                        config.imageURL
                    )

                    else -> config.imageURL
                },
                contentDescription = "Button Image",
                modifier = Modifier
                    .fillMaxSize()
                    //.clip(config.shape)
            )
            Text(
                text = config.text,
                fontSize = config.fontSize.sp,
                color = Color(android.graphics.Color.parseColor(config.textColor))
            )
        }
    }


@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun PixelLayout(
    uiConfig: UIConfig,
    onButtonStateChanged: (String, Boolean) -> Unit,
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
            CustomButton(
                config = button,
                onButtonStateChanged = onButtonStateChanged,
                parentHeight = maxHeight,
                parentWidth = maxWidth
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
                        x = maxWidth * image.xOffsetRel,
                        y = maxHeight * image.yOffsetRel)
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