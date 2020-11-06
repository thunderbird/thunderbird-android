package com.fsck.k9.fragment

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.koin.dsl.module

val fragmentModule = module {
    single { SortTypeToastProvider() }
    factory { LocalBroadcastManager.getInstance(get<Context>()) }
}
