package TSX.telkom.final_project.main.Adapter
import android.graphics.drawable.Animatable
import android.widget.ImageView
import TSX.telkom.final_project.R
import java.util.Calendar

object WeatherAnimation {
    fun setWeatherAnimation(imageView: ImageView, weatherCode: Int) {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = currentHour in 18..23 || currentHour in 0..4

        val animationRes = when {
            weatherCode in 0..49 && isNight -> R.drawable.night_animation
            weatherCode in 0..3 -> R.drawable.day_animation
            weatherCode in 4..18 -> R.drawable.cloudy_animation
            weatherCode in 50..99 -> R.drawable.rainy_animation
            else -> R.drawable.ic_launcher_background
        }

        imageView.setImageResource(animationRes)
        val drawable = imageView.drawable
        if (drawable is Animatable) {
            drawable.start()
        }
    }
}