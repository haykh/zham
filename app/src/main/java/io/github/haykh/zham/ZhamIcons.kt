package io.github.haykh.zham

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Minimal line icons on a 24×24 grid: thin round-capped strokes, no fills (apart from the
 * shaded half of [ThemeSystem]). Drawn in black; `Icon` tints them to the content colour.
 * Hand-made from basic shapes, so no icon-library dependency.
 */
object ZhamIcons {
    val Clock = lineIcon("Clock", circle(9f), "M12 7.5V12l3 2")

    val Convert = lineIcon("Convert", "M16 3.5l4 4-4 4", "M20 7.5H5", "M8 12.5l-4 4 4 4", "M4 16.5h15")

    val Settings =
        lineIcon(
            "Settings",
            "M9.66 5.19L10 2.61L14 2.61L14.34 5.19A7.2 7.2 0 0 1 15.16 5.53L17.23 3.95L20.05 6.77" +
                "L18.47 8.84A7.2 7.2 0 0 1 18.81 9.66L21.39 10L21.39 14L18.81 14.34A7.2 7.2 0 0 1 18.47 15.16" +
                "L20.05 17.23L17.23 20.05L15.16 18.47A7.2 7.2 0 0 1 14.34 18.81L14 21.39L10 21.39L9.66 18.81" +
                "A7.2 7.2 0 0 1 8.84 18.47L6.77 20.05L3.95 17.23L5.53 15.16A7.2 7.2 0 0 1 5.19 14.34L2.61 14" +
                "L2.61 10L5.19 9.66A7.2 7.2 0 0 1 5.53 8.84L3.95 6.77L6.77 3.95L8.84 5.53A7.2 7.2 0 0 1 9.66 5.19Z",
            circle(3f),
        )

    val Add = lineIcon("Add", "M12 5v14", "M5 12h14")

    val Delete =
        lineIcon(
            "Delete",
            "M4 7h16",
            "M9.5 7V4.5h5V7",
            "M6 7l0.9 12.1a2 2 0 0 0 2 1.9h6.2a2 2 0 0 0 2-1.9L18 7",
            "M10 11v6",
            "M14 11v6",
        )

    val DragHandle = lineIcon("DragHandle", "M5 8h14", "M5 12h14", "M5 16h14")

    /** Counter-clockwise arrow: "back to the original". */
    val Restore = lineIcon("Restore", "M4.5 12a7.5 7.5 0 1 0 2.2-5.3", "M6.7 2.7v4h4")

    val ChevronDown = lineIcon("ChevronDown", "M6 9l6 6 6-6")

    val ThemeSystem = lineIcon("ThemeSystem", circle(9f), filled = listOf("M12 3a9 9 0 0 1 0 18z"))

    val ThemeLight =
        lineIcon(
            "ThemeLight",
            circle(4f),
            "M12 2.5v2",
            "M12 19.5v2",
            "M2.5 12h2",
            "M19.5 12h2",
            "M5.28 5.28l1.42 1.42",
            "M17.3 17.3l1.42 1.42",
            "M5.28 18.72l1.42-1.42",
            "M17.3 6.7l1.42-1.42",
        )

    val ThemeDark = lineIcon("ThemeDark", "M12 3a6.5 6.5 0 0 0 9 9a9 9 0 1 1-9-9z")
}

/** A full circle of radius [r] centred on the grid, as path data. */
private fun circle(r: Float): String = "M${12 + r} 12a$r $r 0 1 1 ${-2 * r} 0a$r $r 0 1 1 ${2 * r} 0"

private fun lineIcon(
    name: String,
    vararg strokes: String,
    filled: List<String> = emptyList(),
): ImageVector =
    ImageVector
        .Builder(name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            strokes.forEach {
                addPath(
                    pathData = PathParser().parsePathString(it).toNodes(),
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 1.75f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
            filled.forEach {
                addPath(pathData = PathParser().parsePathString(it).toNodes(), fill = SolidColor(Color.Black))
            }
        }.build()
