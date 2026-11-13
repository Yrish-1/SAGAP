package com.example.sagap.ui.screens.test

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sagap.data.model.Spot
import com.example.sagap.data.model.TestResult
import com.example.sagap.data.repository.SpotRepository
import com.example.sagap.ui.components.InfoCard
import com.example.sagap.ui.components.PrimaryButton
import com.example.sagap.ui.components.SpotRow
import com.example.sagap.ui.theme.Teal
import com.example.sagap.ui.theme.TextDark
import com.example.sagap.ui.theme.TextMuted

@Composable
fun LocationPickerScreen(
    spots: List<Spot>,
    results: List<TestResult>,
    selectedId: Int,
    onConfirm: (Spot) -> Unit,
    onBack: () -> Unit
) {
    var pendingId by remember { mutableStateOf(selectedId) }
    var query by remember { mutableStateOf("") }

    val filtered = remember(spots, query) {
        spots.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }
    val pending = spots.first { it.id == pendingId }
    val openCount = spots.count { !it.onHold }
    val holdCount = spots.size - openCount

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Search approved spots") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Teal
                    )
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${spots.size} approved spots", fontWeight = FontWeight.Bold, color = TextDark)
                    Text("$openCount open · $holdCount safety hold", color = TextMuted)
                }
            }
            items(filtered, key = { it.id }) { spot ->
                SpotRow(
                    spot = spot,
                    latest = SpotRepository.latest(results, spot.id),
                    selected = spot.id == pendingId,
                    onClick = { pendingId = spot.id }
                )
            }
            item {
                InfoCard(
                    icon = Icons.Outlined.Place,
                    title = "Campus-managed locations only",
                    body = "You cannot add a spot. Safety-hold spots cannot be selected or recommended. No GPS tracking."
                )
            }
        }
        PrimaryButton(
            text = "Use ${pending.name}",
            icon = Icons.Outlined.Check,
            onClick = { onConfirm(pending) },
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
    }
}
