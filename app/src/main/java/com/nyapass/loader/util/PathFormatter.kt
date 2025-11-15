package com.nyapass.loader.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

object PathFormatter {
    fun formatForDisplay(context: Context, rawPath: String?): String? {
        if (rawPath.isNullOrBlank()) return null
        if (!rawPath.startsWith("content://")) return rawPath
        val uri = Uri.parse(rawPath)
        val treeName = DocumentFile.fromTreeUri(context, uri)?.name
        if (!treeName.isNullOrBlank()) {
            return treeName
        }
        val singleName = DocumentFile.fromSingleUri(context, uri)?.name
        return singleName ?: rawPath
    }
}
