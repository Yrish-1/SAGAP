package com.example.sagap.ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.domain.RatingEngine
import com.example.sagap.domain.clockTime
import com.example.sagap.domain.fmtMbps
import com.example.sagap.ui.components.InfoCard
import com.example.sagap.ui.components.LinkButton
import com.example.sagap.ui.components.PrimaryButton
import com.example.sagap.ui.components.ScreenTitle
import com.example.sagap.ui.components.SectionTitle
import com.example.sagap.ui.components.gradeLabel
import com.example.sagap.ui.components.pressable
import com.example.sagap.ui.theme.*

enum class IssueType { DEAD_ZONE, UNSAFE }

@Composable
fun ReportScreen(
    spots: List<Spot>,
    initialSpotId: Int,
    latestTest: TestResult?,
    onSubmit: (spotId: Int, issue: IssueType, notes: String, attached: TestResult?) -> Unit
) {
    var spotId by remember(initialSpotId) { mutableStateOf(initialSpotId) }
    var issue by remember { mutableStateOf(IssueType.DEAD_ZONE) }
    var notes by remember { mutableStateOf("") }
    var attach by remember { mutableStateOf(true) }
    var menuOpen by remember { mutableStateOf(false) }
    var showDone by remember { mutableStateOf(false) }

    val spot = spots.firstOrNull { it.id == spotId } ?: spots.first()
    val maxChars = 300

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenTitle("Report a problem", "Help SAGAP improve approved study spots.")

        SectionTitle("Approved spot *")
        Box {
            val shape = RoundedCornerShape(18.dp)
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressable(shape) { menuOpen = true }
                    .background(Color.White)
                    .border(1.5.dp, Teal, shape)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = Teal)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(spot.name, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(
                        if (spot.onHold) "Safety hold" else "Campus-approved spot",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null, tint = TextMuted)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                spots.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.name) },
                        onClick = {
                            spotId = s.id
                            menuOpen = false
                        }
                    )
                }
            }
        }
        Text(
            "Choose from the same ${spots.size} campus-managed spots. No custom locations or GPS pin placement.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        SectionTitle("What’s the issue? *")
        IssueOption(
            icon = Icons.Outlined.WifiOff,
            title = "Dead zone / weak Wi-Fi",
            subtitle = "No signal, slow speeds or frequent drops",
            selected = issue == IssueType.DEAD_ZONE,
            onClick = { issue = IssueType.DEAD_ZONE }
        )
        IssueOption(
            icon = Icons.Outlined.Shield,
            title = "Unsafe or inaccessible spot",
            subtitle = "Hazard, restricted access or safety concern",
            selected = issue == IssueType.UNSAFE,
            onClick = { issue = IssueType.UNSAFE }
        )

        SectionTitle("Notes (optional)")
        OutlinedTextField(
            value = notes,
            onValueChange = { if (it.length <= maxChars) notes = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            placeholder = { Text("Describe what happened") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Teal
            )
        )
        Text(
            "${notes.length} / $maxChars characters",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = attach && latestTest != null,
                onCheckedChange = { attach = it },
                enabled = latestTest != null
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text("Attach latest Wi-Fi test", fontWeight = FontWeight.Bold, color = TextDark)
                Text(
                    if (latestTest != null) {
                        val g = RatingEngine.grade(latestTest.downloadMbps, latestTest.pingMs, latestTest.signalDbm)
                        "Sample · ${clockTime(latestTest.timestamp)} · ${gradeLabel(g)} " +
                                "${fmtMbps(latestTest.downloadMbps)} Mbps · ${latestTest.pingMs} ms · ${latestTest.signalDbm} dBm"
                    } else "No test to attach yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }

        InfoCard(
            icon = Icons.Outlined.Shield,
            title = "Safety comes first",
            body = "For immediate danger, contact campus security. Safety-held spots are not recommended " +
                    "until campus staff clear them.",
            container = GradeAmberBg,
            content = GradeAmber
        )

        Text(
            "Your selected spot, notes and attached readings will be sent to the SAGAP team. " +
                    "No GPS location is collected.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted
        )

        PrimaryButton(
            text = "Submit report",
            icon = Icons.AutoMirrored.Filled.Send,
            accent = GradeGreen,
            onClick = {
                onSubmit(spot.id, issue, notes, if (attach) latestTest else null)
                showDone = true
            }
        )
    }

    if (showDone) {
        AlertDialog(
            onDismissRequest = { showDone = false },
            confirmButton = {
                LinkButton("OK", onClick = {
                    showDone = false
                    notes = ""
                })
            },
            title = { Text("Report ready") },
            text = {
                Text(
                    "Demo only: this report isn’t sent anywhere yet. " +
                            "In the final app it will go to the SAGAP team."
                )
            }
        )
    }
}

@Composable
private fun IssueOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .pressable(shape, onClick = onClick)
            .background(if (selected) Color(0xFFE3F1EE) else Color.White)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Teal else Color(0xFFDDE5EA),
                shape = shape
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Teal)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = TextDark)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        RadioButton(selected = selected, onClick = null)
    }
}