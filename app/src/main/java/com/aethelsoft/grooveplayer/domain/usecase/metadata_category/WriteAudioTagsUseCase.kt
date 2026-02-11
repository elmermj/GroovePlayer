package com.aethelsoft.grooveplayer.domain.usecase.metadata_category

import com.aethelsoft.grooveplayer.domain.model.AudioTags
import com.aethelsoft.grooveplayer.domain.repository.AudioTagRepository
import javax.inject.Inject

class WriteAudioTagsUseCase @Inject constructor(
    private val audioTagRepository: AudioTagRepository
) {
    suspend operator fun invoke(contentUri: String, tags: AudioTags): Result<Unit> =
        audioTagRepository.writeTags(contentUri, tags)
}
