package com.myanmar.twodthreed

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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

private val BackgroundTop = Color(0xFF07152B)
private val BackgroundBottom = Color(0xFF102E5D)
private val Gold = Color(0xFFFFD166)
private val TextPrimary = Color(0xFFF7F9FF)
private val TextSecondary = Color(0xFFB7C7E4)
private val CardColor = Color(0xFF132D50)
private val ErrorColor = Color(0xFFFFB4AB)
private const val PreferencesName = "myanmar_2d_3d_settings"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = BackgroundTop)) {
                ResultsApp()
            }
        }
    }
}

@Composable
private fun ResultsApp() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PreferencesName, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("Live") }
    var live by remember { mutableStateOf<JsonObject?>(null) }
    var history2d by remember { mutableStateOf<JsonObject?>(null) }
    var history3d by remember { mutableStateOf<JsonObject?>(null) }
    var latest3d by remember { mutableStateOf<JsonObject?>(null) }
    var history3dPage by remember { mutableIntStateOf(1) }
    var history3dLastPage by remember { mutableIntStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var online by remember { mutableStateOf(isNetworkAvailable(context)) }
    var autoRefresh by remember { mutableStateOf(preferences.getBoolean("auto_refresh", false)) }
    var refreshSeconds by remember { mutableIntStateOf(preferences.getInt("refresh_seconds", 30)) }
    var username by remember { mutableStateOf(preferences.getString("chat_username", "ဧည့်သည်") ?: "ဧည့်သည်") }

    fun refresh() {
        online = isNetworkAvailable(context)
        if (!online) {
            error = "အင်တာနက်ချိတ်ဆက်မှု မရှိပါ။ ပြန်စစ်ဆေးပြီး ထပ်ကြိုးစားပါ။"
            return
        }
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
                error = "ဒေတာရယူမရပါ။ ခဏစောင့်ပြီး ပြန်ကြိုးစားပါ။"
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

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom)))) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("မြန်မာ 2D / 3D", color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(screenSubtitle(tab), color = TextSecondary, fontSize = 14.sp)
                }
                if (tab in listOf("Live", "2D", "3D")) {
                    Row(Modifier.background(CardColor, CircleShape).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(if (online) Color(0xFF57D9A3) else ErrorColor, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text(if (online) "ချိတ်ဆက်ထားသည်" else "အော့ဖ်လိုင်း", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
            if (error != null && tab in listOf("Live", "2D", "3D")) {
                Surface(color = Color(0xFF512C34), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error.orEmpty(), color = ErrorColor, modifier = Modifier.weight(1f), fontSize = 13.sp)
                        TextButton(onClick = { refresh() }) { Text("ပြန်စမ်း", color = Gold) }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            if (loading && tab in listOf("Live", "2D", "3D")) {
                LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = Color(0x333D5272))
                Spacer(Modifier.height(12.dp))
            }

            when (tab) {
                "Live" -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        val data = live?.getAsJsonObject("data")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(color = CardColor, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("လက်ရှိ 2D", color = TextSecondary, fontSize = 16.sp)
                                    Text(data?.text("live") ?: "--", color = Gold, fontSize = 82.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 90.sp)
                                    Text(data?.text("date") ?: "ရလဒ်အသစ်ကို စောင့်နေသည်", color = TextSecondary, fontSize = 13.sp)
                                }
                            }
                            ResultCard("နေ့လယ်", data?.text("result_1100") ?: "--", data?.text("live_set") ?: "--", data?.text("live_val") ?: "--")
                            ResultCard("ညနေ", data?.text("live") ?: "--", data?.text("live_set") ?: "--", data?.text("live_val") ?: "--")
                        }
                    }
                }
                "2D" -> LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { SectionTitle("2D မှတ်တမ်း") }
                    val rows = history2d?.getAsJsonArray("data")
                    if (rows == null || rows.size() == 0) item { EmptyState(loading) }
                    else items(rows.size()) { index ->
                        val row = rows[index].asJsonObject
                        Surface(color = CardColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                Text(row.text("date") ?: "", color = Gold, fontWeight = FontWeight.Bold)
                                HorizontalDivider(color = Color(0x334A6284))
                                Text("11:00   ${row.text("result_1100") ?: "--"}       12:00   ${row.text("result_1200") ?: "--"}", color = TextPrimary)
                                Text("3:00     ${row.text("result_300") ?: "--"}       4:30     ${row.text("result_430") ?: "--"}", color = TextPrimary)
                            }
                        }
                    }
                }
                "3D" -> ThreeDResultsScreen(
                    modifier = Modifier.weight(1f),
                    latest = latest3d,
                    history = history3d,
                    loading = loading,
                    currentPage = history3dPage,
                    lastPage = history3dLastPage,
                    onPageChange = { page -> history3dPage = page.coerceIn(1, history3dLastPage) }
                )
                "Chat" -> ChatScreen(
                    modifier = Modifier.weight(1f),
                    username = username,
                    onUsernameChange = { name -> username = name; preferences.edit().putString("chat_username", name).apply() }
                )
                else -> SettingsScreen(
                    modifier = Modifier.weight(1f),
                    autoRefresh = autoRefresh,
                    refreshSeconds = refreshSeconds,
                    onAutoRefreshChange = { enabled -> autoRefresh = enabled; preferences.edit().putBoolean("auto_refresh", enabled).apply() },
                    onRefreshSecondsChange = { seconds -> refreshSeconds = seconds; preferences.edit().putInt("refresh_seconds", seconds).apply() }
                )
            }

            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp, top = 6.dp).background(Color(0xFF091D38), RoundedCornerShape(24.dp)).padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Live", "2D", "3D", "Chat", "Settings").forEach { label ->
                    val selected = tab == label
                    TextButton(onClick = { tab = label }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(18.dp), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp), colors = ButtonDefaults.textButtonColors(containerColor = if (selected) Gold else Color.Transparent, contentColor = if (selected) BackgroundTop else TextSecondary)) {
                        Text(when (label) { "Settings" -> "ဆက်တင်"; "Chat" -> "Chat"; else -> label }, fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1)
                    }
                }
                if (tab in listOf("Live", "2D", "3D")) TextButton(onClick = { refresh() }, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("↻", color = Gold, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun ThreeDResultsScreen(
    modifier: Modifier,
    latest: JsonObject?,
    history: JsonObject?,
    loading: Boolean,
    currentPage: Int,
    lastPage: Int,
    onPageChange: (Int) -> Unit
) {
    val latestData = latest?.getAsJsonObject("data")
    val schedule = latest?.getAsJsonObject("schedule")
    val rows = history?.getAsJsonArray("data")
    val meta = history?.getAsJsonObject("meta")
    val total = meta?.int("total")

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("နောက်ဆုံး 3D ရလဒ်") }
        item {
            Surface(color = CardColor, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("ထွက်ဂဏန်း", color = TextSecondary, fontSize = 14.sp)
                    Text(latestData?.text("threed_number") ?: "---", color = Gold, fontSize = 54.sp, fontWeight = FontWeight.ExtraBold)
                    InfoLine("ဆုကြီးနံပါတ်", latestData?.text("first_prize_number") ?: "--")
                    InfoLine("ထွက်ရက်", latestData?.text("draw_date") ?: "--")
                    if (schedule != null) {
                        HorizontalDivider(color = Color(0x334A6284))
                        Text("နောက်လာမည့် အစီအစဉ်", color = Gold, fontWeight = FontWeight.Bold)
                        InfoLine("ထွက်မည့်ရက်", schedule.text("next_draw_date") ?: schedule.text("date") ?: "--")
                        InfoLine("ထွက်ချိန်", "${schedule.text("window_start") ?: "--"} - ${schedule.text("window_end") ?: "--"} (${schedule.text("timezone") ?: "Asia/Yangon"})")
                        InfoLine("ရလဒ်ထုတ်ပြန်ချိန်", schedule.text("result_by") ?: "--")
                        InfoLine("အခြေအနေ", schedule.text("phase") ?: "--")
                    }
                }
            }
        }
        item { SectionTitle("3D မှတ်တမ်း${total?.let { " ($it)" } ?: ""}") }
        if (rows == null || rows.size() == 0) {
            item { EmptyState(loading) }
        } else {
            items(rows.size()) { index ->
                val row = rows[index].asJsonObject
                Surface(color = CardColor, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(row.text("draw_date") ?: "ရက်စွဲမရှိ", color = TextSecondary, fontSize = 14.sp)
                            Text("3D ထွက်ဂဏန်း", color = TextSecondary, fontSize = 12.sp)
                        }
                        Text(row.text("threed_number") ?: "---", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onPageChange(currentPage - 1) }, enabled = currentPage > 1 && !loading) { Text("‹ အရင်", color = if (currentPage > 1) Gold else TextSecondary) }
                Text("စာမျက်နှာ $currentPage / $lastPage", color = TextSecondary, fontSize = 13.sp)
                TextButton(onClick = { onPageChange(currentPage + 1) }, enabled = currentPage < lastPage && !loading) { Text("နောက် ›", color = if (currentPage < lastPage) Gold else TextSecondary) }
            }
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ChatScreen(modifier: Modifier, username: String, onUsernameChange: (String) -> Unit) {
    val chat = remember { ChatClient(BuildConfig.CHAT_SERVER_URL) }
    var messages by remember { mutableStateOf(emptyList<ChatMessage>()) }
    var connected by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    DisposableEffect(chat) {
        chat.connect(onConnected = { connected = it }, onHistory = { messages = it }, onMessage = { message -> messages = (messages + message).takeLast(100) }, onError = { status = it })
        onDispose { chat.close() }
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(color = CardColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).background(if (connected) Color(0xFF57D9A3) else ErrorColor, CircleShape))
                Spacer(Modifier.width(8.dp))
                Text(if (connected) "Chat ချိတ်ဆက်ထားသည်" else "Chat server မချိတ်ဆက်ရသေးပါ", color = if (connected) TextPrimary else Gold, fontSize = 13.sp)
            }
        }
        status?.let { Text(it, color = ErrorColor, fontSize = 12.sp) }
        OutlinedTextField(value = username, onValueChange = { onUsernameChange(it.take(24)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("သင့်အမည်") })
        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0x55091D38), RoundedCornerShape(18.dp)).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (messages.isEmpty()) item { Text(if (BuildConfig.CHAT_SERVER_URL.isBlank()) "Chat server URL ကို သတ်မှတ်ပါ။" else "Chat မက်ဆေ့ချ် မရှိသေးပါ။", color = TextSecondary, modifier = Modifier.padding(10.dp), fontSize = 13.sp) }
            items(messages, key = { it.id }) { message ->
                Surface(color = CardColor, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                        Text(message.username, color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(message.text, color = TextPrimary, fontSize = 14.sp)
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = draft, onValueChange = { draft = it.take(500) }, modifier = Modifier.weight(1f), placeholder = { Text("စာရေးပါ…") }, maxLines = 3)
            Button(onClick = { val text = draft.trim(); if (text.isNotEmpty() && connected) { chat.send(username.ifBlank { "ဧည့်သည်" }, text); draft = "" } else status = "Chat server မချိတ်ဆက်ထားပါ။" }, enabled = connected && draft.isNotBlank()) { Text("ပို့မည်") }
        }
    }
}

