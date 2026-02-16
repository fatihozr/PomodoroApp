package com.fatih.pomodoroapp1.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TimerActionReceiver : BroadcastReceiver() {

    companion object {
        private var onActionListener: ((String) -> Unit)? = null

        fun setActionListener(listener: (String) -> Unit) {
            onActionListener = listener
        }

        fun clearActionListener() {
            onActionListener = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        onActionListener?.invoke(action)
    }
}