package dev.sjaramillo.nemi.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

internal data class Palette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val accent: Color,
    val track: Color,
    val bar: Color,
)

internal val LightPalette =
    Palette(Color(0xFFF8F7F2), Color.White, Color(0xFF202722), Color(0xFF3D5948), Color(0xFFECF2ED), Color(0xFF49728C))
internal val DarkPalette =
    Palette(Color(0xFF111914), Color(0xFF1C2920), Color(0xFFE6F0E7), Color(0xFF9BDAAA), Color(0xFF35483A), Color(0xFF8EB4CD))
internal val LocalPalette = staticCompositionLocalOf { LightPalette }

@Composable
internal fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().background(LocalPalette.current.surface, RoundedCornerShape(24.dp)).padding(20.dp),
    ) {
        AppText(title, Modifier.fillMaxWidth().semantics { heading() }, size = 18, bold = true, align = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
internal fun ScreenToolbar(
    title: String,
    actionAtStart: Boolean = false,
    action: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp).padding(horizontal = 16.dp)) {
        AppText(
            title,
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 48.dp)
                .semantics { heading() },
            size = 24,
            bold = true,
            align = TextAlign.Center,
        )
        Box(Modifier.align(if (actionAtStart) Alignment.CenterStart else Alignment.CenterEnd)) {
            action()
        }
    }
}

@Composable
internal fun SectionDivider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(LocalPalette.current.text.copy(alpha = 0.10f)))
}

@Composable
internal fun ToolbarAction(
    icon: Int,
    label: String,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = label }
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            colorFilter = ColorFilter.tint(LocalPalette.current.accent),
        )
    }
}

@Composable
internal fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .safeDrawingPadding()
                .imePadding()
                .fillMaxWidth()
                .background(LocalPalette.current.surface, RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppText(title, Modifier.semantics { heading() }, size = 24, bold = true)
            content()
        }
    }
}

@Composable
internal fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 16,
    bold: Boolean = false,
    accent: Boolean = false,
    secondary: Boolean = false,
    align: TextAlign = TextAlign.Start,
) {
    val palette = LocalPalette.current
    BasicText(
        text,
        modifier,
        style =
            TextStyle(
                color =
                    when {
                        accent -> palette.accent
                        secondary -> palette.text.copy(alpha = 0.72f)
                        else -> palette.text
                    },
                fontSize = size.sp,
                textAlign = align,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            ),
    )
}

@Composable
internal fun TextAction(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        AppText(label, bold = true, accent = enabled)
    }
}
