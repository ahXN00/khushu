package com.kaizen.khushu.ui.screens.learn

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaizen.khushu.data.repository.LearnRepository
import com.kaizen.khushu.ui.theme.BeVietnamPro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnSectionDetailScreen(
    sectionTitle: String,
    onBack: () -> Unit,
    onCardTap: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val titleFraction = scrollBehavior.state.collapsedFraction
    val titleFontSize = androidx.compose.ui.util.lerp(28f, 20f, titleFraction).sp

    // Find the matching section from real data; fall back to empty list
    val section = LearnRepository.getSections().find { it.sectionTitle == sectionTitle }
    val sectionId = section?.id ?: ""
    val sectionColor = section?.let { androidx.compose.ui.graphics.Color(it.color) }
        ?: androidx.compose.ui.graphics.Color(0xFF3B4A6BL)
    val topics = section?.topics ?: emptyList()

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = sectionTitle,
                        fontFamily = BeVietnamPro,
                        fontSize = titleFontSize,
                        fontWeight = FontWeight.Normal,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            contentPadding = paddingValues,
            modifier = Modifier.fillMaxSize(),
        ) {
            items(topics.size) { index ->
                val topic = topics[index]
                LearnCard(
                    title = topic.title,
                    color = sectionColor,
                    sectionId = sectionId,
                    shape = RoundedCornerShape(28.dp),
                    onClick = { onCardTap(topic.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}
