package TSX.telkom.final_project.main
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.STOP_ALERTS_ACTION) {
            SoundManager.getInstance(context).stopEmergencySound()
            NotificationHelper(context).cancelNotification()
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "ALARM STOPPED", Toast.LENGTH_SHORT).show()
            }

        }
    }
}