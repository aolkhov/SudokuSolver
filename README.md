# Sudoku Solver
Heuristic based Sudoku solver

The main project goal was to learn Kotlin while having fun developing Sudoku
heuristics. One can notice how parts of the code written later adopt more
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

These heuristic set was enough to solve all Sudoku tested so far.
This of course does not prove they will solve every Sudoku out there. 

## Heuristics

##### SingleValueInCellHeuristic
When cell has a known value, the value may be removed from the list of possible values
of all other cells.

Complexity: O(N)
 
##### AllButOneAreKnownInRow, Column, or Quadrant
If all values but one are known for any row, column, or a quadrant, the only remaining cell
with undetermined value has that missing value.

Complexity: O(N)

##### UniqueValueLeftInRow, Column, or Quadrant
When a value only appears in one cell and not anywhere else, then that's the cell value

Complexity: O(N)

##### CombinationInRow, Column, or Quadrant
When a group of N cells contains K cells (K < N) with the same K possible values,
no other cells in the group may contain these values.

Complexity: O(N^2)

##### ClosetSubsetInRow, Column, or Quadrant
When a group of K cells (K < N) contain the same subset of K possible values, and the values
do not appear in cells outside of the group, then the group members only contain these values.
All other possible values can be safely removed from these K cells.

Complexity: NP

##### MatchingLineSubsets
Applies to a horizontal or vertical stripe of quadrants

If a value only appears in the same K lines of K quadrants (K < N),
then the value can't be present in these lines of the remaining quadrants.
<br>(*Here, "line" denotes a row or a column.*)

###### Example
**Source**:
```
Row   Q1        Q2        Q3        Q4
  1   . x . .   . . . x   x x x x   x x x x
  2   . . . .   . . . .   x x x x   x x x x
  3   . . x .   . x . .   x x x x   x x x x
  4   . . . .   . . . .   x x x x   x x x x
```
*(candidate values of '.' cells do not contain x)*

Since value *x* appears in two rows (1 and 3) of two quadrants (Q1 and Q2),
it can not appear in these rows of the other quadrants Q3 and Q4.  

**Result**
```
Row   Q1        Q2        Q3        Q4
  1   . x . .   . . . x   . . . .   . . . .
  2   . . . .   . . . .   x x x x   x x x x
  3   . . x .   . x . .   . . . .   . . . .
  4   . . . .   . . . .   x x x x   x x x x
```
*(x has been cleared from rows 1 and 3 in quadrants Q3 and Q4)*

Complexity: O(N^3) for one stripe. O(N^4) for the entire matrix
