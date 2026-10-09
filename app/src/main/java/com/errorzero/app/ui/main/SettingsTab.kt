package com.errorzero.app.ui.main

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.errorzero.app.BuildConfig
import com.errorzero.app.data.ImportMode
import com.errorzero.app.review.ReviewSchedule
import com.errorzero.app.review.Reminders
import com.errorzero.app.ui.components.Hairline
import com.errorzero.app.ui.components.PageHeader
import com.errorzero.app.ui.components.SectionLabel
import com.errorzero.app.ui.components.SettingRow
import com.errorzero.app.ui.components.TimePickDialog
import com.errorzero.app.util.Dates

@Composable
fun SettingsTab(
    vm: MainViewModel,
    padding: PaddingValues,
    onManageSubjects: () -> Unit,
    onManageTags: () -> Unit,
) {
    val context = LocalContext.current
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val tags by vm.tagUsage.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    var showTime by remember { mutableStateOf(false) }
    var importUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.export(uri)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        importUri = uri
    }

    LazyColumn(contentPadding = padding) {
        item { PageHeader("设置") }

        item { Group("分类") }
        item { SettingRow("科目管理", "${subjects.size} 个科目，可新增、重命名、排序", onClick = onManageSubjects) }
        item {
            SettingRow(
                "标签管理",
                "错因 ${tags.count { it.type == 0 }} 个 · 模块 ${tags.count { it.type == 1 }} 个",
                onClick = onManageTags,
            )
        }

        item { Group("提醒") }
        item {
            SettingRow(
                "每日重做提醒",
                if (reminder.enabled) "有待重做题目时发送通知" else "已关闭",
                onClick = { toggleReminder(vm, reminder.enabled, context, permissionLauncher::launch) },
                trailing = {
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = { toggleReminder(vm, reminder.enabled, context, permissionLauncher::launch) },
                        colors = SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                },
            )
        }
        if (reminder.enabled) {
            item {
                SettingRow(
                    "提醒时间",
                    onClick = { showTime = true },
                    trailing = {
                        Text("%02d:%02d".format(reminder.hour, reminder.minute), style = MaterialTheme.typography.bodyLarge)
                    },
                )
            }
        }
        item {
            SettingRow(
                "复习间隔",
                "${ReviewSchedule.INTERVALS.joinToString(" / ")} 天，可在题目详情中单独调整",
            )
        }

        item { Group("数据") }
        item {
            SettingRow(
                "导出数据",
                "打包为 zip，包含全部错题、图片与复习记录",
                onClick = { exportLauncher.launch("ErrorZero-备份-${Dates.fileStamp()}.zip") },
            )
        }
        item {
            SettingRow(
                "导入数据",
                "从 zip 备份恢复，换手机时使用",
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream")) },
            )
        }

        item { Group("关于") }
        item { SettingRow("ErrorZero", "版本 ${BuildConfig.VERSION_NAME}") }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showTime) {
        TimePickDialog(
            hour = reminder.hour,
            minute = reminder.minute,
            onDismiss = { showTime = false },
            onPick = { h, m -> vm.setReminder(reminder.copy(hour = h, minute = m)) },
        )
    }

    importUri?.let { uri ->
        ImportDialog(
            onDismiss = { importUri = null },
            onConfirm = { mode ->
                importUri = null
                vm.import(uri, mode)
            },
        )
    }

    busy?.let { text ->
        AlertDialog(
            onDismissRequest = {},
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            shape = MaterialTheme.shapes.extraLarge,
            confirmButton = {},
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(16.dp))
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                }
            },
        )
    }
}

private fun toggleReminder(
    vm: MainViewModel,
    currentlyEnabled: Boolean,
    context: android.content.Context,
    requestPermission: (String) -> Unit,
) {
    val enable = !currentlyEnabled
    if (enable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Reminders.canNotify(context)) {
        requestPermission(Manifest.permission.POST_NOTIFICATIONS)
    }
    vm.setReminder(vm.reminder.value.copy(enabled = enable))
}

@Composable
private fun Group(title: String) {
    Column {
        Spacer(Modifier.height(16.dp))
        Hairline()
        SectionLabel(title, Modifier.padding(start = 24.dp, top = 20.dp, bottom = 4.dp))
    }
}

@Composable
private fun ImportDialog(onDismiss: () -> Unit, onConfirm: (ImportMode) -> Unit) {
    var mode by rememberSaveable { mutableStateOf(ImportMode.MERGE) }
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("导入方式", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                ModeOption("合并", "保留当前数据，只添加备份中没有的错题", mode == ImportMode.MERGE) { mode = ImportMode.MERGE }
                Spacer(Modifier.height(8.dp))
                ModeOption("覆盖", "清空当前全部数据，替换为备份内容。换新手机时推荐", mode == ImportMode.REPLACE) { mode = ImportMode.REPLACE }
                if (mode == ImportMode.REPLACE) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "当前手机上的错题将被删除，且无法撤销。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(mode) }) { Text("开始导入") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } },
    )
}

@Composable
private fun ModeOption(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
        )
        Column(Modifier.padding(top = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
