package com.app.emilockerapp.uilayer.views.auth

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.emilockerapp.coordinator.BaseChildNavGraph
import com.app.emilockerapp.utils.ComposeBaseExtensions.routeWithArgs

class LoginScreen(navHostController: NavHostController) : BaseChildNavGraph {
    object Routes {
        const val emi_Login_Screen = "emi_locker/login"
    }

    override fun createChildNavGraphBuilder(): NavGraphBuilder.() -> Unit {
        val output: NavGraphBuilder.() -> Unit = {
            composable(routeWithArgs(Routes.emi_Login_Screen)) {
                LoginScreenUI()
            }

        }
        return output
    }

    @Composable
    fun LoginScreenUI() {
        LoginUiScreen()
    }
}


@Preview
@Composable
fun PreviewLoginScreenUI() {
    val mNavHostController = rememberNavController()
    LoginScreen(mNavHostController).LoginScreenUI()
}