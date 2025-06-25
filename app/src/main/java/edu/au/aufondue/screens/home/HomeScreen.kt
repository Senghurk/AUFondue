// Location: app/src/main/java/edu/au/aufondue/screens/home/HomeScreen.kt
// COMPLETE UPDATED FILE

package edu.au.aufondue.screens.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.transformations
import coil3.transform.CircleCropTransformation
import edu.au.aufondue.R
import edu.au.aufondue.auth.UserPreferences
import java.util.*

data class QuickStatCard(
    val title: String,
    val count: Int,
    val icon: ImageVector,
    val color: Color
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToReport: () -> Unit,
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    // Get user preferences
    val userPreferences = UserPreferences.getInstance(context)
    val username = userPreferences.getUsername() ?: stringResource(R.string.nav_profile)
    val userEmail = userPreferences.getUserEmail() ?: ""

    // Time-based greeting using string resources - moved outside remember
    val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> stringResource(R.string.good_morning)
        in 12..16 -> stringResource(R.string.good_afternoon)
        in 17..20 -> stringResource(R.string.good_evening)
        else -> stringResource(R.string.good_night)
    }

    // Generate avatar URL (using the same method as ProfileViewModel)
    val avatarUrl = remember {
        val randomSeed = kotlin.random.Random.nextInt(1000)
        val timestamp = System.currentTimeMillis()
        "https://robohash.org/$randomSeed?set=set4&size=200x200&ts=$timestamp"
    }

    // Get theme colors outside remember
    val primaryColor = MaterialTheme.colorScheme.primary

    // Get string resources outside remember block
    val totalReportsLabel = stringResource(R.string.total_reports)
    val pendingLabel = stringResource(R.string.pending)
    val inProgressLabel = stringResource(R.string.in_progress)
    val completedLabel = stringResource(R.string.completed)

    // Calculate quick stats from current data
    val quickStats = remember(state.submittedReports, primaryColor, totalReportsLabel, pendingLabel, inProgressLabel, completedLabel) {
        val reports = state.submittedReports
        val totalReports = reports.size
        val pendingReports = reports.count { it.status == "PENDING" }
        val inProgressReports = reports.count { it.status == "IN PROGRESS" }
        val completedReports = reports.count { it.status == "COMPLETED" }

        listOf(
            QuickStatCard(
                title = totalReportsLabel,
                count = totalReports,
                icon = Icons.Default.Assignment,
                color = primaryColor
            ),
            QuickStatCard(
                title = pendingLabel,
                count = pendingReports,
                icon = Icons.Default.Pending,
                color = Color(0xFFFFA000)
            ),
            QuickStatCard(
                title = inProgressLabel,
                count = inProgressReports,
                icon = Icons.Default.HourglassEmpty,
                color = Color(0xFF2196F3)
            ),
            QuickStatCard(
                title = completedLabel,
                count = completedReports,
                icon = Icons.Default.CheckCircle,
                color = Color(0xFF4CAF50)
            )
        )
    }

    // Handle refresh completion
    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            isRefreshing = false
        }
    }

    // Load submitted reports when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadReports(context, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToReport,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.create_report),
                    tint = Color.White
                )
            }
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshData(context, true)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome Section
                item {
                    WelcomeSection(
                        greeting = greeting,
                        username = username,
                        avatarUrl = avatarUrl,
                        pendingReportsCount = quickStats[1].count // Pending reports count
                    )
                }

                // Quick Stats Cards
                item {
                    QuickStatsSection(stats = quickStats)
                }

                // Reports Section Header
                item {
                    Text(
                        text = stringResource(R.string.your_submitted_reports),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Loading, Error, or Reports Content
                if (state.isLoading && !isRefreshing) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (state.error != null) {
                    item {
                        ErrorSection(
                            error = state.error!!,
                            onRetry = {
                                isRefreshing = true
                                viewModel.refreshData(context, true)
                            }
                        )
                    }
                } else {
                    val reports = state.submittedReports

                    if (reports.isEmpty()) {
                        item {
                            EmptyReportsSection(onNavigateToReport = onNavigateToReport)
                        }
                    } else {
                        items(reports) { report ->
                            ReportCard(report = report)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WelcomeSection(
    greeting: String,
    username: String,
    avatarUrl: String,
    pendingReportsCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "$greeting, $username!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (pendingReportsCount > 0) {
                        stringResource(
                            R.string.pending_reports,
                            pendingReportsCount,
                            if (pendingReportsCount == 1) "" else "s"
                        )
                    } else {
                        stringResource(R.string.no_pending_reports)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }

            // User Avatar
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(avatarUrl)
                    .transformations(listOf(CircleCropTransformation()))
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Avatar",
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun QuickStatsSection(stats: List<QuickStatCard>) {
    Column {
        Text(
            text = stringResource(R.string.quick_overview),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(stats) { stat ->
                QuickStatCard(stat = stat)
            }
        }
    }
}

@Composable
private fun QuickStatCard(stat: QuickStatCard) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = stat.color.copy(alpha = 0.1f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(stat.color.copy(alpha = 0.3f))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = stat.icon,
                    contentDescription = stat.title,
                    tint = stat.color,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = stat.count.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = stat.color
                )
                Text(
                    text = stat.title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ReportCard(report: ReportItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = report.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = report.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = report.status,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (report.status) {
                        "COMPLETED" -> Color(0xFF4CAF50)
                        "IN PROGRESS" -> Color(0xFF2196F3)
                        else -> Color(0xFFFFA000)
                    },
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = report.timeAgo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ErrorSection(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun EmptyReportsSection(onNavigateToReport: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = "No reports",
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Text(
                text = stringResource(R.string.no_reports_yet),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = onNavigateToReport,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.submit_a_report))
            }
        }
    }
}