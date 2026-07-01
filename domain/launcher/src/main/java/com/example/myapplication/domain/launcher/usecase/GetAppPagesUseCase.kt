package com.example.myapplication.domain.launcher.usecase

import com.example.myapplication.core.model.AppShortcut
import com.example.myapplication.domain.launcher.repository.LauncherRepository
import javax.inject.Inject

class GetAppPagesUseCase
    @Inject
    constructor(
        private val launcherRepository: LauncherRepository,
    ) {
        suspend operator fun invoke(): List<List<AppShortcut>> = launcherRepository.getAppPages()
    }
