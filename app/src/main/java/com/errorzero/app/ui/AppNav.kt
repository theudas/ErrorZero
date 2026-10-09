package com.errorzero.app.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.errorzero.app.ui.detail.DetailScreen
import com.errorzero.app.ui.edit.EditScreen
import com.errorzero.app.ui.main.MainScreen
import com.errorzero.app.ui.manage.SubjectsScreen
import com.errorzero.app.ui.manage.TagsScreen
import com.errorzero.app.ui.viewer.ImageViewerScreen

@Composable
fun AppNav(openReviewSignal: Int) {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "main") {
        composable("main") {
            MainScreen(
                openReviewSignal = openReviewSignal,
                onOpen = { nav.navigate("detail/$it") },
                onAdd = { subject -> nav.navigate("edit/0?subject=${subject ?: -1}") },
                onManageSubjects = { nav.navigate("subjects") },
                onManageTags = { nav.navigate("tags") },
            )
        }
        composable(
            "edit/{id}?subject={subject}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("subject") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val id = entry.arguments!!.getLong("id")
            val subject = entry.arguments!!.getLong("subject").takeIf { it > 0 }
            EditScreen(
                mistakeId = id,
                presetSubjectId = subject,
                onClose = { nav.popBackStack() },
                onSaved = { nav.popBackStack() },
            )
        }
        composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            val id = entry.arguments!!.getLong("id")
            DetailScreen(
                mistakeId = id,
                onBack = { if (nav.currentBackStackEntry == entry) nav.popBackStack() },
                onEdit = { nav.navigate("edit/$id") },
                onViewImage = { kind, index -> nav.navigate("viewer/$id/$kind/$index") },
            )
        }
        composable(
            "viewer/{id}/{kind}/{index}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType },
                navArgument("kind") { type = NavType.IntType },
                navArgument("index") { type = NavType.IntType },
            ),
        ) { entry ->
            val args = entry.arguments!!
            ImageViewerScreen(
                mistakeId = args.getLong("id"),
                kind = args.getInt("kind"),
                startIndex = args.getInt("index"),
                onClose = { nav.popBackStack() },
            )
        }
        composable("subjects") { SubjectsScreen(onBack = { nav.popBackStack() }) }
        composable("tags") { TagsScreen(onBack = { nav.popBackStack() }) }
    }
}
