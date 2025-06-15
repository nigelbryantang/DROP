package TSX.telkom.final_project.main

import TSX.telkom.final_project.main.Adapter.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherApi{
    @GET("forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = "temperature_2m,weather_code",
        @Query("timezone") timezone: String = "Asia/Bangkok"
    ): Response<WeatherResponse>
}
