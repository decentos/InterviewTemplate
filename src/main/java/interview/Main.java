package interview;

import java.util.ArrayDeque;
import java.util.Deque;

public class Main {

    // - Island -
    //Build a class/function with below conditions:
    //Given 2D coordinates in a land, each coordinate has a height.
    //Find the highest coordinate, then pour water on this coordinate,
    // please return how many coordinates will be flown by water.

    // Pour unlimited amount of water from which coordinate,
    // it can get the max number of coordinates that are covered by water?

    // Eg.
    //A)
    //0 0 1
    //1 2 0
    //3 1 0 = 3
    //
    //x x x
    //1 x x
    //3 1 x
    //
    //B)
    //1 2 1
    //1 3 2
    //2 1 1 = 8
    //
    //1 2 1
    //1 3 2
    //x 1 1
    //
    //C)
    //0 0 1
    //0 1 0
    //4 0 0 = 1

    public static void main(String[] args) {
        int[][] land1 = {{0, 0, 1}, {1, 2, 0}, {3, 1, 0}};
        System.out.println("land1: " + flownCount(land1));

        int[][] land2 = {{1, 2, 1}, {1, 3, 2}, {2, 1, 1}};
        System.out.println("land2: " + flownCount(land2));

        int[][] land3 = {{0, 0, 1}, {0, 1, 0}, {4, 0, 0}};
        System.out.println("land3: " + flownCount(land3));

        int[][] land4 = {{0, 0, 0}, {0, 0, 0}};
        System.out.println("land4: " + flownCount(land4));
        System.out.println("land5: " + flownCount(null));
    }

    private static int flownCount(int[][] land) {
        if (land == null) {
            return 0;
        }
        int m = land.length, n = land[0].length;

        if (m == 0 && n == 0) {
            return 0;
        }

//        int highest = 0, maxRow = 0, maxCol = 0;
//        for (int row = 0; row < m; row++) {
//            for (int col = 0; col < n; col++) {
//                if (land[row][col] > highest) {
//                    highest = land[row][col];
//                    maxRow = row;
//                    maxCol = col;
//                }
//            }
//        }
//
//        if (highest == 0) {
//            return 0;
//        }

        int maxFlown = 0, maxRow = 0, maxCol = 0;
        for (int row = 0; row < m; row++) {
            for (int col = 0; col < n; col++) {
                if (land[row][col] == 0) {
                    continue;
                }

                int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
                boolean[][] visited = new boolean[m][n];
                visited[row][col] = true;
                Deque<int[]> queue = new ArrayDeque<>();
                queue.offer(new int[]{row, col});
                int count = 0;

                while (!queue.isEmpty()) {
                    int[] curr = queue.poll();
                    int currRow = curr[0];
                    int currCol = curr[1];
                    count++;

                    for (int[] dir : dirs) {
                        int nextRow = currRow + dir[0];
                        int nextCol = currCol + dir[1];

                        if (nextRow < 0 || nextRow >= m || nextCol < 0 || nextCol >= n
                                || visited[nextRow][nextCol]
                                || land[nextRow][nextCol] == 0
                                || land[nextRow][nextCol] > land[currRow][currCol]) {
                            continue;
                        }

                        visited[nextRow][nextCol] = true;
                        queue.offer(new int[]{nextRow, nextCol});
                    }
                }

                if (count > maxFlown) {
                    maxFlown = count;
                    maxRow = row;
                    maxCol = col;
                }
            }
        }
        return maxFlown;

//        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
//        boolean[][] visited = new boolean[m][n];
//        visited[maxRow][maxCol] = true;
//        Deque<int[]> queue = new ArrayDeque<>();
//        queue.offer(new int[]{maxRow, maxCol});
//        int count = 0;
//
//        while (!queue.isEmpty()) {
//            int[] curr = queue.poll();
//            int currRow = curr[0];
//            int currCol = curr[1];
//            count++;
//
//            for (int[] dir : dirs) {
//                int nextRow = currRow + dir[0];
//                int nextCol = currCol + dir[1];
//
//                if (nextRow < 0 || nextRow >= m || nextCol < 0 || nextCol >= n
//                        || visited[nextRow][nextCol]
//                        || land[nextRow][nextCol] == 0
//                        || land[nextRow][nextCol] > land[currRow][currCol]) {
//                    continue;
//                }
//
//                visited[nextRow][nextCol] = true;
//                queue.offer(new int[]{nextRow, nextCol});
//            }
//        }
//        return count;
    }
}
