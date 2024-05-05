package com.mehedi.retrofitfiledownload.filedownload.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class DownloadFile(
    var progress: Int = 0,
    var currentFileSize: Int = 0,
    var totalFileSize: Int = 0,
) : Parcelable





