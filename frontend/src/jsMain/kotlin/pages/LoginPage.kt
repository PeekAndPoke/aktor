package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Nav
import de.peekandpoke.aktor.frontend.State
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.reaktor.auth.widgets.LoginWidget
import kotlinx.css.*
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.LoginPage() = comp {
    LoginPage(it)
}

class LoginPage(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {

        div {
            css {
                height = 100.vh

                backgroundImage = Image(
                    "url(https://miro.medium.com/v2/resize:fit:720/format:webp/0*m_7JMnJZnFN2338H.png)"
                )

                backgroundSize = "cover"
                backgroundPosition = RelativePosition.center
                backgroundRepeat = BackgroundRepeat.noRepeat
            }


            ui.container {
                css {
                    height = 100.vh
                    display = Display.flex
                    alignItems = Align.center
                }

                ui.stackable.grid {
                    css {
                        width = 100.pct
                    }

                    ui.eight.wide.centered.column {
                        ui.padded.segment {
                            css {
                                backgroundColor = Color.white.withAlpha(0.9)
                            }

                            ui.header { +"Login" }

                            LoginWidget(
                                state = State.auth,
                                onLoginSuccessUri = Nav.dashboard(),
                            )
                        }
                    }
                }
            }
        }
    }
}
