package com.mehedi.retrofitfiledownload.filedownload

import android.app.Notification.*
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Environment
import android.os.IBinder
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mehedi.retrofitfiledownload.MainActivity
import com.mehedi.retrofitfiledownload.R
import com.mehedi.retrofitfiledownload.filedownload.models.DownloadFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.pow


class DownloadService : Service() {
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager
    private var totalFileSize = 0

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        private const val NOTIFICATION_ID = 123
        const val CHANNEL_ID = "ForegroundServiceChannel"
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {


        // Perform any long-running tasks here
        onHandleDownload()
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        // If the system kills the service after onStartCommand() returns, do not recreate the service
        return START_NOT_STICKY
    }

    private fun onHandleDownload() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle("Download")
            .setContentText("Downloading File")
            .setAutoCancel(true)
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())

        initDownload()
    }

    private fun initDownload() {
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl("https://file-examples.com/storage/fe4996602366316ffa06467/2017/04/")
            .build()

        val retrofitInterface: RetrofitInterface = retrofit.create<RetrofitInterface>(
            RetrofitInterface::class.java
        )


        serviceScope.launch {
            val request: Response<ResponseBody?>? = retrofitInterface.downloadFile("https://file-examples.com/storage/fe4996602366316ffa06467/2017/04/file_example_MP4_1920_18MG.mp4")
            try {
                request?.body()?.let {
                    downloadFile(it)
                }

            } catch (e: IOException) {
                e.printStackTrace()
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
        }


    }

    @Throws(IOException::class)
    private fun downloadFile(body: ResponseBody) {
        var count: Int
        val data = ByteArray(1024 * 4)
        val fileSize: Long = body.contentLength()
        val bis: InputStream = BufferedInputStream(body.byteStream(), 1024 * 8)
        val outputFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "file.zip"
        )
        val mb: Double = 1024.0
        val output: OutputStream = FileOutputStream(outputFile)
        var total: Long = 0
        val startTime = System.currentTimeMillis()
        var timeCount = 1
        while ((bis.read(data).also { count = it }) != -1) {
            total += count.toLong()
            totalFileSize = (fileSize / (mb.pow(2.0))).toInt()
            val current = Math.round(total / (mb.pow(2.0))).toDouble()

            val progress = ((total * 100) / fileSize).toInt()

            val currentTime = System.currentTimeMillis() - startTime

            val download = DownloadFile()
            download.totalFileSize = totalFileSize

            if (currentTime > 1000 * timeCount) {
                download.currentFileSize = current.toInt()
                download.progress = progress
                sendNotification(download)
                timeCount++
            }

            output.write(data, 0, count)
        }
        onDownloadComplete()
        output.flush()
        output.close()
        bis.close()
    }

    private fun sendNotification(download: DownloadFile) {
        sendIntent(download)
        notificationBuilder.setProgress(100, download.progress, false)
        notificationBuilder.setContentText(
            String.format(
                "Downloaded (%d/%d) MB",
                download.currentFileSize,
                download.totalFileSize
            )
        )
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }


    private fun sendIntent(download: DownloadFile) {
        val intent = Intent(MainActivity.MESSAGE_PROGRESS)
        intent.putExtra("download", download)
        LocalBroadcastManager.getInstance(this@DownloadService).sendBroadcast(intent)
    }

    private fun onDownloadComplete() {
        val download = DownloadFile()
        download.progress = 100
        sendIntent(download)

        notificationManager.cancel(0)
        notificationBuilder.setProgress(0, 0, false)
        notificationBuilder.setContentText("File Downloaded")
        notificationManager.notify(0, notificationBuilder.build())
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        notificationManager.cancel(0)
    }


}
