package dev.sjaramillo.pedometer.util

import java.text.NumberFormat
import java.util.*

object FormatUtil {
    val numberFormat: NumberFormat
        get() = NumberFormat.getInstance(Locale.getDefault())
}