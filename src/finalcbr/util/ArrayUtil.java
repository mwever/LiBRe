package finalcbr.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

public class ArrayUtil {

	private ArrayUtil() {
		// Prevent instantiation of this util class.
	}

	private static void columnSanityCheck(final int arrayLength, final Collection<Integer> columnIndices) {
		if (columnIndices.stream().filter(x -> x >= arrayLength || x < 0).findAny().isPresent()) {
			throw new IllegalArgumentException("Cannot exclude non existing columns (" + columnIndices + "), array length: " + arrayLength);
		}
	}

	/**
	 * Copies an array of type <T> without copying the columns in columnsToExclude.
	 *
	 * @param <T> The data type of objects contained in the array.
	 * @param array The array to copy excluding the given columns.
	 * @param columnsToExclude The columns to exclude when copying.
	 * @param clazz The class object for the type T.
	 * @return The copy of the original array without the excluded values.
	 */
	public static <T> T[] copyArrayExlcuding(final T[] array, final Collection<Integer> columnsToExclude) {
		columnSanityCheck(array.length, columnsToExclude);

		T[] arrayCopy = Arrays.copyOf(array, array.length - columnsToExclude.size());
		int pointer = 0;
		for (int i = 0; i < array.length; i++) {
			if (columnsToExclude.contains(i)) {
				continue;
			}
			arrayCopy[pointer++] = array[i];
		}
		return arrayCopy;
	}

	/**
	 * Copies an array of type <T> retaining the columns in columnsToRetain.
	 *
	 * @param <T> The data type of objects contained in the array.
	 * @param array The array to copy retaining the given columns.
	 * @param columnsToExclude The columns to retain when copying.
	 * @param clazz The class object for the type T.
	 * @return The copy of the original array retaining the given column values only.
	 */
	public static <T> T[] copyArrayRetaining(final T[] array, final Collection<Integer> columnsToRetain) {
		columnSanityCheck(array.length, columnsToRetain);
		T[] arrayCopy = Arrays.copyOf(array, columnsToRetain.size());
		int pointer = 0;
		for (int i = 0; i < array.length; i++) {
			if (!columnsToRetain.contains(i)) {
				continue;
			}
			arrayCopy[pointer++] = array[i];
		}
		return arrayCopy;
	}

	/**
	 * Transposes a matrix A and returns A^T.
	 * @param matrix The given matrix A.
	 * @return The transposed matrix A^T originating from A.
	 */
	public static double[][] transposeMatrix(final double[][] matrix) {
		double[][] transposed = new double[matrix[0].length][matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				transposed[j][i] = matrix[i][j];
			}
		}
		return transposed;
	}

	/**
	 * Transposes a matrix A and returns A^T.
	 * @param matrix The given matrix A.
	 * @return The transposed matrix A^T originating from A.
	 */
	public static int[][] transposeMatrix(final int[][] matrix) {
		int[][] transposed = new int[matrix[0].length][matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			for (int j = 0; j < matrix[0].length; j++) {
				transposed[j][i] = matrix[i][j];
			}
		}
		return transposed;
	}

	private static String cleanArrayString(final String arrayString) {
		String cleanArrayString = arrayString.trim();
		if (cleanArrayString.startsWith("[") && cleanArrayString.endsWith("]")) {
			cleanArrayString = cleanArrayString.substring(1, cleanArrayString.length() - 1);
		}
		return cleanArrayString;
	}

	public static double[] parseStringToDoubleArray(final String arrayString) {
		return Arrays.stream(cleanArrayString(arrayString).split(",")).mapToDouble(Double::parseDouble).toArray();
	}

	public static int[] parseStringToIntArray(final String arrayString) {
		return Arrays.stream(cleanArrayString(arrayString).split(",")).mapToInt(Integer::parseInt).toArray();
	}

	public static String[] parseStringToStringArray(final String arrayString) {
		return (String[]) Arrays.stream(cleanArrayString(arrayString).split(",")).toArray();
	}

	public static int[] thresholdDoubleToBinaryArray(final double[] array, final double threshold) {
		return Arrays.stream(array).mapToInt(x -> x >= threshold ? 1 : 0).toArray();
	}

	public static int[][] thresholdDoubleToBinaryMatrix(final double[][] matrix, final double threshold) {
		return thresholdDoubleToBinaryMatrix(matrix, IntStream.range(0, matrix[0].length).mapToDouble(x -> threshold).toArray());
	}

	public static int[][] thresholdDoubleToBinaryMatrix(final double[][] matrix, final double[] threshold) {
		int[][] thresholdedMatrix = new int[matrix[0].length][];
		IntStream.range(0, matrix[0].length).forEach(l -> thresholdedMatrix[l] = ArrayUtil.thresholdDoubleToBinaryArray(ArrayUtil.extractColumn(matrix, l), threshold[l]));
		return ArrayUtil.transposeMatrix(thresholdedMatrix);
	}

	public static double[] extractColumn(final double[][] matrix, final int columnIndex) {
		double[] column = new double[matrix.length];
		IntStream.range(0, matrix.length).forEach(x -> column[x] = matrix[x][columnIndex]);
		return column;
	}

	public static int[] extractColumn(final int[][] matrix, final int columnIndex) {
		int[] column = new int[matrix.length];
		IntStream.range(0, matrix.length).forEach(x -> column[x] = matrix[x][columnIndex]);
		return column;
	}

	public static double[] concat(final double[] arg0, final double[] arg1) {
		double[] concat = new double[arg0.length + arg1.length];
		System.arraycopy(arg0, 0, concat, 0, arg0.length);
		System.arraycopy(arg1, 0, concat, arg0.length, arg1.length);
		return concat;
	}

	public static void equals(final int[][] gt0, final int[][] groundTruthMatrix) {
		if (gt0.length != groundTruthMatrix.length) {
			System.err.println("Unequal length");
		}
		if (gt0[0].length != groundTruthMatrix[0].length) {
			System.err.println("Unequal width!");
		}

		for (int i = 0; i < gt0.length; i++) {
			for (int j = 0; j < gt0[0].length; j++) {
				if (gt0[i][j] != groundTruthMatrix[i][j]) {
					System.err.println("Unequal entry for " + i + " " + j);
				}
			}
		}
	}

}
