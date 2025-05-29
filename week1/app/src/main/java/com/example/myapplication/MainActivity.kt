package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedItem by remember { mutableIntStateOf(0) }
    
    val navItems = listOf(
        BottomNavItem("clock", Icons.Default.Schedule, "时钟"),
        BottomNavItem("alarm", Icons.Default.Alarm, "闹钟")
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(navItems[selectedItem].label)
                }
            )
        },
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                item.icon, 
                                contentDescription = item.label
                            ) 
                        },
                        label = { Text(item.label) },
                        selected = selectedItem == index,
                        onClick = {
                            selectedItem = index
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "clock",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(
                "clock",
                enterTransition = {
                    when (initialState.destination.route) {
                        "alarm" -> slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(300)
                        )
                        else -> null
                    }
                },
                exitTransition = {
                    when (targetState.destination.route) {
                        "alarm" -> slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(300)
                        )
                        else -> null
                    }
                }
            ) {
                ClockScreen()
            }
            composable(
                "alarm",
                enterTransition = {
                    when (initialState.destination.route) {
                        "clock" -> slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(300)
                        )
                        else -> null
                    }
                },
                exitTransition = {
                    when (targetState.destination.route) {
                        "clock" -> slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(300)
                        )
                        else -> null
                    }
                }
            ) {
                AlarmScreen()
            }
        }
    }
}