package ua.studyplanner.planning.dto;
import java.time.*;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record ImportInput(@NotBlank @Size(max=1000000) String content) {}
