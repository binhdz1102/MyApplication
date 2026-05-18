package com.example.myapplication.presentation.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R
import com.example.myapplication.databinding.DialogUserFormBinding
import com.example.myapplication.domain.model.User
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class UserFormDialogFragment : DialogFragment() {

    private var _binding: DialogUserFormBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogUserFormBinding.inflate(layoutInflater)

        val isEditMode = requireArguments().getBoolean(ARG_IS_EDIT_MODE)
        populateFields(isEditMode)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isEditMode) R.string.dialog_edit_user_title else R.string.dialog_add_user_title)
            .setView(binding.root)
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(
                if (isEditMode) R.string.action_confirm_update else R.string.action_confirm_add,
                null,
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val user = buildUserFromInput() ?: return@setOnClickListener
                val result = Bundle().apply {
                    putBoolean(RESULT_IS_EDIT_MODE, isEditMode)
                    putLong(RESULT_USER_ID, user.id)
                    putString(RESULT_USER_NAME, user.name)
                    putInt(RESULT_USER_AGE, user.age)
                    putFloat(RESULT_USER_WEIGHT, user.weight)
                }
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    result,
                )
                dismiss()
            }
        }

        return dialog
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun populateFields(isEditMode: Boolean) {
        if (!isEditMode) {
            return
        }

        val arguments = requireArguments()
        binding.editUserId.setText(arguments.getLong(ARG_USER_ID).toString())
        binding.editUserName.setText(arguments.getString(ARG_USER_NAME).orEmpty())
        binding.editUserAge.setText(arguments.getInt(ARG_USER_AGE).toString())
        binding.editUserWeight.setText(arguments.getFloat(ARG_USER_WEIGHT).toString())
        binding.editUserId.isEnabled = false
    }

    private fun buildUserFromInput(): User? {
        clearErrors()

        val idText = binding.editUserId.text?.toString().orEmpty().trim()
        val name = binding.editUserName.text?.toString().orEmpty().trim()
        val ageText = binding.editUserAge.text?.toString().orEmpty().trim()
        val weightText = binding.editUserWeight.text?.toString().orEmpty().trim().replace(',', '.')

        val userId = idText.toLongOrNull()
        val userAge = ageText.toIntOrNull()
        val userWeight = weightText.toFloatOrNull()

        var isValid = true

        if (userId == null || userId <= 0L) {
            binding.layoutUserId.error = getString(R.string.message_invalid_user_id)
            isValid = false
        }

        if (name.isBlank()) {
            binding.layoutUserName.error = getString(R.string.message_invalid_user_name)
            isValid = false
        }

        if (userAge == null || userAge <= 0) {
            binding.layoutUserAge.error = getString(R.string.message_invalid_user_age)
            isValid = false
        }

        if (userWeight == null || userWeight <= 0f) {
            binding.layoutUserWeight.error = getString(R.string.message_invalid_user_weight)
            isValid = false
        }

        if (!isValid || userId == null || userAge == null || userWeight == null) {
            return null
        }

        return User(
            id = userId,
            name = name,
            age = userAge,
            weight = userWeight,
        )
    }

    private fun clearErrors() {
        binding.layoutUserId.error = null
        binding.layoutUserName.error = null
        binding.layoutUserAge.error = null
        binding.layoutUserWeight.error = null
    }

    companion object {
        const val TAG: String = "UserFormDialogFragment"
        const val REQUEST_KEY: String = "user_form_result"
        const val RESULT_IS_EDIT_MODE: String = "result_is_edit_mode"
        const val RESULT_USER_ID: String = "result_user_id"
        const val RESULT_USER_NAME: String = "result_user_name"
        const val RESULT_USER_AGE: String = "result_user_age"
        const val RESULT_USER_WEIGHT: String = "result_user_weight"

        private const val ARG_IS_EDIT_MODE: String = "arg_is_edit_mode"
        private const val ARG_USER_ID: String = "arg_user_id"
        private const val ARG_USER_NAME: String = "arg_user_name"
        private const val ARG_USER_AGE: String = "arg_user_age"
        private const val ARG_USER_WEIGHT: String = "arg_user_weight"

        fun newAddDialog(): UserFormDialogFragment {
            return UserFormDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_EDIT_MODE, false)
                }
            }
        }

        fun newEditDialog(user: User): UserFormDialogFragment {
            return UserFormDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_EDIT_MODE, true)
                    putLong(ARG_USER_ID, user.id)
                    putString(ARG_USER_NAME, user.name)
                    putInt(ARG_USER_AGE, user.age)
                    putFloat(ARG_USER_WEIGHT, user.weight)
                }
            }
        }
    }
}
