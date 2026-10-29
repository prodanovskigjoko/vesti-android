package mk.vesti.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import mk.vesti.app.data.outageWhen
import mk.vesti.core.OutageMatch
import mk.vesti.core.SavedAddress

private val QuickLabels = listOf("Дома", "Работа", "Родители")
private val PowerOrange = Color(0xFFE5532D)

/** Saved addresses and the planned power cuts EVN lists for them. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutagesScreen(
    addresses: List<SavedAddress>,
    outages: List<OutageMatch>,
    loaded: Boolean?,
    onAdd: (SavedAddress) -> Unit,
    onRemove: (SavedAddress) -> Unit,
    onBack: () -> Unit,
) {
    var adding by rememberSaveable { mutableStateOf(addresses.isEmpty()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Прекини на струја", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Внеси ги адресите што те интересираат. Секои неколку часа ги проверуваме планираните прекини " +
                        "што ги објавува ЕВН и те известуваме ден однапред. Адресите остануваат само на твојот телефон.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { SectionTitle("Мои адреси") }
            items(addresses, key = { "a:" + it.label + it.place + it.street }) { address ->
                AddressRow(address, onRemove = { onRemove(address) })
            }
            item {
                if (adding) {
                    AddressForm(
                        onSave = { onAdd(it); adding = false },
                        onCancel = if (addresses.isEmpty()) null else ({ adding = false }),
                    )
                } else {
                    OutlinedButton(onClick = { adding = true }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Додај адреса")
                    }
                }
            }
            if (addresses.isNotEmpty()) {
                item { SectionTitle("Претстојни прекини") }
                when {
                    loaded == null -> item { Hint("Ги проверуваме прекините…") }
                    loaded == false && outages.isEmpty() -> item {
                        Hint("Не успеавме да ги вчитаме прекините од ЕВН. Обиди се повторно подоцна.")
                    }
                    outages.isEmpty() -> item { Hint("Нема планирани прекини на твоите адреси. ЕВН ги објавува околу три недели однапред.") }
                    else -> items(outages, key = { "o:" + it.outage.id + it.address.label }) { OutageCard(it) }
                }
                item {
                    Hint(
                        "ЕВН ги наведува улиците слободно, често „дел од улица“, па прекинот може да не ја зафати баш твојата зграда. Извор: ЕВН Македонија.",
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AddressRow(address: SavedAddress, onRemove: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(address.label, style = MaterialTheme.typography.titleSmall)
                Text(
                    listOf(address.street, address.place).filter { it.isNotBlank() }.joinToString(", "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) { Icon(Icons.Outlined.Delete, contentDescription = "Избриши") }
        }
    }
}

@Composable
private fun AddressForm(onSave: (SavedAddress) -> Unit, onCancel: (() -> Unit)?) {
    var label by rememberSaveable { mutableStateOf(QuickLabels.first()) }
    var place by rememberSaveable { mutableStateOf("") }
    var street by rememberSaveable { mutableStateOf("") }
    val words = KeyboardOptions(capitalization = KeyboardCapitalization.Words)

    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickLabels.forEach { q -> FilterChip(selected = label == q, onClick = { label = q }, label = { Text(q) }) }
            }
            OutlinedTextField(label, { label = it }, label = { Text("Назив") }, singleLine = true, keyboardOptions = words, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(
                place, { place = it },
                label = { Text("Место") },
                placeholder = { Text("на пр. Скопје, Битола или Дихово") },
                singleLine = true, keyboardOptions = words, modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                street, { street = it },
                label = { Text("Улица") },
                placeholder = { Text("на пр. Партизанска (не е задолжително во село)") },
                singleLine = true, keyboardOptions = words, modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onCancel != null) TextButton(onClick = onCancel) { Text("Откажи") }
                Spacer(Modifier.width(8.dp))
                Button(
                    enabled = label.isNotBlank() && place.isNotBlank(),
                    onClick = { onSave(SavedAddress(label.trim(), place.trim(), street.trim())) },
                ) { Text("Зачувај") }
            }
        }
    }
}

@Composable
private fun OutageCard(match: OutageMatch) {
    val outage = match.outage
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "⚡ ${match.address.label}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.clip(MaterialTheme.shapes.small).background(PowerOrange).padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(outage.place, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(outageWhen(outage).replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(outage.area, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
