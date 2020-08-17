package com.tngdev.trackmeapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class Utils {

    companion object {
        @JvmStatic
        fun convertMpsToKmh(speed : Double) : Double {
            return speed * 18.0 / 5
        }

        fun convertDurationToString(duration : Long) : String {
            val hour = duration / 3600
            val minute = duration.rem(3600) / 60
            val second = duration.rem(60)

            return String.format("%02d:%02d:%02d", hour, minute, second)
        }

        fun dateToString(time: Long) : String {
            val format = SimpleDateFormat("MMMM dd, yyyy' at 'hh:mm aa", Locale.getDefault())
            return format.format(Date(time))
        }

        fun formatToYesterdayOrToday(time: Long): String? {
            val calendar = Calendar.getInstance()
            calendar.time = Date(time)
            val today = Calendar.getInstance()
            val yesterday = Calendar.getInstance()
            yesterday.add(Calendar.DATE, -1)
            val timeFormatter = SimpleDateFormat("'at 'hh:mm aa", Locale.getDefault())
            return if (calendar[Calendar.YEAR] == today[Calendar.YEAR]
                && calendar[Calendar.DAY_OF_YEAR] == today[Calendar.DAY_OF_YEAR]
            ) {
                "Today " + timeFormatter.format(calendar.time)
            } else if (calendar[Calendar.YEAR] == yesterday[Calendar.YEAR] && calendar[Calendar.DAY_OF_YEAR] == yesterday[Calendar.DAY_OF_YEAR]
            ) {
                "Yesterday " + timeFormatter.format(calendar.time)
            } else {
                dateToString(time)
            }
        }

        fun dateToStringWithSecond(time: Long) : String {
            val format = SimpleDateFormat("dd-mm-yyyy hh:mm:ss", Locale.getDefault())
            return format.format(Date(time))
        }

        fun getPeriodOfTime(time: Long): String {
            val calendar = Calendar.getInstance()
            calendar.time = Date(time)
            return when (calendar.get(Calendar.HOUR_OF_DAY)) {
                in 0..11 -> "Morning"
                in 12..15 -> "Afternoon"
                in 16..20 -> "Evening"
                in 21..23 -> "Night"
                else -> ""
            }
        }

        fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
            val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
            vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
            val bitmap = Bitmap.createBitmap(vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            vectorDrawable.draw(canvas)
            return BitmapDescriptorFactory.fromBitmap(bitmap)
        }

        fun bitmapFromDrawable(context: Context, drawableId : Int): Bitmap? {
            val drawable = ContextCompat.getDrawable(context, drawableId)
            drawable ?.let {
                val canvas = Canvas()
                val bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888)
                canvas.setBitmap(bitmap)
                drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight())
                drawable.draw(canvas)

                return bitmap
            }

            return null
        }

        fun saveBitmapToFile(fileName: String, bitmap: Bitmap, context: Context) : String? {

            val internalFile = context.filesDir
            val imageFile = File(internalFile?.path + File.separator + "$fileName.png")
            saveBitmapToFile(bitmap, imageFile.path)

            return imageFile.path
        }

        fun saveBitmapToFile(bitmap: Bitmap, filePath : String) {
            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(filePath)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
                // PNG is a lossless format, the compression factor (100) is ignored
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    out?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
     }
}