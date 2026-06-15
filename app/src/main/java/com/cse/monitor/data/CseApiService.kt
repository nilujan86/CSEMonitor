package com.cse.monitor.data

import com.cse.monitor.model.CompanyInfoResponse
import com.cse.monitor.model.IntradayPoint
import com.cse.monitor.model.MarketSummaryResponse
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit interface for the unofficial CSE API (https://www.cse.lk/api/).
 *
 * All endpoints use POST with form-encoded bodies — mirroring the CSE web portal.
 */
interface CseApiService {

    /** Full company info + price for a single symbol */
    @FormUrlEncoded
    @POST("companyInfoSummery")
    suspend fun getCompanyInfo(
        @Field("symbol") symbol: String
    ): Response<CompanyInfoResponse>

    /** Market summary – ASPI, S&P SL20, turnover, volume */
    @GET("marketSummary")
    suspend fun getMarketSummary(): Response<MarketSummaryResponse>

    /** Intraday price series for charting */
    @FormUrlEncoded
    @POST("intradayPriceData")
    suspend fun getIntradayData(
        @Field("symbol") symbol: String
    ): Response<List<IntradayPoint>>

    /** Search companies by keyword */
    @FormUrlEncoded
    @POST("symbolSearch")
    suspend fun searchSymbols(
        @Field("keyword") keyword: String
    ): Response<List<Map<String, String>>>
}
