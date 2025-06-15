package TSX.telkom.final_project.main

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.util.Log

class SoundManager private constructor(private val context: Context) {
    private var alarmRingtone: Ringtone? = null
    private var audioManager: AudioManager? = null
    private var isPlaying = false

    companion object {
        @Volatile private var instance: SoundManager? = null

        fun getInstance(context: Context): SoundManager {
            return instance ?: synchronized(this) {
                instance ?: SoundManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    fun startEmergencySound() {
        stopEmergencySound()
        try {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            alarmRingtone = RingtoneManager.getRingtone(context, alarmSound).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    streamType = AudioManager.STREAM_ALARM
                }
                play()
            }
            isPlaying = true
        } catch (e: Exception) {
            Log.e("SoundManager", "Error starting alarm", e)
        }
    }

    fun stopEmergencySound() {
        try {
            alarmRingtone?.stop()
            val playerField = Ringtone::class.java.getDeclaredField("mLocalPlayer").apply {
                isAccessible = true
            }
            val mediaPlayer = playerField.get(alarmRingtone) as? android.media.MediaPlayer
            mediaPlayer?.apply {
                reset()
                release()
            }

            audioManager?.abandonAudioFocus(null)
            alarmRingtone = null
            audioManager = null

        } catch (e: Exception) {
            Log.e("SoundManager", "Force stop failed", e)
        }
    }
}

