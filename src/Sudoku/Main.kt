package Sudoku

import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.File

fun main(args: Array<String>) {
    val reader: BufferedReader = if( args.size > 0) File(args[0]).bufferedReader() else BufferedReader(InputStreamReader(System.`in`))

    val sm = SudokuMatrix.read(reader)

    val cellHeuristics: Array<CellHeuristic> = arrayOf( SingleValueInCellHeuristic() )
    val  rowHeuristics: Array<LineHeuristic> = arrayOf( AllButOneAreKnownInRow(), UniqueValueLeftInRow(), CombinationInRow(), ClosetSubsetInRow() )
    val  colHeuristics: Array<LineHeuristic> = arrayOf( AllButOneAreKnownInCol(), UniqueValueLeftInCol(), CombinationInCol(), ClosetSubsetInCol() )
    val quadrantHeuristics: Array<QuadrantHeuristic> = arrayOf( AllButOneAreKnownInQuadrant(), UniqueValueLeftInQuadrant(), CombinationInQuadrant(), ClosetSubsetInQuadrant() )
    val matrixHeuristics: Array<MatrixHeuristic> = arrayOf( MatchingLineSubsets(sm) )

    var pass = 0
    sm.prepare()
    while( sm.nextWorkSet.hasWork() ) {
        pass++
        sm.startNextCycle()

        Log.info("Starting pass $pass")
        if( Log.level >= Log.Level.Debug ) {
            Log.debug("Values Matrix:");   SudokuMatrix.print(sm)
            Log.debug("Action Matrix:");   sm.currWorkSet.print()
        }

        for(r in 0 until sm.currWorkSet.cells.size)
            for(c in 0 until sm.currWorkSet.cells[r].size)
                if( sm.currWorkSet.cells[r][c] )
                    cellHeuristics.forEach{
                        if( it.apply(sm, sm.cells[r][c]) ) {
                            sm.nextWorkSet.markModified(sm.cells[r][c])
                            if( Log.level >= Log.Level.Debug ) { print("Pass $pass, cell heuristics\n"); SudokuMatrix.print(sm) }
                        }
                    }

        for(k in 0 until sm.sideCellCount) {
            if (sm.currWorkSet.rows[k])
                rowHeuristics.forEach{
                    if( it.apply(sm, k) )
                        if( Log.level >= Log.Level.Debug ) { print("Pass $pass, row heuristics\n"); SudokuMatrix.print(sm) }
                }
            if (sm.currWorkSet.cols[k])
                colHeuristics.forEach{
                    if( it.apply(sm, k) )
                        if( Log.level >= Log.Level.Debug ) { print("Pass $pass, col heuristics\n"); SudokuMatrix.print(sm) }
                }
        }

        for(qr in 0 until sm.sideCellCount step sm.quadrantSideLen)
            for(qc in 0 until sm.sideCellCount step sm.quadrantSideLen)
                quadrantHeuristics.forEach{
                    if( it.apply(sm, qr, qc) )
                        if( Log.level >= Log.Level.Debug ) { print("Pass $pass, quadrant heuristics\n"); SudokuMatrix.print(sm) }
                }

        matrixHeuristics.forEach{
            if( it.apply() )
                if( Log.level >= Log.Level.Debug ) { print("Pass $pass, matrix heuristics\n"); SudokuMatrix.print(sm) }
        }
    }

    SudokuMatrix.print(sm)
}

class InputFormatException(lineNum: Int, msg: String): Exception("error at input line $lineNum: $msg")

abstract class CellHeuristic {
    abstract fun apply(sm: SudokuMatrix, cell: SudokuMatrix.Cell): Boolean
}

abstract class LineHeuristic {
    abstract fun apply(sm: SudokuMatrix, line: Int): Boolean
}

abstract class QuadrantHeuristic {
    abstract fun apply(sm: SudokuMatrix, qrow0: Int, qcol0: Int): Boolean
}

abstract class MatrixHeuristic(protected val sm: SudokuMatrix) {
    abstract fun apply(): Boolean
}
