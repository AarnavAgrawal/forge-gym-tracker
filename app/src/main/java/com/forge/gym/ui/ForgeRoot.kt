package com.forge.gym.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.forge.gym.AppContainer
import com.forge.gym.data.AppSettings
import com.forge.gym.domain.formatClock
import com.forge.gym.ui.components.bounceClick
import com.forge.gym.ui.screens.CalendarScreen
import com.forge.gym.ui.screens.ExerciseDetailScreen
import com.forge.gym.ui.screens.HistoryScreen
import com.forge.gym.ui.screens.HomeScreen
import com.forge.gym.ui.screens.LibraryScreen
import com.forge.gym.ui.screens.ProgressScreen
import com.forge.gym.ui.screens.SettingsScreen
import com.forge.gym.ui.screens.SummaryScreen
import com.forge.gym.ui.screens.TemplateEditorScreen
import com.forge.gym.ui.screens.WorkoutDetailScreen
import com.forge.gym.ui.screens.WorkoutEditorScreen
import com.forge.gym.ui.theme.Forge
import kotlinx.coroutines.delay

val LocalContainer = staticCompositionLocalOf<AppContainer> { error("No container") }

@Composable
fun rememberSettings(): AppSettings {
    val s by LocalContainer.current.settings.state.collectAsStateWithLifecycle()
    return s
}

object Routes {
    const val HOME = "home"
    const val CALENDAR = "calendar"
    const val PROGRESS = "progress"
    const val LIBRARY = "library"
    const val WORKOUT = "workout/{id}"
    const val SUMMARY = "summary/{id}"
    const val DETAIL = "detail/{id}"
    const val EXERCISE = "exercise/{id}"
    const val TEMPLATE = "template/{id}"
    const val HISTORY = "history"
    const val SETTINGS = "settings"

    fun workout(id: Long) = "workout/$id"
    fun summary(id: Long) = "summary/$id"
    fun detail(id: Long) = "detail/$id"
    fun exercise(id: Long) = "exercise/$id"
    fun template(id: Long) = "template/$id"
}

