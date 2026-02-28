package pe.nanamochi.banchus.components

data class ReplayFrame(
    var buttonState: Int = 0,
    var taikoByte: Int = 0,
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var time: Int = 0,
)
