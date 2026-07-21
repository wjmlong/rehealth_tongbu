package com.rehealth.genie.network

import java.io.IOException

/**
 * Sealed hierarchy describing exactly why a remote PHM call failed, so the caller
 * (RemotePhmService) can decide whether to retry and whether to fall back to the
 * local mock service. Never carries raw user health data.
 */
sealed class RemotePhmError(open val eventName: String, open val message: String) {
    data class BackendUnavailable(override val message: String) :
        RemotePhmError("backend_unavailable", message)

    data class Timeout(override val message: String) :
        RemotePhmError("timeout", message)

    data class HttpStatusError(val code: Int, override val message: String) :
        RemotePhmError("http_status", message)

    data class ModelServiceUnavailable(override val message: String) :
        RemotePhmError("model_service_unavailable", message)

    data class InvalidDto(override val message: String) :
        RemotePhmError("invalid_dto", message)

    data class MissingFeatureFields(val fields: List<String>, override val message: String) :
        RemotePhmError("missing_feature_fields", message)

    data class Unknown(override val message: String) :
        RemotePhmError("unknown", message)
}

/** True when [throwable] is an OkHttp/Retrofit connect/read/write timeout. */
fun isTimeout(throwable: Throwable?): Boolean {
    if (throwable == null) return false
    return runCatching {
        val className = throwable.javaClass.name
        className.endsWith(".SocketTimeoutException") || className.endsWith(".TimeoutException")
    }.getOrDefault(false)
}

/** Maps arbitrary Retrofit/network failures into a [RemotePhmError]. */
fun Throwable.toRemotePhmError(): RemotePhmError = when (this) {
    is RemotePhmError -> this
    else -> when {
        isTimeout(this) -> RemotePhmError.Timeout(message ?: "Network timeout while contacting backend.")
        this is IOException -> RemotePhmError.BackendUnavailable(message ?: "Backend is unavailable.")
        else -> RemotePhmError.Unknown(message ?: "Unexpected remote PHM failure.")
    }
}

/** Known JeecgBoot/ReHealth backend error codes reported inside the 200 envelope. */
internal object ReHealthErrorCodes {
    const val MODEL_SERVICE_CONFIG_INVALID = 55001
    const val MODEL_SERVICE_UNAVAILABLE = 55002
    const val MODEL_SERVICE_INVALID_RESPONSE = 55003
    const val MODEL_CONTRACT_VIOLATION = 55004
}
