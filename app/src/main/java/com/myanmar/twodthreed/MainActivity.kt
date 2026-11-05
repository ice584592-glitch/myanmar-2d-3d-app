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
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var online by remember { mutableStateOf(isNetworkAvailable(context)) }
    var autoRefresh by remember { mutableStateOf(preferences.getBoolean("auto_refresh", false)) }
    var refreshSeconds by remember { mutableIntStateOf(preferences.getInt("refresh_seconds", 30)) }

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
                    "3D" -> history3d = ApiClient.api.history3d()
                }
            } catch (_: Exception) {
                error = "ဒေတာရယူမရပါ။ ခဏစောင့်ပြီး ပြန်ကြိုးစားပါ။"
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(tab) {
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

    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BackgroundTop, BackgroundBottom)))
    ) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("မြန်မာ 2D / 3D", color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(screenSubtitle(tab), color = TextSecondary, fontSize = 14.sp)
                }
                if (tab != "Settings" && tab != "Chat") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.background(CardColor, CircleShape).padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Box(Modifier.size(8.dp).background(if (online) Color(0xFF57D9A3) else ErrorColor, CircleShape))
                        Spacer(Modifier.width(7.dp))
                        Text(if (online) "ချိတ်ဆက်ထားသည်" else "အော့ဖ်လိုင်း", color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }

            if (error != null && tab in listOf("Live", "2D", "3D")) {
                Surface(color = Color(0xFF512C34), shape = RoundedCornerShape(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                "Live" -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        val data = live?.getAsJsonObject("data")
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(color = CardColor, shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
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
                "2D" -> LazyColumn(
                    modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                "3D" -> LazyColumn(
                    modifier = Modifier.weight(1f), contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { SectionTitle("3D မှတ်တမ်း") }
                    val rows = history3d?.getAsJsonArray("data")
                    if (rows == null || rows.size() == 0) item { EmptyState(loading) }
                    else items(rows.size()) { index ->
                        val row = rows[index].asJsonObject
                        Surface(color = CardColor, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(row.text("draw_date") ?: "", Modifier.weight(1f), color = TextSecondary, fontSize = 15.sp)
                                Text(row.text("threed_number") ?: "---", color = Gold, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                "Chat" -> ChatScreen()
                else -> SettingsScreen(
                    autoRefresh = autoRefresh,
                    refreshSeconds = refreshSeconds,
                    onAutoRefreshChange = { enabled ->
                        autoRefresh = enabled
                        preferences.edit().putBoolean("auto_refresh", enabled).apply()
                    },
                    onRefreshSecondsChange = { seconds ->
                        refreshSeconds = seconds
                        preferences.edit().putInt("refresh_seconds", seconds).apply()
                    }
                )
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 10.dp, top = 6.dp)
                    .background(Color(0xFF091D38), RoundedCornerShape(24.dp)).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("Live", "2D", "3D", "Chat", "Settings").forEach { label ->
                    val selected = tab == label
                    TextButton(
                        onClick = { tab = label }, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = if (selected) Gold else Color.Transparent,
                            contentColor = if (selected) BackgroundTop else TextSecondary
                        )
                    ) {
                        Text(
                            text = when (label) { "Settings" -> "ဆက်တင်"; "Chat" -> "Chat"; else -> label },
                            fontWeight = FontWeight.Bold, fontSize = 11.sp, maxLines = 1
                        )
                    }
                }
                if (tab in listOf("Live", "2D", "3D")) {
                    TextButton(onClick = { refresh() }, contentPadding = PaddingValues(horizontal = 5.dp)) {
                        Text("↻", color = Gold, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatScreen() {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(color = CardColor, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Live Chat", color = TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("အသုံးပြုသူများ အချင်းချင်း တိုက်ရိုက်ဆွေးနွေးရန်", color = TextSecondary)
                HorizontalDivider(color = Color(0x334A6284))
                Text("Chat server မချိတ်ဆက်ရသေးပါ", color = Gold, fontWeight = FontWeight.SemiBold)
                Text(
                    "ဒီနေရာမှာ Live Chat UI ကို ပြင်ဆင်ထားပါတယ်။ တကယ်တမ်း စာပို့/လက်ခံနိုင်ဖို့ chat backend သို့မဟုတ် WebSocket ဝန်ဆောင်မှု ချိတ်ဆက်ဖို့ လိုပါတယ်။",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            }
        }
        Surface(color = Color(0xFF091D38), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Chat server ချိတ်ဆက်ပြီးနောက် စာရေးနိုင်ပါမည်", color = TextSecondary, modifier = Modifier.weight(1f), fontSize = 13.sp)
                Text("ပို့ရန်", color = Color(0xFF7890B2), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    autoRefresh: Boolean,
    refreshSeconds: Int,
    onAutoRefreshChange: (Boolean) -> Unit,
    onRefreshSecondsChange: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
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
                            Text("${refreshSeconds} စက္ကန့်တိုင်း", color = TextSecondary, fontSize = 12.sp)
                        }
                        Box {
                            TextButton(onClick = { expanded = true }) { Text("ပြောင်းရန် ▾", color = Gold) }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                listOf(15, 30, 60).forEach { seconds ->
                                    DropdownMenuItem(
                                        text = { Text("${seconds} စက္ကန့်") },
                                        onClick = {
                                            onRefreshSecondsChange(seconds)
                                            expanded = false
                                        }
                                    )
                                }
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
                    Text("ရလဒ်ဒေတာကို သတ်မှတ်ထားသော API server မှ ရယူပါသည်။", color = TextSecondary, fontSize = 13.sp)
                    Text("အလိုအလျောက်ပြန်လည်ရယူမှုနှင့် ကြားကာလကို ဒီစက်တွင် သိမ်းဆည်းထားသည်။", color = TextSecondary, fontSize = 13.sp)
                }
            }
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

@Composable
private fun SectionTitle(title: String) {
    Text(title, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun EmptyState(loading: Boolean) {
    Surface(color = CardColor, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            if (loading) "ဒေတာရယူနေသည်…" else "ပြသရန် ဒေတာမရှိသေးပါ။ ပြန်လည်ရယူရန် ↻ ကိုနှိပ်ပါ။",
            color = TextSecondary,
            modifier = Modifier.padding(20.dp)
        )
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

private fun JsonObject.text(key: String): String? =
    get(key)?.takeUnless { it.isJsonNull }?.let { element -> runCatching { element.asString }.getOrNull() }

private fun isNetworkAvailable(context: Context): Boolean {
    val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = manager.activeNetwork ?: return false
    val capabilities = manager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
