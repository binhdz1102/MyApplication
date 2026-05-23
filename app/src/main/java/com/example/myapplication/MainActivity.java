package com.example.myapplication;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.example.myapplication.databinding.ActivityMainBinding;
import com.example.myapplication.presentation.ui.AidlUserListFragment;
import com.example.myapplication.presentation.ui.BackgroundPlaybackFragment;
import com.example.myapplication.presentation.ui.BluetoothUseCasesFragment;
import com.example.myapplication.presentation.ui.MediaControllerUiFragment;
import com.example.myapplication.presentation.ui.MediaSessionPreviewFragment;
import com.example.myapplication.presentation.ui.NotificationLockscreenFragment;
import com.example.myapplication.presentation.ui.MusicPlayerFragment;
import com.example.myapplication.presentation.ui.RoomUserListFragment;
import com.example.myapplication.presentation.ui.ScreenConfiguration;
import com.example.myapplication.presentation.ui.ScreenConfigurationHost;
import com.example.myapplication.presentation.ui.SourcePickerFragment;
import com.example.myapplication.presentation.ui.UserActionHandler;

public class MainActivity extends AppCompatActivity
        implements SourcePickerFragment.Navigator, BluetoothUseCasesFragment.Navigator, ScreenConfigurationHost {

    private ActivityMainBinding binding;
    private ScreenConfiguration currentScreenConfiguration = new ScreenConfiguration(
            R.string.title_source_picker,
            false,
            false,
            false
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        binding.fab.setOnClickListener(view -> {
            Fragment fragment = currentFragment();
            if (fragment instanceof UserActionHandler) {
                ((UserActionHandler) fragment).onAddRequested();
            }
        });

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new SourcePickerFragment(), SourcePickerFragment.TAG)
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem refreshItem = menu.findItem(R.id.action_refresh_users);
        if (refreshItem != null) {
            refreshItem.setVisible(currentScreenConfiguration.isShowRefresh());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_refresh_users) {
            Fragment fragment = currentFragment();
            if (fragment instanceof UserActionHandler) {
                ((UserActionHandler) fragment).onRefreshRequested();
            }
            return true;
        }
        if (itemId == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    @Override
    public void openAidlUsers() {
        navigateTo(AidlUserListFragment.newAidlFragment(), AidlUserListFragment.TAG_AIDL);
    }

    @Override
    public void openRoomUsers() {
        navigateTo(new RoomUserListFragment(), RoomUserListFragment.TAG);
    }

    @Override
    public void openAidlRoomUsers() {
        navigateTo(
                AidlUserListFragment.newAidlRoomDatabaseFragment(),
                AidlUserListFragment.TAG_AIDL_ROOM
        );
    }

    @Override
    public void openBasicMusicPlayer() {
        navigateTo(new MusicPlayerFragment(), MusicPlayerFragment.TAG);
    }

    @Override
    public void openMediaSessionPreview() {
        navigateTo(new MediaSessionPreviewFragment(), MediaSessionPreviewFragment.TAG);
    }

    @Override
    public void openBackgroundPlayback() {
        navigateTo(new BackgroundPlaybackFragment(), BackgroundPlaybackFragment.TAG);
    }

    @Override
    public void openNotificationLockscreen() {
        navigateTo(new NotificationLockscreenFragment(), NotificationLockscreenFragment.TAG);
    }

    @Override
    public void openMediaControllerUi() {
        navigateTo(new MediaControllerUiFragment(), MediaControllerUiFragment.TAG);
    }

    @Override
    public void openBluetoothUseCases() {
        navigateTo(new BluetoothUseCasesFragment(), BluetoothUseCasesFragment.TAG);
    }

    @Override
    public void updateScreenConfiguration(ScreenConfiguration configuration) {
        currentScreenConfiguration = configuration;
        binding.toolbar.setTitle(configuration.getTitleRes());

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(configuration.isShowBackButton());
        }

        if (configuration.isShowFab()) {
            binding.fab.show();
        } else {
            binding.fab.hide();
        }

        invalidateOptionsMenu();
    }

    private void navigateTo(Fragment fragment, String tag) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(tag)
                .commit();
    }

    private Fragment currentFragment() {
        return getSupportFragmentManager().findFragmentById(R.id.fragment_container);
    }
}
