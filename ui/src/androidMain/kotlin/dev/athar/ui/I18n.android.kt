package dev.athar.ui

actual fun systemLanguageCode(): String = java.util.Locale.getDefault().language
