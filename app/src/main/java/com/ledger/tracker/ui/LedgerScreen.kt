package com.ledger.tracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ledger.tracker.data.EntryType
import com.ledger.tracker.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LedgerScreen(
    viewModel: LedgerViewModel,
    notificationAccessEnabled: Boolean,
    onOpenNotificationSettings: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var pasteText by remember { mutableStateOf("") }
    var draft by remember { mutableStateOf<TransactionEntity?>(null) }

    Scaffold(topBar = {
        TopAppBar(title = { Text("Ledger") })
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            if (!notificationAccessEnabled) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.NotificationsActive, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Notification access is off", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "Turn this on so payment notifications are captured automatically.",
                                modifier = Modifier.padding(top = 4.dp)
                            )
                            Button(onClick = onOpenNotificationSettings, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Open settings")
                            }
                        }
                    }
                }
            }

            item {
                SummaryRow(state.totals)
            }

            item {
                Card {
                    Column(Modifier.padding(16.dp)) {
                        Text("Paste a notification", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = pasteText,
                            onValueChange = { pasteText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            placeholder = { Text("e.g. Rs.450 debited to Swiggy on 12-Jul-26") },
                            minLines = 2
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = {
                                draft = TransactionEntity(amount = 0.0, type = EntryType.DEBIT, party = "")
                            }) { Text("Add manually") }
                            Spacer(Modifier.width(4.dp))
                            Button(
                                enabled = pasteText.isNotBlank(),
                                onClick = {
                                    draft = viewModel.parseDraft(pasteText)
                                    pasteText = ""
                                }
                            ) { Text("Read entry") }
                        }
                    }
                }
            }

            draft?.let { d ->
                item {
                    DraftCard(
                        draft = d,
                        onChange = { draft = it },
                        onDiscard = { draft = null },
                        onSave = {
                            viewModel.save(it)
                            draft = null
                        }
                    )
                }
            }

            if (state.unreviewed.isNotEmpty()) {
                item {
                    Text(
                        "Auto-captured — needs a quick check (${state.unreviewed.size})",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(state.unreviewed, key = { it.id }) { entry ->
                    DraftCard(
                        draft = entry,
                        onChange = { },
                        onDiscard = { viewModel.delete(entry) },
                        onSave = { viewModel.save(it) }
                    )
                }
            }

            item {
                Text(
                    "All entries (${state.entries.size})",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(state.entries, key = { it.id }) { entry ->
                EntryRow(entry, onDelete = { viewModel.delete(entry) })
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SummaryRow(totals: Totals) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard("Balance", totals.balance, Modifier.weight(1f))
            SummaryCard("Net dues", totals.netDue, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            SummaryCard("Spent", -totals.debit, Modifier.weight(1f))
            SummaryCard("Received", totals.credit, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryCard(label: String, value: Double, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                (if (value < 0) "-" else "") + "₹" + "%,.2f".format(Math.abs(value)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftCard(
    draft: TransactionEntity,
    onChange: (TransactionEntity) -> Unit,
    onDiscard: () -> Unit,
    onSave: (TransactionEntity) -> Unit
) {
    var amount by remember(draft.id) { mutableStateOf(if (draft.amount == 0.0) "" else draft.amount.toString()) }
    var type by remember(draft.id) { mutableStateOf(draft.type) }
    var party by remember(draft.id) { mutableStateOf(draft.party) }
    var note by remember(draft.id) { mutableStateOf(draft.note) }
    var expanded by remember { mutableStateOf(false) }

    Card {
        Column(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                OutlinedTextField(
                    value = typeLabel(type),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    EntryType.values().forEach { t ->
                        DropdownMenuItem(text = { Text(typeLabel(t)) }, onClick = {
                            type = t; expanded = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = party,
                onValueChange = { party = it },
                label = { Text("Who's this with?") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDiscard) { Text("Discard") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    onSave(
                        draft.copy(
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            type = type,
                            party = party.ifBlank { "Unknown" },
                            note = note
                        )
                    )
                }) { Text("Save") }
            }
        }
    }
}

@Composable
private fun EntryRow(entry: TransactionEntity, onDelete: () -> Unit) {
    val dateFmt = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    val sign = if (entry.type == EntryType.DEBIT || entry.type == EntryType.DUE_OWE) "-" else "+"

    Card {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(entry.party, fontWeight = FontWeight.Medium)
                Text(
                    "${typeLabel(entry.type)} · ${dateFmt.format(Date(entry.timestamp))}" +
                        if (entry.note.isNotBlank()) " · ${entry.note}" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                "$sign₹" + "%,.2f".format(entry.amount),
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
            }
        }
    }
}

private fun typeLabel(type: EntryType): String = when (type) {
    EntryType.DEBIT -> "Spent"
    EntryType.CREDIT -> "Received"
    EntryType.DUE_OWE -> "You owe"
    EntryType.DUE_OWED -> "Owed to you"
}
