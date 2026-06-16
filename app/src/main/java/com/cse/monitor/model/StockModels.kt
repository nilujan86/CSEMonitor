package com.cse.monitor.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

// ── API Response Models ──────────────────────────────────────────────────────

data class CompanyInfoResponse(
    @SerializedName("reqSymbolInfo") val symbolInfo: SymbolInfo?,
    @SerializedName("reqLogo") val logo: LogoInfo?,
    @SerializedName("reqSymbolBetaInfo") val betaInfo: BetaInfo?
)

data class SymbolInfo(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name") val name: String,
    @SerializedName("lastTradedPrice") val lastTradedPrice: Double,
    @SerializedName("change") val change: Double,
    @SerializedName("changePercentage") val changePercentage: Double,
    @SerializedName("marketCap") val marketCap: Long?
)

data class LogoInfo(
    @SerializedName("id") val id: Int?,
    @SerializedName("path") val path: String?
)

data class BetaInfo(
    @SerializedName("betaValueSPSL") val betaValue: Double?
)

data class MarketSummaryResponse(
    @SerializedName("aspi") val aspi: Double?,
    @SerializedName("aspiChange") val aspiChange: Double?,
    @SerializedName("aspiChangePercentage") val aspiChangePercentage: Double?,
    @SerializedName("s&p20") val sp20: Double?,
    @SerializedName("turnover") val turnover: Double?,
    @SerializedName("volume") val volume: Long?,
    @SerializedName("trades") val trades: Int?
)

data class IntradayPoint(
    @SerializedName("time") val time: String,
    @SerializedName("price") val price: Double,
    @SerializedName("volume") val volume: Long?
)
data class SymbolSearchResult(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("name")   val name: String
)
// ── Room Entity: Watched Stocks ───────────────────────────────────────────────

@Entity(tableName = "watched_stocks")
data class WatchedStock(
    @PrimaryKey val symbol: String,
    val name: String,
    val lastTradedPrice: Double,
    val change: Double,
    val changePercentage: Double,
    val marketCap: Long?,
    val logoPath: String?,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isWidget: Boolean = false,       // pinned to widget
    val displayOrder: Int = 0
)

// ── UI display model ─────────────────────────────────────────────────────────

data class StockUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val stock: WatchedStock? = null,
    val intradayPoints: List<IntradayPoint> = emptyList(),
    val marketSummary: MarketSummaryResponse? = null
)

// ── Well-known CSE symbols for search / autocomplete ────────────────────────

val CSE_POPULAR_STOCKS = listOf(
    Pair("LOLC.N0000",  "LOLC Holdings PLC"),
    Pair("JKH.N0000",   "John Keells Holdings PLC"),
    Pair("COMB.N0000",  "Commercial Bank of Ceylon PLC"),
    Pair("HNB.N0000",   "Hatton National Bank PLC"),
    Pair("DIAL.N0000",  "Dialog Axiata PLC"),
    Pair("SAMP.N0000",  "Sampath Bank PLC"),
    Pair("LIOC.N0000",  "Lanka IOC PLC"),
    Pair("NTB.N0000",   "Nations Trust Bank PLC"),
    Pair("TBEV.N0000",  "Three Coins (Beverages)"),
    Pair("RICH.N0000",  "Richard Pieris & Company PLC"),
    Pair("DIST.N0000",  "Distilleries Company of Sri Lanka PLC"),
    Pair("CTC.N0000",   "Ceylon Tobacco Company PLC"),
    Pair("EDEN.N0000",  "Eden Hotel Lanka PLC"),
    Pair("AHPL.N0000",  "Asian Hotels & Properties PLC"),
    Pair("TJL.N0000",   "Textured Jersey Lanka PLC")
)
