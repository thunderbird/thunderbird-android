package net.discdd.k9.onboarding.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable
import androidx.navigation.compose.NavHost
import net.discdd.k9.onboarding.ui.login.LoginScreen
import net.discdd.k9.onboarding.ui.login.LoginViewModel
import net.discdd.k9.onboarding.ui.pending.PendingScreen
import net.discdd.k9.onboarding.ui.register.RegisterScreen
import net.discdd.k9.onboarding.ui.register.RegisterViewModel
import org.koin.androidx.compose.koinViewModel

private const val NESTED_NAVIGATION_ROUTE_REGISTER = "register"
private const val NESTED_NAVIGATION_ROUTE_LOGIN = "login"
private const val NESTED_NAVIGATION_ROUTE_PENDING = "pending"

private fun NavController.navigateToRegister() {
    navigate(NESTED_NAVIGATION_ROUTE_REGISTER)
}

private fun NavController.navigateToLogin() {
    navigate(NESTED_NAVIGATION_ROUTE_LOGIN)
}

private fun NavController.navigateToPending() {
    Log.d("DDDOnboarding", "navigating to pending")
    navigate(NESTED_NAVIGATION_ROUTE_PENDING)
}

@Composable
fun OnboardingNavHost(
    onFinish: (String?) -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = NESTED_NAVIGATION_ROUTE_LOGIN
    ) {
        composable(route = NESTED_NAVIGATION_ROUTE_LOGIN){
            LoginScreen(
                onRegisterClick = { navController.navigateToRegister() },
                viewModel =  koinViewModel<LoginViewModel>(),
                onPendingState = { navController.navigateToPending() },
                onFinish = { createdAccountUuid: String ->
                    onFinish(createdAccountUuid)
                }
            )
        }
        composable(route = NESTED_NAVIGATION_ROUTE_REGISTER){
            RegisterScreen(
                onLoginClick = { navController.navigateToLogin() },
                viewModel =  koinViewModel<RegisterViewModel>(),
                onPendingState = { navController.navigateToPending() },
            )
        }
        composable(route = NESTED_NAVIGATION_ROUTE_PENDING){
            PendingScreen()
        }
    }
}
