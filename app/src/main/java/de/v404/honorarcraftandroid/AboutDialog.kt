package de.v404.honorarcraftandroid

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    // Handler zum Öffnen von URLs im Systembrowser
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text("Über HonorarCraft") },
        text = {
            Column {
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Datenschutz & Sicherheit",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alle Daten (Kunden, Rechnungen, Honorarsätze) werden ausschließlich lokal auf Ihrem Gerät in einer geschützten Datenbank gespeichert. Es findet keine Übertragung an externe Server statt.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Hinweis",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Die App dient der Verwaltung von Honorarabrechnungen. Für die steuerliche Korrektheit der Angaben ist der Nutzer selbst verantwortlich.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Links zu Impressum und Datenschutz
                TextButton(
                    onClick = { uriHandler.openUri("https://v404.tech/impressum") }
                ) {
                    Text(
                        text = "Impressum",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                TextButton(
                    onClick = { uriHandler.openUri("https://v404.tech/datenschutz-honorarcraft") }
                ) {
                    Text(
                        text = "Datenschutzerklärung",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "v404.tech",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}