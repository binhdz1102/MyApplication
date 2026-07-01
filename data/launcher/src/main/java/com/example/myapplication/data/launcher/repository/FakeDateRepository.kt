package com.example.myapplication.data.launcher.repository

import com.example.myapplication.core.model.DateCardInfo
import com.example.myapplication.domain.launcher.repository.DateRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDateRepository
    @Inject
    constructor() : DateRepository {
        private val dayFormatter = DateTimeFormatter.ofPattern("EEEE")
        private val dateFormatter = DateTimeFormatter.ofPattern("dd")
        private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        private val fullDateFormatter = DateTimeFormatter.ofPattern("dd MMM")

        override fun observeDateCard(): Flow<DateCardInfo> =
            flow {
                while (currentCoroutineContext().isActive) {
                    val now = ZonedDateTime.now()
                    emit(
                        DateCardInfo(
                            dayOfWeek = now.format(dayFormatter),
                            dayOfMonth = now.format(dateFormatter),
                            monthYear = now.format(monthYearFormatter),
                            fullDateLabel = now.format(fullDateFormatter),
                        ),
                    )
                    delay(60_000L)
                }
            }
    }
