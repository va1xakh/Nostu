package com.example.searchnostalgia

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val dao = db.searchDao()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen(dao)
                }
            }
        }
    }
}

@Composable
fun MainScreen(dao: SearchDao) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var statusText by remember { mutableStateOf("Upload your Takeout JSON file to begin") }
    var onThisDayList by remember { mutableStateOf<List<SearchRecord>>(emptyList()) }
    var randomSearch by remember { mutableStateOf<SearchRecord?>(null) }

    fun refreshData() {
        coroutineScope.launch {
            val today = LocalDate.now()
            onThisDayList = dao.getOnThisDay(today.monthValue, today.dayOfMonth, today.year)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            statusText = "Importing records..."
            coroutineScope.launch {
                TakeoutParser.parseAndStore(context, it, dao) { imported ->
                    statusText = "Imported $imported searches..."
                }
                statusText = "Import complete!"
                refreshData()
            }
        }
    }

    LaunchedEffect(Unit) { refreshData() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Search Nostalgia", style = MaterialTheme.typography.headlineMedium)
        Text("100% Offline • Private", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { launcher.launch(arrayOf("application/json")) }) {
                Text("Import Takeout")
            }
            OutlinedButton(onClick = {
                coroutineScope.launch {
                    randomSearch = dao.getRandomSearch()
                }
            }) {
                Text("Surprise Me")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(statusText, style = MaterialTheme.typography.bodyMedium)

        randomSearch?.let { record ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Flashback from ${record.year}:", style = MaterialTheme.typography.labelMedium)
                    Text("\"${record.query}\"", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Look what I searched in ${record.year}: \"${record.query}\" 😂")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Flashback"))
                    }) {
                        Text("Share")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("On This Day in History", style = MaterialTheme.typography.titleMedium)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(onThisDayList) { item ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    ListItem(
                        overlineContent = { Text("${item.year}") },
                        headlineContent = { Text(item.query) }
                    )
                }
            }
        }
    }
}
