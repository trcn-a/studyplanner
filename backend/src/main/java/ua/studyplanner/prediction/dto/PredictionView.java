package ua.studyplanner.prediction.dto;

import ua.studyplanner.prediction.domain.Confidence;
import ua.studyplanner.prediction.domain.PredictionMethod;

import java.util.UUID;

/** Прогноз тривалості задачі: очікуване значення та типовий діапазон у хвилинах. */
public record PredictionView(
        UUID taskId,
        int minutes,
        int lowMinutes,
        int highMinutes,
        PredictionMethod method,
        Confidence confidence,
        int samples) {}
