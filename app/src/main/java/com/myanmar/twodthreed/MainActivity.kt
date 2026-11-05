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
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Navy)) { ResultsApp() } }
    }
}

@Composable
private fun ResultsApp() {
    var tab by remember { mutableStateOf("Live") }
    var live by remember { mutableStateOf<JsonObject?>(null) }
    var history2d by remember { mutableStateOf<JsonObject?>(null) }
    var history3d by remember { mutableStateOf<JsonObject?>(null) }
    var latest3d by remember { mutableStateOf<JsonObject?>(null) }
    var history3dPage by remember { mutableIntStateOf(1) }
    var history3dLastPage by remember { mutableIntStateOf(1) }
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
                    "3D" -> {
                        latest3d = ApiClient.api.latest3d()
                        history3d = ApiClient.api.history3d(history3dPage)
                        val meta = history3d?.getAsJsonObject("meta")
                        history3dPage = meta?.int("current_page") ?: history3dPage
                        history3dLastPage = (meta?.int("last_page") ?: 1).coerceAtLeast(1)
                    }
                }
            } catch (_: Exception) {
                error = "ဒေတာရယူမရပါ။ API server နဲ့ အင်တာနက်ကို စစ်ဆေးပြီး ပြန်ကြိုးစားပါ။"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(tab, history3dPage) { refresh() }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, Blue, Color(0xFF174EAB))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("မြန်မာ 2D / 3D", Modifier.weight(1f), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Text("ရလဒ်", color = Gold, fontWeight = FontWeight.Bold)
            }
            error?.let { Text(it, color = Color(0xFFFFD5D5), modifier = Modifier.padding(bottom = 8.dp)) }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)

            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        if (rows == null || rows.size() == 0) item { EmptyState(loading) }
                        else items(rows.size()) { index ->
                            val row = rows[index].asJsonObject
                            Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(row.text("date") ?: "", color = Gold, fontWeight = FontWeight.Bold)
                                    Text("11:00  ${row.text("result_1100") ?: "--"}     12:00  ${row.text("result_1200") ?: "--"}", color = Color.White)
                                    Text("3:00  ${row.text("result_300") ?: "--"}       4:30  ${row.text("result_430") ?: "--"}", color = Color.White)
                                }
                            }
                        }
                    }
                    else -> {
                        item { Text("နောက်ဆုံး 3D ရလဒ်", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                        item {
                            val data = latest3d?.getAsJsonObject("data")
                            val schedule = latest3d?.getAsJsonObject("schedule")
                            Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                    Text(data?.text("threed_number") ?: "---", color = Gold, fontSize = 58.sp, fontWeight = FontWeight.ExtraBold)
                                    InfoLine("ဆုကြီးနံပါတ်", data?.text("first_prize_number") ?: "--")
                                    InfoLine("ထွက်ရက်", data?.text("draw_date") ?: "--")
                                    if (schedule != null) {
                                        HorizontalDivider(color = Color(0x5577A6E8))
                                        Text("နောက်လာမည့် အစီအစဉ်", color = Gold, fontWeight = FontWeight.Bold)
                                        InfoLine("ထွက်မည့်ရက်", schedule.text("next_draw_date") ?: schedule.text("date") ?: "--")
                                        InfoLine("ထွက်ချိန်", "${schedule.text("window_start") ?: "--"} - ${schedule.text("window_end") ?: "--"}")
                                        InfoLine("ရလဒ်ထုတ်ပြန်ချိန်", schedule.text("result_by") ?: "--")
                                        InfoLine("အခြေအနေ", schedule.text("phase") ?: "--")
                                    }
                                }
                            }
                        }
                        val rows = history3d?.getAsJsonArray("data")
                        val total = history3d?.getAsJsonObject("meta")?.int("total")
                        item { Text("3D မှတ်တမ်း${total?.let { " ($it)" } ?: ""}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
                        if (rows == null || rows.size() == 0) item { EmptyState(loading) }
                        else items(rows.size()) { index ->
                            val row = rows[index].asJsonObject
                            Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(row.text("draw_date") ?: "", Modifier.weight(1f), color = Pale, fontSize = 16.sp)
                                    Text(row.text("threed_number") ?: "---", color = Gold, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { history3dPage = (history3dPage - 1).coerceAtLeast(1) }, enabled = history3dPage > 1 && !loading) { Text("‹ အရင်", color = Gold) }
                                Text("စာမျက်နှာ $history3dPage / $history3dLastPage", color = Pale)
                                TextButton(onClick = { history3dPage = (history3dPage + 1).coerceAtMost(history3dLastPage) }, enabled = history3dPage < history3dLastPage && !loading) { Text("နောက် ›", color = Gold) }
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
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Pale, fontSize = 13.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EmptyState(loading: Boolean) {
    Text(if (loading) "ဒေတာရယူနေသည်…" else "ပြသရန် ဒေတာမရှိသေးပါ။", color = Pale, modifier = Modifier.padding(16.dp))
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

private fun JsonObject.text(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asString }.getOrNull() }
private fun JsonObject.int(key: String): Int? = get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() }
