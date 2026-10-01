package com.aethelworks.grooveplayer.domain.usecase.metadata_category

import com.aethelworks.grooveplayer.domain.model.AudioTags
import com.aethelworks.grooveplayer.domain.repository.AudioTagRepository
import javax.inject.Inject

class WriteAudioTagsUseCase @Inject constructor(
    private val audioTagRepository: AudioTagRepository
) {
    suspend operator fun invoke(
        contentUri: String,
        tags: AudioTags,
        replaceFrontCover: Boolean = false,
    ): Result<Unit> = audioTagRepository.writeTags(contentUri, tags, replaceFrontCover)
}
