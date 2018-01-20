package Sudoku

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class ClosetSubsetHeuristicTest : StringSpec() { init {
    Log.level = Log.Level.Error

    val sm = SudokuMatrix(3)

    "ClosetSubset heuristic should remove K values appearing in K cells from the other cells" {
        // we use cell.row as the expected value
        val allValuesSet = (1..sm.possibleValueCount).toSet()
        val cells = IntRange(1, sm.possibleValueCount).map { v -> sm.Cell(v, 0, allValuesSet.toMutableSet())}

        // Negative test case: when there is no K of K subset, there should be no changes
        combinationRemover("not important", cells) shouldBe false
        cells.forEach { it.vals shouldBe allValuesSet }

        // Positive test case
        val cellA = cells[4]
        val cellB = cells[8]
        val kSet = setOf(4, 7)
        cellA.vals = kSet.toMutableSet()
        cellB.vals = kSet.toMutableSet()
        combinationRemover("not important", cells) shouldBe true
        cellA.vals shouldBe kSet
        cellB.vals shouldBe kSet
        cells.filter { it !== cellA && it !== cellB }.forEach {
            it.vals shouldBe allValuesSet - kSet
        }
    }
}}
