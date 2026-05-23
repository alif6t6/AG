package com.example

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()

        // Extract repository singleton from Application class
        val app = application as MainApplication
        val repository = app.repository

        // Inline custom factory definition for MainViewModel
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(app, repository) as T
            }
        }
        val viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        // Load onboarding state from SharedPreferences
        val prefs = getSharedPreferences("devil_gpt_prefs", Context.MODE_PRIVATE)
        val isFirstLaunch = prefs.getBoolean("first_launch", true)

        setContent {
            var darkTheme by remember { mutableStateOf(true) }
            
            MyApplicationTheme(darkTheme = darkTheme) {
                var screenState by remember { mutableStateOf("splash") } // "splash", "onboarding", "main"

                LaunchedEffect(screenState) {
                    if (screenState == "onboarding") {
                        prefs.edit().putBoolean("first_launch", false).apply()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (screenState) {
                        "splash" -> {
                            SplashScreen(
                                onFinished = {
                                    screenState = if (isFirstLaunch) "onboarding" else "main"
                                }
                            )
                        }
                        "onboarding" -> {
                            OnboardingScreen(
                                onFinished = {
                                    screenState = "main"
                                }
                            )
                        }
                        "main" -> {
                            MainContainer(
                                viewModel = viewModel,
                                onToggleTheme = { darkTheme = !darkTheme },
                                darkTheme = darkTheme
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainContainer(
    viewModel: MainViewModel,
    onToggleTheme: () -> Unit,
    darkTheme: Boolean
) {
    var activeTab by remember { mutableStateOf("home") } // "home", "expense", "notes", "agent"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            CustomBottomNavigation(
                activeTab = activeTab,
                onTabSelected = { activeTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                "home" -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTab = { activeTab = it },
                        onToggleTheme = onToggleTheme,
                        darkTheme = darkTheme
                    )
                }
                "expense" -> {
                    ExpensesScreen(viewModel = viewModel)
                }
                "notes" -> {
                    NotesScreen(viewModel = viewModel)
                }
                "agent" -> {
                    AiAgentScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigation(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    val items = listOf(
        Pair("home", "🏠 Home"),
        Pair("expense", "💸 Expense"),
        Pair("notes", "📝 Notes"),
        Pair("agent", "😈 Agent")
    )

    Column {
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BorderColor))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding() // Safely shifts layout above gesture bars
                .height(60.dp)
                .background(DarkSurface),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
        items.forEach { (tabName, label) ->
            val isSelected = activeTab == tabName
            val icon = label.split(" ")[0]
            val text = label.split(" ")[1]

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tabName) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = icon,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 2.dp),
                    color = if (isSelected) DevilRed else TextGrey
                )
                Text(
                    text = text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) DevilRed else TextGrey
                )
                
                if (isSelected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(DevilRed)
                    )
                }
            }
        }
    }
}
}
