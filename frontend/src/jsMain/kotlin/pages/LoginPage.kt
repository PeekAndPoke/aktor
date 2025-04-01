package de.peekandpoke.aktor.frontend.pages

import de.peekandpoke.aktor.frontend.Nav
import de.peekandpoke.aktor.frontend.State
import de.peekandpoke.funktor.auth.widgets.LoginWidget
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.css.*
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.LoginPage() = comp {
    LoginPage(it)
}

class LoginPage(ctx: NoProps) : PureComponent(ctx) {

    init {
        lifecycle {
            onMount {
//                val canvas = dom?.querySelector(".background canvas") as HTMLCanvasElement
//
//                val effect = LaserCutImageEffect(
//                    imageUrl = "https://miro.medium.com/v2/resize:fit:2048/format:webp/0*m_7JMnJZnFN2338H.png",
//                    canvas = canvas,
//                )
//
//                // TODO: stop effect on unmount
//                effect.run()
            }
        }
    }

    override fun VDom.render() {

        div {
            css {
                height = 100.vh

                backgroundImage = Image(
                    "url(https://miro.medium.com/v2/resize:fit:2048/format:webp/0*m_7JMnJZnFN2338H.png)"
                )

                backgroundSize = "cover"
                backgroundPosition = RelativePosition.center
                backgroundRepeat = BackgroundRepeat.noRepeat
            }

//            div(classes = "background") {
//                css {
//                    position = Position.absolute
//                    top = 0.px
//                    bottom = 0.px
//                    left = 0.px
//                    right = 0.px
//                }
//
//                canvas {
//                    css {
//                        width = 100.pct
//                        height = 100.pct
//                    }
//                }
//            }


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

                    ui.six.wide.centered.column {
                        ui.padded.segment {
                            css {
                                opacity = 0.95
                            }

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
