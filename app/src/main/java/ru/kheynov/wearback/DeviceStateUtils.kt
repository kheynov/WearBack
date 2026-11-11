package ru.kheynov.wearback

import android.app.KeyguardManager
import android.content.Context
import android.os.PowerManager

internal fun Context.isReadyForBack(): Boolean =
    getSystemService(PowerManager::class.java).isInteractive &&
        !getSystemService(KeyguardManager::class.java).isKeyguardLocked
