package io.peekandpoke.aktor.backend.credentials.api

import de.peekandpoke.funktor.core.broker.OutgoingConverter
import de.peekandpoke.funktor.rest.ApiRoutes
import de.peekandpoke.funktor.rest.RestAuthRuleMarker
import de.peekandpoke.funktor.rest.auth.AuthRule
import de.peekandpoke.funktor.rest.docs.codeGen
import de.peekandpoke.funktor.rest.docs.docs
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.remote.ApiAccessLevel
import de.peekandpoke.ultra.common.remote.ApiResponse
import de.peekandpoke.ultra.common.toBase64
import io.peekandpoke.aktor.backend.credentials.credentials
import io.peekandpoke.aktor.backend.credentials.db.asApiModel
import io.peekandpoke.aktor.shared.credentials.api.CredentialsApiClient
import io.peekandpoke.aktor.shared.credentials.model.GoogleOAuthFlow
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

interface UserIdAwareParam {
    fun getUserId(): String
}

/**
 * This rule checks if the calling user impersonates the [UserIdAwareParam] given in the request parameters.
 */
@RestAuthRuleMarker
fun <PARAMS : UserIdAwareParam, BODY> isOwningUser(): AuthRule<PARAMS, BODY> =
    AuthRule.forCall(
        description = "Is owning user",
        estimateFn = { ApiAccessLevel.Partial },
        checkFn = {
            user.record.userId == params.getUserId()
        }
    )


class CredentialsApi(converter: OutgoingConverter) : ApiRoutes("credentials", converter) {

    sealed interface CredentialsOAuthState {
        data class Google(
            override val state: String,
            val redirectUri: String,
            val userId: String,
            val createdAt: MpInstant,
            override val expiresAtMs: Long = createdAt.plus(5.minutes).toEpochMillis(),
            val scopes: Set<String>,
        ) : CredentialsOAuthState

        val state: String
        val expiresAtMs: Long
    }

    // TODO: store this in the database
    private object State {
        private val rnd = SecureRandom()
        private val kronos = Kronos.systemUtc

        /**
         * Minimal in-memory state store.
         * Replace with a persistent DB or session-backed store in production.
         * Maps state -> (userId, createdAt)
         */
        val oauthStateStore = ConcurrentHashMap<String, CredentialsOAuthState>()

        /** Utility: generate cryptographically secure url-safe state string */
        fun generateState(
            userId: String,
            redirectUri: String,
            scopes: Set<String>,
        ): String {
            val bytes = ByteArray(24)
            rnd.nextBytes(bytes)

            val base64 = bytes.toBase64()

            // % min ttl
            oauthStateStore[base64] = CredentialsOAuthState.Google(
                state = base64,
                redirectUri = redirectUri,
                userId = userId,
                createdAt = kronos.instantNow(),
                scopes = scopes
            )

            return base64
        }

        /**
         * Optional: utility to validate and consume state on callback.
         * Returns userId if state was valid, otherwise null.
         * Call this from your /oauth2/callback handler when Google redirects back.
         */
        inline fun <reified T : CredentialsOAuthState> consumeState(state: String): T? {
            // get entry from store
            val entry = oauthStateStore.remove(state)
                .let { it as? T }
                .takeIf { it?.state == state }
                .takeIf { (it?.expiresAtMs ?: 0) > kronos.millisNow() }

            return entry
        }
    }

    data class UserParam(
        val user: String,
    ) : UserIdAwareParam {
        override fun getUserId() = user
    }

    val list = CredentialsApiClient.List.mount(UserParam::class) {
        docs {
            name = "list"
        }.codeGen {
            funcName = "list"
        }.authorize {
            isOwningUser()
        }.handle { params ->

            val found = credentials.userCredentialsRepo.findByUserId(params.user)

            ApiResponse.ok(
                found.map { it.asApiModel() }
            )
        }
    }

    val initGoogleFlow = CredentialsApiClient.InitGoogleFlow.mount(UserParam::class) {
        docs {
            name = "initGoogleFlow"
        }.codeGen {
            funcName = "initGoogleFlow"
        }.authorize {
            isOwningUser()
        }.handle { params, body ->
            val googleOAuth = credentials.googleOAuth
                ?: return@handle ApiResponse.internalServerError<GoogleOAuthFlow.InitResponse>()
                    .withError("Google OAuth is not configured")

            val state = State
                .generateState(userId = params.user, redirectUri = body.redirectUri, scopes = body.scopes)

            val authUrl = googleOAuth
                .createAuthUrl(state = state, redirectUri = body.redirectUri, scopes = body.scopes)

            ApiResponse.ok(
                GoogleOAuthFlow.InitResponse(state = state, url = authUrl)
            )
        }
    }

    val completeGoogleFlow = CredentialsApiClient.CompleteGoogleFlow.mount(UserParam::class) {
        docs {
            name = "completeGoogleFlow"
        }.codeGen {
            funcName = "completeGoogleFlow"
        }.authorize {
            isOwningUser()
        }.handle { params, body ->
            val state = State.consumeState<CredentialsOAuthState.Google>(body.state)
                ?: return@handle ApiResponse.notFound()

            val googleOAuth = credentials.googleOAuth
                ?: return@handle ApiResponse.internalServerError<GoogleOAuthFlow.CompleteResponse>()
                    .withError("Google OAuth is not configured")

            googleOAuth.completeAuth(userId = params.user, code = body.code, redirectUri = state.redirectUri)

            ApiResponse.ok(
                GoogleOAuthFlow.CompleteResponse(
                    success = true,
                )
            )
        }
    }
}
