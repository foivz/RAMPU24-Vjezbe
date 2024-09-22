package hr.foi.rampu.memento.helpers

import hr.foi.rampu.memento.database.TasksDatabase
import hr.foi.rampu.memento.entities.Task
import hr.foi.rampu.memento.entities.TaskCategory
import java.util.Date

object MockDataLoader {

    fun loadMockData() {
        val tasksDao = TasksDatabase.getInstance().getTasksDao()
        val taskCategoriesDao = TasksDatabase.getInstance().getTaskCategoriesDao()

        if (tasksDao.getAllTasks(false).isEmpty() &&
            tasksDao.getAllTasks(true).isEmpty() &&
            taskCategoriesDao.getAllCategories().isEmpty()) {

            val categories = arrayOf(
                TaskCategory(1, "RAMPU", "#000080"),
                TaskCategory(2, "RPP", "#FF0000"),
                TaskCategory(3, "RWA", "#CCCCCC")
            )
            taskCategoriesDao.insertCategory(*categories)

            val dbCategories = taskCategoriesDao.getAllCategories()

            val tasks = arrayOf(
                Task(1, "Submit seminar paper", Date(), dbCategories[0].id, false),
                Task(2, "Prepare for exercises", Date(), dbCategories[1].id, false),
                Task(3, "Rally a project team", Date(), dbCategories[0].id, false),
                Task(4, "Work on 1st homework", Date(), dbCategories[2].id, false)
            )
            tasksDao.insertTask(*tasks)
        }
    }
    /*
    fun getDemoData(): MutableList<Task> {

        return mutableListOf(
            Task(0,"Submit seminar paper", Date(), 0, false),
            Task(1,"Prepare for exercises", Date(), 1, false),
            Task(2,"Rally a project team", Date(), 0, false),
            Task(3,"Work on 1st homework", Date(), 2, false)
        )
    }

    fun getDemoCategories(): List<TaskCategory> = listOf(
        TaskCategory(0,"RAMPU", "#000080"),
        TaskCategory(1,"RPP", "#FF0000"),
        TaskCategory(2,"RWA", "#CCCCCC")
    ) */
}