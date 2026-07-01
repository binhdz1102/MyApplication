package com.example.myapplication.domain.launcher.repository

import com.example.myapplication.core.model.AppShortcut
import com.example.myapplication.core.model.NavigationCardInfo

interface LauncherRepository {
    suspend fun getNavigationCardInfo(): NavigationCardInfo

    suspend fun getAppPages(): List<List<AppShortcut>>
}
