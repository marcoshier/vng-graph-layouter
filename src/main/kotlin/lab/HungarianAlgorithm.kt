package lab

import org.openrndr.math.Vector2
import kotlin.math.max

/**
 * Hungarian Algorithm implementation for OPENRNDR Vector points.
 * This algorithm solves the assignment problem, finding the optimal matching
 * between two sets of points to minimize the total distance/cost.
 *
 * lifted from https://github.com/KevinStern/software-and-algorithms/blob/master/src/main/java/blogspot/software_and_algorithms/stern_library/optimization/HungarianAlgorithm.java
 * translated to Kotlin for Vector2 by Claude
 */

class HungarianAlgorithm(pointsA: List<Vector2>, pointsB: List<Vector2>) {
    private val costMatrix: Array<DoubleArray>
    private val rows: Int
    private val cols: Int
    private val dim: Int
    private val labelByWorker: DoubleArray
    private val labelByJob: DoubleArray
    private val minSlackWorkerByJob: IntArray
    private val minSlackValueByJob: DoubleArray
    private val matchJobByWorker: IntArray
    private val matchWorkerByJob: IntArray
    private val parentWorkerByCommittedJob: IntArray
    private val committedWorkers: BooleanArray

    init {
        // Create cost matrix based on distances between Vector2 points
        val initialCostMatrix = Array(pointsA.size) { i ->
            DoubleArray(pointsB.size) { j ->
                pointsA[i].distanceTo(pointsB[j])
            }
        }

        this.rows = initialCostMatrix.size
        this.cols = initialCostMatrix[0].size
        this.dim = max(rows, cols)

        // Initialize the square cost matrix (may need padding)
        this.costMatrix = Array(dim) { DoubleArray(dim) }

        // Copy the initial cost matrix to the padded square matrix
        for (w in 0 until dim) {
            if (w < initialCostMatrix.size) {
                for (j in 0 until cols) {
                    costMatrix[w][j] = initialCostMatrix[w][j]
                }
            }
        }

        labelByWorker = DoubleArray(dim)
        labelByJob = DoubleArray(dim)
        minSlackWorkerByJob = IntArray(dim)
        minSlackValueByJob = DoubleArray(dim)
        committedWorkers = BooleanArray(dim)
        parentWorkerByCommittedJob = IntArray(dim)
        matchJobByWorker = IntArray(dim) { -1 }
        matchWorkerByJob = IntArray(dim) { -1 }
    }

    /**
     * Compute an initial feasible solution by assigning zero labels to the
     * workers and by assigning to each job a label equal to the minimum cost
     * among its incident edges.
     */
    private fun computeInitialFeasibleSolution() {
        for (j in 0 until dim) {
            labelByJob[j] = Double.POSITIVE_INFINITY
        }
        for (w in 0 until dim) {
            for (j in 0 until dim) {
                if (costMatrix[w][j] < labelByJob[j]) {
                    labelByJob[j] = costMatrix[w][j]
                }
            }
        }
    }

    /**
     * Execute the algorithm.
     *
     * @return an array where each entry [i] represents the index of the
     *         point from set B that is matched to point i from set A.
     *         A value of -1 indicates that the point is unmatched.
     */
    fun execute(): IntArray {
        // Reduce rows and columns by their smallest element
        reduce()

        // Compute an initial non-zero dual feasible solution
        computeInitialFeasibleSolution()

        // Create a greedy matching from workers to jobs
        greedyMatch()

        var w = fetchUnmatchedWorker()
        while (w < dim) {
            initializePhase(w)
            executePhase()
            w = fetchUnmatchedWorker()
        }

        // Return matches, adjusted for the actual number of rows
        val result = matchJobByWorker.copyOf(rows)
        for (i in result.indices) {
            if (result[i] >= cols) {
                result[i] = -1
            }
        }
        return result
    }

