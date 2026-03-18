package fi.seamk.kodera.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "grades")
public class Grade {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "submission_id", nullable = false, unique = true)
    private Long submissionId;
    
    @Column(columnDefinition = "LONGTEXT")
    private String geminiAnalysis;
    
    @Column(nullable = false)
    private Integer points;
    
    @Column(columnDefinition = "TEXT")
    private String feedback;
    
    @Column(name = "graded_at")
    private LocalDateTime gradedAt;
    
    @PrePersist
    protected void onCreate() {
        gradedAt = LocalDateTime.now();
    }
    
    // Gettersit ja settersit
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getSubmissionId() { return submissionId; }
    public void setSubmissionId(Long submissionId) { this.submissionId = submissionId; }
    
    public String getGeminiAnalysis() { return geminiAnalysis; }
    public void setGeminiAnalysis(String geminiAnalysis) { this.geminiAnalysis = geminiAnalysis; }
    
    public Integer getPoints() { return points; }
    public void setPoints(Integer points) { this.points = points; }
    
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    
    public LocalDateTime getGradedAt() { return gradedAt; }
    public void setGradedAt(LocalDateTime gradedAt) { this.gradedAt = gradedAt; }
}
