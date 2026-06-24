package com.retrivedmods.wclient.router.main

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.retrivedmods.wclient.util.LocalSnackbarHostState
import com.retrivedmods.wclient.util.SnackbarHostStateScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialPageContent() {
    SnackbarHostStateScope {
        val snackbarHostState = LocalSnackbarHostState.current
        val context = LocalContext.current

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("WClient Tutorials") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        titleContentColor = contentColorFor(MaterialTheme.colorScheme.surfaceContainer)
                    )
                )
            },
            bottomBar = {
                SnackbarHost(snackbarHostState)
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TutorialCard(
                    title = "📥 How To Setup WClient",
                    description = "Learn how to install and launch WClient on your device.",
                    link = "https://zany-leonie-thunderlinks-0733fa02.koyeb.app/stream?url=https://zany-leonie-thunderlinks-0733fa02.koyeb.app/file?path=/9WDJCX"
                )

                TutorialCard(
                    title = "⚙️ How To Add Config",
                    description = "Step-by-step guide to import and use config files.",
                    link = "https://zany-leonie-thunderlinks-0733fa02.koyeb.app/stream?url=https://zany-leonie-thunderlinks-0733fa02.koyeb.app/file?path=/J7IUWV"
                )

                TutorialCard(
                    title = "🌐 Join Server Without LAN",
                    description = "Fix LAN not showing issue and join servers manually.",
                    link = "https://zany-leonie-thunderlinks-0733fa02.koyeb.app/stream?url=https://zany-leonie-thunderlinks-0733fa02.koyeb.app/file?path=/27A70L"
                )
            }
        }
    }
}

@Composable
private fun TutorialCard(title: String, description: String, link: String) {
    val context = LocalContext.current

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                context.startActivity(intent)
            },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Watch Now ➜",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}
