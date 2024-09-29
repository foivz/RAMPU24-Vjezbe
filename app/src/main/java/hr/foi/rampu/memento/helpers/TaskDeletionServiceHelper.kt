package hr.foi.rampu.memento.helpers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import hr.foi.rampu.memento.services.TaskDeletionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TaskDeletionServiceHelper(private val context: Context) {

    private var taskDeletionService: TaskDeletionService? = null
    private var serviceConnection: ServiceConnection? = null
    private var scheduledExecutorService: ScheduledExecutorService? = null

    fun activateTaskDeletionService(onTaskDeletion: (Int) -> Unit) {
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as TaskDeletionService.TaskDeletionBinder
                taskDeletionService = binder.getService()
                scheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
                scheduledExecutorService?.scheduleWithFixedDelay(
                    {
                        taskDeletionService?.deleteOldTasks { deletedId ->
                            CoroutineScope(Dispatchers.Main).launch {
                                    onTaskDeletion(deletedId)
                                }
                        }
                    }, 0, 15, TimeUnit.MINUTES
                )
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                taskDeletionService = null
            }

        }

        val intent = Intent(context, TaskDeletionService::class.java)
        context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    fun deactivateTaskDeletionService() {
        serviceConnection?.let { context.unbindService(it)}
        scheduledExecutorService?.shutdown()
    }


}