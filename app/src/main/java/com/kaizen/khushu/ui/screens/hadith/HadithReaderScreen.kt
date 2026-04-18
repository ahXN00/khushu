package com.kaizen.khushu.ui.screens.hadith

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaizen.khushu.ui.screens.learn.BlockRenderer
import com.kaizen.khushu.ui.screens.settings.SettingsViewModel
import com.kaizen.khushu.ui.theme.BeVietnamPro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HadithReaderScreen(
    bookId: String,
    sectionNumber: Int,
    sectionTitle: String,
    bookName: String,
    onBack: () -> Unit,
    viewModel: HadithViewModel,
    settingsViewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val hadiths by viewModel.currentHadiths
    val isLoading by viewModel.isLoading
    val settings by settingsViewModel.settings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(bookId, sectionNumber) {
        viewModel.loadHadiths(bookId, sectionNumber, bookName)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Column {
                            Text(
                                text = sectionTitle,
                                fontFamily = BeVietnamPro,
                                fontSize = 20.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            Text(
                                text = bookName,
                                fontFamily = BeVietnamPro,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Black.copy(alpha = 0.9f),
                        titleContentColor = Color.White
                    )
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(hadiths) { block ->
                        BlockRenderer(
                            block = block,
                            settings = settings,
                            fg = Color.White,
                            bg = Color.Black,
                            onBlockClick = { /* Actions can be added later */ },
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    item { Spacer(Modifier.height(100.dp)) }
                }
            }
        }
    }
}
