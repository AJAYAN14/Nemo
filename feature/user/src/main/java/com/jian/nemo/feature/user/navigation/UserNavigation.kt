package com.jian.nemo.feature.user.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import com.jian.nemo.feature.user.LoginScreen


const val ROUTE_LOGIN = "login"
const val ROUTE_PROFILE = "profile"
const val ROUTE_PROFILE_SETUP = "profile_setup"

fun NavController.navigateToLogin(navOptions: NavOptions? = null) {
    this.navigate(ROUTE_LOGIN, navOptions)
}

fun NavController.navigateToProfile(navOptions: NavOptions? = null) {
    this.navigate(ROUTE_PROFILE, navOptions)
}

fun NavController.navigateToProfileSetup(navOptions: NavOptions? = null) {
    this.navigate(ROUTE_PROFILE_SETUP, navOptions)
}

fun NavGraphBuilder.userGraph(
    navController: NavController,
    onLoginSuccess: (isNewUser: Boolean) -> Unit,
    onSetupComplete: () -> Unit
) {
    composable(ROUTE_LOGIN) {
        LoginScreen(
            onNavigateToRegister = { /* redundant in current UI */ },
            onLoginSuccess = onLoginSuccess
        )
    }
    composable(ROUTE_PROFILE_SETUP) {
        com.jian.nemo.feature.user.ProfileSetupScreen(
            onSetupComplete = {
                // Clear setup from backstack and navigate to main
                onSetupComplete()
            }
        )
    }
    composable(ROUTE_PROFILE) {
        com.jian.nemo.feature.user.AccountManagementScreen(
            onNavigateBack = {
                navController.popBackStack()
            },

            onLogoutSuccess = {
                // Navigate to Login and clear backstack
                navController.navigate(ROUTE_LOGIN) {
                    popUpTo(0)
                }
            }
        )
    }
}
