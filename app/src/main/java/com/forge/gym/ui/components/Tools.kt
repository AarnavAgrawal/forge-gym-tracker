package com.forge.gym.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.forge.gym.data.WeightUnit
import com.forge.gym.domain.e1rm
import com.forge.gym.domain.trimNum
import com.forge.gym.ui.LocalContainer
import com.forge.gym.ui.rememberSettings
import com.forge.gym.ui.theme.Forge
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgeSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Forge.Surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = { SheetHandle() },
    ) { content() }
}

private fun plateColor(index: Int): Color = listOf(Forge.Coral, Forge.Blue, Forge.Amber, Forge.Lime, Forge.Teal, Forge.Violet, Forge.Pink)[index.coerceAtLeast(0) % 7]

/** Greedy plate breakdown for one side of the bar. Returns plates and leftover weight per side. */
fun platesPerSide(target: Double, bar: Double, plates: List<Double>): Pair<List<Double>, Double> {
    var remaining = (target - bar) / 2
    if (remaining <= 0) return emptyList<Double>() to 0.0
    val out = mutableListOf<Double>()
    for (p in plates) {
        while (remaining >= p - 1e-9) {
            out += p
            remaining -= p
        }
    }
    return out to remaining
}

@Composable
fun PlateCalculatorSheet(onDismiss: () -> Unit) {
    val settings = rememberSettings()
    val store = LocalContainer.current.settings
    val unit = settings.unit
    val plates = if (unit == WeightUnit.LB) listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5) else listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25)
    val barOptions = if (unit == WeightUnit.LB) listOf(45.0, 35.0, 15.0) else listOf(20.0, 15.0, 10.0)
    val bar = unit.fromKg(settings.barWeightKg).let { b -> barOptions.minBy { kotlin.math.abs(it - b) } }
    var target by remember { mutableStateOf("") }
    val t = target.toDoubleOrNull() ?: 0.0
    val (perSide, left) = platesPerSide(t, bar, plates)

    ForgeSheet(onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            Text("Plate calculator", style = MaterialTheme.typography.headlineSmall, color = Forge.Text1)
            Spacer(Modifier.height(4.dp))
            Text("What to load on each side of the bar", style = MaterialTheme.typography.bodyMedium, color = Forge.Text2)
            Spacer(Modifier.height(18.dp))
            ForgeField(
                target, { v -> target = v.filter { it.isDigit() || it == '.' }.take(6) }, "Target weight (${unit.label})",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Bar", style = MaterialTheme.typography.labelLarge, color = Forge.Text2)
                barOptions.forEach { b ->
                    val sel = b == bar
                    Pill(
                        "${trimNum(b)} ${unit.label}",
                        Modifier.bounceClick { store.update { it.copy(barWeightKg = unit.toKg(b)) } },
                        color = if (sel) Forge.Lime else Forge.Surface2,
                        textColor = if (sel) Forge.OnLime else Forge.Text2,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(20.dp)).background(Forge.Surface2),
                contentAlignment = Alignment.CenterStart,
            ) {
                Box(Modifier.padding(start = 16.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(4.dp)).background(Forge.StrokeStrong))
                Box(Modifier.padding(start = 16.dp).width(24.dp).height(26.dp).clip(RoundedCornerShape(4.dp)).background(Forge.Text3))
                AnimatedContent(
                    targetState = perSide,
                    transitionSpec = { (fadeIn(tween(220)) + scaleIn(spring(dampingRatio = 0.6f), initialScale = 0.85f)) togetherWith fadeOut(tween(120)) },
                    label = "plates",
                ) { list ->
                    Row(Modifier.padding(start = 44.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        list.forEach { p ->
                            val idx = plates.indexOf(p)
                            val h = 44 + (plates.size - idx) * 12
                            Box(
                                Modifier.width(if (idx < 2) 16.dp else 12.dp).height(h.dp).clip(RoundedCornerShape(4.dp)).background(plateColor(idx)),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (t > 0 && t < bar) {
                Text("That is lighter than the bar.", style = MaterialTheme.typography.bodyMedium, color = Forge.Coral)
            } else if (perSide.isNotEmpty()) {
                Text("Per side", style = MaterialTheme.typography.labelMedium, color = Forge.Text2)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    perSide.groupingBy { it }.eachCount().forEach { (p, n) ->
                        val idx = plates.indexOf(p)
                        Row(
                            Modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, plateColor(idx).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text("${n}x ", style = MaterialTheme.typography.labelLarge, color = Forge.Text2)
                            Text(trimNum(p, 2), style = MaterialTheme.typography.labelLarge, color = plateColor(idx))
                        }
                    }
                }
                if (left > 0.01) {
                    Spacer(Modifier.height(10.dp))
                    Text("${trimNum(left * 2, 2)} ${unit.label} can't be made with standard plates.", style = MaterialTheme.typography.bodySmall, color = Forge.Amber)
                }
            } else if (t > 0) {
                Text("Just the empty bar.", style = MaterialTheme.typography.bodyMedium, color = Forge.Text2)
            }
        }
    }
}

@Composable
fun OneRepMaxSheet(onDismiss: () -> Unit) {
    val unit = rememberSettings().unit
    var weight by remember { mutableStateOf("") }
    var reps by remember { mutableStateOf("") }
    val w = weight.toDoubleOrNull() ?: 0.0
    val r = reps.toIntOrNull() ?: 0
    val max = e1rm(w, r)

    ForgeSheet(onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 24.dp),
        ) {
            Text("1RM calculator", style = MaterialTheme.typography.headlineSmall, color = Forge.Text1)
            Spacer(Modifier.height(4.dp))
            Text("Estimate your one-rep max (Epley formula)", style = MaterialTheme.typography.bodyMedium, color = Forge.Text2)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ForgeField(
                    weight, { v -> weight = v.filter { it.isDigit() || it == '.' }.take(6) }, "Weight (${unit.label})", Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                ForgeField(
                    reps, { v -> reps = v.filter { it.isDigit() }.take(2) }, "Reps", Modifier.weight(0.7f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
            }
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Forge.LimeDim).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("ESTIMATED 1RM", style = MaterialTheme.typography.labelSmall, color = Forge.Lime)
                AnimatedNumber(max, { if (it <= 0) "--" else trimNum(it, 1) }, MaterialTheme.typography.displayMedium, Forge.Lime)
                Text(unit.label, style = MaterialTheme.typography.labelLarge, color = Forge.Lime.copy(alpha = 0.7f))
            }
            if (max > 0) {
                Spacer(Modifier.height(18.dp))
                listOf(95, 90, 85, 80, 75, 70, 65, 60).forEach { pct ->
                    val repsAt = (30.0 * (100.0 / pct - 1)).roundToInt().coerceAtLeast(1)
                    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("$pct%", style = MaterialTheme.typography.titleSmall, color = Forge.Text1, modifier = Modifier.width(56.dp))
                        Text("~$repsAt reps", style = MaterialTheme.typography.bodyMedium, color = Forge.Text2, modifier = Modifier.weight(1f))
                        Text("${trimNum(max * pct / 100, 1)} ${unit.label}", style = MaterialTheme.typography.titleSmall, color = Forge.Text1, textAlign = TextAlign.End)
                    }
                    Divider()
                }
            }
        }
    }
}
