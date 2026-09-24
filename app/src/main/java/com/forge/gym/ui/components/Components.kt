package com.forge.gym.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.forge.gym.ui.theme.Forge
import com.forge.gym.ui.theme.muscleColor

/** Springy press-to-shrink click with no ripple. */
fun Modifier.bounceClick(enabled: Boolean = true, scaleDown: Float = 0.95f, onClick: () -> Unit): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && enabled) scaleDown else 1f,
        spring(dampingRatio = 0.5f, stiffness = 700f),
        label = "press",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun ForgeCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = Forge.Surface,
    borderColor: Color = Forge.Stroke,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .then(if (onClick != null) Modifier.bounceClick(scaleDown = 0.97f, onClick = onClick) else Modifier)
            .clip(shape)
            .background(color)
            .border(1.dp, borderColor, shape)
            .padding(padding),
        content = content,
    )
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    color: Color = Forge.Lime,
    contentColor: Color = Forge.OnLime,
    enabled: Boolean = true,
    height: Dp = 56.dp,
) {
    Row(
        modifier
            .height(height)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .bounceClick(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(18.dp))
            .background(color)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = contentColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall, color = contentColor, maxLines = 1)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    contentColor: Color = Forge.Text1,
    height: Dp = 52.dp,
) = PrimaryButton(text, onClick, modifier, icon, Forge.Surface2, contentColor, true, height)

@Composable
fun CircleIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Forge.Surface2,
    tint: Color = Forge.Text1,
    size: Dp = 44.dp,
    contentDescription: String? = null,
) {
    Box(
        modifier
            .size(size)
            .bounceClick(onClick = onClick)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(size * 0.48f))
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Forge.Text1, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = Forge.Lime,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp),
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = Forge.Text2)
                Spacer(Modifier.height(2.dp))
            }
            Text(title, style = MaterialTheme.typography.headlineLarge, color = Forge.Text1)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
fun DetailTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, onBack, contentDescription = "Back")
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Forge.Text1, maxLines = 1, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** Number that counts up/down smoothly when it changes. */
@Composable
fun AnimatedNumber(
    value: Double,
    format: (Double) -> String,
    style: TextStyle,
    color: Color = Forge.Text1,
    modifier: Modifier = Modifier,
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(900, easing = FastOutSlowInEasing)) }
    Text(format(anim.value.toDouble()), style = style, color = color, modifier = modifier, maxLines = 1)
}

@Composable
fun StatBlock(label: String, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    Column(modifier) {
        value()
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
    }
}

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    stroke: Dp = 8.dp,
    color: Color = Forge.Lime,
    track: Color = Forge.Surface3,
    content: @Composable () -> Unit = {},
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { anim.animateTo(progress.coerceIn(0f, 1f), tween(1000, easing = FastOutSlowInEasing)) }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            inset(s / 2) {
                drawArc(track, -90f, 360f, false, style = Stroke(s))
                drawArc(color, -90f, 360f * anim.value, false, style = Stroke(s, cap = StrokeCap.Round))
            }
        }
        content()
    }
}

@Composable
fun MuscleTag(group: String, modifier: Modifier = Modifier) {
    val c = muscleColor(group)
    Row(
        modifier.clip(RoundedCornerShape(8.dp)).background(c.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(c))
        Spacer(Modifier.width(5.dp))
        Text(group, style = MaterialTheme.typography.labelSmall, color = c)
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, color: Color = Forge.Surface2, textColor: Color = Forge.Text2) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = textColor,
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(color).padding(horizontal = 10.dp, vertical = 5.dp),
        maxLines = 1,
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(modifier.fillMaxWidth().padding(vertical = 28.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Forge.Surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Forge.Lime, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = Forge.Text1, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Forge.Text2, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            PrimaryButton(action, onAction, height = 46.dp)
        }
    }
}

@Composable
fun ForgeField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    leading: (@Composable () -> Unit)? = null,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        textStyle = textStyle.copy(color = Forge.Text1),
        cursorBrush = SolidColor(Forge.Lime),
        modifier = modifier,
        decorationBox = { inner ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Forge.Surface2)
                    .border(1.dp, Forge.Stroke, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(10.dp))
                }
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text(placeholder, style = textStyle, color = Forge.Text3)
                    inner()
                }
            }
        },
    )
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier, min: Int = 1, max: Int = 99, suffix: String = "") {
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(Forge.Surface2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clickable(enabled = value > min) { onChange(value - 1) },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Remove, "Decrease", tint = if (value > min) Forge.Text1 else Forge.Text3, modifier = Modifier.size(18.dp)) }
        Text(
            "$value$suffix",
            style = MaterialTheme.typography.titleSmall,
            color = Forge.Text1,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(44.dp),
        )
        Box(
            Modifier.size(36.dp).clickable(enabled = value < max) { onChange(value + 1) },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Add, "Increase", tint = if (value < max) Forge.Text1 else Forge.Text3, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forge.Surface2,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = Forge.Text1) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium, color = Forge.Text2) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmText, color = if (destructive) Forge.Coral else Forge.Lime, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Forge.Text2, style = MaterialTheme.typography.labelLarge) }
        },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    placeholder: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "Save",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forge.Surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text(title, style = MaterialTheme.typography.headlineSmall, color = Forge.Text1) },
        text = { ForgeField(text, { text = it }, placeholder, keyboardOptions = keyboardOptions, singleLine = singleLine) },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim()); onDismiss() }) {
                Text(confirmText, color = Forge.Lime, style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Forge.Text2, style = MaterialTheme.typography.labelLarge) }
        },
    )
}

@Composable
fun SheetHandle() {
    Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Forge.StrokeStrong))
    }
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(Forge.Stroke))
}
