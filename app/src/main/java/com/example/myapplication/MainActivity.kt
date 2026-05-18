package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.presentation.ui.AidlUserListFragment
import com.example.myapplication.presentation.ui.RoomUserListFragment
import com.example.myapplication.presentation.ui.ScreenConfiguration
import com.example.myapplication.presentation.ui.ScreenConfigurationHost
import com.example.myapplication.presentation.ui.SourcePickerFragment
import com.example.myapplication.presentation.ui.UserActionHandler

class MainActivity : AppCompatActivity(), SourcePickerFragment.Navigator, ScreenConfigurationHost {

    private lateinit var binding: ActivityMainBinding
    private var currentScreenConfiguration = ScreenConfiguration(
        titleRes = R.string.title_source_picker,
        showBackButton = false,
        showFab = false,
        showRefresh = false,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.fab.setOnClickListener {
            (currentFragment() as? UserActionHandler)?.onAddRequested()
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SourcePickerFragment(), SourcePickerFragment.TAG)
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_refresh_users)?.isVisible = currentScreenConfiguration.showRefresh
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh_users -> {
                (currentFragment() as? UserActionHandler)?.onRefreshRequested()
                true
            }

            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun openAidlUsers() {
        navigateTo(AidlUserListFragment(), AidlUserListFragment.TAG)
    }

    override fun openRoomUsers() {
        navigateTo(RoomUserListFragment(), RoomUserListFragment.TAG)
    }

    override fun updateScreenConfiguration(configuration: ScreenConfiguration) {
        currentScreenConfiguration = configuration
        binding.toolbar.setTitle(configuration.titleRes)
        supportActionBar?.setDisplayHomeAsUpEnabled(configuration.showBackButton)
        if (configuration.showFab) {
            binding.fab.show()
        } else {
            binding.fab.hide()
        }
        invalidateOptionsMenu()
    }

    private fun navigateTo(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    private fun currentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.fragment_container)
    }
}
