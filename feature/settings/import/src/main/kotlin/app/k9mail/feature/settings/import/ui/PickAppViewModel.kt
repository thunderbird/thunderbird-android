package app.k9mail.feature.settings.import.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class PickAppViewModel(
    private val importAppFetcher: ImportAppFetcher,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val _appInfoFlow = MutableStateFlow<List<AppInfo>>(emptyList())
    val appInfoFlow: StateFlow<List<AppInfo>> = _appInfoFlow

    init {
        fetchImportApps()
    }

    private fun fetchImportApps() {
        viewModelScope.launch(backgroundDispatcher) {
            val appInfoList = importAppFetcher.getAppInfoList()
            _appInfoFlow.emit(appInfoList)
        }
    }
}
