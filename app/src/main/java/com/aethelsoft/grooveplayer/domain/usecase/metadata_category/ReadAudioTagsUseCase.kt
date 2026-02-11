package com.aethelsoft.grooveplayer.domain.usecase.metadata_category

import com.aethelsoft.grooveplayer.domain.model.AudioTags
import com.aethelsoft.grooveplayer.domain.repository.AudioTagRepository
import javax.inject.Inject

class ReadAudioTagsUseCase @Inject constructor(
    private val audioTagRepository: AudioTagRepository
) {
    suspend operator fun invoke(contentUri: String): AudioTags? =
        audioTagRepository.readTags(contentUri)
}
