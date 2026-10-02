package com.aethelworks.grooveplayer.domain.usecase.user_category

import com.aethelworks.grooveplayer.domain.repository.MusicRepository
import javax.inject.Inject

/**
 * UseCase for getting distinct folder paths that contain music (from MediaStore).
 * Used for excluded-folder suggestions in profile settings.
 */
class GetMusicFolderPathsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke(): List<String> = musicRepository.getMusicFolderPaths()
}
