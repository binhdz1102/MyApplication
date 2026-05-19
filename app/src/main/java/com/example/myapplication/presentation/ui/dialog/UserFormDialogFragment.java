package com.example.myapplication.presentation.ui.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.example.myapplication.R;
import com.example.myapplication.databinding.DialogUserFormBinding;
import com.example.myapplication.domain.model.User;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class UserFormDialogFragment extends DialogFragment {

    public static final String TAG = "UserFormDialogFragment";
    public static final String REQUEST_KEY = "user_form_result";
    public static final String RESULT_IS_EDIT_MODE = "result_is_edit_mode";
    public static final String RESULT_USER_ID = "result_user_id";
    public static final String RESULT_USER_NAME = "result_user_name";
    public static final String RESULT_USER_AGE = "result_user_age";
    public static final String RESULT_USER_WEIGHT = "result_user_weight";

    private static final String ARG_IS_EDIT_MODE = "arg_is_edit_mode";
    private static final String ARG_USER_ID = "arg_user_id";
    private static final String ARG_USER_NAME = "arg_user_name";
    private static final String ARG_USER_AGE = "arg_user_age";
    private static final String ARG_USER_WEIGHT = "arg_user_weight";

    private DialogUserFormBinding binding;

    public static UserFormDialogFragment newAddDialog() {
        UserFormDialogFragment fragment = new UserFormDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_IS_EDIT_MODE, false);
        fragment.setArguments(arguments);
        return fragment;
    }

    public static UserFormDialogFragment newEditDialog(User user) {
        UserFormDialogFragment fragment = new UserFormDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putBoolean(ARG_IS_EDIT_MODE, true);
        arguments.putLong(ARG_USER_ID, user.getId());
        arguments.putString(ARG_USER_NAME, user.getName());
        arguments.putInt(ARG_USER_AGE, user.getAge());
        arguments.putFloat(ARG_USER_WEIGHT, user.getWeight());
        fragment.setArguments(arguments);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogUserFormBinding.inflate(getLayoutInflater());

        boolean isEditMode = requireArguments().getBoolean(ARG_IS_EDIT_MODE);
        populateFields(isEditMode);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(isEditMode ? R.string.dialog_edit_user_title : R.string.dialog_add_user_title)
                .setView(binding.getRoot())
                .setNegativeButton(R.string.action_cancel, null)
                .setPositiveButton(
                        isEditMode ? R.string.action_confirm_update : R.string.action_confirm_add,
                        null
                )
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            User user = buildUserFromInput();
            if (user == null) {
                return;
            }

            Bundle result = new Bundle();
            result.putBoolean(RESULT_IS_EDIT_MODE, isEditMode);
            result.putLong(RESULT_USER_ID, user.getId());
            result.putString(RESULT_USER_NAME, user.getName());
            result.putInt(RESULT_USER_AGE, user.getAge());
            result.putFloat(RESULT_USER_WEIGHT, user.getWeight());
            getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
            dismiss();
        }));

        return dialog;
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private void populateFields(boolean isEditMode) {
        if (!isEditMode) {
            return;
        }

        Bundle arguments = requireArguments();
        binding.editUserId.setText(String.valueOf(arguments.getLong(ARG_USER_ID)));
        binding.editUserName.setText(readString(arguments, ARG_USER_NAME));
        binding.editUserAge.setText(String.valueOf(arguments.getInt(ARG_USER_AGE)));
        binding.editUserWeight.setText(String.valueOf(arguments.getFloat(ARG_USER_WEIGHT)));
        binding.editUserId.setEnabled(false);
    }

    private User buildUserFromInput() {
        clearErrors();

        String idText = readText(binding.editUserId);
        String name = readText(binding.editUserName);
        String ageText = readText(binding.editUserAge);
        String weightText = readText(binding.editUserWeight).replace(',', '.');

        Long userId = parseLong(idText);
        Integer userAge = parseInteger(ageText);
        Float userWeight = parseFloat(weightText);

        boolean isValid = true;

        if (userId == null || userId <= 0L) {
            binding.layoutUserId.setError(getString(R.string.message_invalid_user_id));
            isValid = false;
        }

        if (name.isEmpty()) {
            binding.layoutUserName.setError(getString(R.string.message_invalid_user_name));
            isValid = false;
        }

        if (userAge == null || userAge <= 0) {
            binding.layoutUserAge.setError(getString(R.string.message_invalid_user_age));
            isValid = false;
        }

        if (userWeight == null || userWeight <= 0f) {
            binding.layoutUserWeight.setError(getString(R.string.message_invalid_user_weight));
            isValid = false;
        }

        if (!isValid || userId == null || userAge == null || userWeight == null) {
            return null;
        }

        return new User(userId, name, userAge, userWeight);
    }

    private void clearErrors() {
        binding.layoutUserId.setError(null);
        binding.layoutUserName.setError(null);
        binding.layoutUserAge.setError(null);
        binding.layoutUserWeight.setError(null);
    }

    private String readText(TextView textView) {
        Editable text = textView.getEditableText();
        return text == null ? "" : text.toString().trim();
    }

    private String readString(Bundle bundle, String key) {
        String value = bundle.getString(key);
        return value == null ? "" : value;
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Float parseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
