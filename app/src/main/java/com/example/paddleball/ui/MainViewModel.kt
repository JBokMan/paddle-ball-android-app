package com.example.paddleball.ui

import androidx.lifecycle.ViewModel
import com.example.paddleball.ui.di.BackStackRepository
import com.example.paddleball.ui.di.NavigationRoutes
import org.koin.android.annotation.KoinViewModel

@KoinViewModel
class MainViewModel(private val backStackRepository: BackStackRepository) : ViewModel() {
    val backStack = backStackRepository.backStack

    fun popBackStack(index: Int) {
        backStackRepository.pop()
    }

    fun navigateToGameScreen(width: Float, height: Float) {
        backStackRepository.push(NavigationRoutes.Game(width, height))
    }
}
