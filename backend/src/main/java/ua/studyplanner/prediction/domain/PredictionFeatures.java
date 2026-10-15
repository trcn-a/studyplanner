package ua.studyplanner.prediction.domain;

import java.util.*;
import ua.studyplanner.task.dto.TaskSample;
import ua.studyplanner.task.domain.*;
import ua.studyplanner.prediction.dto.*;

public final class PredictionFeatures {
    static final double SHRINK = 2.0;
    static double[] features(TaskSample s, Stats stats, Double ownLog, Double ownRatio) {
        Sums subject = stats.bySubject.getOrDefault(s.subjectId(), Sums.EMPTY);
        Sums total = stats.total;
        double ownR = ownRatio == null ? 0 : ownRatio;
        int ownRatioN = ownRatio == null ? 0 : 1;
        double ownL = ownLog == null ? 0 : ownLog;
        int ownN = ownLog == null ? 0 : 1;

        // Статистики історії рахуємо без самого прикладу, інакше ознака «підглядає» у відповідь.
        double subjectBias = (subject.ratioSum - ownR) / (subject.ratioCount - ownRatioN + SHRINK);
        double userBias = (total.ratioSum - ownR) / (total.ratioCount - ownRatioN + SHRINK);
        int totalN = total.count - ownN;
        double globalLog = totalN > 0 ? (total.logSum - ownL) / totalN : 0;
        double subjectLog = ((subject.logSum - ownL) + SHRINK * globalLog) / (subject.count - ownN + SHRINK);

        Integer estimate = s.estimatedMinutes();
        return new double[] {
            estimate != null ? 1 : 0,
            estimate != null ? Math.log(estimate) : 0,
            s.priority() == Priority.MEDIUM ? 1 : 0,
            s.priority() == Priority.HIGH ? 1 : 0,
            s.taskType() == TaskType.LABORATORY ? 1 : 0,
            s.taskType() == TaskType.PRACTICE ? 1 : 0,
            s.taskType() == TaskType.NOTES ? 1 : 0,
            s.taskType() == TaskType.EXAM_PREPARATION ? 1 : 0,
            s.taskType() == TaskType.OTHER ? 1 : 0,
            s.difficulty() == Difficulty.LOW ? 1 : 0,
            s.difficulty() == Difficulty.MEDIUM ? 1 : 0,
            s.difficulty() == Difficulty.HIGH ? 1 : 0,
            s.subtask() ? 1 : 0,
            Math.log1p(s.subtaskCount()),
            // Поправки на оцінку мають сенс лише для задач з оцінкою, середній час — лише для задач без неї.
            estimate != null ? subjectBias : 0,
            estimate != null ? userBias : 0,
            estimate == null ? subjectLog : 0
        };
    }

    static double offset(TaskSample s) {
        return s.estimatedMinutes() == null ? 0 : Math.log(s.estimatedMinutes());
    }

    static final class Sums {
        static final Sums EMPTY = new Sums();
        double ratioSum;
        int ratioCount;
        double logSum;
        int count;

        void add(TaskSample s) {
            double log = Math.log(s.actualMinutes());
            logSum += log;
            count++;
            if (s.estimatedMinutes() != null) {
                ratioSum += log - Math.log(s.estimatedMinutes());
                ratioCount++;
            }
        }
    }

    static final class Stats {
        final Map<UUID, Sums> bySubject = new HashMap<>();
        final Map<UUID, List<Integer>> actualsBySubject = new HashMap<>();
        final Sums total = new Sums();
        final List<Integer> actuals = new ArrayList<>();
        int withEstimate;
        double minLog = Double.MAX_VALUE;
        double maxLog = -Double.MAX_VALUE;

        static Stats of(List<TaskSample> history) {
            Stats stats = new Stats();
            for (TaskSample s : history) {
                stats.bySubject.computeIfAbsent(s.subjectId(), id -> new Sums()).add(s);
                stats.actualsBySubject.computeIfAbsent(s.subjectId(), id -> new ArrayList<>()).add(s.actualMinutes());
                stats.total.add(s);
                if (s.estimatedMinutes() != null) {
                    stats.withEstimate++;
                }
                stats.actuals.add(s.actualMinutes());
                double log = Math.log(s.actualMinutes());
                stats.minLog = Math.min(stats.minLog, log);
                stats.maxLog = Math.max(stats.maxLog, log);
            }
            return stats;
        }

        /** Медіана фактичного часу по предмету, а за його відсутності — по всіх задачах. */
        double median(UUID subjectId) {
            List<Integer> values = actualsBySubject.getOrDefault(subjectId, actuals);
            List<Integer> sorted = values.stream().sorted().toList();
            int mid = sorted.size() / 2;
            return sorted.size() % 2 == 1 ? sorted.get(mid) : (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
        }
    }
}
