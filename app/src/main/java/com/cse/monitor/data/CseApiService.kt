package com.cse.monitor.data

import com.cse.monitor.model.CompanyInfoResponse
import com.cse.monitor.model.IntradayPoint
import com.cse.monitor.model.MarketSummaryResponse
import com.cse.monitor.model.SymbolSearchResult
import retrofit2.Response
import retrofit2.http.*

interface CseApiService {

    @FormUrlEncoded
    @POST("companyInfoSummery")
    suspend fun getCompanyInfo(
        @Field("symbol") symbol: String
    ): Response<CompanyInfoResponse>

    @GET("marketSummary")
    suspend fun getMarketSummary(): Response<MarketSummaryResponse>

    @FormUrlEncoded
    @POST("intradayPriceData")
    suspend fun getIntradayData(
        @Field("symbol") symbol: String
    ): Response<List<IntradayPoint>>

    /** Search by keyword — returns matching companies from the full CSE listing */
    @FormUrlEncoded
    @POST("symbolSearch")
    suspend fun searchSymbols(
        @Field("keyword") keyword: String
    ): Response<List<SymbolSearchResult>>

    /** Fetch ALL listed companies (used for full browse) */
    @GET("allSymbols")
    suspend fun getAllSymbols(): Response<List<SymbolSearchResult>>
}
