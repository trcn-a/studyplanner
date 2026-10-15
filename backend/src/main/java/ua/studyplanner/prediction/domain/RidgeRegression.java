package ua.studyplanner.prediction.domain;

/**
 * Гребенева (L2-регуляризована) лінійна регресія на стандартизованих ознаках.
 *
 * <p>Розв'язок знаходиться в замкненому вигляді: w = (ZᵀZ + λI)⁻¹ Zᵀ(y − ȳ). Матриця (ZᵀZ + λI)⁻¹ також
 * дає діагональ матриці впливу, тому похибки leave-one-out (оцінка якості без розділення вибірки на
 * навчальну й тестову) обчислюються без повторного навчання: e_i / (1 − h_ii).
 */
public final class RidgeRegression {
    private static final double MIN_SCALE = 0.1;

    private final double[] mean;
    private final double[] sd;
    private final double[] weights;
    private final double intercept;
    private final double[] looResiduals;

    private RidgeRegression(
            double[] mean, double[] sd, double[] weights, double intercept, double[] looResiduals) {
        this.mean = mean;
        this.sd = sd;
        this.weights = weights;
        this.intercept = intercept;
        this.looResiduals = looResiduals;
    }

    public static RidgeRegression fit(double[][] x, double[] y, double lambda) {
        int n = x.length;
        int d = x[0].length;
        double[] mean = new double[d];
        double[] sd = new double[d];
        for (int j = 0; j < d; j++) {
            for (double[] row : x) {
                mean[j] += row[j];
            }
            mean[j] /= n;
            double variance = 0;
            for (double[] row : x) {
                variance += (row[j] - mean[j]) * (row[j] - mean[j]);
            }
            double deviation = Math.sqrt(variance / n);
            // Нижня межа масштабу: майже константна ознака інакше дає величезні z-значення для звичайного шуму.
            sd[j] = deviation < 1e-9 ? 1 : Math.max(deviation, MIN_SCALE);
        }
        double[][] z = new double[n][d];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < d; j++) {
                z[i][j] = (x[i][j] - mean[j]) / sd[j];
            }
        }
        double yMean = 0;
        for (double value : y) {
            yMean += value;
        }
        yMean /= n;

        double[][] gram = new double[d][d];
        double[] moment = new double[d];
        for (int i = 0; i < n; i++) {
            for (int a = 0; a < d; a++) {
                moment[a] += z[i][a] * (y[i] - yMean);
                for (int b = 0; b < d; b++) {
                    gram[a][b] += z[i][a] * z[i][b];
                }
            }
        }
        for (int a = 0; a < d; a++) {
            gram[a][a] += lambda;
        }
        double[][] inverse = invert(gram);

        double[] weights = new double[d];
        for (int a = 0; a < d; a++) {
            for (int b = 0; b < d; b++) {
                weights[a] += inverse[a][b] * moment[b];
            }
        }
        double[] loo = new double[n];
        for (int i = 0; i < n; i++) {
            double fitted = yMean;
            double leverage = 1.0 / n;
            for (int a = 0; a < d; a++) {
                fitted += weights[a] * z[i][a];
                for (int b = 0; b < d; b++) {
                    leverage += z[i][a] * inverse[a][b] * z[i][b];
                }
            }
            loo[i] = (y[i] - fitted) / Math.max(1 - leverage, 0.05);
        }
        return new RidgeRegression(mean, sd, weights, yMean, loo);
    }

    public double predict(double[] x) {
        double result = intercept;
        for (int j = 0; j < weights.length; j++) {
            result += weights[j] * (x[j] - mean[j]) / sd[j];
        }
        return result;
    }

    /** Похибки передбачення кожного навчального прикладу моделлю, навченою без нього (y − ŷ). */
    public double[] looResiduals() {
        return looResiduals.clone();
    }

    /** Обернення симетричної додатно визначеної матриці методом Гаусса–Жордана з вибором провідного елемента. */
    static double[][] invert(double[][] matrix) {
        int n = matrix.length;
        double[][] a = new double[n][2 * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(matrix[i], 0, a[i], 0, n);
            a[i][n + i] = 1;
        }
        for (int col = 0; col < n; col++) {
            int pivot = col;
            for (int row = col + 1; row < n; row++) {
                if (Math.abs(a[row][col]) > Math.abs(a[pivot][col])) {
                    pivot = row;
                }
            }
            double[] swap = a[col];
            a[col] = a[pivot];
            a[pivot] = swap;
            double divisor = a[col][col];
            for (int k = 0; k < 2 * n; k++) {
                a[col][k] /= divisor;
            }
            for (int row = 0; row < n; row++) {
                if (row != col) {
                    double factor = a[row][col];
                    for (int k = 0; k < 2 * n; k++) {
                        a[row][k] -= factor * a[col][k];
                    }
                }
            }
        }
        double[][] inverse = new double[n][n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(a[i], n, inverse[i], 0, n);
        }
        return inverse;
    }
}
