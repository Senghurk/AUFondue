package edu.au.unimend.aufondue.screens.notification
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import edu.au.unimend.aufondue.api.RetrofitClient
import edu.au.unimend.aufondue.api.models.UpdateResponse
import edu.au.unimend.aufondue.components.FullScreenVideoDialog
import edu.au.unimend.aufondue.components.PhotoViewerDialog
import edu.au.unimend.aufondue.components.VideoPlayer
import edu.au.unimend.aufondue.utils.MediaUtils
import edu.au.unimend.aufondue.R

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
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

                        // Original Photos and Videos
                        val allMediaUrls = issue.photoUrls + issue.videoUrls
                        if (allMediaUrls.isNotEmpty()) {
                            item {
                                IssuePhotosCard(
                                    photos = allMediaUrls,
                                    videoUrls = issue.videoUrls // Pass video URLs separately for better detection
                                )
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

@Composable
fun IssuePhotosCard(photos: List<String>, videoUrls: List<String> = emptyList()) {
    val context = LocalContext.current

    // State for photo viewer
    var showPhotoViewer by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }
    
    // State for full screen video
    var showFullScreenVideo by remember { mutableStateOf(false) }
    var fullScreenVideoUrl by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.report_photos),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (photos.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_photos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                val pagerState = rememberPagerState(pageCount = { photos.size })

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
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    selectedPhotoIndex = page
                                    showPhotoViewer = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val originalUrl = photos[page]
                            val fixedUrl = RetrofitClient.fixMediaUrl(originalUrl)
                            
                            // Determine media type - prioritize videoUrls array over extension detection
                            val isKnownVideo = videoUrls.any { it == originalUrl }
                            val mediaType = if (isKnownVideo) {
                                MediaUtils.MediaType.VIDEO
                            } else {
                                MediaUtils.getMediaType(fixedUrl)
                            }
                            
                            Log.d("IssuePhotosCard", "Original URL: $originalUrl")
                            Log.d("IssuePhotosCard", "Fixed URL: $fixedUrl")
                            Log.d("IssuePhotosCard", "Is known video: $isKnownVideo")
                            Log.d("IssuePhotosCard", "Detected media type: $mediaType")
                            Log.d("IssuePhotosCard", "URL extension: ${fixedUrl.substringAfterLast('.', "")}")

                            when (mediaType) {
                                MediaUtils.MediaType.VIDEO -> {
                                    // Display video player
                                    VideoPlayer(
                                        videoUrl = fixedUrl,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        autoPlay = false,
                                        showControls = true,
                                        onFullScreenClick = {
                                            fullScreenVideoUrl = fixedUrl
                                            showFullScreenVideo = true
                                        }
                                    )
                                }
                                MediaUtils.MediaType.IMAGE -> {
                                    // Display image (existing code)
                                    // Use remember with the URL as key to reset states when URL changes
                                    var isLoading by remember(fixedUrl) { mutableStateOf(true) }
                                    var isError by remember(fixedUrl) { mutableStateOf(false) }

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
                                            onLoading = {
                                                isLoading = true
                                                isError = false
                                            },
                                            onSuccess = {
                                                isLoading = false
                                                isError = false
                                                Log.d("IssuePhotosCard", "Image loaded successfully: $fixedUrl")
                                            },
                                            onError = {
                                                isLoading = false
                                                isError = true
                                                Log.e("IssuePhotosCard", "Failed to load image: $fixedUrl", it.result.throwable)
                                            }
                                        )

                                // Show loading indicator only while loading
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

                                // Show error state if image failed to load
                                if (isError) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.LightGray)
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = "Error",
                                            modifier = Modifier.size(48.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            stringResource(R.string.error_loading_image),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                    }
                                }
                                MediaUtils.MediaType.UNKNOWN -> {
                                    // Default to image behavior for unknown types
                                    var isLoading by remember(fixedUrl) { mutableStateOf(true) }
                                    var isError by remember(fixedUrl) { mutableStateOf(false) }

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
                                            onLoading = {
                                                isLoading = true
                                                isError = false
                                            },
                                            onSuccess = {
                                                isLoading = false
                                                isError = false
                                                Log.d("IssuePhotosCard", "Image loaded successfully: $fixedUrl")
                                            },
                                            onError = {
                                                isLoading = false
                                                isError = true
                                                Log.e("IssuePhotosCard", "Failed to load image: $fixedUrl", it.result.throwable)
                                            }
                                        )

                                        // Show loading indicator only while loading
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

                                        // Show error state if image failed to load
                                        if (isError) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.LightGray)
                                                    .padding(16.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Error,
                                                    contentDescription = "Error",
                                                    modifier = Modifier.size(48.dp),
                                                    tint = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    stringResource(R.string.error_loading_image),
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Fallback for empty URLs
                            if (originalUrl.isEmpty()) {
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
                    if (photos.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            photos.indices.forEach { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == pagerState.currentPage) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Photo Viewer Dialog
    if (showPhotoViewer) {
        PhotoViewerDialog(
            photos = photos,
            initialPage = selectedPhotoIndex,
            onDismiss = { showPhotoViewer = false }
        )
    }
    
    // Full Screen Video Dialog
    if (showFullScreenVideo) {
        FullScreenVideoDialog(
            videoUrl = fullScreenVideoUrl,
            onDismiss = { showFullScreenVideo = false }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun UpdateCard(update: UpdateResponse, viewModel: NotificationDetailsViewModel) {
    // State for photo viewer
    var showPhotoViewer by remember { mutableStateOf(false) }
    var selectedPhotoIndex by remember { mutableIntStateOf(0) }
    
    // State for full screen video
    var showFullScreenVideo by remember { mutableStateOf(false) }
    var fullScreenVideoUrl by remember { mutableStateOf("") }
    
    // Combine photo and video URLs
    val allUpdateMediaUrls = update.photoUrls + update.videoUrls

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with date and status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = viewModel.formatDateTime(update.updateTime),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = getStatusColor(update.status).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = getStatusText(update.status),
                        style = MaterialTheme.typography.labelMedium,
                        color = getStatusColor(update.status),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comment - handle nullable comment
            if (!update.comment.isNullOrEmpty()) {
                Text(
                    text = stringResource(R.string.comments),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = update.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Photos and videos if any
            if (allUpdateMediaUrls.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.update_photos)} (${allUpdateMediaUrls.size}):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Display photos and videos in a horizontal pager
                val pagerState = rememberPagerState(pageCount = { allUpdateMediaUrls.size })
                val context = LocalContext.current

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(top = 8.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                                .clickable {
                                    selectedPhotoIndex = page
                                    showPhotoViewer = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            val originalUrl = allUpdateMediaUrls[page]
                            val fixedUrl = RetrofitClient.fixMediaUrl(originalUrl)
                            val mediaType = MediaUtils.getMediaType(fixedUrl)
                            
                            Log.d("UpdateCard", "Loading update media from URL: $fixedUrl, type: $mediaType")

                            when (mediaType) {
                                MediaUtils.MediaType.VIDEO -> {
                                    // Display video player
                                    VideoPlayer(
                                        videoUrl = fixedUrl,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        autoPlay = false,
                                        showControls = true,
                                        onFullScreenClick = {
                                            fullScreenVideoUrl = fixedUrl
                                            showFullScreenVideo = true
                                        }
                                    )
                                }
                                MediaUtils.MediaType.IMAGE, MediaUtils.MediaType.UNKNOWN -> {
                                    // Display image (existing code + unknown types)
                                    // Use remember with the URL as key to reset states when URL changes
                                    var isLoading by remember(fixedUrl) { mutableStateOf(true) }
                                    var isError by remember(fixedUrl) { mutableStateOf(false) }

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
                                    onLoading = {
                                        isLoading = true
                                        isError = false
                                    },
                                    onSuccess = {
                                        isLoading = false
                                        isError = false
                                        Log.d("UpdateCard", "Update image loaded successfully: $fixedUrl")
                                    },
                                    onError = {
                                        isLoading = false
                                        isError = true
                                        Log.e("UpdateCard", "Failed to load update image: $fixedUrl", it.result.throwable)
                                    }
                                )

                                // Show loading indicator only while loading
                                if (isLoading) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(40.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Show error state if image failed to load
                                if (isError) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.LightGray)
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Error,
                                            contentDescription = "Error",
                                            modifier = Modifier.size(32.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            stringResource(R.string.error_loading_image),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                                    }
                                }
                            }

                            // Fallback for empty URLs
                            if (originalUrl.isEmpty()) {
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
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    // Page Indicator for multiple photos/videos
                    if (allUpdateMediaUrls.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            allUpdateMediaUrls.indices.forEach { index ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (index == pagerState.currentPage) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Photo/Video Viewer Dialog
    if (showPhotoViewer) {
        PhotoViewerDialog(
            photos = allUpdateMediaUrls,
            initialPage = selectedPhotoIndex,
            onDismiss = { showPhotoViewer = false }
        )
    }
    
    // Full Screen Video Dialog
    if (showFullScreenVideo) {
        FullScreenVideoDialog(
            videoUrl = fullScreenVideoUrl,
            onDismiss = { showFullScreenVideo = false }
        )
    }
}

// Helper function to get status color
@Composable
private fun getStatusColor(status: String): Color {
    return when (status) {
        "PENDING" -> Color(0xFFFFA726) // Orange
        "IN PROGRESS" -> Color(0xFF42A5F5) // Blue
        "COMPLETED" -> Color(0xFF66BB6A) // Green
        else -> MaterialTheme.colorScheme.onSurface
    }
}