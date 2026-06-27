package com.example.myapplication.presentation.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.myapplication.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DeleteUserDialogFragment extends DialogFragment {

    public static final String TAG = "DeleteUserDialogFragment";
    public static final String REQUEST_KEY = "delete_user_result";
    public static final String RESULT_USER_ID = "result_user_id";

    private static final String ARG_USER_ID = "arg_user_id";
    private static final String ARG_USER_NAME = "arg_user_name";

    public static DeleteUserDialogFragment newInstance(long userId, String userName) {
        DeleteUserDialogFragment fragment = new DeleteUserDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_USER_ID, userId);
        arguments.putString(ARG_USER_NAME, userName);
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle arguments = requireArguments();
        long userId = arguments.getLong(ARG_USER_ID);
        String userName = arguments.getString(ARG_USER_NAME);
        if (userName == null) {
            userName = "";
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dialog_delete_user_title)
                .setMessage(getString(R.string.dialog_delete_user_message, userId, userName))
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(R.string.action_confirm_delete, (dialog, which) -> {
                    Bundle result = new Bundle();
                    result.putLong(RESULT_USER_ID, userId);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                })
                .create();
    }
}
