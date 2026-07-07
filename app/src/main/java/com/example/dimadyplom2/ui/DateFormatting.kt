package com.example.dimadyplom2.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Formats a real timestamp (e.g. trip creation) with date + hour:minute in local time. */
fun formatCreatedAt(millis: Long): String {
    val formatter = SimpleDateFormat("d MMM yyyy HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

/**
 * Formats a date-only value. The Material date picker returns UTC midnight, so we
 * format in UTC to avoid an off-by-one-day shift.
 */
fun formatTripDate(millisUtc: Long): String {
    val formatter = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
    formatter.timeZone = TimeZone.getTimeZone("UTC")
    return formatter.format(Date(millisUtc))
}


