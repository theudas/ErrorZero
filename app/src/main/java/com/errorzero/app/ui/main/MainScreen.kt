package com.errorzero.app.ui.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.errorzero.app.app
import com.errorzero.app.review.Reminders
import com.errorzero.app.ui.components.ButtonShape
import com.errorzero.app.ui.components.Hairline

private data class TabItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val Tabs = listOf(
    TabItem("错题本", Icons.AutoMirrored.Outlined.MenuBook),
    TabItem("重做", Icons.Outlined.Event),
    TabItem("设置", Icons.Outlined.Tune),
)

@Composable
fun MainScreen(
    openReviewSignal: Int,
    onOpen: (Long) -> Unit,
    onAdd: (presetSubjectId: Long?) -> Unit,
    onManageSubjects: () -> Unit,
    onManageTags: () -> Unit,
) {
    val context = LocalContext.current
    val app = context.app
    val vm: MainViewModel = viewModel { MainViewModel(app) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(openReviewSignal) {
        if (openReviewSignal > 0) tab = 1
    }
    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    // 首次启动时申请通知权限（Android 13+）
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !Reminders.canNotify(context) && !app.settings.notificationPermissionAsked
        ) {
            app.settings.notificationPermissionAsked = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val filter by vm.filter.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                Hairline()
                NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                    Tabs.forEachIndexed { i, t ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(t.icon, null) },
                            label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onBackground,
                                selectedTextColor = MaterialTheme.colorScheme.onBackground,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (tab == 0) {
                ExtendedFloatingActionButton(
                    onClick = { onAdd(filter.subjectId) },
                    shape = ButtonShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(2.dp, 2.dp, 2.dp, 2.dp),
                    icon = { Icon(Icons.Outlined.Add, null) },
                    text = { Text("记录错题") },
                )
            }
        },
    ) { padding ->
        when (tab) {
            0 -> LibraryTab(vm, padding, onOpen)
            1 -> ReviewTab(vm, padding, onOpen)
            else -> SettingsTab(vm, padding, onManageSubjects, onManageTags)
        }
    }
}
