package com.example.paddleball.ui.di

import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.core.annotation.Single

@Single
class BackStackRepository {
    val backStack = MutableStateFlow(listOf<NavigationRoutes>(NavigationRoutes.Home))

    fun push(route: NavigationRoutes) {
        backStack.value = backStack.value + route
    }

    fun pop(): NavigationRoutes? {
        val backStack = backStack.value
        if (backStack.size > 1) {
            this.backStack.value = backStack.dropLast(1)
            return backStack.last()
        }
        return null
    }

    fun peek(): NavigationRoutes? {
        return backStack.value.lastOrNull()
    }

    fun clear() {
        backStack.value = listOf()
    }
}

sealed interface NavigationRoutes {
    data object Home : NavigationRoutes
    data class Game(val width: Float, val height: Float) : NavigationRoutes
}