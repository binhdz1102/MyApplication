package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.Menu
import android.view.MenuItem
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.di.AppContainer
import com.example.myapplication.domain.model.User
import com.example.myapplication.presentation.model.ServiceConnectionUiState
import com.example.myapplication.presentation.ui.ServiceStatusFragment
import com.example.myapplication.presentation.ui.UserListFragment
import com.example.myapplication.presentation.ui.dialog.DeleteUserDialogFragment
import com.example.myapplication.presentation.ui.dialog.UserFormDialogFragment
import com.example.myapplication.presentation.viewmodel.UserViewModel
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity(), UserListFragment.UserItemListener {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: UserViewModel by viewModels {
        AppContainer.provideUserViewModelFactory(applicationContext)
    }

    private var currentServiceState: ServiceConnectionUiState = ServiceConnectionUiState.CONNECTING

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

        binding.fab.setOnClickListener {
            UserFormDialogFragment.newAddDialog()
                .show(supportFragmentManager, UserFormDialogFragment.TAG)
        }

        registerDialogResults()
        viewModel.connectService()

        observeViewModel()

        if (savedInstanceState == null) {
            showFragment(ServiceConnectionUiState.CONNECTING)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_refresh_users)?.isVisible =
            currentServiceState == ServiceConnectionUiState.CONNECTED
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh_users -> {
                viewModel.loadUsers()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onUserClicked(user: User) {
        UserFormDialogFragment.newEditDialog(user)
            .show(supportFragmentManager, UserFormDialogFragment.TAG)
    }

    override fun onUserLongClicked(user: User) {
        DeleteUserDialogFragment.newInstance(user.id, user.name)
            .show(supportFragmentManager, DeleteUserDialogFragment.TAG)
    }

    private fun registerDialogResults() {
        supportFragmentManager.setFragmentResultListener(
            UserFormDialogFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            val user = User(
                id = bundle.getLong(UserFormDialogFragment.RESULT_USER_ID),
                name = bundle.getString(UserFormDialogFragment.RESULT_USER_NAME).orEmpty(),
                age = bundle.getInt(UserFormDialogFragment.RESULT_USER_AGE),
                weight = bundle.getFloat(UserFormDialogFragment.RESULT_USER_WEIGHT),
            )

            if (bundle.getBoolean(UserFormDialogFragment.RESULT_IS_EDIT_MODE)) {
                viewModel.updateUser(user)
            } else {
                viewModel.addUser(user)
            }
        }

        supportFragmentManager.setFragmentResultListener(
            DeleteUserDialogFragment.REQUEST_KEY,
            this,
        ) { _, bundle ->
            viewModel.deleteUser(bundle.getLong(DeleteUserDialogFragment.RESULT_USER_ID))
        }
    }

    private fun observeViewModel() {
        viewModel.serviceConnectionState.observe(this) { state ->
            currentServiceState = state
            showFragment(state)
            binding.toolbar.subtitle = when (state) {
                ServiceConnectionUiState.CONNECTING -> getString(R.string.service_connecting_subtitle)
                ServiceConnectionUiState.CONNECTED -> getString(R.string.service_connected_subtitle)
                ServiceConnectionUiState.DISCONNECTED -> getString(R.string.service_disconnected_subtitle)
            }
            if (state == ServiceConnectionUiState.CONNECTED) {
                binding.fab.show()
            } else {
                binding.fab.hide()
            }
            invalidateOptionsMenu()
        }

        viewModel.message.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                val snackbar = Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
                if (binding.fab.isShown) {
                    snackbar.setAnchorView(binding.fab)
                }
                snackbar.show()
                viewModel.consumeMessage()
            }
        }
    }

    private fun showFragment(state: ServiceConnectionUiState) {
        val targetTag = if (state == ServiceConnectionUiState.CONNECTED) {
            UserListFragment.TAG
        } else {
            ServiceStatusFragment.TAG
        }

        val currentTag = supportFragmentManager.findFragmentById(R.id.fragment_container)?.tag
        if (currentTag == targetTag) {
            return
        }

        val fragment = if (state == ServiceConnectionUiState.CONNECTED) {
            UserListFragment()
        } else {
            ServiceStatusFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment, targetTag)
            .commit()
    }
}
