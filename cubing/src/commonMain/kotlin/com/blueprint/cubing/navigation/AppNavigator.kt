package com.blueprint.cubing.navigation

interface AppNavigator {
    fun back()
    fun navigateTo(route: Route, popUpTo: Route? = null)
}