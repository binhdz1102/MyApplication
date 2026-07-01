package com.example.myapplication.domain.launcher.repository

import com.example.myapplication.core.model.DateCardInfo
import kotlinx.coroutines.flow.Flow

interface DateRepository {
    fun observeDateCard(): Flow<DateCardInfo>
}
