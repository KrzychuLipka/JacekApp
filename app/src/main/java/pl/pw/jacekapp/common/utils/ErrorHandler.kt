package pl.pw.jacekapp.common.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import javax.inject.Inject

class ErrorHandler @Inject constructor(
    private val context: Context,
) {

    fun handleError(
        @StringRes errorMessageResId: Int,
        tag: String? = null,
        technicalMessage: String? = null,
    ) {
        technicalMessage?.let { Log.e(tag, it) }
        val errorMessage = context.getString(errorMessageResId)
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }
}
