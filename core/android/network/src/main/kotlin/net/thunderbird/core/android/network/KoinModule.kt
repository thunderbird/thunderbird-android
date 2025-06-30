package net.thunderbird.core.android.network

import android.content.Context
import org.koin.dsl.module
import android.net.ConnectivityManager as SystemConnectivityManager

val coreAndroidNetworkModule = module {
    single { get<Context>().getSystemService(Context.CONNECTIVITY_SERVICE) as SystemConnectivityManager }
    single { ConnectivityManager(systemConnectivityManager = get()) }
}
