package app.k9mail.feature.settings.import.ui

import android.app.Dialog
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.k9mail.feature.settings.importing.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import com.fsck.k9.ui.base.R as BaseR

/**
 * A dialog that allows the user to pick an app from which to import settings/accounts.
 */
internal class PickAppDialogFragment : DialogFragment() {
    private val viewModel: PickAppViewModel by viewModel()

    private var selectedPackageName: String? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = PickAppAdapter(requireContext())

        viewModel.appInfoFlow.observe(this) { appInfoList ->
            adapter.clear()
            adapter.addAll(appInfoList)
            adapter.notifyDataSetChanged()
        }

        val clickListener = OnClickListener { _, index ->
            val packageName = adapter.getItem(index)?.packageName ?: return@OnClickListener
            selectedPackageName = packageName
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_import_pick_app_dialog_title)
            .setAdapter(adapter, clickListener)
            .setNegativeButton(BaseR.string.cancel_action, null)
            .create()
    }

    override fun onDismiss(dialog: DialogInterface) {
        setFragmentResult(FRAGMENT_RESULT_KEY, bundleOf(FRAGMENT_RESULT_APP to selectedPackageName))
        super.onDismiss(dialog)
    }

    companion object {
        const val FRAGMENT_RESULT_KEY = "pickApp"
        const val FRAGMENT_RESULT_APP = "packageName"
    }
}

private fun <T> Flow<T>.observe(lifecycleOwner: LifecycleOwner, collector: FlowCollector<T>) {
    lifecycleOwner.lifecycleScope.launch {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect(collector)
        }
    }
}
