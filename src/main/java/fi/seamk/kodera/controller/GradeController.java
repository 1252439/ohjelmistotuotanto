package fi.seamk.kodera.controller;

import fi.seamk.kodera.model.Grade;
import fi.seamk.kodera.service.GradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/grades")
@CrossOrigin(origins = "*")
public class GradeController {
    
    @Autowired
    private GradeService gradeService;
    
    @PostMapping("/{submissionId}")
    public ResponseEntity<Grade> gradeSubmission(@PathVariable Long submissionId) {
        try {
            Grade grade = gradeService.gradeSubmission(submissionId);
            return new ResponseEntity<>(grade, HttpStatus.CREATED);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{submissionId}")
    public ResponseEntity<Grade> getGrade(@PathVariable Long submissionId) {
        return gradeService.getGrade(submissionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}
