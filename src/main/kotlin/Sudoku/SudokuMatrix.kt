package Sudoku

import java.io.BufferedReader

class SudokuMatrix(val quadrantsPerSide: Int) {

    inner class Cell(val row: Int, val col: Int, knownValue: Int) {
        var vals: Set<Int> = emptySet() //IntRange(1, this@SudokuMatrix.possibleValueCount).toMutableSet()

        init {
            this.vals = if( knownValue != 0 ) setOf(knownValue) else IntRange(1, this@SudokuMatrix.possibleValueCount).toMutableSet()
        }

        constructor(): this(-1, -1, -1)
        val isKnown get() = this.vals.size == 1
        val value   get() = if( this.isKnown ) this.vals.iterator().next() else throw Exception("attempt to get single value from multi-value cell $this")

        fun str() = this.vals.sorted().joinToString(",")

        private val firstQuadrantRow get() = (this.row / this@SudokuMatrix.quadrantSideLen) * this@SudokuMatrix.quadrantSideLen
        private val untilQuadrantRow get() = this.firstQuadrantRow + this@SudokuMatrix.quadrantSideLen
        private val firstQuadrantCol get() = (this.col / this@SudokuMatrix.quadrantSideLen) * this@SudokuMatrix.quadrantSideLen
        private val untilQuadrantCol get() = this.firstQuadrantCol + this@SudokuMatrix.quadrantSideLen

        val quadrantRows get() = this.firstQuadrantRow until this.untilQuadrantRow
        val quadrantCols get() = this.firstQuadrantCol until this.untilQuadrantCol

        val quadrantRow = this.row % this@SudokuMatrix.quadrantSideLen
        val quadrantCol = this.col % this@SudokuMatrix.quadrantSideLen

        fun removeCandidateValue(v: Int): Boolean {
            if( !this.vals.contains(v) )
                return false

            this.vals -= v
            this@SudokuMatrix.nextWorkSet.markModified(this)
            return true
        }

        fun removeCandidateValues(vv: Set<Int>): Boolean {
            val oldSz = this.vals.size
            this.vals -= vv
            if( this.vals.size == oldSz )
                return false

            this@SudokuMatrix.nextWorkSet.markModified(this)
            return true
        }


        fun setValue(v: Int): Boolean {
            require(v >= 1 && v <= this@SudokuMatrix.possibleValueCount)
            if( this.isKnown && this.value == v )
                return false

            this.vals = setOf(v)
            this@SudokuMatrix.nextWorkSet.markModified(this)
            return true
        }

        fun allRelated(): Array<Cell> {
            val cells: Array<Cell> = Array( this@SudokuMatrix.sideCellCount * 2 + this@SudokuMatrix.possibleValueCount - 3) { Cell() }
            var k = 0
            (0 until this@SudokuMatrix.sideCellCount).filter{ it != this.row }.forEach{ cells[k++] = this@SudokuMatrix.cells[it][this.col] }
            (0 until this@SudokuMatrix.sideCellCount).filter{ it != this.col }.forEach{ cells[k++] = this@SudokuMatrix.cells[this.row][it] }
            for(r in this.quadrantRows)
                this.quadrantCols.filter{ r != this.row && it != this.col }.forEach{ cells[k++] = this@SudokuMatrix.cells[r][it] }
            return cells
        }
    }

    inner class WorkSet {
        val cells: Array<Array<Boolean>> =  Array( this@SudokuMatrix.sideCellCount ) { Array(this@SudokuMatrix.sideCellCount) { false } }
        val rows:  Array<Boolean> = Array(this@SudokuMatrix.sideCellCount) { false }
        val cols:  Array<Boolean> = Array(this@SudokuMatrix.sideCellCount) { false }
        private val quadrants: Array<Array<Boolean>> = Array(this@SudokuMatrix.quadrantsPerSide) { Array(this@SudokuMatrix.quadrantsPerSide) { false } }

        fun hasWork() = this.rows.any { it } || this.cells.any { it.any { it } }

        fun clear() {
            for(k in 0 until this.cells.size) {
                this.rows[k] = false
                this.cols[k] = false
                for(c in 0 until this.cols.size)
                    this.cells[k][c] = false
            }

            for(r in 0 until this@SudokuMatrix.quadrantsPerSide)
                for(c in 0 until this@SudokuMatrix.quadrantsPerSide)
                    this.quadrants[r][c] = false
        }

        fun markModified(cell: Cell) {
            this.rows[cell.row] = true
            this.cols[cell.col] = true
            this.quadrants[cell.quadrantRow][cell.quadrantCol] = true

            for(k in 0 until this@SudokuMatrix.sideCellCount) {
                this.cells[k][cell.col] = true
                this.cells[cell.row][k] = true
            }

            for(r in cell.quadrantRows)
                for(c in cell.quadrantCols)
                    this.cells[r][c] = true
        }

        fun print() {
            for(rowArr in this.cells) {
                Log.print("   ")
                for (c in rowArr)
                    Log.print(if (c) "x" else ".")
                Log.print("\n")
            }
        }

    }

