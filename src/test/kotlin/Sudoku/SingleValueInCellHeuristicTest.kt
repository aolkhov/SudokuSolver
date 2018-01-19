package Sudoku

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class SingleValueInCellHeuristicTest : StringSpec() { init {
    Log.level = Log.Level.Error
    "SingleValueInCellHeuristic should remove a known cell value from related cells" {
        val sm = SudokuMatrix(3)
        val cellVal = 8

        val touchedVals = IntRange(1, sm.possibleValueCount).filter { it != cellVal }.toSet()
        val untouchedVals = IntRange(1, sm.possibleValueCount).toSet()

        for (r in 0 until sm.sideCellCount)
            for (c in 0 until sm.sideCellCount)
                sm.cells[r][c] = sm.Cell(r, c, untouchedVals.toMutableSet())

        val row = 3
        val col = 4
        val qcellXY: Set<Pair<Int, Int>> = setOf(
            Pair(3, 3), Pair(3, 4), Pair(3, 5),
            Pair(4, 3), Pair(4, 4), Pair(4, 5),
            Pair(5, 3), Pair(5, 4), Pair(5, 5)
        )

        sm.prepare()
        sm.cells[row][col].vals = setOf(cellVal)

        SingleValueInCellHeuristic().apply(sm, sm.cells[row][col]) shouldBe true

        sm.cells.forEach { it.forEach {
            if (it.row == row && it.col == col)
                it.vals shouldBe setOf(cellVal)
            else if (it.row == row || it.col == col)
                it.vals shouldBe touchedVals
            else if (qcellXY.contains(Pair<Int, Int>(it.row, it.col)))
                it.vals shouldBe touchedVals
            else
                it.vals shouldBe untouchedVals
        }}
    }
}}
