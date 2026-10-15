package ua.studyplanner.prediction.dto;

import java.util.List;

public record PredictionReport(ModelInfo model, List<PredictionView> predictions) {}
