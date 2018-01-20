package Sudoku

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class AllButOneAreKnownHeuristicTest : StringSpec() { init {
    Log.level = Log.Level.Error

    val sm = SudokuMatrix(3)

    "AllButOneAreKnown heuristic should assign the only remaining value" {
        // we use cell.row as the expected value
        val cells = IntRange(1, sm.possibleValueCount).map { v -> sm.Cell(v, 0, v)}

        // when everything is known, there should be no changes
        allButOneAreKnown("not important", sm, cells) shouldBe false
        cells.forEach { it.value shouldBe it.row }

        val pos = 5
        val uncertainSet = setOf(1,3,8)

        // when a single value is unknown, it must be set
        cells[pos].vals = uncertainSet.toSet()
        allButOneAreKnown("not important", sm, cells) shouldBe true
        cells.forEach { it.value shouldBe it.row }

        // when a more than one value is uncertain, there should be no changes
        cells[pos+1].vals = uncertainSet.toSet()
        cells[pos+2].vals = uncertainSet.toSet()
        allButOneAreKnown("not important", sm, cells) shouldBe false
        cells.forEach {
            val cellPos = it.row - 1   // rows sart with 1, and pos start with 0
            if( cellPos == pos+1 || cellPos == pos+2 )
                it.vals shouldBe uncertainSet
            else
                it.vals shouldBe  setOf(it.row)
        }

    }
}}
