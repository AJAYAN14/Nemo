package com.jian.nemo.feature.collection.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.CompositionLocalProvider
import com.jian.nemo.core.ui.animation.LocalNavAnimatedVisibilityScope
import com.jian.nemo.feature.collection.favorites.FavoritesScreen
import com.jian.nemo.feature.collection.favorites.FavoriteWordsScreen
import com.jian.nemo.feature.collection.favorites.FavoriteGrammarsScreen
import com.jian.nemo.feature.collection.favorites.FavoriteQuestionsScreen
import com.jian.nemo.feature.collection.mistakes.MistakesScreen
import com.jian.nemo.feature.collection.mistakes.WrongWordsScreen
import com.jian.nemo.feature.collection.mistakes.WrongGrammarsScreen

/**
 * 收藏与错题导航路由
 */
const val FAVORITES_ROUTE = "favorites"
const val FAVORITE_WORDS_ROUTE = "favorite_words"
const val FAVORITE_GRAMMARS_ROUTE = "favorite_grammars"
const val FAVORITE_QUESTIONS_ROUTE = "favorite_questions"
const val MISTAKES_ROUTE = "mistakes"
const val WRONG_WORDS_ROUTE = "wrong_words"
const val WRONG_GRAMMARS_ROUTE = "wrong_grammars"

/**
 * 导航到收藏列表
 */
fun NavController.navigateToFavorites(navOptions: NavOptions? = null) {
    navigate(FAVORITES_ROUTE, navOptions)
}

/**
 * 导航到收藏单词列表
 */
fun NavController.navigateToFavoriteWords(navOptions: NavOptions? = null) {
    navigate(FAVORITE_WORDS_ROUTE, navOptions)
}

/**
 * 导航到收藏语法列表
 */
fun NavController.navigateToFavoriteGrammars(navOptions: NavOptions? = null) {
    navigate(FAVORITE_GRAMMARS_ROUTE, navOptions)
}

/**
 * 导航到收藏题目列表
 */
fun NavController.navigateToFavoriteQuestions(navOptions: NavOptions? = null) {
    navigate(FAVORITE_QUESTIONS_ROUTE, navOptions)
}

/**
 * 导航到错题本
 */
fun NavController.navigateToMistakes(navOptions: NavOptions? = null) {
    navigate(MISTAKES_ROUTE, navOptions)
}

/**
 * 导航到错误单词列表
 */
fun NavController.navigateToWrongWords(navOptions: NavOptions? = null) {
    navigate(WRONG_WORDS_ROUTE, navOptions)
}

/**
 * 导航到错误语法列表
 */
fun NavController.navigateToWrongGrammars(navOptions: NavOptions? = null) {
    navigate(WRONG_GRAMMARS_ROUTE, navOptions)
}

/**
 * 添加收藏列表到导航图
 */
fun NavGraphBuilder.favoritesScreen(
    onNavigateToWordFavorites: () -> Unit,
    onNavigateToGrammarFavorites: () -> Unit,
    onWordClick: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(
        route = FAVORITES_ROUTE,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            FavoritesScreen(
                onNavigateToWordFavorites = onNavigateToWordFavorites,
                onNavigateToGrammarFavorites = onNavigateToGrammarFavorites,
                onWordClick = onWordClick,
                onNavigateBack = onNavigateBack
            )
        }
    }
}

/**
 * 添加收藏单词列表到导航图
 */
fun NavGraphBuilder.favoriteWordsScreen(
    onWordClick: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(route = FAVORITE_WORDS_ROUTE) {
        FavoriteWordsScreen(
            onWordClick = onWordClick,
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * 添加收藏语法列表到导航图
 */
fun NavGraphBuilder.favoriteGrammarsScreen(
    onGrammarClick: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(route = FAVORITE_GRAMMARS_ROUTE) {
        FavoriteGrammarsScreen(
            onGrammarClick = onGrammarClick,
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * 添加收藏题目列表到导航图
 */
fun NavGraphBuilder.favoriteQuestionsScreen(
    onNavigateBack: () -> Unit
) {
    composable(route = FAVORITE_QUESTIONS_ROUTE) {
        FavoriteQuestionsScreen(
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * 添加错题本到导航图
 */
fun NavGraphBuilder.mistakesScreen(
    onNavigateToWordMistakes: () -> Unit,
    onNavigateToGrammarMistakes: () -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(
        route = MISTAKES_ROUTE,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) },
        popEnterTransition = { fadeIn(animationSpec = tween(300)) },
        popExitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
            MistakesScreen(
                onNavigateToWordMistakes = onNavigateToWordMistakes,
                onNavigateToGrammarMistakes = onNavigateToGrammarMistakes,
                onNavigateBack = onNavigateBack
            )
        }
    }
}

/**
 * 添加错误单词列表到导航图
 */
fun NavGraphBuilder.wrongWordsScreen(
    onWordClick: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(route = WRONG_WORDS_ROUTE) {
        WrongWordsScreen(
            onWordClick = onWordClick,
            onNavigateBack = onNavigateBack
        )
    }
}

/**
 * 添加错误语法列表到导航图
 */
fun NavGraphBuilder.wrongGrammarsScreen(
    onGrammarClick: (Int) -> Unit,
    onNavigateBack: () -> Unit
) {
    composable(route = WRONG_GRAMMARS_ROUTE) {
        WrongGrammarsScreen(
            onGrammarClick = onGrammarClick,
            onNavigateBack = onNavigateBack
        )
    }
}
