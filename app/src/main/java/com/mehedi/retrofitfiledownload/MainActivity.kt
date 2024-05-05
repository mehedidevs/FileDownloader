package com.mehedi.retrofitfiledownload

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.snackbar.Snackbar
import com.mehedi.retrofitfiledownload.databinding.ActivityMainBinding
import com.mehedi.retrofitfiledownload.filedownload.DownloadService
import com.mehedi.retrofitfiledownload.filedownload.models.DownloadFile


class MainActivity : AppCompatActivity() {

    companion object {
        const val MESSAGE_PROGRESS: String = "message_progress"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mProgressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mProgressBar = binding.progress
        mProgressText = binding.progressText

        registerReceiver()

        binding.btnDownload.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                downloadFile()
            }
        }

    }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun downloadFile() {
        if (checkPermission()) {
            startDownload()
        } else {
            requestPermission()
            requestForStoragePermissions()
        }
    }

    private fun startDownload() {
        val intent = Intent(this, DownloadService::class.java)
        startService(intent)
    }

    private fun registerReceiver() {
        val bManager: LocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(MESSAGE_PROGRESS)
        bManager.registerReceiver(broadcastReceiver, intentFilter)
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("DefaultLocale")
        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == MESSAGE_PROGRESS) {
                val download: DownloadFile =
                    intent.getParcelableExtra("download", DownloadFile::class.java)!!
                mProgressBar.progress = download.progress
                if (download.progress == 100) {
                    mProgressText.text = getString(R.string.file_download_complete)
                } else {
                    mProgressText.text = java.lang.String.format(
                        "Downloaded (%d/%d) MB",
                        download.currentFileSize,
                        download.totalFileSize
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkPermission(): Boolean {
        val result: Int = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        )
        return result == PackageManager.PERMISSION_GRANTED
    }


    private fun requestForStoragePermissions() {
        //Android is 11 (R) or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                val uri = Uri.fromParts("package", this.packageName, null)
                intent.setData(uri)
                storageActivityResultLauncher.launch(intent)
            } catch (e: Exception) {
                val intent = Intent()
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storageActivityResultLauncher.launch(intent)
            }
        } else {
            //Below android 11
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private val storageActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                //Android is 11 (R) or above
                if (Environment.isExternalStorageManager()) {
                    //Manage External Storage Permissions Granted
                    Log.d("TAG", "onActivityResult: Manage External Storage Permissions Granted")
                } else {
                    Toast.makeText(
                        this@MainActivity,
                        "Storage Permissions Denied",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                //Below android 11
            }
        }


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun requestPermission() {
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)


    }


    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startDownload()
        } else {
            Snackbar.make(
                findViewById(R.id.main),
                "Permission Denied, Please allow to proceed !",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }


}