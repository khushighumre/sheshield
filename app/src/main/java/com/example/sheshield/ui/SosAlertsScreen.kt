package com.example.sheshield.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Directions
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.sheshield.data.SosAlertRecord
import com.example.sheshield.data.SosAlertStore
import com.example.sheshield.ui.theme.*
import org.osmdroid.util.GeoPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SosAlertsScreen(nav: NavController) {
    val context = LocalContext.current
    val store = remember { SosAlertStore(context) }
    var alerts by remember { mutableStateOf(store.loadAlerts()) }

    // Refresh when screen is displayed
    LaunchedEffect(Unit) {
        alerts = store.loadAlerts()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("SOS Alerts", style = MaterialTheme.typography.headlineMedium, color = SheBrown)
        Text(
            "Emergency alerts received via offline P2P from nearby devices.",
            style = MaterialTheme.typography.bodyMedium,
            color = SheBrownSoft
        )

        if (alerts.isEmpty()) {
            Spacer(Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Outlined.Warning, contentDescription = null, tint = SheBrownSoft, modifier = Modifier.size(48.dp))
                Text("No SOS alerts received yet", style = MaterialTheme.typography.titleMedium, color = SheBrown)
                Text("Incoming emergency alerts will appear here automatically.", style = MaterialTheme.typography.bodySmall, color = SheBrownSoft)
            }
        } else {
            alerts.forEach { alert ->
                AlertCard(alert) {
                    nav.navigate("sos_detail/${alert.incidentId}")
                }
            }
        }
    }
}

@Composable
private fun AlertCard(alert: SosAlertRecord, onClick: () -> Unit) {
    val sdf = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm:ss a", Locale.getDefault()) }
    val timeStr = sdf.format(Date(alert.receivedAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sender: ${alert.senderId}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SheBrown
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (alert.trustState) {
                        "VERIFIED_TRUSTED" -> if (alert.isSafe) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        "SIGNED_UNVERIFIED" -> Color(0xFFFFF3E0)
                        else -> Color(0xFFECEFF1)
                    }
                ) {
                    Text(
                        text = when (alert.trustState) {
                            "VERIFIED_TRUSTED" -> if (alert.isSafe) "RESOLVED (Safe)" else "ACTIVE SOS"
                            "SIGNED_UNVERIFIED" -> "⚠️ UNVERIFIED"
                            else -> "ℹ️ LEGACY"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (alert.trustState) {
                            "VERIFIED_TRUSTED" -> if (alert.isSafe) Color(0xFF2E7D32) else Color(0xFFC62828)
                            "SIGNED_UNVERIFIED" -> Color(0xFFEF6C00)
                            else -> Color(0xFF546E7A)
                        },
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = alert.messageText,
                style = MaterialTheme.typography.bodyMedium,
                color = SheBrown
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Received: $timeStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = SheBrownSoft
                )
                Text(
                    text = if (alert.lat != null && alert.lng != null) "📍 Has Location" else "📍 Location unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (alert.lat != null) SheAccent else SheBrownSoft
                )
            }
        }
    }
}

@Composable
fun SosDetailScreen(incidentId: String?, nav: NavController) {
    val context = LocalContext.current
    val store = remember { SosAlertStore(context) }
    val alert = remember(incidentId) { incidentId?.let { store.getAlert(it) } }

    if (alert == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Alert not found", style = MaterialTheme.typography.titleMedium, color = SheBrown)
        }
        return
    }

    val sdf = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm:ss a", Locale.getDefault()) }
    val timeStr = sdf.format(Date(alert.receivedAt))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SOS Alert Details", style = MaterialTheme.typography.headlineMedium, color = SheBrown)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (alert.isSafe) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ) {
                Text(
                    text = if (alert.isSafe) "RESOLVED (Safe)" else "ACTIVE SOS",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (alert.isSafe) Color(0xFF2E7D32) else Color(0xFFC62828),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sender ID: ${alert.senderId}", style = MaterialTheme.typography.titleSmall, color = SheBrown)
                Text("Incident ID: ${alert.incidentId}", style = MaterialTheme.typography.bodySmall, color = SheBrownSoft)
                Text("Received At: $timeStr", style = MaterialTheme.typography.bodySmall, color = SheBrownSoft)
                Divider(color = SheBorder, thickness = 1.dp)
                Text(alert.messageText, style = MaterialTheme.typography.bodyLarge, color = SheBrown)
            }
        }

        Text("Sender Location", style = MaterialTheme.typography.titleLarge, color = SheBrown)

        if (alert.lat != null && alert.lng != null) {
            val geoPoint = GeoPoint(alert.lat, alert.lng)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                OsmMap(
                    modifier = Modifier.fillMaxSize(),
                    userLocation = geoPoint
                )
            }

            Button(
                onClick = {
                    val uri = Uri.parse("geo:${alert.lat},${alert.lng}?q=${alert.lat},${alert.lng}(Sender SOS)")
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Fallback to browser web maps
                        val webUri = Uri.parse("https://maps.google.com/?q=${alert.lat},${alert.lng}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SheAccent)
            ) {
                Icon(Icons.Outlined.Directions, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Open Directions in Map App", color = Color.White, fontSize = 16.sp)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Location unavailable", style = MaterialTheme.typography.bodyMedium, color = SheBrownSoft)
                }
            }
        }
    }
}
