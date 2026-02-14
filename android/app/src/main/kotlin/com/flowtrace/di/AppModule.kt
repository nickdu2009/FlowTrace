package com.flowtrace.di

import com.flowtrace.application.StartCaptureUseCase
import com.flowtrace.application.StopCaptureUseCase
import com.flowtrace.capture.InMemorySessionAggregator
import com.flowtrace.domain.capture.CaptureEngine
import com.flowtrace.domain.capture.SessionAggregator
import com.flowtrace.infra.sunnynet.SunnyNetCaptureEngine
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindingsModule {

  @Binds
  @Singleton
  abstract fun bindCaptureEngine(impl: SunnyNetCaptureEngine): CaptureEngine

  @Binds
  @Singleton
  abstract fun bindSessionAggregator(impl: InMemorySessionAggregator): SessionAggregator
}

@Module
@InstallIn(SingletonComponent::class)
object AppProvidesModule {

  @Provides
  @Singleton
  fun provideStartCaptureUseCase(engine: CaptureEngine): StartCaptureUseCase = StartCaptureUseCase(engine)

  @Provides
  @Singleton
  fun provideStopCaptureUseCase(engine: CaptureEngine): StopCaptureUseCase = StopCaptureUseCase(engine)
}

