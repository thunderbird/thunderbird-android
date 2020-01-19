package com.fsck.k9.ui.addaccount

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fsck.k9.jmap.R
import com.fsck.k9.jmap.databinding.FragmentAddAccountBinding
import com.fsck.k9.ui.observeNotNull
import org.koin.androidx.viewmodel.ext.android.viewModel

class AddAccountFragment : Fragment() {
    private val viewModel: AddAccountViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.getActionEvents().observeNotNull(this) { handleActionEvents(it) }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentAddAccountBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    private fun handleActionEvents(action: Action) {
        when (action) {
            is Action.GoToMessageList -> goToMessageList()
        }
    }

    private fun goToMessageList() {
        findNavController().navigate(R.id.action_addJmapAccountScreen_to_messageListScreen)
    }
}
