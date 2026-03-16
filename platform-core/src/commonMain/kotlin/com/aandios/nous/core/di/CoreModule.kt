package com.aandios.nous.core.di

import com.aandios.nous.api.market.NetworkManager
import com.aandios.nous.core.network.NetworkManagerImpl
import com.aandios.nous.core.plugin.ProviderLoader
import org.koin.dsl.module

val coreModule = module {
    single<NetworkManager> { NetworkManagerImpl() }
    single { get<NetworkManager>().httpClient }
    single { ProviderLoader() }
}