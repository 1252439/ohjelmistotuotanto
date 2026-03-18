package fi.seamk.kodera.service;

import fi.seamk.kodera.model.Submission;
import fi.seamk.kodera.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SubmissionService {
    
    @Autowired
    private SubmissionRepository submissionRepository;
    
    @Transactional
    public Submission submitCode(Long assignmentId, Long studentId, MultipartFile zipFile) throws IOException {
        Submission submission = new Submission();
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(studentId);
        submission.setCodeZip(zipFile.getBytes());
        submission.setSubmittedAt(LocalDateTime.now());
        return submissionRepository.save(submission);
    }
    
    public Optional<Submission> getSubmission(Long id) {
        return submissionRepository.findById(id);
    }
    
    public List<Submission> getAllSubmissions() {
        return submissionRepository.findAll();
    }
    
    public List<Submission> getSubmissionsByAssignment(Long assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }
    
    public Optional<Submission> getSubmissionByAssignmentAndStudent(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);
    }
    
    @Transactional
    public void deleteSubmission(Long id) {
        submissionRepository.deleteById(id);
    }
}
