package com.tngdev.trackmeapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.io.FileOutputStream
import java.io.IOException


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

        fun saveBitmapToFile(bitmap: Bitmap, filename : String) {
            var out: FileOutputStream? = null
            try {
                out = FileOutputStream(filename)
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