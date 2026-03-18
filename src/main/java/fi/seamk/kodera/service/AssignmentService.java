package fi.seamk.kodera.service;

import fi.seamk.kodera.model.Assignment;
import fi.seamk.kodera.repository.AssignmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AssignmentService {
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Transactional
    public Assignment createAssignment(Assignment assignment, Long teacherId) {
        assignment.setTeacherId(teacherId);
        assignment.setCreatedAt(LocalDateTime.now());
        return assignmentRepository.save(assignment);
    }
    
    @Transactional
    public Assignment publishAssignment(Long assignmentId) {
        Optional<Assignment> assignment = assignmentRepository.findById(assignmentId);
        if (assignment.isPresent()) {
            Assignment a = assignment.get();
            a.setPublishedAt(LocalDateTime.now());
            return assignmentRepository.save(a);
        }
        throw new RuntimeException("Assignment not found");
    }
    
    public Optional<Assignment> getAssignment(Long id) {
        return assignmentRepository.findById(id);
    }
    
    public List<Assignment> getAssignmentsByTeacher(Long teacherId) {
        return assignmentRepository.findByTeacherId(teacherId);
    }
    
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }
    
    @Transactional
    public void deleteAssignment(Long id) {
        assignmentRepository.deleteById(id);
    }
}
