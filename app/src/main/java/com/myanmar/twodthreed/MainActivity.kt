package com.myanmar.twodthreed

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private val Navy = Color(0xFF08285F)
private val Blue = Color(0xFF1557B8)
private val Gold = Color(0xFFFFD34D)
private val Pale = Color(0xFFB8D0FF)
private const val PreferencesName = "myanmar_2d_3d_settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Navy)) { ResultsApp() } }
    }
}

@Composable
private fun ResultsApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE) }
    var tab by remember { mutableStateOf("Live") }
    var live by remember { mutableStateOf<JsonObject?>(null) }
    var history2d by remember { mutableStateOf<JsonObject?>(null) }
    var history3d by remember { mutableStateOf<JsonObject?>(null) }
    var latest3d by remember { mutableStateOf<JsonObject?>(null) }
    var history3dPage by remember { mutableIntStateOf(1) }
    var history3dLastPage by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var autoRefresh by remember { mutableStateOf(preferences.getBoolean("auto_refresh", false)) }
    var refreshSeconds by remember { mutableIntStateOf(preferences.getInt("refresh_seconds", 30)) }
    var luckyNumbers by remember { mutableStateOf(emptyList<String>()) }
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

    LaunchedEffect(tab, history3dPage) {
        if (tab in listOf("Live", "2D", "3D")) refresh()
    }
    LaunchedEffect(tab, autoRefresh, refreshSeconds) {
        if (tab == "Live" && autoRefresh) {
            while (true) {
                delay(refreshSeconds * 1_000L)
                refresh()
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Navy, Blue, Color(0xFF174EAB))))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("မြန်မာ 2D / 3D", Modifier.weight(1f), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("ရလဒ်", color = Gold, fontWeight = FontWeight.Bold)
            }
            error?.let { Text(it, color = Color(0xFFFFD5D5), modifier = Modifier.padding(bottom = 8.dp)) }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold)

            when (tab) {
                "Live" -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        val data = live?.getAsJsonObject("data")
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(data?.text("live") ?: "--", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.ExtraBold)
                            Text("လက်ရှိ 2D", color = Pale, fontSize = 18.sp)
                            Spacer(Modifier.height(16.dp))
                            ResultCard("မနက် ၉:၃၀", data?.firstText("result_930", "result_0930", "result_9_30") ?: "--", data?.text("live_set") ?: "--", data?.text("live_val") ?: "--")
                            ResultCard("နေ့လယ် ၁၁:၀၀", data?.firstText("result_1100") ?: "--", data?.text("live_set") ?: "--", data?.text("live_val") ?: "--")
                            ResultCard("နေ့လယ် ၂:၀၀", data?.firstText("result_200", "result_1400", "result_2") ?: "--", data?.text("live_set") ?: "--", data?.text("live_val") ?: "--")
                            ResultCard("ညနေ ၄:၃၀", data?.firstText("result_430", "result_430") ?: "--", data?.text("live_set") ?: "--", data?.text("live_val") ?: "--")
                            Text(data?.text("date") ?: "", color = Pale, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
                "2D" -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("2D မှတ်တမ်း", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                    val rows = history2d?.getAsJsonArray("data")
                    if (rows == null || rows.size() == 0) item { EmptyState(loading) }
                    else items(rows.size()) { index ->
                        val row = rows[index].asJsonObject
                        Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Text(row.text("date") ?: "", color = Gold, fontWeight = FontWeight.Bold)
                                InfoLine("မနက် ၉:၃၀", row.firstText("result_930", "result_0930", "result_9_30") ?: "--")
                                InfoLine("နေ့လယ် ၁၁:၀၀", row.text("result_1100") ?: "--")
                                InfoLine("နေ့လယ် ၁၂:၀၀", row.text("result_1200") ?: "--")
                                InfoLine("နေ့လယ် ၂:၀၀", row.firstText("result_200", "result_1400", "result_2") ?: "--")
                                InfoLine("နေ့လယ် ၃:၀၀", row.text("result_300") ?: "--")
                                InfoLine("ညနေ ၄:၃၀", row.text("result_430") ?: "--")
                            }
                        }
                    }
                }
                "3D" -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                "Lucky" -> LuckyNumberScreen(luckyNumbers = luckyNumbers, onGenerate = {
                    luckyNumbers = List(6) { Random.nextInt(0, 100).toString().padStart(2, '0') }
                }, modifier = Modifier.weight(1f))
                else -> SettingsScreen(
                    modifier = Modifier.weight(1f),
                    autoRefresh = autoRefresh,
                    refreshSeconds = refreshSeconds,
                    onAutoRefreshChange = { enabled -> autoRefresh = enabled; preferences.edit().putBoolean("auto_refresh", enabled).apply() },
                    onRefreshSecondsChange = { seconds -> refreshSeconds = seconds; preferences.edit().putInt("refresh_seconds", seconds).apply() }
                )
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp).background(Color(0xFF071F4F), RoundedCornerShape(28.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Live", "2D", "3D", "Lucky", "Settings").forEach { label ->
                    TextButton(onClick = { tab = label }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 1.dp, vertical = 7.dp)) {
                        Text(when (label) { "Lucky" -> "ကံစမ်း"; "Settings" -> "ဆက်တင်"; else -> label }, color = if (tab == label) Gold else Pale, fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
                    }
                }
                if (tab in listOf("Live", "2D", "3D")) TextButton(onClick = { refresh() }, contentPadding = PaddingValues(horizontal = 3.dp)) { Text("↻", color = Gold, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun LuckyNumberScreen(luckyNumbers: List<String>, onGenerate: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("ကံစမ်းဂဏန်း", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("ကျပန်း 2D ဂဏန်း ၆ လုံး", color = Pale, fontSize = 14.sp)
        if (luckyNumbers.isEmpty()) {
            Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Text("ဂဏန်းထုတ်ရန် ခလုတ်ကို နှိပ်ပါ", color = Pale, modifier = Modifier.padding(24.dp))
            }
        } else {
            luckyNumbers.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { number ->
                        Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
                            Text(number, color = Gold, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(vertical = 20.dp).wrapContentWidth(Alignment.CenterHorizontally))
                        }
                    }
                }
            }
        }
        Button(onClick = onGenerate, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Navy)) { Text("ကံစမ်းဂဏန်းထုတ်မည်", fontWeight = FontWeight.Bold) }
        Text("ဤဂဏန်းများသည် ကျပန်းထုတ်ပေးခြင်းသာဖြစ်ပြီး ရလဒ်အာမခံချက် မရှိပါ။", color = Pale, fontSize = 12.sp)
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    autoRefresh: Boolean,
    refreshSeconds: Int,
    onAutoRefreshChange: (Boolean) -> Unit,
    onRefreshSecondsChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("ဆက်တင်များ", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
        item {
            Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Live ကို အလိုအလျောက် ပြန်လည်ရယူရန်", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Live စာမျက်နှာမှာရှိချိန်သာ အလုပ်လုပ်မည်", color = Pale, fontSize = 12.sp)
                        }
                        Switch(checked = autoRefresh, onCheckedChange = onAutoRefreshChange)
                    }
                    HorizontalDivider(color = Color(0x5577A6E8))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ပြန်လည်ရယူချိန်", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("$refreshSeconds စက္ကန့်တိုင်း", color = Pale, fontSize = 12.sp)
                        }
                        Box {
                            TextButton(onClick = { expanded = true }) { Text("ပြောင်းရန် ▾", color = Gold) }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf(15, 30, 60).forEach { seconds ->
                                    DropdownMenuItem(text = { Text("$seconds စက္ကန့်") }, onClick = { onRefreshSecondsChange(seconds); expanded = false })
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("API ဆက်သွယ်မှု", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("ရလဒ်များရယူရန် backend API URL ကို build configuration မှာ သတ်မှတ်ထားရပါမည်။", color = Pale, fontSize = 13.sp)
                    Text("SETLIVE_API_KEY ကို Android app ထဲမထည့်ပါနှင့်။ Backend server မှာသာ လုံခြုံစွာထားပါ။", color = Pale, fontSize = 12.sp)
                }
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
    Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("SET $set   VALUE $value", color = Pale, fontSize = 12.sp)
            }
            Text(result, color = Gold, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun screenSubtitle(tab: String): String = when (tab) {
    "Live" -> "တိုက်ရိုက်ရလဒ်"
    "2D" -> "2D မှတ်တမ်း"
    "3D" -> "3D မှတ်တမ်း"
    "Lucky" -> "ကျပန်းကံစမ်းဂဏန်း"
    else -> "အက်ပ်ဆက်တင်များ"
}

private fun JsonObject.text(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asString }.getOrNull() }
private fun JsonObject.firstText(vararg keys: String): String? = keys.firstNotNullOfOrNull { text(it) }
private fun JsonObject.int(key: String): Int? = get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() }
