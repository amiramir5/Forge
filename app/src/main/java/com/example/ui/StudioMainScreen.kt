package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.ExportedVideoItem
import com.example.ui.components.AiMotionEditorDialog
import com.example.ui.components.CodeEditorTab
import com.example.ui.components.EditTitleDialog
import com.example.ui.components.GalleryTab
import com.example.ui.components.PreviewTab
import com.example.ui.components.RenderProgressDialog
import com.example.ui.components.SettingsTab
import com.example.ui.components.StudioHeader
import com.example.ui.components.StudioNavigationBar
import com.example.ui.components.TemplatePickerModal
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.BackgroundCanvas
import com.example.ui.theme.InkBlack
import com.example.ui.theme.InkCharcoal
import com.example.ui.theme.ShadowElevated
import com.example.util.VideoExportManager

@Composable
fun StudioMainScreen(
    viewModel: StudioViewModel
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val projectTitle by viewModel.projectTitle.collectAsStateWithLifecycle()
    val htmlCode by viewModel.htmlCode.collectAsStateWithLifecycle()
    val hasChangesToReset by viewModel.hasChangesToReset.collectAsStateWithLifecycle()
    val selectedPreset by viewModel.selectedPreset.collectAsStateWithLifecycle()
    val selectedFps by viewModel.selectedFps.collectAsStateWithLifecycle()
    val durationSec by viewModel.durationSec.collectAsStateWithLifecycle()
    val selectedBitrate by viewModel.selectedBitrate.collectAsStateWithLifecycle()
    val captureMode by viewModel.captureMode.collectAsStateWithLifecycle()
    val autoCleanControls by viewModel.autoCleanControls.collectAsStateWithLifecycle()
    val renderPhase by viewModel.renderPhase.collectAsStateWithLifecycle()
    val exportedVideos by viewModel.exportedVideos.collectAsStateWithLifecycle()
    val previewKey by viewModel.previewKey.collectAsStateWithLifecycle()
    val showTemplatePicker by viewModel.showTemplatePicker.collectAsStateWithLifecycle()
    val activeVideoPreview by viewModel.activeVideoPreview.collectAsStateWithLifecycle()

    val showAiDialog by viewModel.showAiDialog.collectAsStateWithLifecycle()
    val isAiThinking by viewModel.isAiThinking.collectAsStateWithLifecycle()
    val aiError by viewModel.aiError.collectAsStateWithLifecycle()

    var showEditTitleDialog by remember { mutableStateOf(false) }

    // System back button handling
    BackHandler(enabled = selectedTab != StudioTab.PREVIEW || activeVideoPreview != null || showTemplatePicker || showEditTitleDialog || showAiDialog) {
        when {
            showEditTitleDialog -> showEditTitleDialog = false
            showAiDialog -> viewModel.closeAiDialog()
            showTemplatePicker -> viewModel.closeTemplatePicker()
            activeVideoPreview != null -> viewModel.closeVideoPlayer()
            selectedTab != StudioTab.PREVIEW -> viewModel.selectTab(StudioTab.PREVIEW)
        }
    }

    val currentActivity = (context as? android.app.Activity) ?: com.example.MainActivity.currentActivity

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            StudioHeader(
                title = projectTitle,
                preset = selectedPreset,
                fps = selectedFps,
                durationSec = durationSec,
                onTitleClick = { showEditTitleDialog = true },
                onTemplatesClick = { viewModel.openTemplatePicker() },
                onQuickSettingsClick = { viewModel.selectTab(StudioTab.SETTINGS) }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val latestVideo = exportedVideos.firstOrNull()
                AnimatedVisibility(
                    visible = latestVideo != null && activeVideoPreview == null,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
                    if (latestVideo != null) {
                        FloatingMiniVideoBar(
                            item = latestVideo,
                            onPlay = { viewModel.playVideo(latestVideo) },
                            onSave = { VideoExportManager.saveVideoToGallery(context, latestVideo.file) }
                        )
                    }
                }

                StudioNavigationBar(
                    currentTab = selectedTab,
                    exportCount = exportedVideos.size,
                    onTabSelected = { tab -> viewModel.selectTab(tab) }
                )
            }
        },
        containerColor = BackgroundCanvas
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundCanvas)
        ) {
            Crossfade(
                targetState = selectedTab,
                label = "studio_tab_transition"
            ) { tab ->
                when (tab) {
                    StudioTab.PREVIEW -> {
                        PreviewTab(
                            title = projectTitle,
                            htmlCode = htmlCode,
                            preset = selectedPreset,
                            fps = selectedFps,
                            durationSec = durationSec,
                            previewKey = previewKey,
                            autoCleanControls = autoCleanControls,
                            onPresetChange = { viewModel.selectPreset(it) },
                            onRefresh = { viewModel.refreshPreview() },
                            onOpenTemplates = { viewModel.openTemplatePicker() },
                            onStartRender = { webView -> viewModel.startRender(currentActivity, webView) }
                        )
                    }

                    StudioTab.CODE -> {
                        CodeEditorTab(
                            htmlCode = htmlCode,
                            hasChangesToReset = hasChangesToReset,
                            onCodeChange = { viewModel.updateHtml(it) },
                            onOpenTemplates = { viewModel.openTemplatePicker() },
                            onOpenAiEditor = { viewModel.openAiDialog() },
                            onResetAllChanges = { viewModel.resetAllChanges() },
                            onApplyAndPreview = {
                                viewModel.refreshPreview()
                                viewModel.selectTab(StudioTab.PREVIEW)
                            }
                        )
                    }

                    StudioTab.SETTINGS -> {
                        SettingsTab(
                            preferences = viewModel.preferences,
                            preset = selectedPreset,
                            fps = selectedFps,
                            durationSec = durationSec,
                            bitrate = selectedBitrate,
                            captureMode = captureMode,
                            autoCleanControls = autoCleanControls,
                            onPresetChange = { viewModel.selectPreset(it) },
                            onFpsChange = { viewModel.selectFps(it) },
                            onDurationChange = { viewModel.setDuration(it) },
                            onBitrateChange = { viewModel.selectBitrate(it) },
                            onCaptureModeChange = { viewModel.selectCaptureMode(it) },
                            onAutoCleanControlsToggle = { viewModel.toggleAutoCleanControls(it) },
                            onStartRender = { viewModel.startRender(currentActivity) }
                        )
                    }

                    StudioTab.EXPORTS -> {
                        GalleryTab(
                            videos = exportedVideos,
                            onPlayVideo = { viewModel.playVideo(it) },
                            onDeleteVideo = { viewModel.deleteExportedVideo(it) },
                            onNavigateToRender = { viewModel.selectTab(StudioTab.PREVIEW) }
                        )
                    }
                }
            }

            // Render Progress Dialog Overlay
            RenderProgressDialog(
                renderPhase = renderPhase,
                onCancel = { viewModel.cancelRender() }
            )

            // Video Player Dialog Modal
            activeVideoPreview?.let { item ->
                VideoPlayerDialog(
                    item = item,
                    onDismiss = { viewModel.closeVideoPlayer() }
                )
            }

            // Template Picker Modal
            if (showTemplatePicker) {
                TemplatePickerModal(
                    onSelectTemplate = { template ->
                        viewModel.applyTemplate(template)
                    },
                    onDismiss = { viewModel.closeTemplatePicker() }
                )
            }

            // Edit Title Dialog
            if (showEditTitleDialog) {
                EditTitleDialog(
                    initialTitle = projectTitle,
                    onSave = { newTitle -> viewModel.updateTitle(newTitle) },
                    onDismiss = { showEditTitleDialog = false }
                )
            }

            // AI Motion Editor Dialog
            if (showAiDialog) {
                AiMotionEditorDialog(
                    modelName = viewModel.preferences.selectedModel,
                    isThinking = isAiThinking,
                    errorMessage = aiError,
                    onExecuteTask = { taskType, prompt ->
                        viewModel.executeAiMotionTask(taskType, prompt)
                    },
                    onDismiss = { viewModel.closeAiDialog() }
                )
            }
        }
    }
}

@Composable
fun FloatingMiniVideoBar(
    item: ExportedVideoItem,
    onPlay: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = ShadowElevated)
            .clickable { onPlay() },
        shape = RoundedCornerShape(20.dp),
        color = InkCharcoal
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (item.thumbnail != null) {
                    Image(
                        bitmap = item.thumbnail.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "MP4 Ready • ${item.resolution}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.65f)
                )
            }

            IconButton(
                onClick = onSave,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Save to Gallery",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onPlay() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = InkBlack,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
