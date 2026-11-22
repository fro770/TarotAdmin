package com.example.tarotadmin

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.tarotadmin.data.DeviceInfo
import com.example.tarotadmin.feature.DeviceInfoScreen
import com.example.tarotadmin.feature.HtmlViewerScreen
import com.example.tarotadmin.feature.MatrixGraphicScreen
import com.example.tarotadmin.feature.UserDetailsScreen
import com.example.tarotadmin.feature.UsersScreen

private val AdminDarkColors = darkColorScheme(
    primary = Color(0xFFA78BFA),
    background = Color(0xFF0D0B14),
    surface = Color(0xFF1A142B),
    onPrimary = Color(0xFF17102B),
    onBackground = Color(0xFFF5F2FF),
    onSurface = Color(0xFFF5F2FF),
    error = Color(0xFFFF8A80)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = AdminDarkColors) {
                val navController = rememberNavController()
                // ✅ Храним DeviceInfo в памяти для передачи на экран
                var selectedDeviceInfo by remember { mutableStateOf<DeviceInfo?>(null) }

                NavHost(navController = navController, startDestination = "users_list") {
                    composable("users_list") {
                        UsersScreen(onUserClick = { navController.navigate("user_details/$it") })
                    }

                    composable(
                        route = "user_details/{userFolder}",
                        arguments = listOf(navArgument("userFolder") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val userFolder = backStackEntry.arguments?.getString("userFolder") ?: ""
                        UserDetailsScreen(
                            userFolder = userFolder,
                            onBack = { navController.popBackStack() },
                            onOpenHtml = { subFolder, id, title ->
                                navController.navigate("html_viewer/$userFolder/$subFolder/$id/${Uri.encode(title)}")
                            },
                            onOpenMatrixGraphic = { type, date1, date2, title, aiReport ->
                                navController.navigate("matrix_graphic/$type/$date1/$date2/${Uri.encode(title)}/${Uri.encode(aiReport)}")
                            },
                            onOpenDeviceInfo = { info ->
                                selectedDeviceInfo = info
                                navController.navigate("device_info")
                            }
                        )
                    }

                    composable("device_info") {
                        DeviceInfoScreen(
                            info = selectedDeviceInfo,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "html_viewer/{userFolder}/{subFolder}/{id}/{title}",
                        arguments = listOf(
                            navArgument("userFolder") { type = NavType.StringType },
                            navArgument("subFolder") { type = NavType.StringType },
                            navArgument("id") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        HtmlViewerScreen(
                            userFolder = backStackEntry.arguments?.getString("userFolder") ?: "",
                            subFolder = backStackEntry.arguments?.getString("subFolder") ?: "",
                            id = backStackEntry.arguments?.getString("id") ?: "",
                            title = Uri.decode(backStackEntry.arguments?.getString("title") ?: ""),
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "matrix_graphic/{type}/{date1}/{date2}/{title}/{aiReport}",
                        arguments = listOf(
                            navArgument("type") { type = NavType.StringType },
                            navArgument("date1") { type = NavType.StringType },
                            navArgument("date2") { type = NavType.StringType },
                            navArgument("title") { type = NavType.StringType },
                            navArgument("aiReport") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        MatrixGraphicScreen(
                            matrixType = backStackEntry.arguments?.getString("type") ?: "",
                            date1 = backStackEntry.arguments?.getString("date1") ?: "",
                            date2 = backStackEntry.arguments?.getString("date2") ?: "",
                            title = Uri.decode(backStackEntry.arguments?.getString("title") ?: ""),
                            aiReport = Uri.decode(backStackEntry.arguments?.getString("aiReport") ?: ""),
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}