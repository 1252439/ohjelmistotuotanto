package fi.seamk.kodera.controller;

import fi.seamk.kodera.model.Assignment;
import fi.seamk.kodera.service.AssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@CrossOrigin(origins = "*")
public class AssignmentController {
    
    @Autowired
    private AssignmentService assignmentService;
    
    @GetMapping("/health")
    public String health() {
        return "Kodera API is running!";
    }
    
    @PostMapping
    public ResponseEntity<Assignment> createAssignment(
            @RequestBody Assignment assignment,
            @RequestHeader(value = "X-Teacher-ID", defaultValue = "1") Long teacherId) {
        Assignment created = assignmentService.createAssignment(assignment, teacherId);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }
    
    @PostMapping("/{id}/publish")
    public ResponseEntity<Assignment> publishAssignment(@PathVariable Long id) {
        Assignment published = assignmentService.publishAssignment(id);
        return ResponseEntity.ok(published);
    }
    
    @GetMapping
    public ResponseEntity<List<Assignment>> getAllAssignments() {
        List<Assignment> assignments = assignmentService.getAllAssignments();
        return ResponseEntity.ok(assignments);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Assignment> getAssignment(@PathVariable Long id) {
        return assignmentService.getAssignment(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<Assignment>> getTeacherAssignments(@PathVariable Long teacherId) {
        List<Assignment> assignments = assignmentService.getAssignmentsByTeacher(teacherId);
        return ResponseEntity.ok(assignments);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable Long id) {
        assignmentService.deleteAssignment(id);
        return ResponseEntity.noContent().build();
    }
}
