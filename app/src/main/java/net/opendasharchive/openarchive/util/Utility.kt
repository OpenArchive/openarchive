package net.opendasharchive.openarchive.util

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import java.io.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.text.SimpleDateFormat
import java.util.*

object Utility {

    fun getMediaType(mediaPath: String): String? {
        // makes comparisons easier
        var mediaPath = mediaPath.toLowerCase()
        var result: String?
        val fileExtension = MimeTypeMap.getFileExtensionFromUrl(mediaPath)
        result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension)
        if (result == null) {
            result = if (mediaPath.endsWith("wav")) {
                "audio/wav"
            } else if (mediaPath.toLowerCase().endsWith("mp3")) {
                "audio/mpeg"
            } else if (mediaPath.endsWith("3gp")) {
                "audio/3gpp"
            } else if (mediaPath.endsWith("mp4")) {
                "video/mp4"
            } else if (mediaPath.endsWith("jpg")) {
                "image/jpeg"
            } else if (mediaPath.endsWith("png")) {
                "image/png"
            } else {
                // for imported files
                mediaPath
            }
        }
        return result
    }

    fun clearWebviewAndCookies(webview: WebView?, activity: Activity?) {
        CookieSyncManager.createInstance(activity)
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookie()
        if (webview != null) {
            webview.clearHistory()
            webview.clearCache(true)
            webview.clearFormData()
            webview.loadUrl("about:blank")
            webview.destroy()
        }
    }

    fun stringNotBlank(string: String?): Boolean {
        return string != null && string != Constants.EMPTY_STRING
    }

    fun stringArrayToCommaString(strings: Array<String>): String {
        return if (strings.isNotEmpty()) {
            val nameBuilder = StringBuilder()
            for (n in strings) {
                nameBuilder.append(n.replace("'".toRegex(), "\\\\'")).append(",")
            }
            nameBuilder.deleteCharAt(nameBuilder.length - 1)
            nameBuilder.toString()
        } else {
            Constants.EMPTY_STRING
        }
    }

    fun commaStringToStringArray(string: String?): Array<String>? {
        return string?.split(",")?.toTypedArray()
    }

    fun toastOnUiThread(activity: Activity, message: String?) {
        toastOnUiThread(activity, message, false)
    }

    fun toastOnUiThread(activity: Activity, message: String?, isLongToast: Boolean) {
        activity.runOnUiThread { Toast.makeText(activity.applicationContext, message, if (isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() }
    }

    fun toastOnUiThread(fragmentActivity: FragmentActivity, message: String?) {
        toastOnUiThread(fragmentActivity, message, false)
    }

    fun toastOnUiThread(fragmentActivity: FragmentActivity, message: String?, isLongToast: Boolean) {
        fragmentActivity.runOnUiThread { Toast.makeText(fragmentActivity.applicationContext, message, if (isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show() }
    }

    private const val TAG = "android-btxfr/Utils"
    private const val DIGEST_ALGO = "SHA1"

    fun intToByteArray(a: Int): ByteArray? {
        val ret = ByteArray(4)
        ret[3] = (a and 0xFF).toByte()
        ret[2] = (a shr 8 and 0xFF).toByte()
        ret[1] = (a shr 16 and 0xFF).toByte()
        ret[0] = (a shr 24 and 0xFF).toByte()
        return ret
    }

    fun digestMatch(imageData: ByteArray?, digestData: ByteArray?): Boolean {
        return Arrays.equals(imageData, digestData)
    }

    fun getDigest(imageData: ByteArray?): ByteArray? {
        return try {
            val messageDigest = MessageDigest.getInstance(DIGEST_ALGO)
            messageDigest.digest(imageData)
        } catch (ex: Exception) {
            Log.e(TAG, ex.toString())
            throw UnsupportedOperationException("$DIGEST_ALGO algorithm not available on this device.")
        }
    }


    fun checkDigest(digestBytes: ByteArray?, updateFile: File?): Boolean {
        val calculatedDigest = getDigest(updateFile)
        if (calculatedDigest == null) {
            Log.e(TAG, "calculatedDigest null")
            return false
        }
        return Arrays.equals(calculatedDigest, digestBytes)
    }

    fun getDigest(updateFile: File?): ByteArray? {
        val digest: MessageDigest
        digest = try {
            MessageDigest.getInstance(DIGEST_ALGO)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Exception while getting digest", e)
            return null
        }
        val `is`: InputStream
        `is` = try {
            FileInputStream(updateFile)
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Exception while getting FileInputStream", e)
            return null
        }
        val buffer = ByteArray(8192)
        var read: Int
        return try {
            while (`is`.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            digest.digest()
        } catch (e: IOException) {
            throw RuntimeException("Unable to process file for $DIGEST_ALGO", e)
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                Log.e(TAG, "Exception on closing input stream", e)
            }
        }
    }

    fun getDigest(`is`: InputStream): ByteArray? {
        val digest: MessageDigest
        digest = try {
            MessageDigest.getInstance(DIGEST_ALGO)
        } catch (e: NoSuchAlgorithmException) {
            Log.e(TAG, "Exception while getting digest", e)
            return null
        }
        val buffer = ByteArray(8192)
        var read: Int
        return try {
            while (`is`.read(buffer).also { read = it } > 0) {
                digest.update(buffer, 0, read)
            }
            digest.digest()
        } catch (e: IOException) {
            throw RuntimeException("Unable to process file for $DIGEST_ALGO", e)
        } finally {
            try {
                `is`.close()
            } catch (e: IOException) {
                Log.e(TAG, "Exception on closing input stream", e)
            }
        }
    }

    fun getMimeType(context: Context, uri: Uri?): String? {
        val cR = context.contentResolver
        val mime = MimeTypeMap.getSingleton()
        return cR.getType(uri!!)
    }

    fun getUriDisplayName(context: Context, uri: Uri?): String? {
        var result: String? = null
        val returnCursor = context.contentResolver.query(uri!!, null, null, null, null)
        /*
         * Get the column indexes of the data in the Cursor,
         * move to the first row in the Cursor, get the data,
         * and display it.
         */if (returnCursor != null) {
            val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (returnCursor.moveToFirst()) {
                //int sizeIndex = returnCursor.getColumnIndex(OpenableColumns.SIZE);
                result = returnCursor.getString(nameIndex)
            }
            returnCursor.close()
        }
        return result
    }

    fun getImageUrlWithAuthority(context: Context, uri: Uri): String? {
        var `is`: InputStream? = null
        if (uri.authority != null) {
            try {
                `is` = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(`is`)
                return writeToTempImageAndGetPathUri(context, bmp).toString()
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } finally {
                try {
                    `is`?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    fun writeToTempImageAndGetPathUri(inContext: Context, inImage: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.contentResolver, inImage, "Title", null)
        return Uri.parse(path)
    }

    fun getOutputMediaFile(prefix: String, ext: String): File? {
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS), "OpenArchive")
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File(mediaStorageDir.path + File.separator +
                prefix + "_" + timeStamp + "." + ext)
    }

    fun getOutputMediaFileByCache(context: Context, fileName: String): File? {
        val mediaStorageDir = context.cacheDir
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null
            }
        }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File(mediaStorageDir,
                "$timeStamp.$fileName")
    }

    fun writeStreamToFile(input: InputStream?, file: File?): Boolean {
        try {
            val output: OutputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024) // or other buffer size
            var read: Int
            input?.let {
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            return false
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        } finally {
            try {
                input?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return true
    }
}