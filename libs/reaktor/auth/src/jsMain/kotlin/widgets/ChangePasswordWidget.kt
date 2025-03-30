package io.peekandpoke.reaktor.auth.widgets

import de.peekandpoke.kraft.addons.forms.formController
import de.peekandpoke.kraft.addons.forms.validation.given
import de.peekandpoke.kraft.addons.forms.validation.strings.notEmpty
import de.peekandpoke.kraft.addons.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onSubmit
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.reaktor.auth.AuthState
import io.peekandpoke.reaktor.auth.model.AuthProviderModel
import io.peekandpoke.reaktor.auth.model.AuthUpdateRequest
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun <USER> Tag.ChangePasswordWidget(
    state: AuthState<USER>,
) = comp(
    ChangePasswordWidget.Props(state = state)
) {
    ChangePasswordWidget(it)
}

class ChangePasswordWidget<USER>(ctx: Ctx<Props<USER>>) : Component<ChangePasswordWidget.Props<USER>>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props<USER>(
        val state: AuthState<USER>,
    )

    private sealed interface State {
        data object Error : State
        data object Edit : State
        data object Done : State
    }

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth get() = props.state
    private val policy get() = auth.getPasswordPolicy()

    private val provider get() = auth().realm?.providers?.first { it.type == AuthProviderModel.TYPE_EMAIL_PASSWORD }
    private val userId get() = auth().tokenUserId

    private var oldPassword by value("")
    private var newPassword by value("")

    private var state: State by value(
        when (provider?.type) {
            AuthProviderModel.TYPE_EMAIL_PASSWORD -> State.Edit
            else -> State.Error
        }
    )

    private val formCtrl = formController()
    private val noDblClick = doubleClickProtection()

    private suspend fun updatePassword() = noDblClick.runBlocking {

        val result = auth.requestAuthUpdate(
            AuthUpdateRequest.SetPassword(
                provider = provider?.id ?: "",
                userId = userId ?: "",
                oldPassword = oldPassword,
                newPassword = newPassword,
            )
        )

        if (result) {
            state = State.Done
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        when (state) {
            is State.Error -> renderError()
            is State.Edit -> renderEdit()
            is State.Done -> renderDone()
        }
    }

    private fun FlowContent.renderError() {
        ui.error.message {
            +"Password update not possible."
        }
    }

    private fun FlowContent.renderDone() {
        ui.success.message {
            +"Password updated successfully."
        }
    }

    private fun FlowContent.renderEdit() {
        ui.form Form {
            onSubmit { evt ->
                evt.preventDefault()

                if (formCtrl.validate()) {
                    launch {
                        updatePassword()
                    }
                }
            }

            UiPasswordField(::oldPassword) {
                label("Current Password")
                revealPasswordIcon()

                accepts(notEmpty())
            }
            UiPasswordField(::newPassword) {
                label("New Password")
                revealPasswordIcon()

                accepts(
                    notEmpty(),
                    given(
                        check = { policy.matches(it) },
                        message = { policy.description },
                    )
                )
            }

            ui.basic.primary.fluid
                .givenNot(noDblClick.canRun) { loading }
                .givenNot(formCtrl.isValid) { disabled }
                .button Submit {
                +"Update Password"
            }
        }
    }
}
