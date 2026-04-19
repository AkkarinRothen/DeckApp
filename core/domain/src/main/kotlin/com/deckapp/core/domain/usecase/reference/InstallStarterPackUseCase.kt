package com.deckapp.core.domain.usecase.reference

import com.deckapp.core.domain.repository.ReferenceRepository
import com.deckapp.core.model.backup.FullBackupDto
import javax.inject.Inject

class InstallStarterPackUseCase @Inject constructor(
    private val repository: ReferenceRepository
) {
    suspend operator fun invoke(pack: FullBackupDto, packName: String) {
        repository.importStarterPack(pack, packName)
    }
}
