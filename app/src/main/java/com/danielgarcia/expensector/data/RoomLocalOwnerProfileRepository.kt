package com.danielgarcia.expensector.data

import com.danielgarcia.expensector.database.LocalOwnerProfileDao
import com.danielgarcia.expensector.database.toDomain
import com.danielgarcia.expensector.database.toEntity
import com.danielgarcia.expensector.domain.LocalOwnerProfile
import com.danielgarcia.expensector.domain.LocalOwnerProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomLocalOwnerProfileRepository @Inject constructor(
    private val dao: LocalOwnerProfileDao,
) : LocalOwnerProfileRepository {
    override fun observeProfile(): Flow<LocalOwnerProfile?> =
        dao.observeProfile().map { it?.toDomain() }

    override suspend fun saveProfile(profile: LocalOwnerProfile) {
        dao.upsert(profile.toEntity())
    }
}
