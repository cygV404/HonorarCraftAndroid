package de.v404.honorarcraftandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AddCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import de.v404.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme
import kotlinx.coroutines.CancellationException

enum class PagerNavigationMode {
    IDLE,
    USER_DRIVEN,
    PROGRAMMATIC
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Offizielle Splash Screen API initialisieren
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HonorarCraftAndroidTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val mainViewModel: MainViewModel = viewModel()
    val selectedTabIndex by mainViewModel.selectedTabIndex.collectAsState()
    val pendingTabIndex by mainViewModel.pendingTabIndex.collectAsState()

    val pagerState = rememberPagerState(
        initialPage = selectedTabIndex,
        pageCount = { 4 }
    )

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var navigationMode by remember { mutableStateOf(PagerNavigationMode.IDLE) }

    // User-Interaktion (Wischen) hat immer Priorität und übernimmt die Authority
    LaunchedEffect(isDragged) {
        if (isDragged) {
            navigationMode = PagerNavigationMode.USER_DRIVEN
        }
    }

    // Erkennung eines programmatischen Wechsels (Tap in NavigationBar)
    LaunchedEffect(selectedTabIndex) {
        // Wenn sich der Index ändert, während wir nicht wischen, ist es ein programmatischer Intent
        if (!isDragged && navigationMode != PagerNavigationMode.USER_DRIVEN) {
            navigationMode = PagerNavigationMode.PROGRAMMATIC
        }
    }

    // Zentraler Navigations-Wächter (Convergence Controller mit State-Machine)
    LaunchedEffect(selectedTabIndex, pagerState, isDragged) {
        androidx.compose.runtime.snapshotFlow { pagerState.isScrollInProgress }
            .collect { inProgress ->
                // Nur agieren, wenn gerade keine aktive physische Bewegung (Wischen/Settling) stattfindet
                if (!inProgress && !isDragged) {
                    when (navigationMode) {
                        PagerNavigationMode.USER_DRIVEN -> {
                            // User-Swipe abgeschlossen -> Intent ins ViewModel übernehmen
                            mainViewModel.updateTabIndexFromPager(pagerState.settledPage)
                            navigationMode = PagerNavigationMode.IDLE
                        }

                        PagerNavigationMode.PROGRAMMATIC, PagerNavigationMode.IDLE -> {
                            // Programmatisches Ziel oder allgemeiner Ghost-Layer-Check
                            val physicallyAtTarget = pagerState.settledPage == selectedTabIndex &&
                                    pagerState.currentPageOffsetFraction == 0f

                            if (!physicallyAtTarget) {
                                try {
                                    pagerState.animateScrollToPage(selectedTabIndex)
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    // Sonstiger Abbruch okay, Loop prüft beim nächsten Stillstand erneut
                                }
                            } else {
                                navigationMode = PagerNavigationMode.IDLE
                            }
                        }
                    }
                }
            }
    }

    val tabs = listOf("Übersicht", "Erstellen", "Vorschau", "Daten")
    val icons = listOf(
        Icons.Default.Home,
        Icons.Default.AddCard,
        Icons.AutoMirrored.Filled.List,
        Icons.Default.Person
    )

    if (pendingTabIndex != null) {
        AlertDialog(
            onDismissRequest = { mainViewModel.cancelTabChange() },
            icon = { Icon(Icons.Default.Warning, contentDescription = null) },
            title = { Text("Ungespeicherte Änderungen") },
            text = { Text("Sie haben ungespeicherte Änderungen im Daten-Fenster. Möchten Sie diese verwerfen und fortfahren?") },
            confirmButton = {
                TextButton(onClick = { mainViewModel.confirmTabChange() }) {
                    Text("Verwerfen")
                }
            },
            dismissButton = {
                TextButton(onClick = { mainViewModel.cancelTabChange() }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    val hasUnsavedChanges by mainViewModel.hasUnsavedChanges.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { mainViewModel.requestTabChange(index) },
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
            .exclude(WindowInsets.statusBars)
            .exclude(WindowInsets.ime)
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            userScrollEnabled = !hasUnsavedChanges
        ) { page ->
            when (page) {
                0 -> DashboardScreen(mainViewModel = mainViewModel)
                1 -> CreateInvoiceScreen(
                    mainViewModel = mainViewModel,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { mainViewModel.requestTabChange(it) }
                )

                2 -> EntryWindowScreen(
                    mainViewModel = mainViewModel,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { mainViewModel.requestTabChange(it) }
                )

                3 -> DataWindowScreen(
                    mainViewModel = mainViewModel,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { mainViewModel.requestTabChange(it) }
                )
            }
        }
    }
}
