package ar.edu.utn.frba.expendinator.screens.expenses

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.edu.utn.frba.expendinator.data.remote.ApiClient
import ar.edu.utn.frba.expendinator.model.dto.*
import io.ktor.client.call.body
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.ByteArrayOutputStream
import kotlin.math.max

sealed class OcrUiState {
    object Idle : OcrUiState()
    object Loading : OcrUiState()
    data class Preview(val data: OcrPreview, val editable: SnapshotStateList<OcrItem>) : OcrUiState()
    object Confirming : OcrUiState()
    object Confirmed : OcrUiState()
    data class Error(val message: String) : OcrUiState()
}

class OcrViewModel : ViewModel() {
    private val _ui = MutableStateFlow<OcrUiState>(OcrUiState.Idle)
    val ui = _ui.asStateFlow()

    private val client = ApiClient.client
    private val baseUrl = ApiClient.baseUrl

    /** Foto recién sacada, esperando a que la pantalla de revisión la procese. */
    private var pendingPhoto: Uri? = null

    fun setPendingPhoto(uri: Uri) {
        pendingPhoto = uri
    }

    /** Si hay una foto pendiente la manda a escanear; si no, usa el mock (útil para probar sin Azure). */
    fun startScan(context: Context) {
        val uri = pendingPhoto
        pendingPhoto = null
        if (uri != null) scanPhoto(context.applicationContext, uri) else loadMock()
    }

    private fun scanPhoto(context: Context, uri: Uri) {
        viewModelScope.launch {
            _ui.value = OcrUiState.Loading
            try {
                val jpeg = withContext(Dispatchers.IO) { prepareImage(context, uri) }

                val resp = client.submitFormWithBinaryData(
                    url = "$baseUrl/ocr/parse",
                    formData = formData {
                        append("image", jpeg, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=\"ticket.jpg\"")
                        })
                    }
                )
                if (!resp.status.isSuccess()) {
                    _ui.value = OcrUiState.Error(serverError(resp))
                    return@launch
                }
                showPreview(resp.body())
            } catch (e: Exception) {
                _ui.value = OcrUiState.Error(e.message ?: "Network error")
            } finally {
                // La foto temporal ya no hace falta
                runCatching { context.contentResolver.delete(uri, null, null) }
            }
        }
    }

    /**
     * Decodifica la foto respetando la orientación EXIF (ImageDecoder lo hace solo),
     * la achica a 2000 px en el lado más largo y la comprime a JPEG.
     * Así subimos ~300-600 KB en vez de varios MB, sin perder legibilidad del ticket.
     */
    private fun prepareImage(context: Context, uri: Uri): ByteArray {
        val maxSide = 2000
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val longest = max(info.size.width, info.size.height)
            if (longest > maxSide) {
                val scale = maxSide.toFloat() / longest
                decoder.setTargetSize(
                    (info.size.width * scale).toInt(),
                    (info.size.height * scale).toInt()
                )
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }

    private suspend fun serverError(resp: HttpResponse): String {
        val fallback = "Scan failed (${resp.status.value})"
        return runCatching {
            Json.parseToJsonElement(resp.bodyAsText()).jsonObject["error"]?.jsonPrimitive?.content
        }.getOrNull() ?: fallback
    }

    private fun showPreview(preview: OcrPreview) {
        val editable = mutableStateListOf<OcrItem>().apply {
            addAll(preview.items.map { it.copy() })
        }
        _ui.value = OcrUiState.Preview(data = preview, editable = editable)
    }

    fun loadMock() {
        viewModelScope.launch {
            _ui.value = OcrUiState.Loading
            try {
                val resp = client.get("$baseUrl/ocr/mock")
                if (!resp.status.isSuccess()) {
                    _ui.value = OcrUiState.Error("Mock OCR failed (${resp.status.value})")
                    return@launch
                }
                showPreview(resp.body())
            } catch (e: Exception) {
                _ui.value = OcrUiState.Error(e.message ?: "Network error")
            }
        }
    }

    fun confirm(onDone: () -> Unit) {
        val current = _ui.value
        if (current !is OcrUiState.Preview) return

        viewModelScope.launch {
            _ui.value = OcrUiState.Confirming
            try {
                val resp = client.post("$baseUrl/ocr/confirm") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        OcrConfirmRequest(
                            items = current.editable.toList(),
                            receiptId = current.data.receiptId
                        )
                    )
                }
                if (!resp.status.isSuccess()) {
                    _ui.value = OcrUiState.Error("Confirm failed (${resp.status.value})")
                    return@launch
                }
                _ui.value = OcrUiState.Confirmed
                onDone()
            } catch (e: Exception) {
                _ui.value = OcrUiState.Error(e.message ?: "Network error")
            }
        }
    }
}
