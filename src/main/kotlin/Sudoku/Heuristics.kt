package Sudoku

private fun sumOfAllPossibleValues(sm: SudokuMatrix) =  sm.possibleValueCount * (sm.possibleValueCount + 1) / 2

class SingleValueInCellHeuristic: CellHeuristic() {
    override fun apply(sm: SudokuMatrix, cell: Cell): Boolean {
        if( !cell.isKnown )
            return false

        var modified = false
        val cellVal = cell.value
        for(zell in cell.allRelated()) {
            val cellModified = zell.removeCandidateValue(cellVal)
            if( cellModified )
                Log.info("SingleValueInCellHeuristic: removed $cellVal from cell[${zell.row}][${zell.col}] because it is present in cell[${cell.row}][${cell.col}]. New value: ${zell.str()}")
            modified = modified || cellModified
        }
        return modified
    }
}

/**
 * If all values but one are known for any row, column, or a quadrant,
 * fill the remaining unknown with the only possible value
 * Input: all cells of a row, column, or quadrant
 */
fun allButOneAreKnown(title: String, sm: SudokuMatrix, cells: List<Cell>): Boolean {
    var allValsSum= sumOfAllPossibleValues(sm)
    var targetCell: Cell? = null
    for(cell in cells) {
        if( cell.isKnown )
            allValsSum -= cell.value
        else if( targetCell == null )
            targetCell = cell
        else
            return false  // more than one not-known cell exists
    }

    if( targetCell == null )
        return false   // all values are known

    Log.info("$title: setting cell[${targetCell.row}][${targetCell.col}] to the only remaining value $allValsSum")
    targetCell.setValue(allValsSum)
    return true
}

class AllButOneAreKnownInRow: LineHeuristic() {
    override fun apply(sm: SudokuMatrix, line: Int): Boolean =
        allButOneAreKnown("AllButOneAreKnownInARow", sm, (0 until sm.sideCellCount).map{ c -> sm.cells[line][c] })
}

class AllButOneAreKnownInCol: LineHeuristic() {
    override fun apply(sm: SudokuMatrix, line: Int): Boolean =
        allButOneAreKnown("AllButOneAreKnownInCol", sm, (0 until sm.sideCellCount).map{ r -> sm.cells[r][line] })
}

class AllButOneAreKnownInQuadrant: QuadrantHeuristic() {
    override fun apply(sm: SudokuMatrix, qrow0: Int, qcol0: Int): Boolean =
        allButOneAreKnown("AllButOneAreKnownInQuadrant", sm, sm.QuadrantCells(qrow0, qcol0).asSequence().toList())
}


/**
 * If all values but one are known for any row, column, or a quadrant,
 * fill the remaining unknown with the only possible value
 * Input: groups of cells with unknown value, group being cells of a row, column, or quadrant
 */
fun uniqueValueLeft(title: String, sm: SudokuMatrix, cells: List<Cell>): Boolean {
    var modified = false
    outer@ for(cell in cells.filter { !it.isKnown }) {
        var vs: Set<Int> = HashSet<Int>(cell.vals) //.toMutableSet()
        for(otherCell in cells.filter { it !== cell }) {
            vs -= otherCell.vals
            if( vs.isEmpty() )
                continue@outer
        }

        if( vs.size == 1 ) {
            cell.vals = vs
            sm.nextWorkSet.markModified(cell)
            modified = true
            Log.info("$title: set cell[${cell.row}][${cell.col}] to unique remaining value ${cell.value}")
        }
    }

    return modified
}

class UniqueValueLeftInRow: LineHeuristic() {
    override fun apply(sm: SudokuMatrix, line: Int): Boolean =
        uniqueValueLeft("UniqueValueLeftInRow", sm, (0 until sm.sideCellCount).map{ c -> sm.cells[line][c] })
}

class UniqueValueLeftInCol: LineHeuristic() {
    override fun apply(sm: SudokuMatrix, line: Int): Boolean =
        uniqueValueLeft("UniqueValueLeftInCol", sm, (0 until sm.sideCellCount).map{ r -> sm.cells[r][line] })
}

class UniqueValueLeftInQuadrant: QuadrantHeuristic() {
    override fun apply(sm: SudokuMatrix, qrow0: Int, qcol0: Int): Boolean =
        uniqueValueLeft("UniqueValueLeftInQuadrant", sm, sm.QuadrantCells(qrow0, qcol0).asSequence().toList())
}


