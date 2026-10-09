package com.errorzero.app.ui.edit

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.errorzero.app.app
import com.errorzero.app.data.ImageKind
import com.errorzero.app.ui.components.Chip
import com.errorzero.app.ui.components.ConfirmDialog
import com.errorzero.app.ui.components.DatePickDialog
import com.errorzero.app.ui.components.Hairline
import com.errorzero.app.ui.components.ImageStrip
import com.errorzero.app.ui.components.SectionLabel
import com.errorzero.app.ui.components.TagPicker
import com.errorzero.app.ui.components.TextInputDialog
import com.errorzero.app.ui.components.TimePickDialog
import com.errorzero.app.util.Dates

fun cropOptions(uri: Uri) = CropImageContractOptions(
    uri,
    CropImageOptions(
        guidelines = CropImageView.Guidelines.ON_TOUCH,
        outputCompressFormat = Bitmap.CompressFormat.JPEG,
        outputCompressQuality = 92,
        activityTitle = "裁剪",
        cropMenuCropButtonTitle = "完成",
        activityBackgroundColor = android.graphics.Color.BLACK,
        toolbarColor = 0xFF111111.toInt(),
        toolbarTitleColor = android.graphics.Color.WHITE,
        toolbarBackButtonColor = android.graphics.Color.WHITE,
        activityMenuIconColor = android.graphics.Color.WHITE,
        borderLineColor = android.graphics.Color.WHITE,
        borderCornerColor = android.graphics.Color.WHITE,
        // 注意：不要设置 activityMenuTextColor。它与 allowFlipping = false 同时使用时，
        // 裁剪库会读取已被移除的翻转菜单项而崩溃（4.5.0）。
        allowFlipping = false,
        autoZoomEnabled = true,
    ),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    mistakeId: Long,
    presetSubjectId: Long?,
    onClose: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val app = LocalContext.current.app
    val vm: EditViewModel = viewModel { EditViewModel(app.repository, mistakeId, presetSubjectId) }
    val s = vm.state
    val subjects by vm.subjects.collectAsStateWithLifecycle()
    val reasonHistory by vm.reasonHistory.collectAsStateWithLifecycle()
    val moduleHistory by vm.moduleHistory.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var showAddSubject by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }

    // 拍照 -> 裁剪 -> 保存；从缩略图菜单进入裁剪则替换原图
    var pendingCamera by rememberSaveable { mutableStateOf<Uri?>(null) }
    var targetKind by rememberSaveable { mutableIntStateOf(ImageKind.QUESTION) }
    var cropReplace by rememberSaveable { mutableStateOf<String?>(null) }

    val cropLauncher = rememberLauncherForActivityResult(CropImageContract()) { result ->
        val uri = result.uriContent
        if (result.isSuccessful && uri != null) {
            val old = cropReplace
            if (old != null) vm.replaceImage(targetKind, old, uri) else vm.addImages(targetKind, listOf(uri))
        }
        cropReplace = null
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCamera
        if (ok && uri != null) {
            cropReplace = null
            cropLauncher.launch(cropOptions(uri))
        }
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(20)) { uris ->
        if (uris.isNotEmpty()) vm.addImages(targetKind, uris)
    }

    fun openCamera(kind: Int) {
        targetKind = kind
        val uri = app.repository.images.newCameraUri()
        pendingCamera = uri
        cameraLauncher.launch(uri)
    }

    fun openGallery(kind: Int) {
        targetKind = kind
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    fun crop(kind: Int, name: String) {
        targetKind = kind
        cropReplace = name
        cropLauncher.launch(cropOptions(Uri.fromFile(app.repository.images.file(name))))
    }

    val tryClose = { if (vm.dirty) confirmDiscard = true else onClose() }
    BackHandler(onBack = tryClose)

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isNew) "记录错题" else "编辑错题", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton(onClick = tryClose) { Icon(Icons.Outlined.Close, "关闭") } },
                actions = {
                    if (vm.saving || vm.processing > 0) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(16.dp))
                    }
                    TextButton(onClick = { vm.save(onSaved) }, enabled = !vm.saving && s.loaded) {
                        Text("保存", style = MaterialTheme.typography.bodyLarge)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp),
        ) {
            Section("科目") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    subjects.forEach { subject ->
                        Chip(subject.name, selected = s.subjectId == subject.id, onClick = { vm.update { it.copy(subjectId = subject.id) } })
                    }
                    Chip("+ 新科目", selected = false, onClick = { showAddSubject = true })
                }
            }

            Section("题目图片") {
                ImageStrip(
                    images = s.questionImages,
                    onCamera = { openCamera(ImageKind.QUESTION) },
                    onGallery = { openGallery(ImageKind.QUESTION) },
                    onCrop = { crop(ImageKind.QUESTION, it) },
                    onRemove = { vm.removeImage(ImageKind.QUESTION, it) },
                    onMoveFirst = { vm.moveFirst(ImageKind.QUESTION, it) },
                )
            }

            Section("答案图片") {
                ImageStrip(
                    images = s.answerImages,
                    onCamera = { openCamera(ImageKind.ANSWER) },
                    onGallery = { openGallery(ImageKind.ANSWER) },
                    onCrop = { crop(ImageKind.ANSWER, it) },
                    onRemove = { vm.removeImage(ImageKind.ANSWER, it) },
                    onMoveFirst = { vm.moveFirst(ImageKind.ANSWER, it) },
                )
            }

            Section("题目概要") {
                OutlinedTextField(
                    value = s.title,
                    onValueChange = { v -> vm.update { it.copy(title = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("可选，例如：导数单调性含参讨论", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
            }

            Section("记录时间") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Dates.date(s.recordedAt), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { showDate = true }) { Text("改日期") }
                    Spacer(Modifier.width(4.dp))
                    Text(Dates.toLocalDateTime(s.recordedAt).toLocalTime().toString().take(5), style = MaterialTheme.typography.bodyLarge)
                    TextButton(onClick = { showTime = true }) { Text("改时间") }
                }
            }

            Section("错因") {
                TagPicker(
                    selected = s.reasonTags,
                    history = reasonHistory,
                    placeholder = "输入错因，如 计算失误、概念不清",
                    onChange = { v -> vm.update { it.copy(reasonTags = v) } },
                )
            }

            Section("模块") {
                TagPicker(
                    selected = s.moduleTags,
                    history = moduleHistory,
                    placeholder = "输入知识模块，如 函数、电磁感应",
                    onChange = { v -> vm.update { it.copy(moduleTags = v) } },
                )
            }

            Section("备注", divider = false) {
                OutlinedTextField(
                    value = s.note,
                    onValueChange = { v -> vm.update { it.copy(note = v) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    placeholder = { Text("解题思路、易错点、老师讲解……", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    shape = MaterialTheme.shapes.small,
                )
            }
        }
    }

    if (showAddSubject) {
        TextInputDialog(title = "新科目", placeholder = "科目名称", onDismiss = { showAddSubject = false }, onConfirm = { vm.addSubject(it) })
    }
    if (showDate) {
        DatePickDialog(
            initial = Dates.toLocalDate(s.recordedAt),
            onDismiss = { showDate = false },
            onPick = { date ->
                val time = Dates.toLocalDateTime(s.recordedAt).toLocalTime()
                vm.update { it.copy(recordedAt = Dates.toMillis(date.atTime(time))) }
            },
        )
    }
    if (showTime) {
        val dt = Dates.toLocalDateTime(s.recordedAt)
        TimePickDialog(
            hour = dt.hour,
            minute = dt.minute,
            onDismiss = { showTime = false },
            onPick = { h, m -> vm.update { it.copy(recordedAt = Dates.toMillis(dt.withHour(h).withMinute(m))) } },
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = "放弃修改？",
            text = "当前内容尚未保存，离开后将丢失。",
            confirmText = "放弃",
            destructive = true,
            onDismiss = { confirmDiscard = false },
            onConfirm = onClose,
        )
    }
}

@Composable
private fun Section(title: String, divider: Boolean = true, content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = 24.dp)) {
        Spacer(Modifier.height(20.dp))
        SectionLabel(title)
        Spacer(Modifier.height(12.dp))
        content()
        Spacer(Modifier.height(20.dp))
        if (divider) Hairline()
    }
}
