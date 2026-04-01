package com.blueprint.cubing.navigation

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.launch

class AppNavigatorCommonImpl() : AppNavigator, NavigationEventHandler {

    private val navigationChain: Channel<NavAction> = Channel(capacity = BUFFERED)

    override fun navigateTo(route: Route, popUpTo: Route?) {
        navigationChain.trySend(NavAction.NavigateTo(route, popUpTo))
    }

    override fun back() {
        navigationChain.trySend(NavAction.NavigateBack())
    }

    override fun handle(lifecycleOwner: LifecycleOwner, block: (NavAction) -> Unit) {
        val scope = lifecycleOwner.lifecycleScope
        scope.launch(context = Dispatchers.Main.immediate) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                for (navAction in navigationChain) {
                    block(navAction)
                }
            }
        }
    }

}