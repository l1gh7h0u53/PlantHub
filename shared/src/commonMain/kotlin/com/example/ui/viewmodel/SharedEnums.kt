package com.example.ui.viewmodel

/** Shared navigation tabs used by FloatingBottomBar and platform hosts across KMP targets. */
enum class NavTab(val title: String) {
    HOME("Plants"),
    DETAIL("Tracker"),
    VAULT("Vault"),
    SETTINGS("Settings")
}

/** Shared application color mode selection across Android, Desktop, and iOS. */
enum class ThemeMode {
    SYSTEM, DARK, LIGHT
}

/** Shared contribution heatmap span selector used in GithubHeatmapGrid. */
enum class HeatmapViewMode {
    WEEKS_16, YEARLY, MONTHLY
}
