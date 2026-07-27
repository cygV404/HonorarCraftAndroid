package de.v404.honorarcraftandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import de.v404.honorarcraftandroid.ui.theme.HonorarCraftAndroidTheme

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

    // Synchronisiere Pager-State mit ViewModel-State
    LaunchedEffect(selectedTabIndex) {
        if (pagerState.currentPage != selectedTabIndex) {
            pagerState.animateScrollToPage(selectedTabIndex)
        }
    }

    // Synchronisiere ViewModel-State mit Pager-State
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTabIndex && pendingTabIndex == null) {
            mainViewModel.setSelectedTabIndex(pagerState.currentPage)
        }
    }

    LaunchedEffect(pendingTabIndex) {
        if (pendingTabIndex != null && pagerState.currentPage != selectedTabIndex) {
            pagerState.scrollToPage(selectedTabIndex)
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { mainViewModel.setSelectedTabIndex(index) },
                        icon = { Icon(icons[index], contentDescription = title) },
                        label = { Text(title) }
                    )
                }
            }
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> DashboardScreen(mainViewModel = mainViewModel)
                1 -> CreateInvoiceScreen(
                    mainViewModel = mainViewModel,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { mainViewModel.setSelectedTabIndex(it) }
                )

                2 -> EntryWindowScreen(
                    mainViewModel = mainViewModel,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { mainViewModel.setSelectedTabIndex(it) }
                )

                3 -> DataWindowScreen(
                    mainViewModel = mainViewModel,
                    selectedTabIndex = selectedTabIndex,
                    onTabSelected = { mainViewModel.setSelectedTabIndex(it) }
                )
            }
        }
    }
}
