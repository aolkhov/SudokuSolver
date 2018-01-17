package Sudoku

object Log {
    enum class Level { Error, Warning, Info, Debug }

    var level: Level = Level.Info

    private fun _raw_print(s: String) = kotlin.io.print(s)
    private fun _raw_println_if(lvl: Level, s: String) { if( lvl <= this.level ) println(s) }

    fun debug(s: String) = _raw_println_if(Level.Debug,   s)
    fun info(s: String)  = _raw_println_if(Level.Info,    s)
    fun warn(s: String)  = _raw_println_if(Level.Warning, s)
    fun error(s: String) = _raw_println_if(Level.Error,   s)

    fun print(s: String) = _raw_print(s)
}