/**
 * If group of cells contains K cells with the same K values, no other cells may contain these values.
 * Input: groups of cells with unknown value, group being cells of a row, column, or quadrant
 */
fun combinationRemover(title: String, uqCells: List<Cell>): Boolean {
    if( uqCells.size <= 1 )
        return false

    var modified = false
    for(cell0 in uqCells.filter { it.vals.size < uqCells.size }) {
        val sameValCells = uqCells.filter { it === cell0 || it.vals == cell0.vals }
        if( sameValCells.count() == cell0.vals.size ) {
            for(otherCell in uqCells.filter{ !sameValCells.contains(it) }) {
                val oldVals = otherCell.vals
                if( otherCell.removeCandidateValues(cell0.vals) ) {
                    modified = true
                    Log.info("$title: reduced cell[${otherCell.row}][${otherCell.col}] from $oldVals to ${otherCell.str()} because of ${sameValCells.size} cells like cell[${cell0.row}][${cell0.col}] ${cell0.str()}")
                }
            }
        }
    }
    return modified
}

class CombinationInRow: LineHeuristic() {
    override fun apply(sm: SudokuMatrix, line: Int): Boolean =
        combinationRemover("CombinationInRow", (0 until sm.sideCellCount).map{ c -> sm.cells[line][c] }.filter{ !it.isKnown })
}

class CombinationInCol: LineHeuristic() {
    override fun apply(sm: SudokuMatrix, line: Int): Boolean =
        combinationRemover("CombinationInCol", (0 until sm.sideCellCount).map{ r -> sm.cells[r][line] }.filter{ !it.isKnown })
}

class CombinationInQuadrant: QuadrantHeuristic() {
    override fun apply(sm: SudokuMatrix, qrow0: Int, qcol0: Int): Boolean =
        combinationRemover("CombinationInQuadrant", sm.QuadrantCells(qrow0, qcol0).asSequence().filter{ !it.isKnown }.toList())
}


/**
 * Redundant values in combination remover
 * If a group of K cells (K < N) has the same values, then that group may not contain any other value
 */
fun closetSubset(title: String, sm: SudokuMatrix, allCells: List<Cell>): Boolean {
    var modified = false

    /**
     * set1: candidate set
     * set2: all other cells
     */
    fun processSubset(set1: Set<Cell>, set2withKnowns: Set<Cell>) {
        // common values of set1 elements are our candidates for closet set
        // Could use set1.forEach { commonValues.retainAll(it.vals) } but we want to break ASAP
        val commonValues: MutableSet<Int> = set1.iterator().next().vals.toMutableSet()
        for(cell in set1) {
            commonValues.retainAll(cell.vals)
            if( commonValues.size < set1.size ) return  // exit early if further search is pointless
        }

        // discard values found in cells outside of the candidate set (e.g. set2)
        val uniqueVals = commonValues.toMutableSet()
        for(v in commonValues) {
            if( set2withKnowns.any { it.vals.contains(v) } ) {
                if( uniqueVals.size-1 < set1.size ) return  // exit early if further search is pointless
                uniqueVals.remove(v)
            }
        }

        // when set1 has K members and contains K unique values, we've found a closet set
        if( uniqueVals.size == set1.size ) {
            val closetSetStr = set1.map { e -> "[${e.row}][${e.col}]" }.joinToString(", ")
            for (cell in set1.filter { it.vals != uniqueVals }) {
                modified = true
                val oldValsStr = cell.str()
                cell.vals = uniqueVals.toMutableSet()  // clone
                sm.nextWorkSet.markModified(cell)
                Log.info("$title: reduced cell[${cell.row}][${cell.col}] from $oldValsStr because of cells $closetSetStr: ${cell.str()}")
            }
        }
    }

    fun processAllSubsets(set1: Set<Cell>, set2: Set<Cell>, setOfKnown: Set<Cell>) {
        if( set1.size > 1 )                 // "last value in the set" is handled by other, less expensive heuristics
            processSubset(set1, set2 + setOfKnown)

        if( set2.size > 1 )
            set2.forEach { processAllSubsets(set1 + it, set2 - it, setOfKnown) }
    }

    val setOfKnown = allCells.filter { it.isKnown }.toSet()
    val cellsSet = allCells.toSet() - setOfKnown
    cellsSet.forEach { processAllSubsets(setOf(it), cellsSet - it, setOfKnown) }

    return modified
}

class ClosetSubsetInRow: LineHeuristic() {
    override fun apply(sm: SudokuMatrix, line: Int): Boolean =
        closetSubset("ClosetSubsetInRow", sm, (0 until sm.sideCellCount).map{ c -> sm.cells[line][c] })
}

