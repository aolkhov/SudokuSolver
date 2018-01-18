import Sudoku.InputFormatException
import Sudoku.SudokuMatrix

import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldThrow
import io.kotlintest.specs.StringSpec

val uncertain4: Set<Int> = setOf(1,2,3,4)
val uncertain9: Set<Int> = setOf(1,2,3,4,5,6,7,8,9)

val input2x2 = """
    # lines starting with '#' should be ignored
    2

    1 2  34        # this text shouild also be ignored
    ..   1 .
    ....
    . .. .
    """.trimIndent()

val result2x2: Array<Array<Set<Int>>> = arrayOf(
    arrayOf(setOf(1),  setOf(2),  setOf(3),  setOf(4)),
    arrayOf(uncertain4, uncertain4, setOf(1), uncertain4),
    arrayOf(uncertain4, uncertain4, uncertain4, uncertain4),
    arrayOf(uncertain4, uncertain4, uncertain4, uncertain4)
)

class SudokuMatrixTest : StringSpec() { init {
    shouldThrow<IllegalArgumentException> { SudokuMatrix.read("0".byteInputStream().bufferedReader()) }
    shouldThrow<IllegalArgumentException> { SudokuMatrix.read("1".byteInputStream().bufferedReader()) }
    shouldThrow<InputFormatException>     { SudokuMatrix.read("2\n ....1\n....".byteInputStream().bufferedReader()) }
    shouldThrow<InputFormatException>     { SudokuMatrix.read("2\n 123\n....".byteInputStream().bufferedReader()) }
    shouldThrow<IllegalArgumentException> { SudokuMatrix.read("2\n 1234\n...5".byteInputStream().bufferedReader()) }

    "SudokuMatrix.read should read the matrix properly" {
        val sm = SudokuMatrix.read(input2x2.byteInputStream().bufferedReader())

        sm.quadrantSideLen shouldBe 2
        sm.sideCellCount shouldBe sm.quadrantSideLen * sm.quadrantSideLen
        sm.possibleValueCount shouldBe sm.quadrantSideLen * sm.quadrantSideLen
        sm.cells.size shouldBe sm.sideCellCount

        for(row in sm.cells)
            row.size shouldBe sm.sideCellCount

        for(r in 0 until sm.sideCellCount)
            for(c in 0 until sm.sideCellCount)
                sm.cells[r][c].vals shouldBe result2x2[r][c]
    }

}}
