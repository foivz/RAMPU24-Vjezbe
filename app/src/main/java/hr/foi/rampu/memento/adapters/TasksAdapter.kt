package hr.foi.rampu.memento.adapters

import android.app.AlertDialog
import android.content.Intent
import android.view.LayoutInflater
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import hr.foi.rampu.memento.R
import hr.foi.rampu.memento.database.TasksDatabase
import hr.foi.rampu.memento.entities.Task
import hr.foi.rampu.memento.helpers.DeletedTaskRecovery
import hr.foi.rampu.memento.services.TaskTimerService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TasksAdapter(
    private val tasksList : MutableList<Task>,
    private val onTaskCompleted: ((taskId: Int) -> Unit)? = null
):
    RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val sdf : SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy. HH:mm", Locale.ENGLISH)

        private val taskName: TextView
        private val taskDueDate: TextView
        private val taskCategoryColor: SurfaceView
        private val taskTimer: ImageView
        private var isTimerActive = false

        init {
            taskName = view.findViewById(R.id.tv_task_name)
            taskDueDate = view.findViewById(R.id.tv_task_due_date)
            taskCategoryColor = view.findViewById(R.id.sv_task_category_color)
            taskTimer = view.findViewById(R.id.iv_task_timer)

            view.setOnClickListener {
                if (Date() < tasksList[adapterPosition].dueDate) {
                    val intent = Intent(view.context, TaskTimerService::class.java).apply {
                        putExtra("task_id", tasksList[adapterPosition].id)
                    }

                    isTimerActive = !isTimerActive

                    if (isTimerActive) {
                        taskTimer.visibility = View.VISIBLE
                    } else {
                        intent.putExtra("cancel", true)
                        taskTimer.visibility = View.GONE
                    }

                    view.context.startService(intent)
                } else if (taskTimer.visibility == View.VISIBLE) {
                    taskTimer.visibility = View.GONE
                }
            }


            view.setOnLongClickListener{
                val currentTask = tasksList[adapterPosition]

                val alertDialogBuilder = AlertDialog.Builder(view.context)
                    .setTitle(taskName.text)
                    .setNeutralButton(view.context.getString(R.string.delete_task)) {_, _ ->
                        DeletedTaskRecovery.pushTask(currentTask, view.context.cacheDir)
                        val tasksDao = TasksDatabase.getInstance().getTasksDao()
                        val snack =
                            Snackbar
                                .make(view, "Revert?", Snackbar.LENGTH_LONG).setAction("Recover") { view ->
                                    try {
                                        val poppedTaskId = DeletedTaskRecovery.popTask(view.context.cacheDir)
                                        val restoredTask = tasksDao.getTask(poppedTaskId)
                                        addTask(restoredTask)
                                    } catch (ex: Exception) {
                                        Toast.makeText(view.context, ex.message, Toast.LENGTH_LONG).show()
                                    }
                                }
                        snack.show()
                        tasksDao.removeTask(currentTask)
                        removeTaskFromList()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }

                if(onTaskCompleted != null) {
                    alertDialogBuilder.setPositiveButton(R.string.mark_as_completed) { _, _ ->
                        val completedTask = tasksList[adapterPosition]
                        completedTask.completed = true
                        TasksDatabase.getInstance().getTasksDao().insertTask(completedTask)
                        removeTaskFromList()
                        onTaskCompleted?.invoke(completedTask.id)
                    }
                }

                alertDialogBuilder.show()
                return@setOnLongClickListener true
            }

        }

        fun bind(task: Task){
            taskName.text = task.name
            taskDueDate.text = sdf.format(task.dueDate)
            taskCategoryColor.setBackgroundColor(task.category.color.toColorInt())
        }

        private fun removeTaskFromList(){
            tasksList.removeAt(adapterPosition)
            notifyItemRemoved(adapterPosition)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val taskView = LayoutInflater
            .from(parent.context)
            .inflate(R.layout.task_list_item, parent, false)
        return TaskViewHolder(taskView)
    }

    override fun getItemCount() = tasksList.size

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(tasksList[position])
    }

    fun addTask(newTask: Task) {
        var newIndexInList = tasksList.indexOfFirst { task ->
            task.dueDate > newTask.dueDate
        }
        if(newIndexInList == -1) {
            newIndexInList = tasksList.size
        }
        tasksList.add(newIndexInList, newTask)
        notifyItemInserted(newIndexInList)
    }

    fun removeTaskById(taskId: Int) {
        val deletedIndex = tasksList.indexOfFirst { task ->
            task.id == taskId
        }
        if (deletedIndex != -1) {
            tasksList.removeAt(deletedIndex)
            notifyItemRemoved(deletedIndex)
        }
    }
}