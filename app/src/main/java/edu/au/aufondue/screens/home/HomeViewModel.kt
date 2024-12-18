package edu.au.aufondue.screens.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReportItem(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val timeAgo: String
)

class HomeViewModel : ViewModel() {
    private val _submittedReports = MutableStateFlow<List<ReportItem>>(generateSubmittedReports())
    val submittedReports: StateFlow<List<ReportItem>> = _submittedReports.asStateFlow()

    private val _allReports = MutableStateFlow<List<ReportItem>>(generateAllReports())
    val allReports: StateFlow<List<ReportItem>> = _allReports.asStateFlow()

    private fun generateSubmittedReports(): List<ReportItem> {
        return listOf(
            ReportItem("1", "Leak Leak Leak", "Water leaking in building A", "PENDING", "1 min ago"),
            ReportItem("2", "Boreke broke Brok", "Broken chair in room 301", "IN_PROGRESS", "12 min ago"),
            ReportItem("3", "Flood flood ohhh", "Flooding in basement", "RESOLVED", "1 day ago")
        )
    }

    private fun generateAllReports(): List<ReportItem> {
        return List(10) { index ->
            ReportItem(
                id = (index + 1).toString(),
                title = "Campus Issue #${index + 1}",
                description = "Issue reported in Building ${('A'.code + index).toChar()}",
                status = when (index % 3) {
                    0 -> "PENDING"
                    1 -> "IN_PROGRESS"
                    else -> "RESOLVED"
                },
                timeAgo = when (index % 4) {
                    0 -> "Just updated"
                    1 -> "12 min ago"
                    2 -> "1 hour ago"
                    else -> "1 day ago"
                }
            )
        }
    }
}