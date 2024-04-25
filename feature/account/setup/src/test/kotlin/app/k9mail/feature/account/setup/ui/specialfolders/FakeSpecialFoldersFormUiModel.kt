package app.k9mail.feature.account.setup.ui.specialfolders

import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormEvent
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormState
import app.k9mail.feature.account.setup.ui.specialfolders.SpecialFoldersContract.FormUiModel

class FakeSpecialFoldersFormUiModel : FormUiModel {

    val events = mutableListOf<FormEvent>()

    override fun event(
        event: FormEvent,
        formState: FormState,
    ): FormState {
        events.add(event)
        return formState
    }
}
