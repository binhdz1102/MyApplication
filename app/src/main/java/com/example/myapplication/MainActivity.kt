package com.example.myapplication

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.feature.carproperty.presentation.ui.CarPropertyTemplateFragment
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.presentation.ui.AidlUserListFragment
import com.example.myapplication.presentation.ui.BackgroundPlaybackFragment
import com.example.myapplication.presentation.ui.MediaControllerUiFragment
import com.example.myapplication.presentation.ui.MediaSessionPreviewFragment
import com.example.myapplication.presentation.ui.MusicPlayerFragment
import com.example.myapplication.presentation.ui.NotificationLockscreenFragment
import com.example.myapplication.presentation.ui.RoomUserListFragment
import com.example.myapplication.presentation.ui.ScreenConfiguration
import com.example.myapplication.presentation.ui.ScreenConfigurationHost
import com.example.myapplication.presentation.ui.SourcePickerFragment
import com.example.myapplication.presentation.ui.UserActionHandler
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SourcePickerFragment.Navigator, ScreenConfigurationHost {

    private lateinit var binding: ActivityMainBinding
    private var currentScreenConfiguration = ScreenConfiguration(
        R.string.title_source_picker,
        false,
        false,
        false,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
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
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment_container, SourcePickerFragment(), SourcePickerFragment.TAG)
                .commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_refresh_users)?.isVisible = currentScreenConfiguration.isShowRefresh
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
        navigateTo(AidlUserListFragment.newAidlFragment(), AidlUserListFragment.TAG_AIDL)
    }

    override fun openRoomUsers() {
        navigateTo(RoomUserListFragment(), RoomUserListFragment.TAG)
    }

    override fun openAidlRoomUsers() {
        navigateTo(
            AidlUserListFragment.newAidlRoomDatabaseFragment(),
            AidlUserListFragment.TAG_AIDL_ROOM,
        )
    }

    override fun openBasicMusicPlayer() {
        navigateTo(MusicPlayerFragment(), MusicPlayerFragment.TAG)
    }

    override fun openMediaSessionPreview() {
        navigateTo(MediaSessionPreviewFragment(), MediaSessionPreviewFragment.TAG)
    }

    override fun openBackgroundPlayback() {
        navigateTo(BackgroundPlaybackFragment(), BackgroundPlaybackFragment.TAG)
    }

    override fun openNotificationLockscreen() {
        navigateTo(NotificationLockscreenFragment(), NotificationLockscreenFragment.TAG)
    }

    override fun openMediaControllerUi() {
        navigateTo(MediaControllerUiFragment(), MediaControllerUiFragment.TAG)
    }

    override fun openCarPropertyTemplate() {
        updateScreenConfiguration(
            ScreenConfiguration(
                R.string.title_car_property_template,
                true,
                false,
                false,
            ),
        )
        navigateTo(CarPropertyTemplateFragment.newInstance(), CarPropertyTemplateFragment.TAG)
    }

    override fun updateScreenConfiguration(configuration: ScreenConfiguration) {
        currentScreenConfiguration = configuration
        binding.toolbar.setTitle(configuration.titleRes)

        supportActionBar?.setDisplayHomeAsUpEnabled(configuration.isShowBackButton)

        if (configuration.isShowFab) {
            binding.fab.show()
        } else {
            binding.fab.hide()
        }

        invalidateOptionsMenu()
    }

    private fun navigateTo(fragment: Fragment, tag: String) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment, tag)
            .addToBackStack(tag)
            .commit()
    }

    private fun currentFragment(): Fragment? {
        return supportFragmentManager.findFragmentById(R.id.fragment_container)
    }
}
