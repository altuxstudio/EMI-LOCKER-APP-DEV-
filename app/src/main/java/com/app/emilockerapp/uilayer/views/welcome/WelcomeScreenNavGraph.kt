package com.app.emilockerapp.uilayer.views.welcome

import androidx.compose.runtime.Composable
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.app.emilockerapp.coordinator.BaseChildNavGraph
import com.app.emilockerapp.utils.ComposeBaseExtensions.routeWithArgs

class WelcomeScreenNavGraph(navHostController: NavHostController) : BaseChildNavGraph {

    object Routes {
        const val welcomeScreen = "welcome/screen"
    }

    override fun createChildNavGraphBuilder(): NavGraphBuilder.() -> Unit {
        return {
            composable(routeWithArgs(Routes.welcomeScreen)) {
                WelcomeScreenUI()
            }
        }
    }

    @Composable
    fun WelcomeScreenUI() {
        WelcomeScreen() // Call your beautiful WelcomeScreen composable
    }
}
