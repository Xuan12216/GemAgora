package com.example.gemagora

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.gemagora.ui.experiments.ThoughtExperimentScreen
import com.example.gemagora.ui.experiments.ThoughtExperimentViewModel
import com.example.gemagora.ui.fallacy.FallacyScreen
import com.example.gemagora.ui.fallacy.FallacyViewModel
import com.example.gemagora.ui.hub.AgoraHubScreen
import com.example.gemagora.ui.hub.AgoraHubViewModel
import com.example.gemagora.ui.journal.JournalScreen
import com.example.gemagora.ui.journal.JournalViewModel
import com.example.gemagora.ui.roundtable.RoundTableScreen
import com.example.gemagora.ui.roundtable.RoundTableViewModel
import com.example.gemagora.ui.setup.ModelSetupViewModel
import com.example.gemagora.ui.setup.SettingsScreen
import com.example.gemagora.ui.socratic.SocraticScreen
import com.example.gemagora.ui.socratic.SocraticViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

private data class BottomDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

@Composable
fun MainNavigation(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as GemAgoraApplication
    val hazeState = rememberHazeState()
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val glassTint = if (isDark) MaterialTheme.colorScheme.surface.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.12f)

    val primaryDestinations = remember {
        listOf(
            BottomDestination("hub", "廣場", Icons.Default.AccountBalance),
            BottomDestination("experiments", "實驗", Icons.Default.Science),
            BottomDestination("roundtable", "圓桌", Icons.Default.Groups),
            BottomDestination("fallacy", "謬誤", Icons.Default.Search),
            BottomDestination("journal", "日記", Icons.AutoMirrored.Filled.MenuBook)
        )
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val isPrimaryRoute = remember(currentRoute, primaryDestinations) {
        primaryDestinations.any { currentRoute == it.route }
    }
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val toolbarContentPadding = 60.dp + navigationBarHeight + 8.dp

    var isNavBarCompact by rememberSaveable { mutableStateOf(false) }

    // Reset to full expanded size whenever the user navigates between top-level tabs
    LaunchedEffect(currentRoute) {
        isNavBarCompact = false
    }

    val currentIsPrimaryRoute by rememberUpdatedState(isPrimaryRoute)

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (!currentIsPrimaryRoute) return Offset.Zero
                val delta = available.y
                // Swiping up (reading downwards): delta < -8f -> shrink navigation bar
                // Swiping down (reading upwards / back to top): delta > 8f -> expand navigation bar
                if (delta < -8f && !isNavBarCompact) {
                    isNavBarCompact = true
                } else if (delta > 8f && isNavBarCompact) {
                    isNavBarCompact = false
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!currentIsPrimaryRoute) return Offset.Zero
                // If user reached top edge and pulled down further
                if (available.y > 0f && isNavBarCompact) {
                    isNavBarCompact = false
                }
                return Offset.Zero
            }
        }
    }

    val navigateToTopLevel: (String) -> Unit = remember(navController) {
        { route ->
            val startDestinationId = navController.graph.findStartDestination().id
            val isStartDestination = route == "hub"
            isNavBarCompact = false
            navController.navigate(route) {
                popUpTo(startDestinationId) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = !isStartDestination
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { outerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(outerPadding)
                .nestedScroll(nestedScrollConnection)
        ) {
            NavHost(
                navController = navController,
                startDestination = "hub",
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState)
            ) {
                // 1. Agora Hub
                composable("hub") {
                    val hubVm: AgoraHubViewModel = viewModel(
                        factory = AgoraHubViewModel.Factory(
                            app.gemmaHelper,
                            app.userPreferenceStore,
                            app.ttsManager
                        )
                    )
                    AgoraHubScreen(
                        viewModel = hubVm,
                        onNavigateToSocratic = { seed ->
                            if (!seed.isNullOrBlank()) {
                                navController.navigate("socratic?seed=" + java.net.URLEncoder.encode(seed, "UTF-8"))
                            } else {
                                navigateToTopLevel("socratic")
                            }
                        },
                        onNavigateToExperiments = { navigateToTopLevel("experiments") },
                        onNavigateToRoundTable = { navigateToTopLevel("roundtable") },
                        onNavigateToFallacy = { navigateToTopLevel("fallacy") },
                        onNavigateToJournal = { navigateToTopLevel("journal") },
                        onNavigateToSettings = { navController.navigate("setup?tab=0") },
                        bottomContentPadding = if (isPrimaryRoute) toolbarContentPadding else 0.dp
                    )
                }

                // 2. Socratic Dialogue
                composable(
                    route = "socratic?seed={seed}",
                    arguments = listOf(navArgument("seed") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { entry ->
                    val seed = entry.arguments?.getString("seed")?.let {
                        java.net.URLDecoder.decode(it, "UTF-8")
                    }
                    val socraticVm: SocraticViewModel = viewModel(
                        factory = SocraticViewModel.Factory(
                            app.chatRepository,
                            app.gemmaHelper,
                            app.historyRepository,
                            app.userPreferenceStore,
                            app.ttsManager
                        )
                    )
                    SocraticScreen(
                        viewModel = socraticVm,
                        initialPrompt = seed,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToSettings = { navController.navigate("setup?tab=1") }
                    )
                }

                // 3. Thought Experiments
                composable("experiments") {
                    val expVm: ThoughtExperimentViewModel = viewModel(
                        factory = ThoughtExperimentViewModel.Factory(
                            app.gemmaHelper,
                            app.historyRepository,
                            app.userPreferenceStore,
                            app.ttsManager
                        )
                    )
                    ThoughtExperimentScreen(
                        viewModel = expVm,
                        bottomContentPadding = if (isPrimaryRoute) toolbarContentPadding else 0.dp
                    )
                }

                // 4. Stoic Journal
                composable("journal") {
                    val journalVm: JournalViewModel = viewModel(
                        factory = JournalViewModel.Factory(
                            app.journalRepository,
                            app.gemmaHelper,
                            app.userPreferenceStore,
                            app.ttsManager
                        )
                    )
                    JournalScreen(
                        viewModel = journalVm,
                        bottomContentPadding = if (isPrimaryRoute) toolbarContentPadding else 0.dp
                    )
                }

                // 5. Multi-school Round Table
                composable("roundtable") {
                    val roundTableVm: RoundTableViewModel = viewModel(
                        factory = RoundTableViewModel.Factory(
                            app.gemmaHelper,
                            app.historyRepository,
                            app.userPreferenceStore,
                            app.ttsManager
                        )
                    )
                    RoundTableScreen(
                        viewModel = roundTableVm,
                        bottomContentPadding = if (isPrimaryRoute) toolbarContentPadding else 0.dp
                    )
                }

                // 6. Fallacy Inspector
                composable("fallacy") {
                    val fallacyVm: FallacyViewModel = viewModel(
                        factory = FallacyViewModel.Factory(
                            app.gemmaHelper,
                            app.historyRepository,
                            app.userPreferenceStore,
                            app.ttsManager
                        )
                    )
                    FallacyScreen(
                        viewModel = fallacyVm,
                        onNavigateBack = null,
                        bottomContentPadding = if (isPrimaryRoute) toolbarContentPadding else 0.dp
                    )
                }

                // 7. Settings
                composable(
                    route = "setup?tab={tab}",
                    arguments = listOf(navArgument("tab") {
                        type = NavType.IntType
                        defaultValue = 0
                    })
                ) { entry ->
                    val setupVm: ModelSetupViewModel = viewModel(
                        factory = ModelSetupViewModel.Factory(
                            app.modelRepository,
                            app.userPreferenceStore,
                            app.modelManager,
                            app.ttsManager
                        )
                    )
                    SettingsScreen(
                        viewModel = setupVm,
                        initialTab = entry.arguments?.getInt("tab") ?: 0,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            val animatedBottomPadding by animateDpAsState(
                targetValue = if (isNavBarCompact) 6.dp else 8.dp,
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                label = "bottomPadding"
            )

            // Frosted Glass Floating Bottom Navigation Toolbar
            AnimatedVisibility(
                visible = isPrimaryRoute,
                enter = fadeIn(animationSpec = tween(220)) +
                        slideInVertically(animationSpec = tween(220), initialOffsetY = { it }),
                exit = fadeOut(animationSpec = tween(160)) +
                        slideOutVertically(animationSpec = tween(160), targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = animatedBottomPadding)
            ) {
                val animatedSpacing by animateDpAsState(
                    targetValue = if (isNavBarCompact) 6.dp else 8.dp,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    label = "navSpacing"
                )
                val animatedSocraticSize by animateDpAsState(
                    targetValue = if (isNavBarCompact) 48.dp else 56.dp,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    label = "socraticSize"
                )
                val animatedSocraticIconSize by animateDpAsState(
                    targetValue = if (isNavBarCompact) 21.dp else 24.dp,
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
                    label = "socraticIconSize"
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(animatedSpacing),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FloatingDestinationToolbar(
                        destinations = primaryDestinations,
                        currentRoute = currentRoute,
                        isCompact = isNavBarCompact,
                        hazeState = hazeState,
                        onNavigate = { route ->
                            isNavBarCompact = false
                            navigateToTopLevel(route)
                        }
                    )

                    // Quick AI Socratic Button
                    Box(
                        modifier = Modifier
                            .size(animatedSocraticSize)
                            .shadow(
                                elevation = if (isDark) 0.dp else 3.dp,
                                shape = CircleShape,
                                ambientColor = Color.Black.copy(alpha = 0.06f),
                                spotColor = Color.Black.copy(alpha = 0.08f)
                            )
                            .clip(CircleShape)
                            .hazeEffect(state = hazeState) {
                                blurRadius = 6.dp
                                tints = listOf(HazeTint(glassTint))
                                noiseFactor = 0f
                            }
                            .background(
                                if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.22f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.30f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f),
                                shape = CircleShape
                            )
                            .clickable {
                                isNavBarCompact = false
                                navController.navigate("socratic") { launchSingleTop = true }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "蘇格拉底反詰",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(animatedSocraticIconSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingDestinationToolbar(
    destinations: List<BottomDestination>,
    currentRoute: String?,
    isCompact: Boolean,
    hazeState: HazeState,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val toolbarGlassTint = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.32f)
    } else {
        Color.White.copy(alpha = 0.12f)
    }

    val animatedHeight by animateDpAsState(
        targetValue = if (isCompact) 48.dp else 56.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "toolbarHeight"
    )
    val animatedCornerRadius by animateDpAsState(
        targetValue = if (isCompact) 24.dp else 28.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "cornerRadius"
    )
    val animatedSlotSpacing by animateDpAsState(
        targetValue = if (isCompact) 42.dp else 52.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "slotSpacing"
    )
    val animatedCapsuleWidth by animateDpAsState(
        targetValue = if (isCompact) 56.dp else 68.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "capsuleWidth"
    )
    val animatedCapsuleHeight by animateDpAsState(
        targetValue = if (isCompact) 40.dp else 46.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "capsuleHeight"
    )
    val animatedCapsuleRadius by animateDpAsState(
        targetValue = if (isCompact) 20.dp else 23.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "capsuleRadius"
    )
    val animatedEndMargin by animateDpAsState(
        targetValue = if (isCompact) 4.dp else 5.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "endMargin"
    )
    val animatedInnerSlotPadding by animateDpAsState(
        targetValue = if (isCompact) 11.dp else 13.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "innerSlotPadding"
    )
    val animatedIconSize by animateDpAsState(
        targetValue = if (isCompact) 21.dp else 24.dp,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
        label = "iconSize"
    )

    val pillShape = RoundedCornerShape(animatedCornerRadius)
    val capsuleShape = RoundedCornerShape(animatedCapsuleRadius)

    val selectedIndex = destinations.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
    val animatedCapsuleOffset by animateDpAsState(
        targetValue = animatedEndMargin + (animatedSlotSpacing.value * selectedIndex).dp,
        animationSpec = spring(
            dampingRatio = 0.82f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "capsuleOffset"
    )

    Surface(
        modifier = modifier
            .height(animatedHeight)
            .shadow(
                elevation = if (isDark) 0.dp else 4.dp,
                shape = pillShape,
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(pillShape)
            .hazeEffect(state = hazeState) {
                blurRadius = 6.dp
                tints = listOf(HazeTint(toolbarGlassTint))
                noiseFactor = 0f
            },
        shape = pillShape,
        color = if (isDark) MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.38f)
                else MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.50f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDark) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
        ),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier.fillMaxHeight(),
            contentAlignment = Alignment.CenterStart
        ) {
            // Sliding Capsule Indicator (underlay that overlaps into adjacent slot space)
            Box(
                modifier = Modifier
                    .offset(x = animatedCapsuleOffset)
                    .size(width = animatedCapsuleWidth, height = animatedCapsuleHeight)
                    .clip(capsuleShape)
                    .background(
                        if (isDark) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.82f)
                        else MaterialTheme.colorScheme.primaryContainer
                    )
            )

            // Equal-spaced Stationary Icon Slots
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = animatedInnerSlotPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                destinations.forEach { destination ->
                    val isSelected = destination.route == currentRoute
                    val iconTint by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(durationMillis = 180),
                        label = "iconTint_${destination.route}"
                    )

                    Box(
                        modifier = Modifier
                            .width(animatedSlotSpacing)
                            .fillMaxHeight()
                            .clip(capsuleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                onNavigate(destination.route)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            tint = iconTint,
                            modifier = Modifier.size(animatedIconSize)
                        )
                    }
                }
            }
        }
    }
}
