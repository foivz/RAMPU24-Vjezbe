package hr.foi.rampu.memento.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import hr.foi.rampu.memento.converters.DateConverter
import hr.foi.rampu.memento.database.TasksDatabase
import java.util.Date

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(
        entity = TaskCategory::class,
        parentColumns = ["id"],
        childColumns = ["category_id"],
        onDelete = ForeignKey.RESTRICT
    )]
)
@TypeConverters(DateConverter::class)
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val name : String,
    @ColumnInfo(name = "due_date")
    val dueDate : Date,
    @ColumnInfo(name = "category_id", index = true)
    val categoryId : Int,
    var completed : Boolean
) {
    @delegate:Ignore
    val category: TaskCategory by lazy {
        TasksDatabase.getInstance().getTaskCategoriesDao().getCategoryById(categoryId)
    }
}
