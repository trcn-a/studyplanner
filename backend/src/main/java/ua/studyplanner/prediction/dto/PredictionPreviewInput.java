package ua.studyplanner.prediction.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import ua.studyplanner.task.dto.TaskInput;

public record PredictionPreviewInput(@NotNull UUID subjectId, UUID taskId, UUID parentId,
        @NotNull @Valid TaskInput input) {}
