package com.lukevanoort.cellarman.dagger

import javax.inject.Qualifier
import javax.inject.Scope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class AppScope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class AppContext

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class ActivityScope


@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ActivityContext

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class UIScheduler

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IOScheduler


@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class RootTaskWad
