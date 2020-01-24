package com.fsck.k9.ui.settings.openpgp

import android.os.Bundle
import android.view.*
import androidx.annotation.Keep
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.fsck.k9.ui.R
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.kotlinandroidextensions.ViewHolder
import kotlinx.android.synthetic.main.openpgp_manage_keys.*
import org.sufficientlysecure.keychain.daos.KeyRepository
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo
import org.sufficientlysecure.keychain.ui.keyview.GenericViewModel

@Keep // full package name referenced in openpgp_settings.xml
class OpenPgpKeysFragment : Fragment() {
    private lateinit var keyRepository: KeyRepository
    private lateinit var settingsAdapter: GroupAdapter<ViewHolder>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        keyRepository = KeyRepository.create(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.openpgp_manage_keys, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initializeList()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.openpgp_keys_option, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == R.id.openpgp_import) {
//
//        }
        return super.onOptionsItemSelected(item)
    }

    private fun initializeList() {
        settingsAdapter = GroupAdapter()
        settingsAdapter.setOnItemClickListener { item, _ ->
            handleItemClick(item)
        }

        with(openPgpKeyList) {
            adapter = settingsAdapter
            layoutManager = LinearLayoutManager(context)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val viewModel = ViewModelProviders.of(this).get(GenericViewModel::class.java)
        val liveData = viewModel.getGenericLiveData(
                requireContext()) { keyRepository.allUnifiedKeyInfoWithSecret }
        liveData.observe(this, Observer { data: List<UnifiedKeyInfo>? -> onLoadUnifiedKeyData(data!!) })
    }

    private fun handleItemClick(item: Item<*>) {

    }

    private fun onLoadUnifiedKeyData(data: List<UnifiedKeyInfo>) {
        settingsAdapter.updateAsync(data.map { OpenPgpKeyItem(it) })
    }
}