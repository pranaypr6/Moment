package com.moment.app.di

import com.moment.app.domain.repository.AuthRepository
import com.moment.app.domain.repository.RelationshipRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun relationshipRepository(): RelationshipRepository
    fun authRepository(): AuthRepository
    fun momentApi(): com.moment.app.data.remote.MomentApi
}
