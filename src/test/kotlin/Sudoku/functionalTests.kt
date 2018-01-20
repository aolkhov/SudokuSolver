package Sudoku

import io.kotlintest.matchers.fail
import io.kotlintest.specs.StringSpec
import java.io.BufferedReader

import java.io.File

class SolverTest : StringSpec() {
    init {
        "Solver must solve the test cases properly" {
            Log.level = Log.Level.Error
            File("testcases").walkTopDown().filter { it.isFile }.forEach {
                solveAndVerify(it)
            }
        }
    }

    private fun readTestCase(reader: BufferedReader, quadrantsPerSide: Int): SudokuMatrix {
        val sm = SudokuMatrix(quadrantsPerSide)
        for(r in 0 until sm.sideCellCount) {
            val elms = SudokuMatrix.getLineOrDie(reader).trim().split(Regex("\\s+"))
            if( elms.size != sm.sideCellCount )
                throw InputFormatException(SudokuMatrix.lineNum, "expected ${sm.sideCellCount} values, got ${elms.size}")
            for(c in 0 until elms.size) {
                val vals = elms[c].split(',').map { v -> v.toInt() }
                sm.cells[r][c] = sm.Cell(r, c, vals.toMutableSet())
            }
        }
        return sm
    }

    private fun solveAndVerify(file: File) {
        print("running test ${file.name} ... ")
        val reader = file.bufferedReader()
        val smt = SudokuMatrix.read(reader)
        val smr = readTestCase(reader, smt.quadrantsPerSide)

        solve(smt)

        for(r in 0 until smt.cells.size)
            for(c in 0 until smt.cells[r].size)
                if( smt.cells[r][c].vals != smr.cells[r][c].vals ) {
                    print("failed\n")
                    fail("cell[$r][$c] is ${smt.cells[r][c].str()}, expected ${smr.cells[r][c].str()}")
                }

        print("passed\n")
    }
}
