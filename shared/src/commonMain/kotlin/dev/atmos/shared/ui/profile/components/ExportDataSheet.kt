package dev.atmos.shared.ui.profile.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.atmos.shared.network.ActivityDto
import dev.atmos.shared.network.ActivityService
import dev.atmos.shared.ui.common.LocalShareLauncher
import dev.atmos.shared.ui.theme.HorizonBlue
import dev.atmos.shared.ui.theme.LocalAtmosColors
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private sealed class ExportState {
    object Loading : ExportState()
    data class Ready(val activities: List<ActivityDto>) : ExportState()
    data class Error(val message: String) : ExportState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportDataSheet(
    activityService: ActivityService,
    onDismiss: () -> Unit,
) {
    val colors = LocalAtmosColors.current
    val shareLauncher = LocalShareLauncher.current
    var fetchAttempt by remember { mutableStateOf(0) }
    var state by remember { mutableStateOf<ExportState>(ExportState.Loading) }

    LaunchedEffect(fetchAttempt) {
        state = ExportState.Loading
        state = activityService.listAllActivities()
            .fold(
                onSuccess = { ExportState.Ready(it) },
                onFailure = { ExportState.Error(it.message ?: "Failed to fetch trips") },
            )
    }

    ModalBottomSheet(
        onDismissRequest = { if (state !is ExportState.Loading) onDismiss() },
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = colors.surface,
        tonalElevation   = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 40.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.FileDownload,
                    contentDescription = null,
                    tint               = HorizonBlue,
                    modifier           = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text       = "Export My Data",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color      = colors.textPrimary,
                )
            }

            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is ExportState.Loading -> {
                    Text(
                        text     = "Fetching your trips…",
                        fontSize = 14.sp,
                        color    = colors.textSecondary,
                    )
                    Spacer(Modifier.height(20.dp))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(36.dp)
                            .align(Alignment.CenterHorizontally),
                        color        = HorizonBlue,
                        strokeWidth  = 3.dp,
                    )
                    Spacer(Modifier.height(16.dp))
                }

                is ExportState.Ready -> {
                    val count = s.activities.size
                    Text(
                        text      = if (count == 0)
                            "No trips found to export."
                        else
                            "$count trip${if (count == 1) "" else "s"} ready to export as a CSV file.",
                        fontSize  = 14.sp,
                        color     = colors.textSecondary,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text      = "Includes date, time, transport mode, distance, and duration for each trip.",
                        fontSize  = 13.sp,
                        color     = colors.textTertiary,
                        lineHeight = 18.sp,
                    )
                    Spacer(Modifier.height(28.dp))
                    if (count > 0) {
                        Button(
                            onClick  = {
                                val csv = buildCsv(s.activities)
                                shareLauncher.share("atmos_trips.csv", csv)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape    = RoundedCornerShape(12.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = HorizonBlue),
                        ) {
                            Icon(Icons.Outlined.FileDownload, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share CSV", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Cancel", color = colors.textSecondary, fontWeight = FontWeight.Medium)
                    }
                }

                is ExportState.Error -> {
                    Text(
                        text      = s.message,
                        fontSize  = 14.sp,
                        color     = colors.textSecondary,
                        lineHeight = 20.sp,
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick  = { fetchAttempt++ },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = HorizonBlue),
                    ) {
                        Text("Retry", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ── CSV builder ───────────────────────────────────────────────────────────────

private fun buildCsv(activities: List<ActivityDto>): String {
    val sb = StringBuilder()
    sb.appendLine("Date,Time,Transport Mode,Distance (km),Duration (min),Source")
    for (activity in activities) {
        val (date, time) = activity.startedAt.toDateAndTime()
        val mode         = activity.transportMode?.toDisplayMode() ?: ""
        val distance     = activity.distanceKm?.let { it.format2dp() } ?: ""
        val duration     = activity.durationMinutes?.toString() ?: ""
        val source       = activity.source?.toDisplaySource() ?: ""
        sb.appendLine("${date.csv()},${time.csv()},${mode.csv()},$distance,$duration,${source.csv()}")
    }
    return sb.toString()
}

// Wraps a value in double-quotes and escapes embedded double-quotes per RFC 4180.
private fun String.csv(): String = "\"${replace("\"", "\"\"")}\""

// Formats a Double to exactly 2 decimal places without String.format (JVM-only).
private fun Double.format2dp(): String {
    val cents = (this * 100 + 0.5).toInt()
    return "${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
}

private fun String.toDateAndTime(): Pair<String, String> {
    return try {
        val ldt = Instant.parse(this).toLocalDateTime(TimeZone.currentSystemDefault())
        val date = "${ldt.year}-${ldt.monthNumber.pad2()}-${ldt.dayOfMonth.pad2()}"
        val time = "${ldt.hour.pad2()}:${ldt.minute.pad2()}"
        date to time
    } catch (_: Exception) {
        "" to ""
    }
}

private fun String.toDisplayMode(): String = when (this) {
    "car"            -> "Car"
    "cab"            -> "Cab"
    "auto_rickshaw"  -> "Auto-rickshaw"
    "bus"            -> "Bus"
    "metro"          -> "Metro"
    "train"          -> "Train"
    "two_wheeler"    -> "Two-wheeler"
    "walking", "walk"     -> "Walking"
    "cycling", "bicycle"  -> "Cycling"
    "flight"         -> "Flight"
    else             -> this
}

private fun String.toDisplaySource(): String = when (this) {
    "manual"      -> "Manual"
    "uber"        -> "Uber"
    "ola"         -> "Ola"
    "rapido"      -> "Rapido"
    "namma_yatri" -> "Namma Yatri"
    "gmail"       -> "Gmail"
    "health_kit"  -> "Health Kit"
    else          -> this
}

private fun Int.pad2() = toString().padStart(2, '0')