    /**
     * Execute a single phase of the algorithm.
     */
    private fun executePhase() {
        while (true) {
            var minSlackWorker = -1
            var minSlackJob = -1
            var minSlackValue = Double.POSITIVE_INFINITY

            for (j in 0 until dim) {
                if (parentWorkerByCommittedJob[j] == -1) {
                    if (minSlackValueByJob[j] < minSlackValue) {
                        minSlackValue = minSlackValueByJob[j]
                        minSlackWorker = minSlackWorkerByJob[j]
                        minSlackJob = j
                    }
                }
            }

            if (minSlackValue > 0) {
                updateLabeling(minSlackValue)
            }

            parentWorkerByCommittedJob[minSlackJob] = minSlackWorker

            if (matchWorkerByJob[minSlackJob] == -1) {
                // An augmenting path has been found
                var committedJob = minSlackJob
                var parentWorker = parentWorkerByCommittedJob[committedJob]

                while (true) {
                    val temp = matchJobByWorker[parentWorker]
                    match(parentWorker, committedJob)
                    committedJob = temp
                    if (committedJob == -1) {
                        break
                    }
                    parentWorker = parentWorkerByCommittedJob[committedJob]
                }
                return
            } else {
                // Update slack values since we increased the committed workers set
                val worker = matchWorkerByJob[minSlackJob]
                committedWorkers[worker] = true

                for (j in 0 until dim) {
                    if (parentWorkerByCommittedJob[j] == -1) {
                        val slack = costMatrix[worker][j] - labelByWorker[worker] - labelByJob[j]
                        if (minSlackValueByJob[j] > slack) {
                            minSlackValueByJob[j] = slack
                            minSlackWorkerByJob[j] = worker
                        }
                    }
                }
            }
        }
    }

    /**
     * @return the first unmatched worker or [dim] if none.
     */
    private fun fetchUnmatchedWorker(): Int {
        var w = 0
        while (w < dim) {
            if (matchJobByWorker[w] == -1) {
                break
            }
            w++
        }
        return w
    }

    /**
     * Find a valid matching by greedily selecting among zero-cost matchings.
     */
    private fun greedyMatch() {
        for (w in 0 until dim) {
            for (j in 0 until dim) {
                if (matchJobByWorker[w] == -1 && matchWorkerByJob[j] == -1
                    && costMatrix[w][j] - labelByWorker[w] - labelByJob[j] == 0.0) {
                    match(w, j)
                }
            }
        }
    }

    /**
     * Initialize the next phase of the algorithm for the specified root worker.
     */
    private fun initializePhase(w: Int) {
        committedWorkers.fill(false)
        parentWorkerByCommittedJob.fill(-1)
        committedWorkers[w] = true

        for (j in 0 until dim) {
            minSlackValueByJob[j] = costMatrix[w][j] - labelByWorker[w] - labelByJob[j]
            minSlackWorkerByJob[j] = w
        }
    }

    /**
     * Record a matching between worker w and job j.
     */
    private fun match(w: Int, j: Int) {
        matchJobByWorker[w] = j
        matchWorkerByJob[j] = w
    }

    /**
     * Reduce the cost matrix by subtracting the smallest element of each row/column.
     */
    private fun reduce() {
        // Reduce rows
        for (w in 0 until dim) {
            var min = Double.POSITIVE_INFINITY
            for (j in 0 until dim) {
                if (costMatrix[w][j] < min) {
                    min = costMatrix[w][j]
                }
            }
            for (j in 0 until dim) {
                costMatrix[w][j] -= min
            }
        }

        // Reduce columns
        val min = DoubleArray(dim) { Double.POSITIVE_INFINITY }
        for (w in 0 until dim) {
            for (j in 0 until dim) {
                if (costMatrix[w][j] < min[j]) {
                    min[j] = costMatrix[w][j]
                }
            }
        }
        for (w in 0 until dim) {
            for (j in 0 until dim) {
                costMatrix[w][j] -= min[j]
            }
        }
    }

    /**
     * Update labels with the specified slack.
     */
    private fun updateLabeling(slack: Double) {
        for (w in 0 until dim) {
            if (committedWorkers[w]) {
                labelByWorker[w] += slack
            }
        }
        for (j in 0 until dim) {
            if (parentWorkerByCommittedJob[j] != -1) {
                labelByJob[j] -= slack
            } else {
                minSlackValueByJob[j] -= slack
            }
        }
    }
}

/**
 * Example usage with OPENRNDR Vector2 points
 */
fun assignPoints(pointsA: List<Vector2>, pointsB: List<Vector2>): List<Pair<Vector2, Vector2?>> {
    val algorithm = HungarianAlgorithm(pointsA, pointsB)
    val assignments = algorithm.execute()

    return pointsA.mapIndexed { index, pointA ->
        val assignedIndex = assignments[index]
        if (assignedIndex == -1) {
            Pair(pointA, null)
        } else {
            Pair(pointA, pointsB[assignedIndex])
        }
    }
}