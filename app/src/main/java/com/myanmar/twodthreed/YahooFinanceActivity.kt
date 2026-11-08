package com.myanmar.twodthreed

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val YahooNavy = Color(0xFF08285F)
private val YahooBlue = Color(0xFF1557B8)
private val YahooGold = Color(0xFFFFD34D)
private val YahooPale = Color(0xFFB8D0FF)

class YahooFinanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { YahooFinanceScreen(onOpenResults = { startActivity(Intent(this, MainActivity::class.java)) }) } }
    }
}

private data class YahooDailyPrice(val date: String, val close: String)

@Composable
private fun YahooFinanceScreen(onOpenResults: () -> Unit) {
    var symbol by remember { mutableStateOf("AAPL") }
    var result by remember { mutableStateOf<JsonObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadChart() {
        val requestedSymbol = symbol.trim().uppercase(Locale.US)
        if (!requestedSymbol.matches(Regex("[A-Z0-9.^=_-]{1,32}"))) {
            error = "မှန်ကန်သော Yahoo Finance symbol ထည့်ပါ။"
            return
        }
        scope.launch {
            loading = true
            error = null
            try {
                result = YahooFinanceClient.api.chart(requestedSymbol)
                val chart = result?.getAsJsonObject("chart")
                if (chart?.get("error")?.isJsonNull == false) {
                    error = chart?.getAsJsonObject("error")?.get("description")?.asString
                        ?: "Yahoo Finance မှ ဒေတာမရပါ။"
                }
            } catch (_: Exception) {
                error = "Yahoo Finance ဒေတာရယူမရပါ။ Worker URL နဲ့ အင်တာနက်ကို စစ်ဆေးပါ။"
                result = null
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadChart() }

    val chart = result?.getAsJsonObject("chart")
    val quoteResult = chart?.getAsJsonArray("result")?.firstOrNull()?.takeUnless { it.isJsonNull }?.asJsonObject
    val metadata = quoteResult?.getAsJsonObject("meta")
    val quote = quoteResult?.getAsJsonObject("indicators")?.getAsJsonArray("quote")
        ?.firstOrNull()?.takeUnless { it.isJsonNull }?.asJsonObject
    val timestamps = quoteResult?.getAsJsonArray("timestamp")
    val closes = quote?.getAsJsonArray("close")
    val rows = if (timestamps != null && closes != null) {
        (0 until minOf(timestamps.size(), closes.size())).mapNotNull { index ->
            val timestamp = timestamps[index].takeUnless { it.isJsonNull }?.asLong ?: return@mapNotNull null
            val close = closes[index].takeUnless { it.isJsonNull }?.asDouble ?: return@mapNotNull null
            YahooDailyPrice(
                date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp * 1000)),
                close = String.format(Locale.US, "%.2f", close),
            )
        }.asReversed()
    } else emptyList()
    val currentPrice = metadata?.get("regularMarketPrice")?.takeUnless { it.isJsonNull }?.asDouble
    val currency = metadata?.get("currency")?.takeUnless { it.isJsonNull }?.asString.orEmpty()
    val name = metadata?.get("longName")?.takeUnless { it.isJsonNull }?.asString
        ?: metadata?.get("shortName")?.takeUnless { it.isJsonNull }?.asString
        ?: symbol.uppercase(Locale.US)

    Column(
        Modifier.fillMaxSize().background(YahooNavy).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Yahoo Finance", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("စတော့ဈေးနှုန်းနှင့် တစ်လစာ နေ့စဉ်မှတ်တမ်း", color = YahooPale)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = symbol,
                onValueChange = { symbol = it },
                label = { Text("Symbol (ဥပမာ AAPL)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { loadChart() },
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = YahooGold, contentColor = YahooNavy),
            ) { Text("ရှာမည်") }
        }
        error?.let { Text(it, color = Color(0xFFFFD5D5)) }
        if (loading) CircularProgressIndicator(color = YahooGold, modifier = Modifier.align(Alignment.CenterHorizontally))
        if (quoteResult != null) {
            Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        currentPrice?.let { "${String.format(Locale.US, "%.2f", it)} $currency" } ?: "ဈေးနှုန်းမရရှိပါ",
                        color = YahooGold,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp,
                    )
                    Text("${metadata?.get("exchangeName")?.takeUnless { it.isJsonNull }?.asString.orEmpty()} · 1 လ / 1 ရက်ကြားကာလ", color = YahooPale)
                }
            }
            Text("နေ့စဉ်ပိတ်ဈေး", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (rows.isEmpty()) Text("ပြသရန် ဈေးနှုန်းမှတ်တမ်း မရှိပါ။", color = YahooPale)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(rows) { row ->
                    Surface(color = Color(0x332D69C1), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(row.date, color = YahooPale)
                            Text("${row.close} $currency", color = YahooGold, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.weight(1f))
        }
        Button(
            onClick = onOpenResults,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = YahooBlue),
        ) { Text("မြန်မာ 2D / 3D ရလဒ်များ") }
    }
}
