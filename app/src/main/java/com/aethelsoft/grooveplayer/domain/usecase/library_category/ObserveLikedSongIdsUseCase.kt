package com.aethelsoft.grooveplayer.domain.usecase.library_category

import com.aethelsoft.grooveplayer.domain.repository.SongLikeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveLikedSongIdsUseCase @Inject constructor(
    private val songLikeRepository: SongLikeRepository,
) {
    operator fun invoke(): Flow<Set<String>> = songLikeRepository.observeLikedSongIds()
}
