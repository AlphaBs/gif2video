package com.github.alphabs.gif2video

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime

class FFmpegService : Service() {

    companion object {
        const val WORKING_NOTIFICATION_ID = 100
        const val CHANNEL_ID = "ffmpeg_service"
        const val START_ACTION = "com.github.alphabs.gif2video.START_ACTION"
        const val STOP_ACTION = "com.github.alphabs.git2video.STOP_ACTION"

        var isRunning: Boolean = false
    }

    private var notification: NotificationCompat.Builder? = null

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null)
            return super.onStartCommand(intent, flags, startId)

        createNotificationChannel()
        val notificationIntent: Intent = Intent(this, MainActivity::class.java);
        val pendingIntent: PendingIntent
                = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getText(R.string.app_name))
                .setContentText(getText(R.string.working_service_text))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)

        startForeground(WORKING_NOTIFICATION_ID, notification?.build());

        if (intent.action == START_ACTION) {
            val option: FFmpegServiceOption = intent.getParcelableExtra("option")
            val th = Thread(Runnable { handleActionStart(option) })
            th.start()
        }
        else if (intent.action == STOP_ACTION) {
            isRunning = false
        }

        return START_REDELIVER_INTENT
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name);
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }

            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        Log.i("FFmpegService", "service destroy")
        isRunning = false
        super.onDestroy()
    }

    private fun handleActionStart(option: FFmpegServiceOption) {
        isRunning = true

        val tContext = this as Context
        NotificationManagerCompat.from(this).apply {

            notification?.setProgress(0, 0, false)
                    ?.setContentText("파일 불러오는 중")
            notify(WORKING_NOTIFICATION_ID, notification?.build()!!)

            val dir = File(option.targetDir)
            if (dir.isFile)
                throw Exception("targetDir was file")
            
            val files: Array<File> = dir.listFiles { file ->
                file.extension == option.targetExtension }

            val fileLength = files.size
            for (i in 0 until fileLength) {
                try {
                    if (!isRunning)
                        break

                    val item = files[i]

                    val basePath = item.parent
                    val name = item.nameWithoutExtension

                    val outTempPath = "$basePath/$name.${option.outputExtension}.temp"
                    val outPath = "$basePath/$name.${option.outputExtension}"

                    val outFile = File(outPath)
                    if (outFile.exists()) continue

                    val outTempFile = File(outTempPath)
                    if (outTempFile.exists()) {
                        outTempFile.delete()
                    }

                    // execute FFMPEG
                    val arg = getCommand(option.ffmpegArguments, item.path, outTempPath, option.outputExtension)
                    Log.i("ffmpeg target", "${item.path} => $outTempPath")
                    Log.i("ffmpeg execute", arg);

                    val rc: Int = FFmpeg.execute(arg)
                    if (rc != 0) {
                        Log.i("ffmpeg error", Integer.toString(rc))
                        Config.printLastCommandOutput(Log.INFO)
                        throw Exception("ffmpeg error : $rc")
                    }

                    // completed file
                    outTempFile.renameTo(outFile)

                    // preserve file date
                    if (option.preserveFileDate) {
                        // original file
                        val attr = Files.readAttributes(item.toPath(), BasicFileAttributes::class.java)
                        val createdAt = attr.creationTime().toMillis()

                        // set file date
                        val attributes = Files.getFileAttributeView(outFile.toPath(), BasicFileAttributeView::class.java)
                        val time = FileTime.fromMillis(createdAt)
                        attributes.setTimes(time, time, time)
                    }

                    // MediaScan to completed file
                    val scanFiles = arrayOf(
                            outFile.path,
                            item.path
                    );
                    MediaScannerConnection.scanFile(tContext, scanFiles, null
                    ) { path, uri ->
                        Log.i("ExternalStorage", "Scanned $path:")
                        Log.i("ExternalStorage", "-> uri=$uri")
                    }

                    // remove file
                    if (option.removeCompletedFile) {
                        var inFile = File(item.path)
                        inFile.delete()
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Log.e("ffmpeg error", ex.toString())
                } finally {
                    // progress
                    val progressValue = i + 1
                    notification?.setProgress(fileLength, progressValue, false)
                    notification?.setContentText("진행 중 $progressValue / $fileLength")
                    notify(WORKING_NOTIFICATION_ID, notification?.build()!!)
                }
            }

            notification?.setProgress(0,0,false)
            notification?.setContentText("완료")
            notify(WORKING_NOTIFICATION_ID, notification?.build()!!)

            isRunning = false
            stopSelf()
        }
    }

    private fun getCommand(arg: String, inPath: String, outPath: String, outext: String): String? {
        return arg.
            replaceFirst("{input}", "\"$inPath\"").
            replaceFirst("{output}", "\"$outPath\"").
            replaceFirst("{outext}", "\"$outext\"");
    }
}