package hr.foi.rampu.memento

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.preference.PreferenceManager
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.wearable.Wearable
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import hr.foi.rampu.memento.adapters.MainPagerAdapter
import hr.foi.rampu.memento.database.TasksDatabase
import hr.foi.rampu.memento.fragments.CompletedFragment
import hr.foi.rampu.memento.fragments.NewsFragment
import hr.foi.rampu.memento.fragments.PendingFragment
import hr.foi.rampu.memento.helpers.MockDataLoader
import hr.foi.rampu.memento.helpers.TaskDeletionServiceHelper
import hr.foi.rampu.memento.sync.WearableSynchronizer
import java.util.Locale

class MainActivity : AppCompatActivity() {

    lateinit var tabLayout: TabLayout
    lateinit var viewPager2: ViewPager2
    lateinit var navDrawerLayout: DrawerLayout
    lateinit var navView: NavigationView
    lateinit var mainPagerAdapter: MainPagerAdapter
    private val taskDeletionServiceHelper by lazy {
        TaskDeletionServiceHelper(applicationContext)
    }
    private val dataClient by lazy { Wearable.getDataClient(this) }
    lateinit var onSharedPreferencesListener: OnSharedPreferenceChangeListener

    private val settingsLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_LANG_CHANGED) {
                recreate()
            }
        }
    private var configurationChanged = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContentView(R.layout.activity_main)
        initializeMainPagerAdapter()

        TasksDatabase.buildInstance(applicationContext)
        MockDataLoader.loadMockData()

        connectViewPagerWithTabLayout()
        connectNavDrawerWithViewPager()

        val channel = NotificationChannel(
            "task-timer", "Task Timer Channel",
            NotificationManager.IMPORTANCE_HIGH
        )
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        activateTaskDeletionService()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initializeMainPagerAdapter() {
        mainPagerAdapter = MainPagerAdapter(supportFragmentManager, lifecycle)
        fillAdapterWithFragments()
    }

    private fun fillAdapterWithFragments() {
        mainPagerAdapter.addFragment(
            MainPagerAdapter.FragmentItem(
                R.string.tasks_pending,
                R.drawable.baseline_assignment_late_24,
                PendingFragment::class
            )
        )

        mainPagerAdapter.addFragment(
            MainPagerAdapter.FragmentItem(
                R.string.tasks_completed,
                R.drawable.baseline_assignment_turned_in_24,
                CompletedFragment::class
            )
        )

        mainPagerAdapter.addFragment(
            MainPagerAdapter.FragmentItem(
                R.string.news,
                R.drawable.baseline_wysiwyg_24,
                NewsFragment::class
            )
        )
    }

    private fun connectViewPagerWithTabLayout() {
        tabLayout = findViewById(R.id.tabs)
        viewPager2 = findViewById(R.id.viewpager)

        viewPager2.adapter = mainPagerAdapter

        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.setText(mainPagerAdapter.fragmentItems[position].titleRes)
            tab.setIcon(mainPagerAdapter.fragmentItems[position].iconRes)
        }.attach()
    }

    private fun connectNavDrawerWithViewPager() {
        navDrawerLayout = findViewById(R.id.nav_drawer_layout)
        navView = findViewById(R.id.nav_view)

        fillNavDrawerWithFragments()

        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.title) {
                getString(R.string.tasks_pending) -> viewPager2.setCurrentItem(0, true)
                getString(R.string.tasks_completed) -> viewPager2.setCurrentItem(1, true)
                getString(R.string.news) -> viewPager2.setCurrentItem(2, true)
            }
            navDrawerLayout.closeDrawers()
            return@setNavigationItemSelectedListener true
        }

        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                navView.menu.getItem(position).isChecked = true
            }
        })
    }

    private fun fillNavDrawerWithFragments() {
        navView.menu.setGroupDividerEnabled(true)

        var newNavMenuIndex = 0

        mainPagerAdapter.fragmentItems.withIndex().forEach { (index, fragmentItem) ->
            navView.menu
                .add(fragmentItem.titleRes)
                .setIcon(fragmentItem.iconRes)
                .setCheckable(true)
                .setChecked((index == 0))
                .setOnMenuItemClickListener {
                    viewPager2.setCurrentItem(index, true)
                    navDrawerLayout.closeDrawers()
                    return@setOnMenuItemClickListener true
                }
        }

        newNavMenuIndex++

        navView.menu
            .add(newNavMenuIndex,
                0,
                newNavMenuIndex,
                getString(R.string.sync_wear_os)
            )
            .setIcon(R.drawable.baseline_watch_24)
            .setOnMenuItemClickListener{
                WearableSynchronizer.sendTasks(
                    TasksDatabase
                        .getInstance()
                        .getTasksDao()
                        .getAllTasks(false),
                    dataClient
                )
                return@setOnMenuItemClickListener true
            }

        newNavMenuIndex++

        val tasksCounterItem = navView.menu
            .add(newNavMenuIndex, newNavMenuIndex, newNavMenuIndex, "")
            .setEnabled(false)

        attachMenuItemToTasksCreatedCount(tasksCounterItem)

        newNavMenuIndex++

        navView.menu
            .add(
                newNavMenuIndex, newNavMenuIndex, newNavMenuIndex,
                getString(R.string.settings_menu_item)
            )
            .setIcon(R.drawable.ic_baseline_settings_24)
            .setOnMenuItemClickListener {
                settingsLauncher.launch(
                    Intent(
                        this,
                        PreferencesActivity::class.java
                    )
                )
                navDrawerLayout.closeDrawers()
                return@setOnMenuItemClickListener true
            }

    }

    private fun activateTaskDeletionService() {
        taskDeletionServiceHelper.activateTaskDeletionService { deletedTaskId ->
            supportFragmentManager.setFragmentResult(
                "task_deleted",
                bundleOf("task_id" to deletedTaskId)
            )
        }
        /*
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, TaskDeletionService::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 2 * 60 * 1000,
            2 * 60 * 1000,
            pendingIntent
        ) */
    }

    override fun onDestroy() {
        taskDeletionServiceHelper.deactivateTaskDeletionService()
        super.onDestroy()
    }

    private fun getTasksCreatedCount(): Int {
        val sharedPreferences = getSharedPreferences(
            "tasks_preferences", Context.MODE_PRIVATE
        )
        return sharedPreferences.getInt("tasks_created_counter", 0)
    }

    private fun attachMenuItemToTasksCreatedCount(tasksCounterItem: MenuItem) {
        val sharedPreferences = getSharedPreferences("tasks_preferences", Context.MODE_PRIVATE)
        onSharedPreferencesListener =
            OnSharedPreferenceChangeListener { _, key ->
                if (key == "tasks_created_counter") {
                    updateTasksCreatedCounter(tasksCounterItem, sharedPreferences)
                }
            }
        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferencesListener)
        updateTasksCreatedCounter(tasksCounterItem, sharedPreferences)
    }

    private fun updateTasksCreatedCounter(
        tasksCounterItem: MenuItem,
        sharedPreferences: SharedPreferences
    ) {
        val tasksCreated = sharedPreferences.getInt("tasks_created_counter", 0)
        tasksCounterItem.title = "Tasks created: $tasksCreated"
    }

    private fun applyUserSettings(newContext: Context?) : Context {
        PreferenceManager.getDefaultSharedPreferences(newContext!!).let { pref ->
            PreferencesActivity.switchDarkMode(
                pref.getBoolean("preference_dark_mode", false)
            )
            val lang = pref.getString("preference_language", "EN")
            if (lang != null) {
                val locale = Locale(lang)
                if (newContext.resources.configuration.locales[0].language != locale.language) {
                    newContext.resources.configuration.setLocale(locale)
                    Locale.setDefault(locale)
                    val config = Configuration(newContext.resources.configuration)
                    config.setLocale(locale)
                    return newContext.createConfigurationContext(newContext.resources.configuration)
                }
            }
        }
        return newContext
    }


    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(applyUserSettings(newBase))
    }


}