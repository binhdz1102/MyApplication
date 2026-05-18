package com.example.myapplication.presentation.ui.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.example.myapplication.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class DeleteUserDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val userId = requireArguments().getLong(ARG_USER_ID)
        val userName = requireArguments().getString(ARG_USER_NAME).orEmpty()

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_user_title)
            .setMessage(getString(R.string.dialog_delete_user_message, userId, userName))
            .setNegativeButton(R.string.action_cancel, null)
            .setPositiveButton(R.string.action_confirm_delete) { _, _ ->
                val result = Bundle().apply {
                    putLong(RESULT_USER_ID, userId)
                }
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    result,
                )
            }
            .create()
    }

    companion object {
        const val TAG: String = "DeleteUserDialogFragment"
        const val REQUEST_KEY: String = "delete_user_result"
        const val RESULT_USER_ID: String = "result_user_id"

        private const val ARG_USER_ID: String = "arg_user_id"
        private const val ARG_USER_NAME: String = "arg_user_name"

        fun newInstance(userId: Long, userName: String): DeleteUserDialogFragment {
            return DeleteUserDialogFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_USER_ID, userId)
                    putString(ARG_USER_NAME, userName)
                }
            }
        }
    }
}
