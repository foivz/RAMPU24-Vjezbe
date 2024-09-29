package hr.foi.rampu.memento.services

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import hr.foi.rampu.memento.database.TasksDatabase

class TaskDeletionService : Service() {

    inner class TaskDeletionBinder : Binder() {
        fun getService(): TaskDeletionService = this@TaskDeletionService
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        TasksDatabase.buildInstance(applicationContext)
        val tasksDAO = TasksDatabase.getInstance().getTasksDao()

        tasksDAO.getAllTasks(true).forEach {
            if (it.isOverdue()) {
                tasksDAO.removeTask(it)
            }
        }

        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = TaskDeletionBinder()

    fun deleteOldTasks(onTaskDeletion: (Int) -> Unit) {
        TasksDatabase.buildInstance(applicationContext)
        val tasksDAO = TasksDatabase.getInstance().getTasksDao()
        tasksDAO.getAllTasks(true).forEach {
            if (it.isOverdue()) {
                tasksDAO.removeTask(it)
                onTaskDeletion(it.id)
            }
        }

    }

}