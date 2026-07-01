package com.example.myapplication.domain.launcher.usecase

import com.example.myapplication.core.model.NavigationCardInfo
import com.example.myapplication.domain.launcher.repository.LauncherRepository
import javax.inject.Inject

class GetNavigationCardInfoUseCase
    @Inject
    constructor(
        private val launcherRepository: LauncherRepository,
    ) {
        suspend operator fun invoke(): NavigationCardInfo = launcherRepository.getNavigationCardInfo()
    }
