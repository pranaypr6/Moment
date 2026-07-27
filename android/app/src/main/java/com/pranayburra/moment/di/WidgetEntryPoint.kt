package com.pranayburra.moment.di

import com.pranayburra.moment.domain.repository.AuthRepository
import com.pranayburra.moment.domain.repository.RelationshipRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun relationshipRepository(): RelationshipRepository
    fun authRepository(): AuthRepository
    fun momentApi(): com.pranayburra.moment.data.remote.MomentApi
}
