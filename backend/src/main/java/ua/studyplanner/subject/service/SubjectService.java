package ua.studyplanner.subject.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ua.studyplanner.common.exception.ResourceNotFoundException;
import ua.studyplanner.subject.domain.Subject;
import ua.studyplanner.subject.dto.SubjectInput;
import ua.studyplanner.subject.dto.SubjectView;
import ua.studyplanner.subject.repository.SubjectRepository;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class SubjectService {
    private final SubjectRepository subjects;
    private final ua.studyplanner.account.service.AccountAccess accounts;
    private void lockOwner(){accounts.lockCurrent();}

    public SubjectService(SubjectRepository subjects,ua.studyplanner.account.service.AccountAccess accounts) {
        this.accounts=accounts;
        this.subjects = subjects;
    }

    @Transactional(readOnly = true)
    public List<SubjectView> subjects() {
        return subjects.findByUserIdOrderByCreatedAtAsc(ua.studyplanner.account.security.CurrentUser.id()).stream().map(SubjectView::of).toList();
    }

    public SubjectView createSubject(SubjectInput input) {
        Subject subject = new Subject(input.name(), input.description(), input.color());
        subject.assignUser(ua.studyplanner.account.security.CurrentUser.id());
        return SubjectView.of(subjects.save(subject));
    }

    public SubjectView updateSubject(UUID id, SubjectInput input) {
        lockOwner();
        Subject subject = subjects.findLockedByIdAndUserId(id,ua.studyplanner.account.security.CurrentUser.id()).orElseThrow(SubjectService::notFound);
        subject.update(input.name(), input.description(), input.color());
        return SubjectView.of(subject);
    }

    @Transactional(readOnly = true)
    public void requireExists(UUID id) {
        if (!subjects.existsByIdAndUserId(id,ua.studyplanner.account.security.CurrentUser.id())) {
            throw notFound();
        }
    }

    // A caller's transaction retains this lock until its whole operation completes.
    public void lockForTaskChange(UUID id) {
        lockOwner();
        subjects.findLockedByIdAndUserId(id,ua.studyplanner.account.security.CurrentUser.id()).orElseThrow(SubjectService::notFound);
    }

    private static ResourceNotFoundException notFound() {
        return new ResourceNotFoundException("Предмет не знайдено");
    }
}

