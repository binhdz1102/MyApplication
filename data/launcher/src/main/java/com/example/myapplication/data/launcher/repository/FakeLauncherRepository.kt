package com.example.myapplication.data.launcher.repository

import com.example.myapplication.core.model.AppShortcut
import com.example.myapplication.core.model.NavigationCardInfo
import com.example.myapplication.domain.launcher.repository.LauncherRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeLauncherRepository
    @Inject
    constructor() : LauncherRepository {
        override suspend fun getNavigationCardInfo(): NavigationCardInfo =
            NavigationCardInfo(
                destination = "to Millennium",
                etaLabel = "16 min",
                distanceLabel = "8.05 km",
                guidanceLabel = "Turn right onto Riverside Drive",
            )

        override suspend fun getAppPages(): List<List<AppShortcut>> {
            val apps =
                listOf(
                    AppShortcut("maps", "Maps", android.R.drawable.ic_dialog_map),
                    AppShortcut("music", "Music", android.R.drawable.ic_media_play),
                    AppShortcut("call", "Phone", android.R.drawable.ic_menu_call),
                    AppShortcut("messages", "Messages", android.R.drawable.ic_dialog_email),
                    AppShortcut("settings", "Settings", android.R.drawable.ic_menu_manage),
                    AppShortcut("calendar", "Calendar", android.R.drawable.ic_menu_my_calendar),
                    AppShortcut("podcasts", "Podcasts", android.R.drawable.ic_btn_speak_now),
                    AppShortcut("camera", "Camera", android.R.drawable.ic_menu_camera),
                    AppShortcut("browser", "Browser", android.R.drawable.ic_menu_search),
                    AppShortcut("charging", "Charge", android.R.drawable.ic_lock_idle_charging),
                    AppShortcut("assistant", "Assistant", android.R.drawable.ic_dialog_info),
                    AppShortcut("garage", "Garage", android.R.drawable.ic_menu_compass),
                    AppShortcut("news", "News", android.R.drawable.ic_dialog_dialer),
                    AppShortcut("contacts", "Contacts", android.R.drawable.ic_menu_myplaces),
                    AppShortcut("video", "Video", android.R.drawable.ic_media_ff),
                    AppShortcut("climate", "Climate", android.R.drawable.ic_menu_day),
                    AppShortcut("trip", "Trip", android.R.drawable.ic_menu_directions),
                    AppShortcut("energy", "Energy", android.R.drawable.ic_lock_idle_low_battery),
                    AppShortcut("files", "Files", android.R.drawable.ic_menu_upload),
                    AppShortcut("radio", "Radio", android.R.drawable.ic_lock_silent_mode_off),
                    AppShortcut("store", "Store", android.R.drawable.ic_menu_share),
                    AppShortcut("service", "Service", android.R.drawable.ic_popup_sync),
                    AppShortcut("profile", "Profile", android.R.drawable.ic_menu_edit),
                    AppShortcut("apps", "Apps", android.R.drawable.ic_menu_agenda),
                )

            return apps.chunked(8)
        }
    }
