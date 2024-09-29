package hr.foi.rampu.memento.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import hr.foi.rampu.memento.R
import hr.foi.rampu.memento.database.TasksDatabase
import hr.foi.rampu.memento.entities.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.concurrent.TimeUnit

const val NOTIFICATION_ID = 1000

class TaskTimerService : Service() {

    private val tasks = mutableListOf<Task>()
    private var started: Boolean = false
    private var scope: CoroutineScope? = null
    private val mutex = Mutex()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val taskId = intent?.getIntExtra("task_id", -1)
        val  isCanceled = intent?.getBooleanExtra("cancel", false)

        println("Running in vanjski thread: ${Thread.currentThread().name}")

        TasksDatabase.buildInstance(applicationContext)
        val task = TasksDatabase.getInstance().getTasksDao().getTask(taskId!!)

        if (tasks.contains(task)) {
            if (isCanceled!!) {
                tasks.remove(task)
            }
        } else if (task.dueDate > Date()) {
            tasks.add(task)

            if (!started) {
                val notification = buildTimerNotification("")
                // Asinkrono pokrenuti foreground servis!
                startForeground(NOTIFICATION_ID, notification)


                scope = CoroutineScope(Dispatchers.Main)
                scope!!.launch {
                    displayUpdatedNotifications()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    started = false
                }
            }
            started = true
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope?.apply {
            if (isActive) cancel()
        }
        started = false
    }
    private fun buildTimerNotification(contentText: String): Notification {
        return NotificationCompat.Builder(applicationContext, "task-timer")
            .setContentTitle("Task countdown")
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setSmallIcon(R.drawable.ic_baseline_info_24)
            .setOnlyAlertOnce(true)
            .build()
    }




    @SuppressLint("MissingPermission", "NotificationPermission")
    private suspend fun displayUpdatedNotifications() {
        println("Running in unutarnji thread: ${Thread.currentThread().name}")
        val sb = StringBuilder()

        while (tasks.isNotEmpty()) {
            var taskThatRequiresDeletion: Task? = null

            mutex.withLock {
                for (task in tasks) {
                    val remainingMiliseconds = task.dueDate.time - Date().time

                    if (remainingMiliseconds <= 0) {
                        taskThatRequiresDeletion = task
                    } else {
                        sb.append(task.name + ": " + getRemainingTime(remainingMiliseconds))
                    }
                }
            }
            if (taskThatRequiresDeletion != null) {
                mutex.withLock {
                    tasks.remove(taskThatRequiresDeletion)
                }
            }
            NotificationManagerCompat.from(applicationContext)
                .notify(NOTIFICATION_ID, buildTimerNotification(sb.toString()))
            sb.clear()

            delay(1000)
        }
    }

    private fun getRemainingTime(remainingMiliseconds: Long): String {
        val remainingDays = TimeUnit.MILLISECONDS.toDays(remainingMiliseconds)
        val remainingHours = TimeUnit.MILLISECONDS.toHours(remainingMiliseconds) % 24
        val remainingMinutes = TimeUnit.MILLISECONDS.toMinutes(remainingMiliseconds) % 60
        val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(remainingMiliseconds) % 60

        var remainingTimeFormatted = String.format("%02d:%02d:%02d",
            remainingHours, remainingMinutes, remainingSeconds)

        if (remainingDays > 0) {
            remainingTimeFormatted = "${remainingDays}d, $remainingTimeFormatted"
        }
        return remainingTimeFormatted

    }
}