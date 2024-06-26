package ru.vaihdass.aikataulus.data.di

import dagger.Module
import ru.vaihdass.aikataulus.data.auth.AuthModule
import ru.vaihdass.aikataulus.data.local.di.DatabaseModule
import ru.vaihdass.aikataulus.data.local.di.SharedPreferencesModule
import ru.vaihdass.aikataulus.data.remote.di.NetworkModule

@Module(
    includes = [
        NetworkModule::class,
        DatabaseModule::class,
        SharedPreferencesModule::class,
        DataModuleBinder::class,
        AuthModule::class,
    ]
)
class DataModule {}