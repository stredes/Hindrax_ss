package com.hindrax.ss.features.termux

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxSetupScreen(onBack: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("TERMUX_BRIDGE_SETUP", fontFamily = FontFamily.Monospace) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Green)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF050505),
                    titleContentColor = Color.Green
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color(0xFF050505))
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "FOLLOW_STEPS_TO_ENABLE_ADVANCED_NODE_OPERATIONS:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(24.dp))

            StepItem(
                number = "01",
                text = "Install Termux from F-Droid (Recommended Build)."
            )

            StepItem(
                number = "02",
                text = "Initialize storage and Hindrax Scripts Directory:",
                command = "termux-setup-storage && mkdir -p ~/.hindrax_ss/scripts /storage/emulated/0/Music/HindraxOfflineMusic"
            )

            StepItem(
                number = "03",
                text = "Deploy Discovery Probe Script:",
                command = "echo '#!/bin/bash\\nping -c 4 \"\$1\"' > ~/.hindrax_ss/scripts/network_ping.sh && chmod +x ~/.hindrax_ss/scripts/network_ping.sh"
            )

            StepItem(
                number = "04",
                text = "Deploy Nmap Scan Script:",
                command = "echo '#!/bin/bash\\nif ! command -v nmap >/dev/null 2>&1; then echo \"nmap not installed: pkg install nmap\"; exit 1; fi\\nnmap -sV \"\$1\"' > ~/.hindrax_ss/scripts/nmap_scan.sh && chmod +x ~/.hindrax_ss/scripts/nmap_scan.sh"
            )

            StepItem(
                number = "05",
                text = "Deploy Banner Grab Script:",
                command = "echo '#!/bin/bash\\nHOST=\"\$1\"\\nPORT=\"\${2:-80}\"\\nprintf \"HEAD / HTTP/1.0\\\\r\\\\n\\\\r\\\\n\" | timeout 5 nc \"\$HOST\" \"\$PORT\"' > ~/.hindrax_ss/scripts/banner_grab.sh && chmod +x ~/.hindrax_ss/scripts/banner_grab.sh"
            )

            StepItem(
                number = "06",
                text = "Install spotDL for the offline music bridge:",
                command = "curl -L https://raw.githubusercontent.com/spotDL/spotify-downloader/master/scripts/termux.sh | sh"
            )

            StepItem(
                number = "07",
                text = "Deploy spotDL Download Script:",
                command = """
                    mkdir -p ~/.hindrax_ss/scripts /storage/emulated/0/Music/HindraxOfflineMusic && cat > ~/.hindrax_ss/scripts/spotdl_download.sh <<'EOF'
                    #!/data/data/com.termux/files/usr/bin/bash
                    set -euo pipefail
                    QUERY="${'$'}{1:-}"
                    OUTPUT_DIR="${'$'}{2:-/storage/emulated/0/Music/HindraxOfflineMusic}"
                    if [ -z "${'$'}QUERY" ]; then echo "missing spotDL query"; exit 2; fi
                    mkdir -p "${'$'}OUTPUT_DIR"
                    cd "${'$'}OUTPUT_DIR"
                    spotdl download "${'$'}QUERY" --output "${'$'}OUTPUT_DIR/{artist} - {title}.{output-ext}"
                    EOF
                    chmod +x ~/.hindrax_ss/scripts/spotdl_download.sh
                """.trimIndent()
            )

            StepItem(
                number = "08",
                text = "Grant 'com.termux.permission.RUN_COMMAND' permission in Android Settings."
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            AlertCard()
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StepItem(number: String, text: String, command: String? = null) {
    val clipboardManager = LocalClipboardManager.current
    
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row {
            Text(
                text = "$number.", 
                fontWeight = FontWeight.Bold, 
                color = Color.Green, 
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text, 
                color = Color.White, 
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
        if (command != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0A0A0A))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = command,
                    color = Color.Cyan,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp
                )
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(command)) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.Green, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
fun AlertCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF220000)),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
        shape = MaterialTheme.shapes.extraSmall
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "SYSTEM_SECURITY_NOTICE",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Red,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Hindrax SS will never execute arbitrary commands outside the authorized (~/.hindrax_ss/scripts/) path.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
    }
}
