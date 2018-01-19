# Sudoku Solver
Heuristic based Sudoku solver

The main project goal was to learn Kotlin while having fun developing Sudoku
hauristics. One can notice how parts of the code written later adopt more
Kotlin constructs and functional style.

Test Sudoku sources:
* https://www.websudoku.com
* http://www.sudokukingdom.com/

## Compiling
All examples below create self-contained JAR.

### Maven
`mvn clean package`

### Command line
Get command line Kotlin compiler from [Kotlin Site](http://kotlinlang.org/docs/tutorials/command-line.html)

Change to project directory and compile the sources:

`kotlinc src/main/kotlin -include-runtime -d sudoku-solver.jar`


## Running
`java -jar sudoku-solver.jar --warn easy1.sudoku`

## The input
Empty lines are ignored, as well as everything following '#'

The first line contains a single number N, denoting matrix side length, in quadrants.
Sizes are 2 (for 4x4 cell, 16 total), 3 (for 9x9 cells, 81 total), etc.

The subsequent lines define matrix rows, one per line. There must be N^2 rows.
Each row must have N^2 column values.

Cell value is entered as a number from 1 to N*N (for matrix sizes greater than 3 they must be separated by at least one space).
Empty cells are represented by dots ('.').

### Sample

```
3
 
..9 .8. ..6    # 259 184 376
... 97. 8..    # 416 973 825
78. ... 4.1    # 783 625 491
 
.3. ..7 .19    # 538 247 619
.97 .3. 2..    # 197 836 254
6.. 5.1 7..    # 624 591 783
 
..2 ... .47    # 862 359 147
... 762 .3.    # 941 762 538
3.5 ..8 ...    # 375 418 962
```


# Internals
The solving process is based entirely on heuristics, there is no backtracking. 
Heuristics differ based on their targets: cell, line (row or a column), quadrant, or the entire matrix.

Usually they are pretty straightforward, but one, closetSubset, is NP-complete.

## Heuristics

##### SingleValueInCellHeuristic
When cell has a known value, no other cells in the same row, column, or quadrant, have that value
 
