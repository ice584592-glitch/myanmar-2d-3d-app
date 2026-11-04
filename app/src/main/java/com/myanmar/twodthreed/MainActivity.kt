package com.myanmar.twodthreed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import kotlinx.coroutines.launch

private val Navy = Color(0xFF08285F)
private val Blue = Color(0xFF1557B8)
private val Gold = Color(0xFFFFD34D)
private val Pale = Color(0xFFB8D0FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ResultsApp() } }
    }
}

@Composable
private fun ResultsApp() {
    var tab by remember { mutableStateOf("Live") }
    var live by remember { mutableStateOf<JsonObject?>(null) }
    var history2d by remember { mutableStateOf<JsonObject?>(null) }
    var history3d by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            loading = true
            error = null
            try {
                when (tab) {
                    "Live" -> live = ApiClient.api.live2d()
                    "2D" -> history2d = ApiClient.api.history2d()
                    else -> history3d = ApiClient.api.history3d()
                }
            } catch (_: Exception) {
                error = "ဒေတာရယူမရပါ။ API server နဲ့ အင်တာနက်ကို စစ်ဆေးပါ။"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(tab) { refresh() }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, Blue, Color(0xFF174EAB))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("မြန်မာ 2D / 3D", Modifier.weight(1f), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("ရလဒ်", color = Gold, fontWeight = FontWeight.Bold)
            }
            error?.let { Text(it, color = Color(0xFFFFD5D5), modifier = Modifier.padding(bottom = 8.dp)) }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (tab) {
                    "Live" -> item {
                        val data = live?.getAsJsonObject("data")
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(data?.text("live") ?: "--", color = Color.White, fontSize = 108.sp, fontWeight = FontWeight.ExtraBold)
                            Text("လက်ရှိ 2D", color = Pale, fontSize = 18.sp)
                            Spacer(Modifier.height(20.dp))
                            ResultCard("နေ့လယ်", data?.text("result_1100") ?: "--", data?.text("live_set") ?: "--", data?.text("live_val") ?: "--")
                            ResultCard("ညနေ", data?.text("live") ?: "--", data?.text("live_set") ?: "--", data?.text("live_val") ?: "--")
                            Text(data?.text("date") ?: "", color = Pale, modifier = Modifier.padding(12.dp))
                        }
                    }
                    "2D" -> {
                        item { Text("2D မှတ်တမ်း", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                        val rows = history2d?.getAsJsonArray("data")
                        if (rows != null) items(rows.size()) { index ->
                            val row = rows[index].asJsonObject
                            Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(18.dp)) {
                                    Text(row.text("date") ?: "", color = Gold, fontWeight = FontWeight.Bold)
                                    Text("11:00  ${row.text("result_1100") ?: "--"}     12:00  ${row.text("result_1200") ?: "--"}", color = Color.White)
                                    Text("3:00  ${row.text("result_300") ?: "--"}       4:30  ${row.text("result_430") ?: "--"}", color = Color.White)
                                }
                            }
                        }
                    }
                    else -> {
                        item { Text("3D မှတ်တမ်း", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                        val rows = history3d?.getAsJsonArray("data")
                        if (rows != null) items(rows.size()) { index ->
                            val row = rows[index].asJsonObject
                            Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(row.text("draw_date") ?: "", Modifier.weight(1f), color = Pale, fontSize = 17.sp)
                                    Text(row.text("threed_number") ?: "---", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 12.dp).background(Color(0xFF071F4F), RoundedCornerShape(32.dp)).padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                listOf("Live", "2D", "3D").forEach { label ->
                    TextButton(onClick = { tab = label }, modifier = Modifier.weight(1f)) {
                        Text(label, color = if (tab == label) Gold else Pale, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                    }
                }
                TextButton(onClick = { refresh() }) { Text("↻", color = Gold, fontSize = 22.sp) }
            }
        }
    }
}

@Composable
private fun ResultCard(title: String, result: String, set: String, value: String) {
    Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("SET $set   VALUE $value", color = Pale)
            }
            Text(result, color = Gold, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun JsonObject.text(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.asString
