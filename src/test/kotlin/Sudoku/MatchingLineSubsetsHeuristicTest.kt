package Sudoku

import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.StringSpec

class MatchingLineSubsetsHeuristicTest : StringSpec() { init {
    Log.level = Log.Level.Error

    val sm = SudokuMatrix(4)
    for(r in 0 until sm.sideCellCount)
        for(c in 0 until sm.sideCellCount)
            sm.cells[r][c] = sm.Cell(r, c, 0)

    val allValues = (1..sm.possibleValueCount).toSet()

    "MatchingLineSubsets heuristic checks lines across quadrant stripes" {

        // -------------------  Negative test case
        MatchingLineSubsets(sm).apply() shouldBe false
        sm.cells.forEach { it.forEach { it.vals shouldBe allValues }}

        // ------------------- Positive test case
        val refVal = 2
        val rowA = 4
        val rowB = 6

        val refRows = setOf(rowA, rowB)
        val refCells = setOf(
                sm.cells[rowA][1], sm.cells[rowB][2]    // Q1 cells
               ,sm.cells[rowA][4], sm.cells[rowB][6]    // Q2 cells
        )

        fun clearXfromQuadrant(qRow: Int, qCol: Int) {
            for(cell in sm.QuadrantCells(qRow, qCol))
                if( cell !in refCells )
                    cell.vals -= refVal
        }

        clearXfromQuadrant(4, 0)
        clearXfromQuadrant(4, 4)

        // run the heuristic
        MatchingLineSubsets(sm).apply() shouldBe true

        val allButRefVal = allValues - setOf(refVal)

        fun verifyRefQuadrant(qRow: Int, qCol: Int) {
            for(cell in sm.QuadrantCells(qRow, qCol))
                if( cell in refCells )
                    cell.vals shouldBe allValues
                else
                    cell.vals shouldBe allButRefVal
        }

        fun verifyNonRefQuadrant(qRow: Int, qCol: Int) {
            for(cell in sm.QuadrantCells(qRow, qCol))
                if( cell.row in refRows )
                    cell.vals shouldBe allButRefVal
                else
                    cell.vals shouldBe allValues
        }

        // cells of Q1 and Q2 should not be modified
        verifyRefQuadrant(4,0)
        verifyRefQuadrant(4,4)

        // cells of Q3 and Q4 should be modified
        verifyNonRefQuadrant(4, 8)
        verifyNonRefQuadrant(4,12)

        // cells of all other quadrants must stay put
        listOf(0, 8, 12).forEach {
            verifyNonRefQuadrant(it,  0)
            verifyNonRefQuadrant(it,  4)
            verifyNonRefQuadrant(it,  8)
            verifyNonRefQuadrant(it, 12)
        }

    }
}}
