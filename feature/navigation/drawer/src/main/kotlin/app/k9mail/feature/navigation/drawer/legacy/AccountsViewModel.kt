package app.k9mail.feature.navigation.drawer.legacy

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import app.k9mail.feature.navigation.drawer.domain.DomainContract.UseCase
import app.k9mail.feature.navigation.drawer.domain.entity.DisplayAccount

class AccountsViewModel(
    getDisplayAccounts: UseCase.GetDisplayAccounts,
) : ViewModel() {
    val displayAccountsLiveData: LiveData<List<DisplayAccount>> = getDisplayAccounts.execute().asLiveData()
}
