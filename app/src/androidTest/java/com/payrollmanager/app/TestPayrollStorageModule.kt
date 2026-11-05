package com.payrollmanager.app

import android.content.Context
import androidx.room.Room
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.payrollmanager.app.di.PayrollStorageModule
import com.payrollmanager.app.feature.payroll.data.local.PayrollDao
import com.payrollmanager.app.feature.payroll.data.local.PayrollDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/** Replaces persistent app storage with an isolated database for instrumentation tests. */
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [PayrollStorageModule::class]
)
object TestPayrollStorageModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PayrollDatabase =
        Room.inMemoryDatabaseBuilder(context, PayrollDatabase::class.java)
            .allowMainThreadQueries()
            .build()

    @Provides
    fun providePayrollDao(database: PayrollDatabase): PayrollDao = database.payrollDao()

    @Provides
    @Singleton
    fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalytics =
        FirebaseAnalytics.getInstance(context)

    @Provides
    @Singleton
    fun provideCrashlytics(): FirebaseCrashlytics = FirebaseCrashlytics.getInstance()
}
