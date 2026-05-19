package com.example.myapplication.presentation.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentAidlUserListBinding;
import com.example.myapplication.di.AppContainer;
import com.example.myapplication.domain.model.User;
import com.example.myapplication.presentation.model.ServiceConnectionUiState;
import com.example.myapplication.presentation.ui.adapter.UserAdapter;
import com.example.myapplication.presentation.ui.dialog.DeleteUserDialogFragment;
import com.example.myapplication.presentation.ui.dialog.UserFormDialogFragment;
import com.example.myapplication.presentation.viewmodel.UserViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class AidlUserListFragment extends Fragment implements UserActionHandler {

    public static final String TAG_AIDL = "AidlUserListFragment";
    public static final String TAG_AIDL_ROOM = "AidlRoomUserListFragment";

    private static final String ARG_SOURCE_MODE = "arg_source_mode";
    private static final String SOURCE_MODE_AIDL = "source_mode_aidl";
    private static final String SOURCE_MODE_AIDL_ROOM = "source_mode_aidl_room";

    private enum SourceMode {
        AIDL,
        AIDL_ROOM
    }

    private FragmentAidlUserListBinding binding;
    private UserViewModel viewModel;
    private ScreenConfigurationHost screenConfigurationHost;
    private ServiceConnectionUiState currentConnectionState = ServiceConnectionUiState.DISCONNECTED;
    private SourceMode sourceMode = SourceMode.AIDL;

    private final UserAdapter userAdapter = new UserAdapter(
            this::showEditUserDialog,
            this::showDeleteUserDialog
    );

    public static AidlUserListFragment newAidlFragment() {
        AidlUserListFragment fragment = new AidlUserListFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_SOURCE_MODE, SOURCE_MODE_AIDL);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static AidlUserListFragment newAidlRoomDatabaseFragment() {
        AidlUserListFragment fragment = new AidlUserListFragment();
        Bundle arguments = new Bundle();
        arguments.putString(ARG_SOURCE_MODE, SOURCE_MODE_AIDL_ROOM);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ScreenConfigurationHost) {
            screenConfigurationHost = (ScreenConfigurationHost) context;
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null && SOURCE_MODE_AIDL_ROOM.equals(arguments.getString(ARG_SOURCE_MODE))) {
            sourceMode = SourceMode.AIDL_ROOM;
        } else {
            sourceMode = SourceMode.AIDL;
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAidlUserListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(
                this,
                sourceMode == SourceMode.AIDL_ROOM
                        ? AppContainer.provideAidlRoomUserViewModelFactory(requireContext().getApplicationContext())
                        : AppContainer.provideAidlUserViewModelFactory(requireContext().getApplicationContext())
        ).get(UserViewModel.class);

        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerUsers.setAdapter(userAdapter);
        binding.buttonRetryConnection.setOnClickListener(v -> viewModel.connectService());

        registerDialogResults();
        observeViewModel();
        viewModel.connectService();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateScreenConfiguration();
    }

    @Override
    public void onAddRequested() {
        if (currentConnectionState != ServiceConnectionUiState.CONNECTED) {
            Snackbar.make(binding.getRoot(), R.string.message_service_not_connected, Snackbar.LENGTH_SHORT).show();
            return;
        }
        UserFormDialogFragment.newAddDialog().show(getChildFragmentManager(), UserFormDialogFragment.TAG);
    }

    @Override
    public void onRefreshRequested() {
        if (currentConnectionState == ServiceConnectionUiState.CONNECTED) {
            viewModel.loadUsers();
        } else {
            viewModel.connectService();
        }
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            binding.recyclerUsers.setAdapter(null);
            binding = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        screenConfigurationHost = null;
        super.onDetach();
    }

    private void registerDialogResults() {
        getChildFragmentManager().setFragmentResultListener(
                UserFormDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> {
                    User user = new User(
                            result.getLong(UserFormDialogFragment.RESULT_USER_ID),
                            readString(result, UserFormDialogFragment.RESULT_USER_NAME),
                            result.getInt(UserFormDialogFragment.RESULT_USER_AGE),
                            result.getFloat(UserFormDialogFragment.RESULT_USER_WEIGHT)
                    );

                    if (result.getBoolean(UserFormDialogFragment.RESULT_IS_EDIT_MODE)) {
                        viewModel.updateUser(user);
                    } else {
                        viewModel.addUser(user);
                    }
                }
        );

        getChildFragmentManager().setFragmentResultListener(
                DeleteUserDialogFragment.REQUEST_KEY,
                getViewLifecycleOwner(),
                (requestKey, result) -> viewModel.deleteUser(result.getLong(DeleteUserDialogFragment.RESULT_USER_ID))
        );
    }

    private void observeViewModel() {
        viewModel.getUsers().observe(getViewLifecycleOwner(), users -> {
            userAdapter.submitList(users);
            updateListState();
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> updateListState());

        viewModel.getServiceConnectionState().observe(getViewLifecycleOwner(), state -> {
            currentConnectionState = state;
            renderConnectionState(state);
            updateListState();
            updateScreenConfiguration();
        });

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessage();
            }
        });
    }

    private void renderConnectionState(ServiceConnectionUiState state) {
        boolean isConnected = state == ServiceConnectionUiState.CONNECTED;
        binding.layoutServiceStatus.setVisibility(isConnected ? View.GONE : View.VISIBLE);
        binding.layoutListContent.setVisibility(isConnected ? View.VISIBLE : View.GONE);

        if (state == ServiceConnectionUiState.CONNECTING) {
            binding.textStatusTitle.setText(R.string.service_connecting_title);
            binding.textStatusMessage.setText(R.string.service_connecting_message);
            binding.buttonRetryConnection.setText(R.string.action_connecting);
            binding.buttonRetryConnection.setEnabled(false);
        } else if (state == ServiceConnectionUiState.CONNECTED) {
            binding.buttonRetryConnection.setEnabled(false);
        } else {
            binding.textStatusTitle.setText(R.string.service_disconnected_title);
            binding.textStatusMessage.setText(R.string.service_disconnected_message);
            binding.buttonRetryConnection.setText(R.string.action_retry_connection);
            binding.buttonRetryConnection.setEnabled(true);
        }
    }

    private void updateListState() {
        if (binding == null) {
            return;
        }

        boolean isConnected = currentConnectionState == ServiceConnectionUiState.CONNECTED;
        boolean isLoading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        List<User> currentUsers = userAdapter.getCurrentList();
        boolean isEmpty = currentUsers == null || currentUsers.isEmpty();

        binding.progressLoadUsers.setVisibility(isConnected && isLoading ? View.VISIBLE : View.GONE);
        binding.textLoading.setVisibility(isConnected && isLoading ? View.VISIBLE : View.GONE);
        binding.layoutEmpty.setVisibility(isConnected && !isLoading && isEmpty ? View.VISIBLE : View.GONE);
    }

    private void updateScreenConfiguration() {
        if (screenConfigurationHost == null) {
            return;
        }
        int titleRes = sourceMode == SourceMode.AIDL_ROOM
                ? R.string.title_aidl_room_users
                : R.string.title_aidl_users;
        screenConfigurationHost.updateScreenConfiguration(
                new ScreenConfiguration(
                        titleRes,
                        true,
                        currentConnectionState == ServiceConnectionUiState.CONNECTED,
                        true
                )
        );
    }

    private void showEditUserDialog(User user) {
        UserFormDialogFragment.newEditDialog(user)
                .show(getChildFragmentManager(), UserFormDialogFragment.TAG);
    }

    private void showDeleteUserDialog(User user) {
        DeleteUserDialogFragment.newInstance(user.getId(), user.getName())
                .show(getChildFragmentManager(), DeleteUserDialogFragment.TAG);
    }

    private String readString(Bundle bundle, String key) {
        String value = bundle.getString(key);
        return value == null ? "" : value;
    }
}
