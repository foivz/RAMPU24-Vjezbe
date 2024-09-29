package hr.foi.rampu.memento.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import hr.foi.rampu.memento.R
import hr.foi.rampu.memento.adapters.TasksAdapter
import hr.foi.rampu.memento.database.TasksDatabase

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [CompletedFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CompletedFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var recyclerView: RecyclerView
    private val tasksDAO = TasksDatabase.getInstance().getTasksDao()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_completed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.rv_completed_tasks)
        val completedTasks = tasksDAO.getAllTasks(true)
        recyclerView.adapter = TasksAdapter(completedTasks.toMutableList())
        recyclerView.layoutManager = LinearLayoutManager(view.context)

        parentFragmentManager.setFragmentResultListener("task_completed", viewLifecycleOwner) { _, bundle ->
            val addedTaskId = bundle.getInt("task_id")
            val tasksAdapter = recyclerView.adapter as TasksAdapter
            tasksAdapter.addTask(tasksDAO.getTask(addedTaskId))
        }

        parentFragmentManager.setFragmentResultListener("task_deleted", viewLifecycleOwner)
        { _, bundle ->
            val deletedTaskId = bundle.getInt("task_id")
            val tasksAdapter = recyclerView.adapter as TasksAdapter
            tasksAdapter.removeTaskById(deletedTaskId)
        }
    }
}