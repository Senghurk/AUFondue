package edu.au.unimend.aufondue.screens.reports

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.au.unimend.aufondue.screens.home.HomeViewModel
import edu.au.unimend.aufondue.screens.home.ReportCard
import edu.au.unimend.aufondue.R

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsListScreen(
    statusFilter: String?,
    onNavigateBack: () -> Unit,
    onNavigateToIssueDetails: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    // Filter reports based on the status
    val filteredReports = remember(state.submittedReports, statusFilter) {
        when (statusFilter) {
            "ALL" -> state.submittedReports
            "PENDING" -> state.submittedReports.filter { it.status == "PENDING" }
            "IN PROGRESS" -> state.submittedReports.filter { it.status == "IN PROGRESS" }
            "COMPLETED" -> state.submittedReports.filter { it.status == "COMPLETED" }
            else -> state.submittedReports
        }
    }

    // Get the title based on the filter
    val screenTitle = when (statusFilter) {
        "ALL" -> stringResource(R.string.all_reports)
        "PENDING" -> stringResource(R.string.pending_reports)
        "IN PROGRESS" -> stringResource(R.string.in_progress_reports)
        "COMPLETED" -> stringResource(R.string.completed_reports)
        else -> stringResource(R.string.all_reports)
    }

    LaunchedEffect(Unit) {
        viewModel.loadReports(context, true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = screenTitle,
                        fontWeight = FontWeight.Medium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Button(onClick = { viewModel.loadReports(context, true) }) {
                            Text(stringResource(R.string.retry))
                        }
                    }
                }
            }
            
            filteredReports.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = when (statusFilter) {
                                "PENDING" -> stringResource(R.string.no_pending_reports)
                                "IN PROGRESS" -> stringResource(R.string.no_in_progress_reports)
                                "COMPLETED" -> stringResource(R.string.no_completed_reports)
                                else -> stringResource(R.string.no_reports)
                            },
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(filteredReports) { report ->
                        ReportCard(
                            report = report,
                            onReportClick = { issueId ->
                                onNavigateToIssueDetails(issueId)
                            }
                        )
                    }
                }
            }
        }
    }
}