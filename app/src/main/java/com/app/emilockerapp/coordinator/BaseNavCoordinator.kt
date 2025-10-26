package com.app.emilockerapp.coordinator

import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost

open class BaseNavCoordinator(
    val mNavHostController: NavHostController,
    private val mainModifier: Modifier,
    private val listOfChildNavGraphs: List<BaseChildNavGraph>,
    private val mStartDestination: String,
    mActivity: AppCompatActivity
) {


    @Composable
    fun BaseNavHost() {
        val mGraphBuilder: NavGraphBuilder.() -> Unit = {
            for (childGraphBuilder in listOfChildNavGraphs) {
                childGraphBuilder.createChildNavGraphBuilder().invoke(this)
            }
        }

        val duration = 200
        val animationSpec: FiniteAnimationSpec<IntOffset> = tween(durationMillis = duration)

        val slideInAnimation = slideInHorizontally(animationSpec) {
            it
        }

        val slideOutAnimation = slideOutHorizontally(animationSpec) {
            -it
        }

        val popSlideInAnimation = slideInHorizontally(animationSpec) {
            -it
        }

        val popSlideOutAnimation = slideOutHorizontally(animationSpec) {
            it
        }

        NavHost(
            modifier = mainModifier,
            navController = mNavHostController,
            startDestination = mStartDestination,
            builder = mGraphBuilder,
            enterTransition = { slideInAnimation },
            exitTransition = { slideOutAnimation },
            popEnterTransition = { popSlideInAnimation },
            popExitTransition = { popSlideOutAnimation }
        )
    }

}