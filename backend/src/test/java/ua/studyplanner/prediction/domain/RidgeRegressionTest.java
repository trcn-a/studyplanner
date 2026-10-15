package ua.studyplanner.prediction.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RidgeRegressionTest {
    @Test
    void invertsMatrix() {
        double[][] inverse = RidgeRegression.invert(new double[][] {{4, 1}, {1, 3}});
        assertEquals(3.0 / 11, inverse[0][0], 1e-9);
        assertEquals(-1.0 / 11, inverse[0][1], 1e-9);
        assertEquals(4.0 / 11, inverse[1][1], 1e-9);
    }

    @Test
    void recoversLinearRelationWithSmallRegularization() {
        double[][] x = new double[20][2];
        double[] y = new double[20];
        for (int i = 0; i < 20; i++) {
            x[i][0] = i;
            x[i][1] = (i * 7) % 5;
            y[i] = 2 + 0.5 * x[i][0] - 0.3 * x[i][1];
        }
        RidgeRegression model = RidgeRegression.fit(x, y, 1e-6);
        assertEquals(2 + 0.5 * 10 - 0.3 * 2, model.predict(new double[] {10, 2}), 1e-3);
        for (double residual : model.looResiduals()) {
            assertEquals(0, residual, 1e-3);
        }
    }

    @Test
    void looResidualsAreLargerThanTrainingNoise() {
        double[][] x = new double[12][1];
        double[] y = new double[12];
        for (int i = 0; i < 12; i++) {
            x[i][0] = i;
            y[i] = i + (i % 2 == 0 ? 0.5 : -0.5);
        }
        RidgeRegression model = RidgeRegression.fit(x, y, 0.1);
        double largest = 0;
        for (double residual : model.looResiduals()) {
            largest = Math.max(largest, Math.abs(residual));
        }
        assertTrue(largest >= 0.5);
    }
}
