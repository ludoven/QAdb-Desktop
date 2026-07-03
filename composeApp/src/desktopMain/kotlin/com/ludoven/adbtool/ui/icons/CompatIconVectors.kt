package com.ludoven.adbtool.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

object CompatIconVectors {
    val Add by lazy { stroke("Add", "M24 8V40", "M8 24H40") }
    val ArrowBack by lazy { stroke("ArrowBack", "M30 12L18 24L30 36", "M19 24H42") }
    val ArrowForward by lazy { stroke("ArrowForward", "M18 12L30 24L18 36", "M6 24H29") }
    val ArrowUpward by lazy { stroke("ArrowUpward", "M24 8L12 20", "M24 8L36 20", "M24 10V42") }
    val ArrowDownward by lazy { stroke("ArrowDownward", "M24 40L12 28", "M24 40L36 28", "M24 6V38") }
    val ArrowDropDown by lazy { filled("ArrowDropDown", "M14 18L24 30L34 18Z") }
    val ArrowDropUp by lazy { filled("ArrowDropUp", "M14 30L24 18L34 30Z") }
    val Check by lazy { stroke("Check", "M10 25L20 35L40 13") }
    val ChevronRight by lazy { stroke("ChevronRight", "M19 12L31 24L19 36") }
    val Close by lazy { stroke("Close", "M14 14L34 34", "M34 14L14 34") }
    val Delete by lazy { stroke("Delete", "M10 14H38", "M18 14V10H30V14", "M16 18V38H32V18", "M21 22V34", "M27 22V34") }
    val Edit by lazy { stroke("Edit", "M12 34L14 42L22 40L39 23L29 13L12 30V34Z", "M27 15L37 25") }
    val External by lazy { stroke("OpenInNew", "M18 10H10V38H38V30", "M28 10H38V20", "M37 11L22 26") }
    val Eye by lazy { stroke("Visibility", "M4 24C9 14 16 10 24 10C32 10 39 14 44 24C39 34 32 38 24 38C16 38 9 34 4 24Z", "M24 30C27.3137 30 30 27.3137 30 24C30 20.6863 27.3137 18 24 18C20.6863 18 18 20.6863 18 24C18 27.3137 20.6863 30 24 30Z") }
    val EyeOff by lazy { stroke("VisibilityOff", "M7 7L41 41", "M10 17C8 19 6 21 4 24C9 34 16 38 24 38C28 38 31.5 37 34.7 35", "M16 12.5C18.5 11 21.1 10 24 10C32 10 39 14 44 24C42.5 27 40.6 29.5 38.5 31.5") }
    val Grid by lazy { stroke("Grid", "M8 8H20V20H8Z", "M28 8H40V20H28Z", "M8 28H20V40H8Z", "M28 28H40V40H28Z") }
    val Keyboard by lazy { stroke("Keyboard", "M6 12H42V36H6Z", "M12 20H14", "M20 20H22", "M28 20H30", "M36 20H38", "M14 28H34") }
    val KeyboardArrowDown by lazy { stroke("KeyboardArrowDown", "M14 19L24 29L34 19") }
    val KeyboardArrowLeft by lazy { stroke("KeyboardArrowLeft", "M29 14L19 24L29 34") }
    val KeyboardArrowRight by lazy { stroke("KeyboardArrowRight", "M19 14L29 24L19 34") }
    val KeyboardArrowUp by lazy { stroke("KeyboardArrowUp", "M14 29L24 19L34 29") }
    val Lock by lazy { stroke("Lock", "M12 22H36V42H12Z", "M18 22V16C18 10.5 21 7 24 7C27 7 30 10.5 30 16V22", "M24 30V35") }
    val Menu by lazy { stroke("Menu", "M8 14H40", "M8 24H40", "M8 34H40") }
    val Pause by lazy { filled("Pause", "M15 10H21V38H15Z", "M27 10H33V38H27Z") }
    val PlayArrow by lazy { filled("PlayArrow", "M16 10V38L38 24Z") }
    val Power by lazy { stroke("Power", "M24 6V24", "M15 12C9 15.5 6 21 7 28C8 37 15 43 24 43C33 43 40 37 41 28C42 21 39 15.5 33 12") }
    val ScreenRotation by lazy { stroke("ScreenRotation", "M15 8L38 31L29 40L6 17Z", "M34 9C39 13 42 18 42 24", "M14 39C9 35 6 30 6 24") }
    val Shield by lazy { stroke("Shield", "M24 4L40 10V22C40 32 33 40 24 44C15 40 8 32 8 22V10L24 4Z", "M24 16V26", "M24 34H24.1") }
    val Sliders by lazy { stroke("Tune", "M8 14H22", "M30 14H40", "M24 10V18", "M8 24H14", "M22 24H40", "M16 20V28", "M8 34H28", "M36 34H40", "M30 30V38") }
    val Star by lazy { filled("Star", "M24 6L29.4 17.2L42 19L33 28L35.2 41L24 34.8L12.8 41L15 28L6 19L18.6 17.2Z") }
    val Stop by lazy { filled("Stop", "M14 14H34V34H14Z") }
    val StopCircle by lazy { stroke("StopCircle", "M24 44C35.0457 44 44 35.0457 44 24C44 12.9543 35.0457 4 24 4C12.9543 4 4 12.9543 4 24C4 35.0457 12.9543 44 24 44Z", "M16 16H32V32H16Z") }
    val SwapHoriz by lazy { stroke("SwapHoriz", "M10 18H36", "M30 12L36 18L30 24", "M38 30H12", "M18 24L12 30L18 36") }
    val Upload by lazy { stroke("Upload", "M6 36V42H42V36", "M24 8V32", "M14 18L24 8L34 18") }
    val VolumeDown by lazy { stroke("VolumeDown", "M8 20V28H16L26 38V10L16 20H8Z", "M32 19C34 22 34 26 32 29") }
    val VolumeMute by lazy { stroke("VolumeMute", "M8 20V28H16L26 38V10L16 20H8Z", "M34 18L42 30", "M42 18L34 30") }
    val VolumeUp by lazy { stroke("VolumeUp", "M8 20V28H16L26 38V10L16 20H8Z", "M32 16C36 20 36 28 32 32", "M36 10C43 17 43 31 36 38") }

    private fun stroke(name: String, vararg paths: String): ImageVector =
        buildIcon(name, fillPaths = emptyList(), strokePaths = paths.toList())

    private fun filled(name: String, vararg paths: String): ImageVector =
        buildIcon(name, fillPaths = paths.toList(), strokePaths = emptyList())

    private fun buildIcon(
        name: String,
        fillPaths: List<String>,
        strokePaths: List<String>
    ): ImageVector = ImageVector.Builder(
        name = "QadbCompat.$name",
        defaultWidth = 48.dp,
        defaultHeight = 48.dp,
        viewportWidth = 48f,
        viewportHeight = 48f
    ).apply {
        fillPaths.forEach { path ->
            addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = SolidColor(Color.Black)
            )
        }
        strokePaths.forEach { path ->
            addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 4f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()
}
