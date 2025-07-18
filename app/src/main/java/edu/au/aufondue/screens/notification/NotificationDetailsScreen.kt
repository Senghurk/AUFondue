// Location: app/src/main/java/edu/au/aufondue/screens/notification/NotificationDetailsScreen.kt
// UPDATE THIS EXISTING FILE - REPLACE ALL CONTENT

package edu.au.aufondue.screens.notification

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import edu.au.aufondue.R
import edu.au.aufondue.api.RetrofitClient
import edu.au.aufondue.api.models.UpdateResponse

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationDetailsScreen(
    issueId: Long,
    onNavigateBack: () -> Unit,
    viewModel: NotificationDetailsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // Load issue details when the screen is first displayed
    LaunchedEffect(issueId) {
        viewModel.loadIssueDetails(issueId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.issue_details)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (state.error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = stringResource(R.string.error),
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = stringResource(R.string.error),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.error
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = state.error ?: stringResource(R.string.something_went_wrong),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.loadIssueDetails(issueId) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.retry))
                    }
                }
            } else {
                val issue = state.issue
                if (issue != null) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Status Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = when (issue.status) {
                                        "COMPLETED" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                                        "IN PROGRESS" -> Color(0xFF2196F3).copy(alpha = 0.2f)
                                        else -> Color(0xFFFFA000).copy(alpha = 0.2f)
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${stringResource(R.string.status)}: ${getStatusText(issue.status)}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when (issue.status) {
                                            "COMPLETED" -> Color(0xFF1B5E20)
                                            "IN PROGRESS" -> Color(0xFF0D47A1)
                                            else -> Color(0xFF7A4F01)
                                        }
                                    )
                                }
                            }
                        }

                        // Location and Category Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // Location
                                    Column {
                                        Text(
                                            text = stringResource(R.string.location),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (issue.usingCustomLocation)
                                                issue.customLocation ?: stringResource(R.string.not_specified)
                                            else
                                                "${stringResource(R.string.latitude)}: ${issue.latitude}, ${stringResource(R.string.longitude)}: ${issue.longitude}",
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }

                                    Divider()

                                    // Category
                                    Column {
                                        Text(
                                            text = stringResource(R.string.category),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = getCategoryText(issue.category),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                }
                            }
                        }

                        // Description Card
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.description),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = issue.description,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }

                        // Original Photos
                        if (issue.photoUrls.isNotEmpty()) {
                            item {
                                IssuePhotosCard(photos = issue.photoUrls)
                            }
                        }

                        // Update From OM Section
                        if (state.updates.isNotEmpty()) {
                            item {
                                Text(
                                    text = stringResource(R.string.update_from_om),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }

                            items(state.updates) { update ->
                                UpdateCard(update = update, viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun getStatusText(status: String): String {
    return when (status) {
        "PENDING" -> stringResource(R.string.status_pending)
        "IN PROGRESS" -> stringResource(R.string.status_in_progress)
        "COMPLETED" -> stringResource(R.string.status_completed)
        else -> status
    }
}

@Composable
private fun getCategoryText(category: String): String {
    return when (category) {
        "Cracked" -> stringResource(R.string.category_cracked)
        "Leaking" -> stringResource(R.string.category_leaking)
        "Flooded" -> stringResource(R.string.category_flooded)
        "Broken" -> stringResource(R.string.category_broken)
        else -> category
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IssuePhotosCard(photos: List<String>) {
    val pagerState = rememberPagerState(pageCount = { photos.size })
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${stringResource(R.string.report_photos)} (${photos.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            ) {
                // Photo Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val originalUrl = photos[page]
                        val fixedUrl = RetrofitClient.fixImageUrl(originalUrl)
                        Log.d("IssuePhotosCard", "Loading image from URL: $fixedUrl")

                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(fixedUrl)
                                    .diskCachePolicy(CachePolicy.ENABLED)
                                    .memoryCachePolicy(CachePolicy.ENABLED)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "${stringResource(R.string.report_photo)} ${page + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Fit,
                            )

                            // Loading indicator inside a 'remember mutableStateOf' to conditionally show
                            var isLoading by remember { mutableStateOf(true) }
                            var isError by remember { mutableStateOf(false) }

                            LaunchedEffect(fixedUrl) {
                                isLoading = true
                                isError = false
                            }

                            if (isLoading) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            if (isError) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.LightGray)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        stringResource(R.string.error_loading_image),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "URL: $fixedUrl",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 8.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Fallback text if image fails to load
                        if (originalUrl.isNullOrEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray)
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    stringResource(R.string.image_not_available),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                // Page Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(photos.size) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        Color.Gray.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpdateCard(update: UpdateResponse, viewModel: NotificationDetailsViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header with status and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${stringResource(R.string.status)}: ${getStatusText(update.status)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = when (update.status) {
                        "COMPLETED" -> Color(0xFF1B5E20)
                        "IN PROGRESS" -> Color(0xFF0D47A1)
                        else -> Color(0xFF7A4F01)
                    }
                )
                Text(
                    text = viewModel.formatDateTime(update.updateTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Comments
            if (!update.comment.isNullOrEmpty()) {
                Text(
                    text = "${stringResource(R.string.comments)}:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = update.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Photos if any
            if (update.photoUrls.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.update_photos)} (${update.photoUrls.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Display photos in a horizontal pager
                val pagerState = rememberPagerState(pageCount = { update.photoUrls.size })
                val context = LocalContext.current

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val originalUrl = update.photoUrls[page]
                            val fixedUrl = RetrofitClient.fixImageUrl(originalUrl)
                            Log.d("UpdateCard", "Loading update image from URL: $fixedUrl")

                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(fixedUrl)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "${stringResource(R.string.update_photo)} ${page + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit,
                                )

                                // Loading indicator inside a 'remember mutableStateOf' to conditionally show
                                var isLoading by remember { mutableStateOf(true) }
                                var isError by remember { mutableStateOf(false) }

                                LaunchedEffect(fixedUrl) {
                                    isLoading = true
                                    isError = false
                                }

                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (isError) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.LightGray)
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            stringResource(R.string.error_loading_image),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "URL: $fixedUrl",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 8.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Fallback text if image fails to load
                            if (originalUrl.isNullOrEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.LightGray)
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        stringResource(R.string.image_not_available),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }

                    // Page Indicator for photos
                    if (update.photoUrls.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(update.photoUrls.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (pagerState.currentPage == index)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                Color.Gray.copy(alpha = 0.5f)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}