package com.blueprint.cubing.navigation

import androidx.lifecycle.LifecycleOwner

interface NavigationEventHandler {
    fun handle(lifecycleOwner: LifecycleOwner, block: (NavAction) -> Unit)
}