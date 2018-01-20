package Sudoku

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class UniqueValueLeftHeuristicTest : StringSpec() { init {
    Log.level = Log.Level.Error

    val sm = SudokuMatrix(3)

    "UniqueValueLeft heuristic should recognize values not appearing anywhere else" {
        // we use cell.row as the expected value
        val uniqueVal = 7
        val uncertainSet = (1..sm.possibleValueCount).filter { it != uniqueVal }.toSet()
        val cells = IntRange(1, sm.possibleValueCount).map { v -> sm.Cell(v, 0, uncertainSet.toMutableSet())}

        // negative test case: when there is no unique value, there should be no changes
        uniqueValueLeft("not important", sm, cells) shouldBe false
        cells.forEach { it.vals shouldBe uncertainSet }

        // positive test case: when unique value is present, it must be set
        val testCell = cells[3]
        (testCell.vals as MutableSet<Int>).add(uniqueVal)
        uniqueValueLeft("not important", sm, cells) shouldBe true
        testCell.isKnown shouldBe true
        testCell.value shouldBe uniqueVal
        cells.filter { it !== testCell }.forEach {
            it.vals shouldBe uncertainSet
        }
    }
}}