    inner class QuadrantCells(private val row0: Int, private val col0: Int): Iterator<Cell> {

        constructor(zCell: Cell): this(zCell.row, zCell.col)

        private var r = row0
        private var c = col0 - 1

        override fun hasNext(): Boolean =
                this.r < this.row0 + this@SudokuMatrix.quadrantSideLen - 1
             || this.c < this.col0 + this@SudokuMatrix.quadrantSideLen - 1

        override fun next(): Cell {
            this.c++
            if( this.c >= this.col0 + this@SudokuMatrix.quadrantSideLen ) {
                this.r++
                this.c = this.col0
            }
            return this@SudokuMatrix.cells[this.r][this.c]
        }
    }

    val sideCellCount = this.quadrantsPerSide * this.quadrantsPerSide
    val possibleValueCount: Int get() = this.sideCellCount
    //val quadrantsPerSide: Int get() = this.quadrantsPerSide
    val quadrantSideLen: Int get() = this.quadrantsPerSide
    val cells: Array<Array<Cell>> = Array( this.sideCellCount ) { Array(this.sideCellCount) { Cell() } }

    var currWorkSet: WorkSet = WorkSet()
    var nextWorkSet: WorkSet = WorkSet()

    fun prepare() {  // Call startNextCycle() to switch curr/next
        for(r in 0 until this.sideCellCount)
            for(c in 0 until this.sideCellCount) {
                this.nextWorkSet.cells[r][c] = this.cells[r][c].isKnown
                //if( this.cells[r][c].isKnown )
                //    this.nextWorkSet.markModified(this.cells[r][c])
            }
    }

    fun startNextCycle() {
        val tmp: WorkSet = this.currWorkSet
        this.currWorkSet = this.nextWorkSet
        this.nextWorkSet = tmp
        this.nextWorkSet.clear()
    }


    companion object {
        private var lineNum = 0

        private fun getLineOrDie(reader: BufferedReader) : String {
            while(true) {
                lineNum++
                val line: String = reader.readLine()?.substringBefore('#')?.trim() ?: throw InputFormatException(lineNum, "unexpected end of input")
                if( line.isNotEmpty() )
                    return line
            }
        }

        fun read(reader: BufferedReader): SudokuMatrix {
            var line = getLineOrDie(reader)
            val quadrantsPerSide = line.toInt()
            require(quadrantsPerSide >= 2)
            val m = SudokuMatrix(quadrantsPerSide)

            for(row in 0 until m.sideCellCount) {
                line = getLineOrDie(reader).replace(".", " . ")
                if( quadrantsPerSide <= 3 )  // allow no spaces between digits for 4x4 and 9x9 Sudoku
                    line = Regex("(\\d)").replace(line, " $1 ")
                  //line = line.replace(Regex("(\\d)"), MatchGroup(" $1 "))
                val elms = line.trim().split(Regex("\\s+"))
                if( elms.size != m.sideCellCount )
                    throw InputFormatException(lineNum, "expected ${m.sideCellCount} values, got ${elms.size}")

                for(col in 0..elms.lastIndex) {
                    val cellVal = if("." == elms[col]) 0 else elms[col].toInt()
                    m.cells[row][col] = m.Cell(row, col, cellVal )
                }
            }

            return m
        }

        fun print(m: SudokuMatrix) {
            val maxDigits = Math.ceil(Math.log10(m.possibleValueCount.toDouble())).toInt()
            val maxColWidth: Array<Int> = Array(m.sideCellCount) { 0 }

            fun elementCountToWidth(cnt: Int): Int = cnt * maxDigits + (cnt-1 /* commas */)

            for(col in 0 until m.sideCellCount)
                for(row in 0 until m.sideCellCount)
                    maxColWidth[col] = maxOf(maxColWidth[col], elementCountToWidth(m.cells[row][col].vals.size) )

            for(row in 0 until m.sideCellCount) {
                if( row > 0 && row % m.quadrantSideLen == 0 )
                    Log.print("\n")
                for(col in 0 until m.sideCellCount) {
                    if( col % m.quadrantSideLen == 0 )
                        Log.print(" | ")
                    Log.print("  ${m.cells[row][col].str().padStart(maxColWidth[col])}")
                }
                Log.print("\n")
            }
        }
    }
}