class ClosetSubsetInCol: LineHeuristic() {
    override fun apply(sm: SudokuMatrix, line: Int): Boolean =
        closetSubset("ClosetSubsetInCol", sm, (0 until sm.sideCellCount).map{ r -> sm.cells[r][line] })
}

class ClosetSubsetInQuadrant: QuadrantHeuristic() {
    override fun apply(sm: SudokuMatrix, qrow0: Int, qcol0: Int): Boolean =
        closetSubset("ClosetSubsetInQuadrant", sm, sm.QuadrantCells(qrow0, qcol0).asSequence().toList())
}


/**
 * If a value only appears in the same K lines of K quadrants (K < quadrant side length),
 * then the value isn't present in these lines of the remaining quadrants
 */
class MatchingLineSubsets(sm: SudokuMatrix): MatrixHeuristic(sm) {

    private fun getUncertainValuesInQuadrant(zell: Cell): Set<Int> {
        val uncertainVals: MutableSet<Int> = mutableSetOf()
        this.sm.QuadrantCells(zell).asSequence().toList().filter{ !it.isKnown }.forEach{ uncertainVals += it.vals }
        return uncertainVals
    }


    private fun getLinesWithValue(zell: Cell, v: Int, rcMapper: (Cell) -> Int): List<Int> =
        this.sm.QuadrantCells(zell).asSequence().toList().filter { it.vals.contains(v) }.map { cell -> rcMapper(cell) }.distinct()


    private fun processQuadrantStrip(baseLine: Int, thisQuadrantLine: Int, isHorizontal: Boolean): Boolean {
        val otherQuadrantLines = (0 until sm.sideCellCount step sm.quadrantSideLen).asSequence().toSet() - thisQuadrantLine
        val zell = if( isHorizontal ) this.sm.cells[baseLine][thisQuadrantLine] else this.sm.cells[thisQuadrantLine][baseLine]
        val distinctVals = this.getUncertainValuesInQuadrant(zell)
        var modified = false

        for(v in distinctVals) {
            fun lineChooser(cell: Cell): Int = if( isHorizontal ) cell.row else cell.col
            val thisLines = this.getLinesWithValue(zell, v, { cell -> lineChooser(cell) }).toSet()
            if( thisLines.size >= sm.quadrantSideLen )  // the value is everywhere, nothing to remove
                continue

            // Build list of quadrants having the value in the same lines as we do
            val sameLineSetQuadrants:  MutableList<Int> = mutableListOf()
            for(otherQuadrantLine in otherQuadrantLines) {
                val qcell = sm.cells[if( isHorizontal) zell.row else otherQuadrantLine][if( isHorizontal) otherQuadrantLine else zell.col]
                val otherLines = this.getLinesWithValue(qcell, v, { cell -> lineChooser(cell) }).toSet()
                if( otherLines == thisLines )
                    sameLineSetQuadrants.add(otherQuadrantLine)
            }

            // see if we have sufficient quadrants with the same line set
            if( 1 + sameLineSetQuadrants.size == thisLines.size ) {
                for(qline in otherQuadrantLines.filter{ !sameLineSetQuadrants.contains(it) }) {  // all other quadrants
                    for(lineToClear in thisLines) {
                        for(qline2 in qline until qline + sm.quadrantSideLen) {              // all its other dimension
                            val r = if( isHorizontal ) lineToClear else qline2
                            val c = if( isHorizontal ) qline2 else lineToClear
                            val cellModified = sm.cells[r][c].removeCandidateValue(v)
                            modified = modified || cellModified
                            if( cellModified ) {
                                val rcText = if( isHorizontal ) "rows" else "cols"
                                val lineLst = thisLines.sorted().joinToString(",")
                                Log.info("MatchingLineSubsets: removed $v from cell[$r][$c] because it is covered by $rcText $lineLst: ${sm.cells[r][c].str()}")
                            }
                        }
                    }
                }
            }

        }

        return modified
    }

    override fun apply(): Boolean {
        var modified = false

        val lineIndexes = (0 until sm.sideCellCount step sm.quadrantSideLen).asSequence().toList()
        for(dim1 in lineIndexes)
            for(dim2 in lineIndexes) {
                modified = processQuadrantStrip(dim1, dim2, true ) || modified
                modified = processQuadrantStrip(dim1, dim2, false) || modified
            }

        return modified
    }

}
