package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.GlobalSettingScreen
import com.example.ui.screens.StoryListScreen
import com.example.ui.screens.StoryScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainScreenTab
import com.example.viewmodel.StoryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: StoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val lifecycleOwner = LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = LifecycleEventObserver { _, event ->
                        when (event) {
                            Lifecycle.Event.ON_RESUME -> viewModel.onResumeScreen()
                            Lifecycle.Event.ON_PAUSE -> viewModel.onPauseScreen()
                            else -> {}
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                StoryVerseApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun StoryVerseApp(viewModel: StoryViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val messages by viewModel.storyMessages.collectAsStateWithLifecycle()
    val images by viewModel.mediaImages.collectAsStateWithLifecycle()
    val bgmTitle by viewModel.bgmTitle.collectAsStateWithLifecycle()
    val isBgmPlaying by viewModel.isBgmPlaying.collectAsStateWithLifecycle()
    val isBgmMuted by viewModel.isBgmMuted.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = uiState.currentScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "AppScreenTransition"
    ) { screen ->
        when (screen) {
            AppScreen.STORY_LIST -> {
                // ========================================================
                // LAYAR UTAMA (Daftar Cerita & Global Setting dengan Ikon Gear)
                // ========================================================
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0C0717),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        MainBottomNavigationBar(
                            activeTab = uiState.mainTab,
                            onTabSelected = { tab -> viewModel.setMainTab(tab) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        AnimatedContent(
                            targetState = uiState.mainTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "MainTabTransition"
                        ) { tab ->
                            when (tab) {
                                MainScreenTab.STORIES -> {
                                    StoryListScreen(
                                        viewModel = viewModel,
                                        onOpenStory = { storyId -> viewModel.openStory(storyId) },
                                        onNavigateToGlobalSettings = {
                                            viewModel.setMainTab(MainScreenTab.GLOBAL_SETTINGS)
                                        }
                                    )
                                }
                                MainScreenTab.GLOBAL_SETTINGS -> {
                                    GlobalSettingScreen(
                                        viewModel = viewModel,
                                        uiState = uiState
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AppScreen.STORY_DETAIL -> {
                // ========================================================
                // MODE CHAT CERITA: BERSIH TOTAL TANPA BOTTOM BAR TAB!
                // Bagian bawah murni hanya ada input text dan tombol kirim.
                // ========================================================
                BackHandler {
                    viewModel.closeStoryToHome()
                }

                StoryScreen(
                    viewModel = viewModel,
                    uiState = uiState,
                    messages = messages,
                    images = images,
                    bgmTitle = bgmTitle,
                    isBgmPlaying = isBgmPlaying,
                    isBgmMuted = isBgmMuted
                )
            }
        }
    }
}

/**
 * Bottom Navigation Bar untuk Layar Utama (Hanya tampil di Layar Utama bersama List Cerita)
 * Menggunakan ikon Gear (Roda Gigi) untuk Global Setting
 */
@Composable
fun MainBottomNavigationBar(
    activeTab: MainScreenTab,
    onTabSelected: (MainScreenTab) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xF00B0616),
        contentColor = Color(0xFFE2D7F5),
        tonalElevation = 6.dp,
        modifier = Modifier.testTag("main_navigation_bar")
    ) {
        val tabs = listOf(
            Triple(MainScreenTab.STORIES, Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
            Triple(MainScreenTab.GLOBAL_SETTINGS, Icons.Filled.Settings, Icons.Outlined.Settings)
        )

        tabs.forEach { (tab, filledIcon, outlinedIcon) ->
            val isSelected = (activeTab == tab)
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) filledIcon else outlinedIcon,
                        contentDescription = tab.title
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color(0xFFFFD54F),
                    selectedTextColor = Color(0xFFFFD54F),
                    indicatorColor = Color(0x33B388FF),
                    unselectedIconColor = Color(0xFF9E8DBA),
                    unselectedTextColor = Color(0xFF9E8DBA)
                ),
                modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
            )
        }
    }
}
