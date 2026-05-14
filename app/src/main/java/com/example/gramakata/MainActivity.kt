package com.example.gramakata

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import android.content.res.Configuration
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.gramakata.ui.screens.CustomerDetailScreen
import com.example.gramakata.ui.screens.DashboardScreen
import com.example.gramakata.ui.screens.ProfileScreen
import com.example.gramakata.ui.theme.GramaKataTheme
import com.example.gramakata.viewmodel.KhataViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("crash_prefs", gitContext.MODE_PRIVATE)
        val lastCrash = prefs.getString("last_crash", null)
        
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val stackTrace = android.util.Log.getStackTraceString(e)
            prefs.edit().putString("last_crash", stackTrace).commit()
            System.exit(1)
        }

        enableEdgeToEdge()
        setContent {
            GramaKataTheme {
                if (lastCrash != null) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { prefs.edit().remove("last_crash").apply() },
                        title = { androidx.compose.material3.Text("App Crashed!") },
                        text = {
                            androidx.compose.foundation.lazy.LazyColumn {
                                item {
                                    androidx.compose.material3.Text(text = lastCrash, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(onClick = { prefs.edit().remove("last_crash").apply() }) {
                                androidx.compose.material3.Text("Clear & Continue")
                            }
                        }
                    )
                } else {
                    GramaKhataApp(application = application)
                }
            }
        }
    }
}

@Composable
fun GramaKhataApp(application: Application) {
    val navController = rememberNavController()
    
    // Provide ViewModel scoped to the NavHost using a custom factory for Application
    val viewModel: KhataViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return KhataViewModel(application) as T
            }
        }
    )

    val languageCode by viewModel.languageCode.collectAsState()

    LocaleWrapper(languageCode = languageCode) {
        NavHost(navController = navController, startDestination = "dashboard") {
            composable("dashboard") {
                DashboardScreen(
                    viewModel = viewModel,
                    onCustomerClick = { customerId ->
                        navController.navigate("customer/$customerId")
                    },
                    onProfileClick = {
                        navController.navigate("profile")
                    }
                )
            }
            composable(
                route = "customer/{customerId}",
                arguments = listOf(navArgument("customerId") { type = NavType.LongType })
            ) { backStackEntry ->
                val customerId = backStackEntry.arguments?.getLong("customerId") ?: return@composable
                CustomerDetailScreen(
                    customerId = customerId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("profile") {
                ProfileScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun LocaleWrapper(languageCode: String, content: @Composable () -> Unit) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    
    val context = LocalContext.current
    val configuration = Configuration(LocalConfiguration.current)
    configuration.setLocale(locale)
    val localeContext = context.createConfigurationContext(configuration)
    
    val activityResultRegistryOwner = androidx.activity.compose.LocalActivityResultRegistryOwner.current
    
    if (activityResultRegistryOwner != null) {
        CompositionLocalProvider(
            LocalContext provides localeContext,
            LocalConfiguration provides configuration,
            androidx.activity.compose.LocalActivityResultRegistryOwner provides activityResultRegistryOwner
        ) {
            content()
        }
    } else {
        CompositionLocalProvider(
            LocalContext provides localeContext,
            LocalConfiguration provides configuration
        ) {
            content()
        }
    }
}