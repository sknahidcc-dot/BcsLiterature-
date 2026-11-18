package com.example.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BengaliUtils {
    private val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    fun toBengaliDigits(number: Int): String = toBengaliDigits(number.toString())

    fun toBengaliDigits(number: Long): String = toBengaliDigits(number.toString())

    fun toBengaliDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(bnDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatTimestampBn(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "এখনো ব্যাকআপ নেওয়া হয়নি"
        val formatter = SimpleDateFormat("dd/MM/yyyy, hh:mm a", Locale.US)
        val formatted = formatter.format(Date(timestampMillis))
            .replace("AM", "সকাল/পূর্বাহ্ণ")
            .replace("PM", "বিকাল/অপরাহ্ণ")
        return toBengaliDigits(formatted)
    }
}
