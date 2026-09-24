package com.forge.gym.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.forge.gym.ui.theme.Forge

/** Segmented control with a sliding indicator. */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    indicator: Color = Forge.Surface3,
    selectedText: Color = Forge.Text1,
) {
    BoxWithConstraints(
        modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(16.dp)).background(Forge.Surface)
            .border(1.dp, Forge.Stroke, RoundedCornerShape(16.dp)).padding(4.dp),
    ) {
        val w = maxWidth / options.size
        val offset by animateDpAsState(w * selected, spring(dampingRatio = 0.8f, stiffness = 400f), label = "seg")
        Box(Modifier.offset(x = offset).width(w).fillMaxHeight().clip(RoundedCornerShape(12.dp)).background(indicator))
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, o ->
                val color by animateColorAsState(if (i == selected) selectedText else Forge.Text2, label = "segText")
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(remember { MutableInteractionSource() }, null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) { Text(o, style = MaterialTheme.typography.labelLarge, color = color) }
            }
        }
    }
}