@Composable
private fun SettingsScreen(modifier: Modifier, autoRefresh: Boolean, refreshSeconds: Int, onAutoRefreshChange: (Boolean) -> Unit, onRefreshSecondsChange: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("ဆက်တင်များ") }
        item {
            Surface(color = CardColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Live အလိုအလျောက် ပြန်လည်ရယူရန်", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("Live စာမျက်နှာဖွင့်ထားချိန်တွင်သာ အလုပ်လုပ်မည်", color = TextSecondary, fontSize = 12.sp)
                        }
                        Switch(checked = autoRefresh, onCheckedChange = onAutoRefreshChange)
                    }
                    HorizontalDivider(color = Color(0x334A6284))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ပြန်လည်ရယူချိန်", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                            Text("$refreshSeconds စက္ကန့်တိုင်း", color = TextSecondary, fontSize = 12.sp)
                        }
                        Box {
                            TextButton(onClick = { expanded = true }) { Text("ပြောင်းရန် ▾", color = Gold) }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf(15, 30, 60).forEach { seconds -> DropdownMenuItem(text = { Text("$seconds စက္ကန့်") }, onClick = { onRefreshSecondsChange(seconds); expanded = false }) }
                            }
                        }
                    }
                }
            }
        }
        item {
            Surface(color = CardColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("API အခြေအနေ", color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("3D နောက်ဆုံးရလဒ်နှင့် မှတ်တမ်းကို API မှ ရယူပါသည်။", color = TextSecondary, fontSize = 13.sp)
                    Text("Live Chat အသုံးပြုရန် CHAT_SERVER_URL ကို သတ်မှတ်ပါ။", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun EmptyState(loading: Boolean) {
    Surface(color = CardColor, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Text(if (loading) "ဒေတာရယူနေသည်…" else "ပြသရန် ဒေတာမရှိသေးပါ။", color = TextSecondary, modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun ResultCard(title: String, result: String, set: String, value: String) {
    Surface(color = CardColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("SET  $set     VALUE  $value", color = TextSecondary, fontSize = 12.sp)
            }
            Text(result, color = Gold, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

private fun screenSubtitle(tab: String): String = when (tab) {
    "Live" -> "တိုက်ရိုက်ရလဒ်"
    "2D" -> "2D မှတ်တမ်း"
    "3D" -> "3D မှတ်တမ်း"
    "Chat" -> "တိုက်ရိုက်စကားပြောခန်း"
    else -> "အက်ပ်ဆက်တင်များ"
}

private fun JsonObject.text(key: String): String? = get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asString }.getOrNull() }
private fun JsonObject.int(key: String): Int? = get(key)?.takeUnless { it.isJsonNull }?.let { runCatching { it.asInt }.getOrNull() }

private fun isNetworkAvailable(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
