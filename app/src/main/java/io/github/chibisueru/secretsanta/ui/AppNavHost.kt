package io.github.chibisueru.secretsanta.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import io.github.chibisueru.secretsanta.data.AppSettings
import io.github.chibisueru.secretsanta.santaApp
import io.github.chibisueru.secretsanta.ui.components.MainNavBar
import io.github.chibisueru.secretsanta.ui.components.MainTab
import io.github.chibisueru.secretsanta.ui.decoder.DecoderScreen
import io.github.chibisueru.secretsanta.ui.group.GroupScreen
import io.github.chibisueru.secretsanta.ui.group.GroupViewModel
import io.github.chibisueru.secretsanta.ui.groups.GroupsScreen
import io.github.chibisueru.secretsanta.ui.groups.GroupsViewModel
import io.github.chibisueru.secretsanta.ui.newgroup.NewGroupScreen
import io.github.chibisueru.secretsanta.ui.newgroup.NewGroupViewModel
import io.github.chibisueru.secretsanta.ui.settings.SettingsScreen
import io.github.chibisueru.secretsanta.ui.share.ShareScreen
import kotlinx.coroutines.launch

private object Routes {
    const val GROUPS = "groups"
    const val DECODER = "decoder"
    const val NEW_GROUP = "newGroup"
    const val GROUP = "group/{groupId}"
    const val SHARE = "share/{groupId}"
    const val SETTINGS = "settings"
    fun group(id: Long) = "group/$id"
    fun share(id: Long) = "share/$id"
}

private val groupIdArg = listOf(navArgument("groupId") { type = NavType.LongType })

/** Switches between the dock's tabs, keeping a single copy of each on the back stack. */
private fun NavHostController.selectTab(tab: MainTab) {
    val route = when (tab) {
        MainTab.HOME -> Routes.GROUPS
        MainTab.DECODER -> Routes.DECODER
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppNavHost(settings: AppSettings) {
    val nav = rememberNavController()
    val app = LocalContext.current.santaApp
    val dao = app.database.dao()
    val scope = rememberCoroutineScope()

    fun back() {
        nav.popBackStack()
    }

    NavHost(navController = nav, startDestination = Routes.GROUPS) {
        composable(Routes.GROUPS) {
            val vm = viewModel { GroupsViewModel(dao) }
            val groups by vm.groups.collectAsStateWithLifecycle()
            GroupsScreen(
                groups = groups,
                snowfall = settings.snowfall,
                onOpenGroup = { nav.navigate(Routes.group(it)) },
                onNewGroup = { nav.navigate(Routes.NEW_GROUP) { launchSingleTop = true } },
                onDeleteGroup = vm::deleteGroup,
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                bottomBar = { MainNavBar(MainTab.HOME, nav::selectTab) },
            )
        }

        composable(Routes.DECODER) {
            DecoderScreen(bottomBar = { MainNavBar(MainTab.DECODER, nav::selectTab) })
        }

        composable(Routes.NEW_GROUP) {
            val vm = viewModel { NewGroupViewModel(dao) }
            val prefill by vm.prefill.collectAsStateWithLifecycle()
            NewGroupScreen(
                prefill = prefill,
                onClose = ::back,
                onCreate = { groupName, name, phone, email, participates ->
                    vm.create(groupName, name, phone, email, participates) { id ->
                        nav.navigate(Routes.group(id)) {
                            popUpTo(Routes.NEW_GROUP) { inclusive = true }
                        }
                    }
                },
            )
        }

        composable(Routes.GROUP, arguments = groupIdArg) { entry ->
            val groupId = entry.arguments!!.getLong("groupId")
            val vm = viewModel { GroupViewModel(dao, app.appScope, groupId) }
            GroupScreen(
                viewModel = vm,
                onBack = { if (nav.currentDestination?.route == Routes.GROUP) back() },
                onOpenShare = { nav.navigate(Routes.share(groupId)) { launchSingleTop = true } },
            )
        }

        composable(Routes.SHARE, arguments = groupIdArg) { entry ->
            val groupId = entry.arguments!!.getLong("groupId")
            val vm = viewModel { GroupViewModel(dao, app.appScope, groupId) }
            ShareScreen(
                viewModel = vm,
                onBack = { if (nav.currentDestination?.route == Routes.SHARE) back() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                onThemeStyle = { scope.launch { app.settings.setThemeStyle(it) } },
                onDarkMode = { scope.launch { app.settings.setDarkMode(it) } },
                onSnowfall = { scope.launch { app.settings.setSnowfall(it) } },
                onBack = ::back,
            )
        }
    }
}
