package com.example.findmyphonebyclaplauncher.ui.aftercall

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CallLogItem(
    val id: Long,
    val name: String?,
    val number: String,
    val type: Int,
    val date: Long,
    val duration: Long,
    val photoUri: String? = null
)

class AfterCallViewModel(application: Application) : AndroidViewModel(application) {

    private val _recentCalls = MutableStateFlow<List<CallLogItem>>(emptyList())
    val recentCalls: StateFlow<List<CallLogItem>> = _recentCalls

    private val _lastCall = MutableStateFlow<CallLogItem?>(null)
    val lastCall: StateFlow<CallLogItem?> = _lastCall

    fun fetchCallLogs(targetNumber: String?) {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        viewModelScope.launch {
            // Fetch immediately so the UI populates instantly
            performFetch(context, targetNumber)

            // Android often takes 1-2 seconds to write the CallLog entry after IDLE state.
            // Delay slightly and fetch again to ensure we get the *current* call's duration.
            delay(1500)
            performFetch(context, targetNumber)
        }
    }

    private suspend fun performFetch(context: Context, targetNumber: String?) {
        val logs = withContext(Dispatchers.IO) {
            val list = mutableListOf<CallLogItem>()
            val projection = arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION
            )

            // Fetch recent 10 calls safely without SQL LIMIT
            val cursor: Cursor? = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CallLog.Calls._ID)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)

                val normalizedTarget = targetNumber?.replace(Regex("[^0-9+]"), "")
                var count = 0
                while (it.moveToNext() && count < 10) {
                    val num = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""

                    if (normalizedTarget != null) {
                        val normalizedNum = num.replace(Regex("[^0-9+]"), "")
                        if (normalizedNum != normalizedTarget) {
                            continue
                        }
                    }

                    val name = if (nameIndex != -1) it.getString(nameIndex) else null
                    val photoUri = getContactPhotoUri(context, num)
                    list.add(
                        CallLogItem(
                            id = if (idIndex != -1) it.getLong(idIndex) else 0L,
                            name = name,
                            number = num,
                            type = if (typeIndex != -1) it.getInt(typeIndex) else 0,
                            date = if (dateIndex != -1) it.getLong(dateIndex) else 0L,
                            duration = if (durationIndex != -1) it.getLong(durationIndex) else 0L,
                            photoUri = photoUri
                        )
                    )
                    count++
                }
            }
            list
        }

        _recentCalls.value = logs

        if (logs.isNotEmpty()) {
            if (targetNumber != null) {
                val match = logs.firstOrNull { it.number.replace(Regex("[^0-9+]"), "") == targetNumber.replace(Regex("[^0-9+]"), "") }
                _lastCall.value = match ?: logs.firstOrNull()
            } else {
                _lastCall.value = logs.firstOrNull()
            }
        }
    }

    @SuppressLint("Range")
    private fun getContactPhotoUri(context: Context, phoneNumber: String): String? {
        if (phoneNumber.isEmpty()) return null
        var photoUri: String? = null
        try {
            val uri = android.net.Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, android.net.Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.PHOTO_URI)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    photoUri = cursor.getString(0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return photoUri
    }

    fun formatDuration(seconds: Long): String {
        if (seconds == 0L) return "0s"
        val m = seconds / 60
        val s = seconds % 60
        return if (m > 0) "${m}m ${s}s" else "${s}s"
    }

    fun formatTime(date: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(date))
    }

    fun getCallTypeText(type: Int): String {
        return when (type) {
            CallLog.Calls.INCOMING_TYPE -> "Incoming Call"
            CallLog.Calls.OUTGOING_TYPE -> "Outgoing Call"
            CallLog.Calls.MISSED_TYPE -> "Missed Call"
            CallLog.Calls.REJECTED_TYPE -> "Rejected Call"
            else -> "Call"
        }
    }
}
