package ar.edu.utn.frba.expendinator.navigation

import ar.edu.utn.frba.expendinator.data.remote.ApiClient


sealed class Dest(val route: String) {
    data object Login : Dest("login")
    data object Register : Dest("register")
    data object Main: Dest("main")
    data object Budget: Dest("budget")
    data object Categories: Dest("categories")
    data object Metrics: Dest("metrics")
    data object OcrReview: Dest("ocrReview")

    data object Settings : Dest("settings")

    companion object {
        val startDestination by lazy { if (ApiClient.isLoggedIn()) Main.route else Login.route }
    }
}
