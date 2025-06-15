package TSX.telkom.final_project.main

import TSX.telkom.final_project.R
import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import androidx.viewpager2.widget.ViewPager2
import TSX.telkom.final_project.main.Adapter.ViewPagerAdapter
import TSX.telkom.final_project.main.Adapter.WeatherAnimation
import TSX.telkom.final_project.main.Adapter.ZoominAdapter
import android.app.PendingIntent
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import pl.droidsonroids.gif.GifImageView
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val WEATHER_API = "https://api.open-meteo.com/v1/"
        private const val CHECK_LAST_FALL_TIME = 10 * 60L
        private const val NOTIFICATION_COOLDOWN = 30_000L
    }
    private var firebaseTime: Long = 0
    private var lastStopTime: Long = System.currentTimeMillis()
    private lateinit var fallCheckRunnable: Runnable
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var soundManager: SoundManager
    private var oldImageList: List<String> = emptyList()
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private var isFallNotifying = false
    private val handler = Handler(Looper.getMainLooper())
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                this,
                "Notification permission denied - alerts may not work",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        notificationHelper = NotificationHelper(this).also { it.createNotificationChannel() }
        soundManager = SoundManager.getInstance(this)

        checkNotificationPermission()
        setupViews()
        setupFirebaseListeners()
        refreshData()

        if (intent?.action == NotificationHelper.STOP_ALERTS_ACTION) {
            showStopAlarmDialog()
        }
    }

    override fun onDestroy() {
        stopPeriodicFallChecking()
        handler.removeCallbacksAndMessages(null)
        soundManager.stopEmergencySound()
        super.onDestroy()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> return

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) -> showPermissionRationale()

                else -> notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Notification Permission Needed")
            .setMessage("This app needs notification permission to alert you about falls")
            .setPositiveButton("OK") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupViews() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, MapFragment())
            .commit()

        findViewById<ImageView>(R.id.weather).apply {
            when (val drawable = drawable) {
                is AnimatedVectorDrawable -> drawable.start()
                is AnimatedVectorDrawableCompat -> drawable.start()
                else -> Log.e("Animation", "Drawable not animate-able")
            }
        }
    }

    private fun setupFirebaseListeners() {
        val database = Firebase.database
        val activityRef = database.reference.child("activity")
        val batteryRef = database.reference.child("batt")
        val timeRef = database.reference.child("current_time")

        activityRef.addValueEventListener(createActivityListener())
        batteryRef.addValueEventListener(createBatteryListener())
        timeRef.addValueEventListener(createTimeListener())
    }

    private fun createActivityListener() = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            val rawActivity = snapshot.value.toString().uppercase()
            updateActivityUI(rawActivity)

            if (rawActivity == "FALL") {
                if (!isFallNotifying) {
                    startEmergencyAlert()
                }
                startPeriodicFallChecking()
            } else {
                stopPeriodicFallChecking()
                if (isFallNotifying) {
                    stopEmergencyAlert()
                }
        }}

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Activity listener cancelled", error.toException())
        }
    }



    private fun stopPeriodicFallChecking() {
        if (::fallCheckRunnable.isInitialized) {
            handler.removeCallbacks(fallCheckRunnable)
        }
    }

    private fun createTimeListener() = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            (snapshot.value as? Number)?.toLong()?.let { timestamp ->
                firebaseTime = timestamp
                findViewById<TextView>(R.id.time).text = SimpleDateFormat(
                    "dd-MM-yyyy \nHH:mm:ss",
                    Locale.getDefault()
                ).format(Date(timestamp * 1000))
            }
        }
        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Time listener cancelled", error.toException())
        }
    }

    private fun createBatteryListener() = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            findViewById<TextView>(R.id.battery).text = ": ${snapshot.value}%"
        }

        override fun onCancelled(error: DatabaseError) {
            Log.e("Firebase", "Battery listener cancelled", error.toException())
        }
    }

    private fun updateActivityUI(activity: String) {
        val (activityText, gifRes) = when (activity) {
            "STAND" -> ": STANDING" to R.drawable.standing
            "WALK" -> ": WALKING" to R.drawable.walking
            "SIT" -> ": SITTING" to R.drawable.sit
            "UNSTABLE" -> ": UNSTABLE" to R.drawable.fall
            "FALL" -> ": FALLING" to R.drawable.fall
            else -> ": UNKNOWN" to R.drawable.unknown
        }

        findViewById<TextView>(R.id.activity_val).text = activityText
        findViewById<GifImageView>(R.id.activity_animation).setImageResource(gifRes)
    }

    private fun stopEmergencyAlert() {
        if (!isFallNotifying) return

        lastStopTime = System.currentTimeMillis()
        isFallNotifying = false
        soundManager.stopEmergencySound()
        notificationHelper.cancelNotification()
        Log.d("ALERT", "Alert stopped at ${Date(lastStopTime)}")
    }

    private fun startEmergencyAlert() {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastStop = currentTime - lastStopTime

        Log.d("ALERT_DEBUG", """
        Checking alert conditions:
        isFallNotifying = $isFallNotifying
        timeSinceLastStop = $timeSinceLastStop ms
        cooldownRemaining = ${NOTIFICATION_COOLDOWN - timeSinceLastStop} ms
    """.trimIndent())

        // Only proceed if not in cooldown and not already notifying
        if (isFallNotifying || timeSinceLastStop < NOTIFICATION_COOLDOWN) {
            Log.d("ALERT", if (isFallNotifying) "Alert already active" else "In cooldown period")
            return
        }

        try {
            isFallNotifying = true
            soundManager.startEmergencySound()
            notificationHelper.showFallNotification(createStopPendingIntent())
            Log.d("ALERT", "Notification successfully triggered")
        } catch (e: Exception) {
            isFallNotifying = false
            Log.e("ALERT", "Failed to trigger notification", e)
        }
    }

    private fun createStopPendingIntent(): PendingIntent {
        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationHelper.STOP_ALERTS_ACTION
            flags = Intent.FLAG_RECEIVER_FOREGROUND
            `package` = packageName
        }
        return PendingIntent.getBroadcast(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refreshData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    fetchImages()
                    fetchWeather()
                    delay(1000)
                }
            }
        }
    }

    private suspend fun fetchImages() {
        try {
            val storageRef = Firebase.storage("gs://fall-mitigation.appspot.com").reference
            val result = storageRef.listAll().await()
            val imageUrls = result.items
                .sortedByDescending { it.name }
                .take(5)
                .map { it.downloadUrl.await().toString() }

            withContext(Dispatchers.Main) {
                updateViewPager(imageUrls)
            }
        } catch (e: Exception) {
            Log.e("Firebase", "Error getting images", e)
        }
    }

    private fun updateViewPager(newImageList: List<String>) {
        val viewPager = findViewById<ViewPager2>(R.id.view_pager1)
        val slider = findViewById<SpringDotsIndicator>(R.id.slider)

        if (!::viewPagerAdapter.isInitialized) {
            viewPagerAdapter = ViewPagerAdapter(newImageList) { index ->
                showFullScreenImage(newImageList, index)
            }
            viewPager.adapter = viewPagerAdapter
            slider.attachTo(viewPager)
        } else if (newImageList != oldImageList) {
            viewPagerAdapter.updateImages(newImageList)
        }
        oldImageList = newImageList
    }

    private fun showFullScreenImage(urls: List<String>, startIndex: Int = 0) {
        Dialog(this).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_full_image)

            findViewById<ViewPager2>(R.id.view_pager).apply {
                adapter = ZoominAdapter(urls)
                setCurrentItem(startIndex, false)
            }

            findViewById<ImageButton>(R.id.btn_close).setOnClickListener { dismiss() }
            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            show()
        }
    }

    private fun fetchWeather() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = Retrofit.Builder()
                    .baseUrl(WEATHER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(WeatherApi::class.java)
                    .getWeather(13.7563, 100.5018)

                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        response.body()?.current?.let { weather ->
                            findViewById<TextView>(R.id.temp).text = "${weather.temperature}°C"
                            WeatherAnimation.setWeatherAnimation(
                                findViewById(R.id.weather),
                                weather.weatherCode?.toInt() ?: -1
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Weather", "Error fetching weather", e)
            }
        }
    }

    private fun showStopAlarmDialog() {
        AlertDialog.Builder(this)
            .setTitle("Stop Alarm")
            .setMessage("Do you want to stop the emergency alarm?")
            .setPositiveButton("Stop") { _, _ ->
                stopEmergencyAlert()
                Toast.makeText(this, "Alarm stopped", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .setCancelable(false)
            .show()
    }

    private fun startPeriodicFallChecking() {
        stopPeriodicFallChecking()

        fallCheckRunnable = Runnable {
            Firebase.database.reference.child("activity").get().addOnSuccessListener { snapshot ->
                val currentActivity = snapshot.value.toString().uppercase()
                val currentSystemTime = System.currentTimeMillis()

                Log.d("FALL_DEBUG", """
                Current Activity: $currentActivity
                Firebase Time: $firebaseTime (${Date(firebaseTime * 1000)})
                System Time: ${currentSystemTime/1000} (${Date(currentSystemTime)})
                Time Diff: ${(currentSystemTime/1000) - firebaseTime} sec
                Last Stop: $lastStopTime (${Date(lastStopTime)})
                Cooldown: ${currentSystemTime - lastStopTime} ms
                isNotifying: $isFallNotifying
            """.trimIndent())

                if (currentActivity == "FALL") {
                    val isRecentFall = (currentSystemTime/1000 - firebaseTime) < CHECK_LAST_FALL_TIME
                    if (isRecentFall) {
                        checkAndTriggerAlert()
                    } else {
                        stopEmergencyAlert()
                    }
                } else {
                    stopEmergencyAlert()
                }

                handler.postDelayed(fallCheckRunnable, NOTIFICATION_COOLDOWN)
            }.addOnFailureListener {
                handler.postDelayed(fallCheckRunnable, NOTIFICATION_COOLDOWN)
            }
        }
        handler.post(fallCheckRunnable)
    }

    private fun checkAndTriggerAlert() {
        val now = System.currentTimeMillis()
        if (!isFallNotifying || (now - lastStopTime) >= NOTIFICATION_COOLDOWN) {
            try {
                isFallNotifying = true
                soundManager.startEmergencySound()
                notificationHelper.showFallNotification(createStopPendingIntent())
                Log.d("ALERT", "Notification triggered at ${Date(now)}")
            } catch (e: Exception) {
                isFallNotifying = false
                Log.e("ALERT", "Notification failed", e)
            }
        }
    }
}