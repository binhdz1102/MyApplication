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
import com.example.myapplication.databinding.FragmentUserListBinding;
import com.example.myapplication.di.AppContainer;
import com.example.myapplication.domain.model.User;
import com.example.myapplication.presentation.ui.adapter.UserAdapter;
import com.example.myapplication.presentation.ui.dialog.DeleteUserDialogFragment;
import com.example.myapplication.presentation.ui.dialog.UserFormDialogFragment;
import com.example.myapplication.presentation.viewmodel.UserViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class RoomUserListFragment extends Fragment implements UserActionHandler {

    public static final String TAG = "RoomUserListFragment";

    private FragmentUserListBinding binding;
    private UserViewModel viewModel;
    private ScreenConfigurationHost screenConfigurationHost;

    private final UserAdapter userAdapter = new UserAdapter(
            this::showEditUserDialog,
            this::showDeleteUserDialog
    );

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof ScreenConfigurationHost) {
            screenConfigurationHost = (ScreenConfigurationHost) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentUserListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(
                this,
                AppContainer.provideRoomUserViewModelFactory(requireContext().getApplicationContext())
        ).get(UserViewModel.class);

        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerUsers.setAdapter(userAdapter);

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
        UserFormDialogFragment.newAddDialog().show(getChildFragmentManager(), UserFormDialogFragment.TAG);
    }

    @Override
    public void onRefreshRequested() {
        viewModel.loadUsers();
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

        viewModel.getMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.trim().isEmpty()) {
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                viewModel.consumeMessage();
            }
        });
    }

    private void updateListState() {
        if (binding == null) {
            return;
        }

        boolean isLoading = Boolean.TRUE.equals(viewModel.getIsLoading().getValue());
        List<User> currentUsers = userAdapter.getCurrentList();
        boolean isEmpty = currentUsers == null || currentUsers.isEmpty();

        binding.progressLoadUsers.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.textLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.layoutEmpty.setVisibility(!isLoading && isEmpty ? View.VISIBLE : View.GONE);
    }

    private void updateScreenConfiguration() {
        if (screenConfigurationHost == null) {
            return;
        }
        screenConfigurationHost.updateScreenConfiguration(
                new ScreenConfiguration(
                        R.string.title_room_users,
                        true,
                        true,
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
