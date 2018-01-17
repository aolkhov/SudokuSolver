package Sudoku

import java.io.InputStreamReader
import java.io.BufferedReader
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {

    val sm = handleArgs(args)

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

fun handleArgs(args: Array<String>): SudokuMatrix {
    var inputFileName: String? = null
    var helpRequested = false
    for(arg in args) {
        when( arg ) {
            "--debug" -> Log.level = Log.Level.Debug
            "--info"  -> Log.level = Log.Level.Info
            "--warn"  -> Log.level = Log.Level.Warning
            "--error" -> Log.level = Log.Level.Error
            "--help", "-h", "-?", "/?" -> helpRequested = true
            else -> inputFileName = arg
        }
    }

    if( helpRequested ) {
        print("""
            Heuristic-based Sudoku Solver, version 0.1. https://github.com/aolkhov/SudokuSolver
            Parameters:
              --debug, --info, --warn --error - set verbosity level. The default is --info
              file-name (optional). Read matrix from the file. The default is stdin

            Example:
              java -jar sudoku-solver.jar --debug input.txt

            Input file format:
              * Empty lines are ignored
              * Everything after '#' is ignored
              * The first value N is side length, in quadrants e.g. 2, 3
              * The following lines are one per matrix line per row, should have N values
                * spaces are ignored
                * dots ('.') are treated as empty cells
                * numbers are 1 to N*N
                * for matrices with 2 or 3 quadrants, no space is needed between values
                  (the values are 1 to 4 and 1 to 9 and can be detected unambiguously)

            Sample input:
                3
                ..9 .8. ..6
                ... 97. 8..
                78. ... 4.1
                .3. ..7 .19
                .97 .3. 2..
                6.. 5.1 7..
                ..2 ... .47
                ... 762 .3.
                3.5 ..8 ...

            """.trimIndent())
        exitProcess(0)
    }

    val reader: BufferedReader = if( inputFileName == null ) BufferedReader(InputStreamReader(System.`in`)) else File(inputFileName).bufferedReader()
    if( inputFileName == null )
        Log.warn("Input file was not provided, reading data from stdin:")

    return SudokuMatrix.read(reader)
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
