package TSX.telkom.final_project.main.Adapter

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val current: CurrentWeather?
)

data class CurrentWeather(
    @SerializedName("temperature_2m") val temperature: String?,
    @SerializedName("weather_code") val weatherCode: Int?
)
