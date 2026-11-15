package com.vunnam.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vunnam.model.FileCategory
import com.vunnam.network.NetworkUtils
import com.vunnam.ui.MainViewModel
import com.vunnam.ui.components.FileItemCard
import com.vunnam.ui.components.vuFilterChipColors
import com.vunnam.ui.components.vuTextFieldColors
import com.vunnam.ui.theme.VuShareTheme

private enum class FileSortOption(val label: String) {
    NEWEST("Newest"),
    NAME("Name (A–Z)"),
    SIZE_DESC("Largest")
}

@Composable
fun SendPickerScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onProceedToDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = VuShareTheme.colors
    val currentCategory by viewModel.currentCategory.collectAsState()
    val categoryFiles by viewModel.categoryFiles.collectAsState()
    val selectedFiles by viewModel.selectedFiles.collectAsState()
    val isLoading by viewModel.isLoadingFiles.collectAsState()
    var searchInput by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(FileSortOption.NEWEST) }
    var showSortMenu by remember { mutableStateOf(false) }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addCustomPickedFiles(uris)
        }
    }

    val filteredFiles = remember(categoryFiles, searchInput, sortOption) {
        val base = if (searchInput.isBlank()) {
            categoryFiles
        } else {
            categoryFiles.filter { it.name.contains(searchInput, ignoreCase = true) }
        }
        when (sortOption) {
            FileSortOption.NEWEST -> base.sortedByDescending { it.dateModified }
            FileSortOption.NAME -> base.sortedBy { it.name.lowercase() }
            FileSortOption.SIZE_DESC -> base.sortedByDescending { it.size }
        }
    }

    val totalSelectedBytes = remember(selectedFiles) {
        selectedFiles.sumOf { it.size }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        bottomBar = {
            AnimatedVisibility(
                visible = selectedFiles.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                Surface(
                    color = colors.elevatedSurface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    tonalElevation = 8.dp,
                    border = BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${selectedFiles.size} selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.primaryText
                            )
                            Text(
                                text = NetworkUtils.formatFileSize(totalSelectedBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.primaryAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = { viewModel.clearFileSelection() },
                                modifier = Modifier.testTag("clear_selection_button")
                            ) {
                                Text("Clear", color = colors.error, fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = onProceedToDiscovery,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.primaryAccent,
                                    contentColor = colors.onPrimaryAccent
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.testTag("proceed_to_discovery_button")
                            ) {
                                Text(
                                    text = "Next",
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
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("picker_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.primaryText
                    )
                }

                Text(
                    text = "Select Files to Send",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.primaryText,
                    modifier = Modifier.weight(1f)
                )

                // Sort menu button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort Files",
                            tint = colors.secondaryText
                        )
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        containerColor = colors.elevatedSurface
                    ) {
                        FileSortOption.values().forEach { opt ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = opt.label,
                                        color = if (sortOption == opt) colors.primaryAccent else colors.primaryText,
                                        fontWeight = if (sortOption == opt) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    sortOption = opt
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                // Browse Storage via SAF button
                IconButton(
                    onClick = { documentPickerLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("saf_browse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Browse Storage",
                        tint = colors.primaryAccent
                    )
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Search files by name...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (searchInput.isNotEmpty()) {
                        IconButton(onClick = { searchInput = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("file_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = vuTextFieldColors()
            )

            // Category Chips Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FileCategory.values()) { category ->
                    val isSelected = currentCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = { Text(category.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                        shape = RoundedCornerShape(12.dp),
                        colors = vuFilterChipColors(),
                        border = BorderStroke(1.dp, if (isSelected) colors.primaryAccent else colors.border)
                    )
                }
            }

            // Quick Selection Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredFiles.size} items • ${sortOption.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.secondaryText
                )

                TextButton(
                    onClick = { viewModel.selectAllCurrentCategory() },
                    modifier = Modifier.testTag("select_all_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = colors.primaryAccent
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select All", color = colors.primaryAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Files List or Loading State
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.primaryAccent)
                }
            } else if (filteredFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No files found in this category",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.secondaryText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { documentPickerLauncher.launch(arrayOf("*/*")) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.border)
                        ) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = colors.primaryAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Browse Storage (SAF)", color = colors.primaryText)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredFiles, key = { it.uri.toString() }) { file ->
                        val isSelected = selectedFiles.any { it.uri == file.uri }
                        FileItemCard(
                            file = file,
                            isSelected = isSelected,
                            onToggle = { viewModel.toggleFileSelection(file) }
                        )
                    }
                }
            }
        }
    }
}
