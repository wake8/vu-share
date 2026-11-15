package com.vunnam.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vunnam.VuShareApp
import com.vunnam.model.FileCategory
import com.vunnam.model.SelectableFile
import com.vunnam.ui.MainViewModel
import com.vunnam.ui.components.vuCheckboxColors
import com.vunnam.ui.theme.VuShareTheme
import kotlinx.coroutines.launch

data class MigrationCategoryItem(
    val category: FileCategory,
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val tintColor: Color
)

@Composable
fun PhoneMigrationScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onProceedToDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    val context = LocalContext.current
    val app = context.applicationContext as VuShareApp
    val coroutineScope = rememberCoroutineScope()

    val selectedCategories = remember {
        mutableStateMapOf(
            FileCategory.PHOTOS to true,
            FileCategory.VIDEOS to true,
            FileCategory.MUSIC to true,
            FileCategory.DOCUMENTS to true,
            FileCategory.APPS to true
        )
    }

    var isPreparing by remember { mutableStateOf(false) }

    val categoriesList = listOf(
        MigrationCategoryItem(FileCategory.PHOTOS, Icons.Default.Image, "Photos", "Gallery images and camera photos", colors.primaryAccent),
        MigrationCategoryItem(FileCategory.VIDEOS, Icons.Default.Movie, "Videos", "Captured videos and recordings", colors.secondaryHue),
        MigrationCategoryItem(FileCategory.MUSIC, Icons.Default.Audiotrack, "Music & Audio", "Audio tracks, songs, podcasts", colors.warning),
        MigrationCategoryItem(FileCategory.DOCUMENTS, Icons.Default.Description, "Documents", "PDFs, spreadsheets, text files", if (colors.isLight) Color(0xFF1D4ED8) else Color(0xFF60A5FA)),
        MigrationCategoryItem(FileCategory.APPS, Icons.Default.Android, "Apps (APKs)", "Installed non-system applications", colors.success)
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        bottomBar = {
            Surface(
                color = colors.elevatedSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                border = BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedCategories.values.count { it }} categories selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = "Batch migrate to your new phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primaryAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isPreparing = true
                                val allFiles = mutableListOf<SelectableFile>()
                                if (selectedCategories[FileCategory.PHOTOS] == true) {
                                    allFiles.addAll(app.storageManager.queryPhotos())
                                }
                                if (selectedCategories[FileCategory.VIDEOS] == true) {
                                    allFiles.addAll(app.storageManager.queryVideos())
                                }
                                if (selectedCategories[FileCategory.MUSIC] == true) {
                                    allFiles.addAll(app.storageManager.queryMusic())
                                }
                                if (selectedCategories[FileCategory.DOCUMENTS] == true) {
                                    allFiles.addAll(app.storageManager.queryDocuments())
                                }
                                if (selectedCategories[FileCategory.APPS] == true) {
                                    allFiles.addAll(app.storageManager.queryInstalledApps())
                                }

                                viewModel.clearFileSelection()
                                viewModel.addCustomPickedFiles(allFiles.map { it.uri })
                                isPreparing = false
                                onProceedToDiscovery()
                            }
                        },
                        enabled = !isPreparing && selectedCategories.values.any { it },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryAccent,
                            contentColor = colors.onPrimaryAccent,
                            disabledContainerColor = colors.disabledSurface,
                            disabledContentColor = colors.disabledText
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("start_migration_button")
                    ) {
                        if (isPreparing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = colors.onPrimaryAccent,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Start Clone",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("migration_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.primaryText
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Phone Migration",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primaryText
                        )
                        Text(
                            text = "One-tap full data migration to a new Android phone",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.secondaryText
                        )
                    }
                }
            }

            items(categoriesList.size) { index ->
                val item = categoriesList[index]
                val isChecked = selectedCategories[item.category] == true

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isChecked) colors.selectedSurface else colors.primaryCard
                    ),
                    border = BorderStroke(
                        if (isChecked) 1.5.dp else 1.dp,
                        if (isChecked) colors.selectedBorder else colors.border
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(item.tintColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = item.tintColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.secondaryText
                            )
                        }

                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedCategories[item.category] = checked
                            },
                            colors = vuCheckboxColors()
                        )
                    }
                }
            }
        }
    }
}
