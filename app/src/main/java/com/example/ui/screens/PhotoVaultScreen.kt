@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.ui.components.PlantIconHelper
import com.example.ui.components.VaultElapsedDaysAnalyticsCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.data.model.GrowthStage
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.ui.viewmodel.PlantViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PhotoVaultScreen(
    viewModel: PlantViewModel,
    plants: List<Plant>,
    selectedPlant: Plant?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val photos by viewModel.selectedPlantPhotos.collectAsStateWithLifecycle()
    val isAnalyticsExpanded by viewModel.vaultAnalyticsExpanded.collectAsStateWithLifecycle()
    var showCaptureModal by remember { mutableStateOf(false) }
    var lightboxPhoto by remember { mutableStateOf<PhotoEntry?>(null) }
    var photoToDelete by remember { mutableStateOf<PhotoEntry?>(null) }
    var photoToEdit by remember { mutableStateOf<PhotoEntry?>(null) }
    var isSpeedDialOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = isSpeedDialOpen) {
        isSpeedDialOpen = false
    }

    if (selectedPlant == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Eco,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Cultivar Selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Select a plant to inspect the photo progression vault.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
            }
        }
        return
    }

    val allPhotosSorted = remember(photos) { photos.sortedBy { it.dayNumber } }
    val maxLoggedDay = remember(allPhotosSorted) { allPhotosSorted.maxOfOrNull { it.dayNumber } ?: 0 }
    val isInitialWeek = maxLoggedDay < 7

    // 7-Day Milestone Logic:
    // If during the initial 7 days (maxLoggedDay < 7), show all pictures.
    // Once Day 7 is reached (maxLoggedDay >= 7), lock intermediate daily photos
    // and display strictly the 7-day milestone pictures (Day 1 Baseline, Day 7, Day 14, Day 21...).
    val milestonePhotos = remember(allPhotosSorted, isInitialWeek, maxLoggedDay) {
        if (isInitialWeek) {
            allPhotosSorted
        } else {
            val list = mutableListOf<PhotoEntry>()
            // 1. Day 1 Baseline milestone (or earliest photo)
            val baseline = allPhotosSorted.firstOrNull { it.dayNumber == 1 } ?: allPhotosSorted.firstOrNull()
            if (baseline != null) {
                list.add(baseline)
            }
            // 2. Weekly 7-day milestones (Day 7, 14, 21, 28, 35...)
            val maxWeek = (maxLoggedDay / 7) + 1
            for (w in 1..maxWeek) {
                val targetDay = w * 7
                if (targetDay > maxLoggedDay + 3) break
                // Match exact day first, otherwise closest day within weekly window
                val exact = allPhotosSorted.lastOrNull { it.dayNumber == targetDay }
                val closest = exact ?: allPhotosSorted
                    .filter { it.dayNumber in (targetDay - 3)..(targetDay + 3) && it.id != baseline?.id }
                    .minByOrNull { kotlin.math.abs(it.dayNumber - targetDay) }

                if (closest != null && !list.any { it.id == closest.id }) {
                    list.add(closest)
                }
            }
            list.sortedBy { it.dayNumber }
        }
    }

    val lockedIntermediateCount = remember(allPhotosSorted, milestonePhotos) {
        (allPhotosSorted.size - milestonePhotos.size).coerceAtLeast(0)
    }

    // Photo picker launcher (importing existing photos to private storage)
    var selectedImageUriString by remember { mutableStateOf<String?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = saveImageLocally(context, uri)
            selectedImageUriString = savedPath
            showCaptureModal = true
        }
    }

    // Camera capture launcher (direct daily progress photos stored strictly in private app sandbox)
    var pendingCameraPhotoPath by remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && pendingCameraPhotoPath != null) {
            val savedPath = pendingCameraPhotoPath!!
            selectedImageUriString = savedPath
            showCaptureModal = true
            pendingCameraPhotoPath = null
        }
    }

    fun launchDailyCamera() {
        val folder = File(context.filesDir, "growth_photos")
        if (!folder.exists()) folder.mkdirs()
        val photoFile = File(folder, "daily_cultivar_${selectedPlant.id}_${System.currentTimeMillis()}.jpg")
        try {
            val photoUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            pendingCameraPhotoPath = photoFile.absolutePath
            cameraLauncher.launch(photoUri)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("photo_vault_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 220.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Plant Selector Filter Chips (M3 Expressive)
        if (plants.size > 1) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    plants.forEach { p ->
                        FilterChip(
                            selected = p.id == selectedPlant.id,
                            onClick = { viewModel.selectPlant(p.id) },
                            shape = RoundedCornerShape(14.dp),
                            label = { Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                            leadingIcon = {
                                Icon(
                                    imageVector = PlantIconHelper.getPlantIcon(p.iconKey),
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Vault Header Card (M3 Expressive Hero Card)
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Progression Vault",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isInitialWeek) "${selectedPlant.name} • ${milestonePhotos.size} Photos (Week 1)" else "${selectedPlant.name} • ${milestonePhotos.size} Weekly Milestones",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mandatory constraint explanation banner
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Mandatory photo attachment enforces verified visual milestones and unlocks 7-day growth comparisons.",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Private Sandbox Hardware Isolation Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(0.6.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Private Storage • Excluded from Phone Gallery",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Expandable Growth Timeline Analytics Card (Interactive Elapsed Days Curve & Node Tooltips)
        item {
            VaultElapsedDaysAnalyticsCard(
                photos = allPhotosSorted,
                onPhotoClick = { lightboxPhoto = it },
                isExpanded = isAnalyticsExpanded,
                onExpandedChange = { viewModel.setVaultAnalyticsExpanded(it) }
            )
        }

        // 7-Day Weekly Progression Unlock & Interactive Compare Hub
        item {
            InteractiveWeeklyReviewModule(
                photos = milestonePhotos,
                onPhotoClick = { lightboxPhoto = it }
            )
        }

        // Section Title: Chronological Milestone Timeline
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isInitialWeek) "Early Growth Timeline (${milestonePhotos.size})" else "Weekly Milestone Timeline (${milestonePhotos.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isInitialWeek) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = if (isInitialWeek) "Week 1 Open" else "7-Day Lock",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isInitialWeek) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Tap photo to inspect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Status Banner explaining the 7-day milestone lock
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isInitialWeek) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        0.6.dp,
                        if (isInitialWeek) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isInitialWeek) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isInitialWeek) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = if (isInitialWeek) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isInitialWeek) "Week 1 Initial Mode" else "Weekly Milestone Lock Active",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isInitialWeek) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = if (isInitialWeek) {
                                    "Showing all photos taken in first 7 days. Once Day 7 arrives, intermediate days lock to keep focus on weekly comparisons."
                                } else {
                                    "Showing strictly 7-day interval milestones (Day 1, 7, 14, 21...). $lockedIntermediateCount intermediate daily snapshots are locked to compare weeks without distraction."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (isInitialWeek) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.95f)
                            )
                        }
                    }
                }
            }
        }

        if (milestonePhotos.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No Photo Milestones Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap the camera button to capture or attach a photo to document your first canopy checkpoint. A 7-day review will unlock after two checkpoints.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }
        } else {
            items(milestonePhotos, key = { it.id }) { photo ->
                ExpressiveMilestoneCard(
                    entry = photo,
                    onPhotoClick = { lightboxPhoto = photo },
                    onEdit = { photoToEdit = photo },
                    onDelete = { photoToDelete = photo }
                )
            }
        }
    }

        // Scrim overlay to dismiss speed dial when tapping anywhere in the Vault
        if (isSpeedDialOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null
                    ) {
                        isSpeedDialOpen = false
                    }
            )
        }

        // Elevated M3 Expressive Camera Speed Dial FAB above the floating bottom menu bar
        val fabRotation by animateFloatAsState(
            targetValue = if (isSpeedDialOpen) 90f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "fab_rotation"
        )
        val fabCornerRadius by animateDpAsState(
            targetValue = if (isSpeedDialOpen) 26.dp else 16.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            ),
            label = "fab_corner_radius"
        )
        val fabContainerColor by animateColorAsState(
            targetValue = if (isSpeedDialOpen) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "fab_container_color"
        )
        val fabContentColor by animateColorAsState(
            targetValue = if (isSpeedDialOpen) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "fab_content_color"
        )

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 20.dp, bottom = 100.dp)
        ) {
            // Vertically expanding speed dial options
            AnimatedVisibility(
                visible = isSpeedDialOpen,
                enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) +
                        slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) { it / 3 },
                exit = fadeOut(spring(stiffness = Spring.StiffnessHigh)) +
                       slideOutVertically { it / 3 }
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Attach Photo Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                isSpeedDialOpen = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 3.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(
                                text = "Attach Photo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                isSpeedDialOpen = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                            modifier = Modifier.testTag("fab_attach_photo")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddAPhoto,
                                contentDescription = "Attach Photo",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // 2. Take Daily Photo Option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                indication = null
                            ) {
                                isSpeedDialOpen = false
                                launchDailyCamera()
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shadowElevation = 3.dp,
                            border = androidx.compose.foundation.BorderStroke(
                                0.8.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                        ) {
                            Text(
                                text = "Take Daily Photo",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                isSpeedDialOpen = false
                                launchDailyCamera()
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 5.dp),
                            modifier = Modifier.testTag("fab_take_photo")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = "Take Daily Photo",
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Main Compact Camera FAB with shape morphing and spring rotation into Close icon
            FloatingActionButton(
                onClick = { isSpeedDialOpen = !isSpeedDialOpen },
                shape = RoundedCornerShape(fabCornerRadius),
                containerColor = fabContainerColor,
                contentColor = fabContentColor,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = if (isSpeedDialOpen) 8.dp else 6.dp,
                    pressedElevation = 10.dp
                ),
                modifier = Modifier
                    .size(54.dp)
                    .testTag("fab_vault_camera_toggle")
            ) {
                Icon(
                    imageVector = if (isSpeedDialOpen) Icons.Filled.Close else Icons.Filled.CameraAlt,
                    contentDescription = if (isSpeedDialOpen) "Close photo actions" else "Add or take photo",
                    modifier = Modifier
                        .size(26.dp)
                        .rotate(fabRotation)
                )
            }
        }
    }

    // Fullscreen Image Lightbox Dialog
    lightboxPhoto?.let { photo ->
        Dialog(onDismissRequest = { lightboxPhoto = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                text = "Day ${photo.dayNumber} • ${photo.title}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            photo.canopyHeightCm?.let {
                                Text(
                                    text = "Canopy Height: $it cm",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        IconButton(onClick = { lightboxPhoto = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    VaultPhotoMedia(
                        photo = photo,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )

                    if (photo.observation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = photo.observation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoToEdit = photo
                                lightboxPhoto = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("btn_edit_checkpoint_lightbox")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Day & Notes", fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = {
                                photoToDelete = photo
                                lightboxPhoto = null
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    photoToDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { photoToDelete = null },
            title = { Text("Delete Milestone Checkpoint?") },
            text = { Text("Are you sure you want to delete Day ${photo.dayNumber} ('${photo.title}')? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePhotoEntry(photo)
                        photoToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { photoToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Milestone Checkpoint Dialog (Allows fixing Day 3 back to Day 2, editing titles & observations)
    photoToEdit?.let { photo ->
        EditPhotoCheckpointDialog(
            photo = photo,
            onDismiss = { photoToEdit = null },
            onSave = { updated ->
                viewModel.updatePhotoEntry(updated)
                Toast.makeText(context, "Updated to Day ${updated.dayNumber} checkpoint", Toast.LENGTH_SHORT).show()
                photoToEdit = null
            }
        )
    }

    // Mandatory Photo Entry Form Modal
    if (showCaptureModal && selectedImageUriString != null) {
        val defaultDay = viewModel.calculateCalendarDay(selectedPlant.plantedDate)
        CapturePhotoDialog(
            imageUri = selectedImageUriString!!,
            defaultStage = selectedPlant.stage,
            defaultDayNumber = defaultDay,
            onDismiss = {
                showCaptureModal = false
                selectedImageUriString = null
            },
            onSave = { day, title, stage, obs, height ->
                viewModel.addPhotoEntry(
                    plantId = selectedPlant.id,
                    imageUri = selectedImageUriString!!,
                    stage = stage,
                    title = title,
                    observation = obs,
                    canopyHeightCm = height,
                    customDayNumber = day
                )
                showCaptureModal = false
                selectedImageUriString = null
            }
        )
    }
}

enum class CompareMode {
    SIDE_BY_SIDE, FLIP_TOGGLE
}

@Composable
private fun InteractiveWeeklyReviewModule(
    photos: List<PhotoEntry>,
    onPhotoClick: (PhotoEntry) -> Unit
) {
    val sorted = photos.sortedBy { it.dayNumber }
    val eligiblePairs = mutableListOf<Pair<PhotoEntry, PhotoEntry>>()

    for (i in 0 until sorted.size - 1) {
        val p1 = sorted[i]
        val p2 = sorted[i + 1]
        if (p2.dayNumber - p1.dayNumber >= 6) {
            eligiblePairs.add(Pair(p1, p2))
        }
    }

    var compareMode by remember { mutableStateOf(CompareMode.SIDE_BY_SIDE) }
    var isShowingAfterInFlip by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (eligiblePairs.isNotEmpty()) Icons.Filled.LockOpen else Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Weekly Growth Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "7-Day structural development analysis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (eligiblePairs.isNotEmpty()) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (eligiblePairs.isNotEmpty()) "UNLOCKED" else "LOCKED",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (eligiblePairs.isNotEmpty()) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (eligiblePairs.isEmpty()) {
                Text(
                    text = "Weekly structural comparison unlocks when two checkpoints are logged at least 7 days apart. Keep capturing photo progress to inspect side-by-side growth.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val latestPair = eligiblePairs.last()
                val dayDiff = latestPair.second.dayNumber - latestPair.first.dayNumber
                val heightDiff = if (latestPair.first.canopyHeightCm != null && latestPair.second.canopyHeightCm != null) {
                    latestPair.second.canopyHeightCm!! - latestPair.first.canopyHeightCm!!
                } else null

                // View Mode Selector (Side-by-side vs Interactive A/B Flip)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = compareMode == CompareMode.SIDE_BY_SIDE,
                        onClick = { compareMode = CompareMode.SIDE_BY_SIDE },
                        label = { Text("Side-by-Side Cards", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Filled.Compare, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = compareMode == CompareMode.FLIP_TOGGLE,
                        onClick = { compareMode = CompareMode.FLIP_TOGGLE },
                        label = { Text("A/B Instant Flip", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (compareMode == CompareMode.SIDE_BY_SIDE) {
                    // Side-by-side swipe cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = "Day ${latestPair.first.dayNumber} (Baseline)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            VaultPhotoMedia(
                                photo = latestPair.first,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onPhotoClick(latestPair.first) }
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Day ${latestPair.second.dayNumber} (+${dayDiff}d)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            VaultPhotoMedia(
                                photo = latestPair.second,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                                    .clickable { onPhotoClick(latestPair.second) }
                            )
                        }
                    }
                } else {
                    // Interactive A/B Flip Toggle: tap button to toggle between Before & After
                    val activePhoto = if (isShowingAfterInFlip) latestPair.second else latestPair.first

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isShowingAfterInFlip) "SHOWING: Day ${latestPair.second.dayNumber} (+${dayDiff}d)"
                                else "SHOWING: Day ${latestPair.first.dayNumber} (Baseline)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isShowingAfterInFlip) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedButton(
                                onClick = { isShowingAfterInFlip = !isShowingAfterInFlip },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isShowingAfterInFlip) "Flip to Before" else "Flip to After", fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        VaultPhotoMedia(
                            photo = activePhoto,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(18.dp))
                                .clickable { onPhotoClick(activePhoto) }
                        )
                    }
                }

                if (heightDiff != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "7-Day Vertical Canopy Stretch",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "+${String.format(Locale.US, "%.1f", heightDiff)} cm",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpressiveMilestoneCard(
    entry: PhotoEntry,
    onPhotoClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val dateString = remember(entry.timestamp) { dateFormat.format(Date(entry.timestamp)) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header Row: Day pill, title, edit and delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "DAY ${entry.dayNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = entry.stage.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    val milestoneTag = remember(entry.dayNumber) {
                        when {
                            entry.dayNumber == 1 -> "Baseline"
                            entry.dayNumber % 7 == 0 -> "Week ${entry.dayNumber / 7}"
                            else -> "Milestone"
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = milestoneTag,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Text(
                        text = entry.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp).testTag("btn_edit_checkpoint_${entry.id}")) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit checkpoint",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp).testTag("btn_delete_checkpoint_${entry.id}")) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete entry",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attached Photo Image (Full-bleed card with zoom indicator)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onPhotoClick)
            ) {
                VaultPhotoMedia(
                    photo = entry,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Zoom Pill Overlay
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = "Zoom",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Inspect",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats row: Canopy Height badge & Date
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entry.canopyHeightCm?.let {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Canopy: $it cm",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }

            if (entry.observation.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = entry.observation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CapturePhotoDialog(
    imageUri: String,
    defaultStage: GrowthStage,
    defaultDayNumber: Int,
    onDismiss: () -> Unit,
    onSave: (dayNumber: Int, title: String, stage: GrowthStage, obs: String, height: Double?) -> Unit
) {
    var dayInput by remember { mutableStateOf(defaultDayNumber.toString()) }
    var title by remember { mutableStateOf("") }
    var selectedStage by remember { mutableStateOf(defaultStage) }
    var heightInput by remember { mutableStateOf("") }
    var observation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Save Photo Progress Checkpoint",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Image Preview confirming attachment
                AsyncImage(
                    model = if (imageUri.startsWith("/")) File(imageUri) else imageUri,
                    contentDescription = "Selected photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dayInput,
                        onValueChange = { dayInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Day #") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(0.35f)
                            .testTag("input_photo_day_number")
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Log Title (e.g. Node 4)") },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .weight(0.65f)
                            .testTag("input_photo_title")
                    )
                }

                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = { Text("Canopy Height in cm (e.g. 24.5)") },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_photo_height")
                )

                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text("Foliage and Health Observations") },
                    minLines = 2,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_photo_obs")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedDay = dayInput.toIntOrNull()?.coerceAtLeast(1) ?: defaultDayNumber
                    val h = heightInput.toDoubleOrNull()
                    onSave(parsedDay, title.ifBlank { "Day $parsedDay Progress" }, selectedStage, observation, h)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = CircleShape,
                modifier = Modifier.testTag("btn_save_photo_entry")
            ) {
                Text("Save Checkpoint", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = CircleShape
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun EditPhotoCheckpointDialog(
    photo: PhotoEntry,
    onDismiss: () -> Unit,
    onSave: (PhotoEntry) -> Unit
) {
    var dayInput by remember { mutableStateOf(photo.dayNumber.toString()) }
    var title by remember { mutableStateOf(photo.title) }
    var heightInput by remember { mutableStateOf(photo.canopyHeightCm?.toString() ?: "") }
    var observation by remember { mutableStateOf(photo.observation) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Milestone Checkpoint",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dayInput,
                        onValueChange = { dayInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Day #") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.35f)
                            .testTag("edit_photo_day_number")
                    )

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Checkpoint Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(0.65f)
                            .testTag("edit_photo_title")
                    )
                }

                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = { Text("Canopy Height in cm") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_photo_height")
                )

                OutlinedTextField(
                    value = observation,
                    onValueChange = { observation = it },
                    label = { Text("Observations & Notes") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_photo_obs")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = dayInput.toIntOrNull()?.coerceAtLeast(1) ?: photo.dayNumber
                    val h = heightInput.toDoubleOrNull()
                    val updated = photo.copy(
                        dayNumber = d,
                        title = title.ifBlank { "Day $d Progress" },
                        observation = observation,
                        canopyHeightCm = h
                    )
                    onSave(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("btn_save_edited_checkpoint")
            ) {
                Text("Update", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun saveImageLocally(context: Context, uri: Uri): String {
    return try {
        val folder = File(context.filesDir, "growth_photos")
        if (!folder.exists()) folder.mkdirs()
        val destFile = File(folder, "photo_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        destFile.absolutePath
    } catch (_: Exception) {
        uri.toString()
    }
}

fun isPhotoFileAvailable(context: Context, rawUri: String): Boolean {
    val trimmed = rawUri.trim()
    if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return false
    return when {
        trimmed.startsWith("/") -> File(trimmed).exists()
        trimmed.startsWith("file://") -> {
            try {
                val path = Uri.parse(trimmed).path
                path != null && File(path).exists()
            } catch (_: Exception) {
                false
            }
        }
        trimmed.startsWith("android.resource://") -> true
        trimmed.startsWith("content://") -> {
            try {
                context.contentResolver.openInputStream(Uri.parse(trimmed))?.use { true } ?: false
            } catch (_: Exception) {
                false
            }
        }
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> true
        else -> false
    }
}

@Composable
fun BotanicalMilestonePlaceholder(
    entry: PhotoEntry,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
        ),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Icon(
                imageVector = Icons.Filled.Eco,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier
                    .size(130.dp)
                    .align(Alignment.CenterEnd)
                    .rotate(-15f)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "DAY ${entry.dayNumber}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = entry.stage.label,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Archived",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text(
                        text = entry.title.ifBlank { "Checkpoint Milestone" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    entry.canopyHeightCm?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.TrendingUp,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Canopy: $it cm",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (entry.observation.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "\"${entry.observation}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Eco,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Botanical entry preserved • Original file on source device",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun VaultPhotoMedia(
    photo: PhotoEntry,
    contentScale: ContentScale = ContentScale.Crop,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageAvailable = remember(photo.imageUri) { isPhotoFileAvailable(context, photo.imageUri) }
    val imageModel = remember(photo.imageUri) {
        if (photo.imageUri.startsWith("/")) File(photo.imageUri) else photo.imageUri
    }

    if (!imageAvailable) {
        BotanicalMilestonePlaceholder(entry = photo, modifier = modifier)
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageModel)
                .crossfade(true)
                .build(),
            contentDescription = photo.title,
            contentScale = contentScale,
            modifier = modifier,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            },
            error = {
                BotanicalMilestonePlaceholder(entry = photo, modifier = Modifier.fillMaxSize())
            }
        )
    }
}