private val tabRoutes = setOf(Routes.HOME, Routes.CALENDAR, Routes.PROGRESS, Routes.LIBRARY)
private val sheetRoutes = setOf(Routes.WORKOUT, Routes.SUMMARY)
private val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch() =
    initialState.destination.route in tabRoutes && targetState.destination.route in tabRoutes

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition = when {
    isTabSwitch() -> fadeIn(tween(240, delayMillis = 40)) + scaleIn(tween(300, easing = Emphasized), initialScale = 0.985f)
    targetState.destination.route in sheetRoutes ->
        slideInVertically(tween(420, easing = Emphasized)) { it / 4 } + fadeIn(tween(300))
    else -> slideInHorizontally(tween(380, easing = Emphasized)) { it / 5 } + fadeIn(tween(280))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition = when {
    isTabSwitch() -> fadeOut(tween(160))
    targetState.destination.route in sheetRoutes -> fadeOut(tween(260)) + scaleOut(tween(380, easing = Emphasized), targetScale = 0.95f)
    else -> slideOutHorizontally(tween(380, easing = Emphasized)) { -it / 10 } + fadeOut(tween(240))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition = when {
    isTabSwitch() -> fadeIn(tween(240))
    initialState.destination.route in sheetRoutes -> fadeIn(tween(300)) + scaleIn(tween(380, easing = Emphasized), initialScale = 0.95f)
    else -> slideInHorizontally(tween(380, easing = Emphasized)) { -it / 10 } + fadeIn(tween(280))
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition = when {
    isTabSwitch() -> fadeOut(tween(160))
    initialState.destination.route in sheetRoutes -> slideOutVertically(tween(380, easing = Emphasized)) { it / 4 } + fadeOut(tween(260))
    else -> slideOutHorizontally(tween(380, easing = Emphasized)) { it / 5 } + fadeOut(tween(240))
}

/** Space tab screens must leave at the bottom for the floating nav bar. */
@Composable
fun bottomBarSpace(): Dp {
    val active by LocalContainer.current.repo.activeWorkout.collectAsStateWithLifecycle()
    return WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 104.dp + if (active != null) 72.dp else 0.dp
}

@Composable
fun ForgeRoot(container: AppContainer) {
    CompositionLocalProvider(LocalContainer provides container) {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val route = entry?.destination?.route
        RestTimerAlarm()

        Box(Modifier.fillMaxSize().background(Forge.Bg)) {
            NavHost(
                navController = nav,
                startDestination = Routes.HOME,
                enterTransition = { enter() },
                exitTransition = { exit() },
                popEnterTransition = { popEnter() },
                popExitTransition = { popExit() },
            ) {
                val idArg = listOf(navArgument("id") { type = NavType.LongType })
                composable(Routes.HOME) { HomeScreen(nav) }
                composable(Routes.CALENDAR) { CalendarScreen(nav) }
                composable(Routes.PROGRESS) { ProgressScreen(nav) }
                composable(Routes.LIBRARY) { LibraryScreen(nav) }
                composable(Routes.HISTORY) { HistoryScreen(nav) }
                composable(Routes.SETTINGS) { SettingsScreen(nav) }
                composable(Routes.WORKOUT, idArg) { WorkoutEditorScreen(nav, it.arguments!!.getLong("id")) }
                composable(Routes.SUMMARY, idArg) { SummaryScreen(nav, it.arguments!!.getLong("id")) }
                composable(Routes.DETAIL, idArg) { WorkoutDetailScreen(nav, it.arguments!!.getLong("id")) }
                composable(Routes.EXERCISE, idArg) { ExerciseDetailScreen(nav, it.arguments!!.getLong("id")) }
                composable(Routes.TEMPLATE, idArg) { TemplateEditorScreen(nav, it.arguments!!.getLong("id")) }
            }

            val showBar = route in tabRoutes
            AnimatedVisibility(showBar, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier.fillMaxWidth().height(bottomBarSpace() + 16.dp)
                        .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Forge.Bg.copy(alpha = 0f), Forge.Bg.copy(alpha = 0.92f), Forge.Bg))),
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ActiveWorkoutBanner(visible = showBar, nav = nav)
                AnimatedVisibility(
                    visible = showBar,
                    enter = slideInVertically(tween(380, easing = Emphasized)) { it * 2 } + fadeIn(),
                    exit = slideOutVertically(tween(300, easing = Emphasized)) { it * 2 } + fadeOut(),
                ) {
                    ForgeBottomBar(route) { target ->
                        if (target != route) {
                            nav.navigate(target) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem(Routes.HOME, "Home", Icons.Rounded.Home),
    TabItem(Routes.CALENDAR, "Calendar", Icons.Rounded.CalendarMonth),
    TabItem(Routes.PROGRESS, "Progress", Icons.Rounded.Insights),
    TabItem(Routes.LIBRARY, "Library", Icons.Rounded.FitnessCenter),
)

@Composable
private fun ForgeBottomBar(current: String?, onSelect: (String) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(Forge.Surface2.copy(alpha = 0.97f))
            .border(1.dp, Forge.StrokeStrong, RoundedCornerShape(32.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == current
            val bg by animateColorAsState(if (selected) Forge.Lime else Forge.Surface2.copy(alpha = 0f), tween(260), label = "tabBg")
            val fg by animateColorAsState(if (selected) Forge.OnLime else Forge.Text2, tween(260), label = "tabFg")
            Row(
                Modifier
                    .clip(RoundedCornerShape(26.dp))
                    .background(bg)
                    .bounceClick { onSelect(tab.route) }
                    .animateContentSize(spring(dampingRatio = 0.75f, stiffness = 400f))
                    .padding(horizontal = if (selected) 18.dp else 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(tab.icon, tab.label, tint = fg, modifier = Modifier.size(22.dp))
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Text(tab.label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkoutBanner(visible: Boolean, nav: NavController) {
    val active by LocalContainer.current.repo.activeWorkout.collectAsStateWithLifecycle()
    val w = active
    AnimatedVisibility(
        visible = visible && w != null,
        enter = expandVertically(tween(350, easing = Emphasized)) + fadeIn(),
        exit = shrinkVertically(tween(300, easing = Emphasized)) + fadeOut(),
    ) {
        if (w == null) return@AnimatedVisibility
        var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
        LaunchedEffect(Unit) {
            while (true) {
                now = System.currentTimeMillis(); delay(1000)
            }
        }
        val pulse = rememberInfiniteTransition(label = "pulse")
        val dot by pulse.animateFloat(0.6f, 1.2f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dot")
        Row(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp)
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Forge.LimeDim)
                .border(1.dp, Forge.Lime.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .bounceClick { nav.navigate(Routes.workout(w.id)) }
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).scale(dot).clip(CircleShape).background(Forge.Lime))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(w.name, style = MaterialTheme.typography.titleSmall, color = Forge.Text1, maxLines = 1)
                Text("In progress  ·  ${formatClock(now - w.startTime)}", style = MaterialTheme.typography.bodySmall, color = Forge.Lime)
            }
            Text("Resume", style = MaterialTheme.typography.labelLarge, color = Forge.Lime)
            Icon(Icons.Rounded.ChevronRight, null, tint = Forge.Lime)
        }
    }
}

@Composable
private fun RestTimerAlarm() {
    val container = LocalContainer.current
    val state by container.restTimer.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(state) {
        val s = state ?: return@LaunchedEffect
        val wait = s.endAt - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        vibrate(context)
        container.restTimer.skip()
    }
}

fun vibrate(context: Context, pattern: LongArray = longArrayOf(0, 200, 120, 200)) {
    val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
    runCatching { vibrator?.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
}
