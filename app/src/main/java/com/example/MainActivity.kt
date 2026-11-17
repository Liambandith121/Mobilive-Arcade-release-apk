package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.UiState
import com.example.ui.auth.AuthScreen
import com.example.ui.components.CommentsBottomSheet
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.ArcadeViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

object ArcadeRoutes {
    const val HOME = "home"
    const val WATCH = "watch"
    const val PLAY = "play"
    const val SOCIAL = "social"
    const val PROFILE = "profile"
}

private data class ArcadeNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val arcadeTabs = listOf(
    ArcadeNavTab(ArcadeRoutes.HOME, "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    ArcadeNavTab(ArcadeRoutes.WATCH, "Watch", Icons.Rounded.LiveTv, Icons.Outlined.LiveTv),
    ArcadeNavTab(ArcadeRoutes.PLAY, "Play", Icons.Rounded.SportsEsports, Icons.Outlined.SportsEsports),
    ArcadeNavTab(ArcadeRoutes.SOCIAL, "Social", Icons.Rounded.Forum, Icons.Outlined.Forum),
    ArcadeNavTab(ArcadeRoutes.PROFILE, "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
)

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MobiliveArcadeTheme(darkTheme = true) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MobiliveArcadeApp(currentUserId = user.uid)
        }
    }
}

@Composable
fun MobiliveArcadeApp(currentUserId: String) {
    val viewModel: ArcadeViewModel = viewModel(
        key = currentUserId,
        factory = ArcadeViewModel.factory(currentUserId)
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ArcadeRoutes.HOME

    val userPrivateState by viewModel.userPrivate.collectAsStateWithLifecycle()
    val myProfileState by viewModel.myProfile.collectAsStateWithLifecycle()
    val allProfilesState by viewModel.allProfiles.collectAsStateWithLifecycle()
    val postsState by viewModel.posts.collectAsStateWithLifecycle()
    val streamsState by viewModel.streams.collectAsStateWithLifecycle()
    val worldsState by viewModel.worlds.collectAsStateWithLifecycle()
    val partiesState by viewModel.parties.collectAsStateWithLifecycle()
    val communitiesState by viewModel.communities.collectAsStateWithLifecycle()
    val tournamentsState by viewModel.tournaments.collectAsStateWithLifecycle()
    val friendRequestsState by viewModel.friendRequests.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()
    val galleryState by viewModel.gallery.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notifications.collectAsStateWithLifecycle()

    val postForComments by viewModel.postForComments.collectAsStateWithLifecycle()
    val commentsState by viewModel.selectedPostComments.collectAsStateWithLifecycle()
    val levelUpBanner by viewModel.levelUpBanner.collectAsStateWithLifecycle()

    val userPrivate = (userPrivateState as? UiState.Success)?.data
    val myProfile = (myProfileState as? UiState.Success)?.data

    var selectedStreamId by remember { mutableStateOf<String?>(null) }
    var selectedConversationId by remember { mutableStateOf<String?>(null) }
    var openProfileSettings by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    fun navigateTab(route: String) {
        if (route != ArcadeRoutes.PROFILE) openProfileSettings = false
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (selectedStreamId != null && currentRoute == ArcadeRoutes.WATCH) {
        BackHandler { selectedStreamId = null }
    }
    if (selectedConversationId != null && currentRoute == ArcadeRoutes.SOCIAL) {
        BackHandler { selectedConversationId = null }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianBg,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail && selectedStreamId == null && selectedConversationId == null) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        arcadeTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanNeon,
                                    selectedTextColor = CyanNeon,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        windowInsets = WindowInsets.safeDrawing
                    ) {
                        Spacer(modifier = Modifier.height(12.dp))
                        arcadeTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyanNeon,
                                    selectedTextColor = CyanNeon,
                                    indicatorColor = CyanContainer,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ArcadeRoutes.HOME,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        composable(ArcadeRoutes.HOME) {
                            HomeScreen(
                                viewModel = viewModel,
                                userPrivate = userPrivate,
                                myProfile = myProfile,
                                postsState = postsState,
                                streamsState = streamsState,
                                profilesState = allProfilesState,
                                worldsState = worldsState,
                                notificationsState = notificationsState,
                                onNavigateWatch = { navigateTab(ArcadeRoutes.WATCH) },
                                onNavigatePlay = { navigateTab(ArcadeRoutes.PLAY) },
                                onNavigateProfile = { navigateTab(ArcadeRoutes.PROFILE) },
                                onOpenStream = { streamId ->
                                    selectedStreamId = streamId
                                    navigateTab(ArcadeRoutes.WATCH)
                                },
                                onOpenSettings = {
                                    openProfileSettings = true
                                    navigateTab(ArcadeRoutes.PROFILE)
                                }
                            )
                        }

                        composable(ArcadeRoutes.WATCH) {
                            WatchScreen(
                                viewModel = viewModel,
                                userPrivate = userPrivate,
                                streamsState = streamsState,
                                postsState = postsState,
                                selectedStreamId = selectedStreamId,
                                onSelectStream = { selectedStreamId = it }
                            )
                        }

                        composable(ArcadeRoutes.PLAY) {
                            PlayScreen(
                                viewModel = viewModel,
                                worldsState = worldsState,
                                partiesState = partiesState,
                                communitiesState = communitiesState,
                                tournamentsState = tournamentsState,
                                onShowMessage = { msg -> viewModel.showBannerMessage(msg) }
                            )
                        }

                        composable(ArcadeRoutes.SOCIAL) {
                            SocialScreen(
                                viewModel = viewModel,
                                userPrivate = userPrivate,
                                profilesState = allProfilesState,
                                friendRequestsState = friendRequestsState,
                                conversationsState = conversationsState,
                                communitiesState = communitiesState,
                                selectedConversationId = selectedConversationId,
                                onSelectConversation = { selectedConversationId = it }
                            )
                        }

                        composable(ArcadeRoutes.PROFILE) {
                            ProfileScreen(
                                viewModel = viewModel,
                                userPrivate = userPrivate,
                                myProfileState = myProfileState,
                                galleryState = galleryState,
                                streamsState = streamsState,
                                postsState = postsState,
                                allProfilesState = allProfilesState,
                                openSettingsInitially = openProfileSettings
                            )
                        }
                    }
                }
            }
        }
    }

    if (postForComments != null) {
        CommentsBottomSheet(
            post = postForComments!!,
            comments = (commentsState as? UiState.Success)?.data.orEmpty(),
            onDismiss = { viewModel.openPostComments(null) },
            onSendComment = { text -> viewModel.addComment(text) }
        )
    }

    if (levelUpBanner != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissLevelUpBanner() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = GoldCoin)
                    Text("LEVEL UP! Level $levelUpBanner")
                }
            },
            text = {
                Text(
                    "Congratulations! You reached Level $levelUpBanner, earned +200 bonus Gift Coins, and unlocked new Arcade Buff rewards!",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissLevelUpBanner() },
                    colors = ButtonDefaults.buttonColors(containerColor = VoltGreen, contentColor = ObsidianBg)
                ) {
                    Text("Awesome!", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
