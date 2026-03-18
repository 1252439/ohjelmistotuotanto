package fi.seamk.kodera.service;

import fi.seamk.kodera.client.GeminiClient;
import fi.seamk.kodera.model.Assignment;
import fi.seamk.kodera.model.Grade;
import fi.seamk.kodera.model.Submission;
import fi.seamk.kodera.repository.AssignmentRepository;
import fi.seamk.kodera.repository.GradeRepository;
import fi.seamk.kodera.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private GradeService gradeService;

    private Submission testSubmission;
    private Assignment testAssignment;
    private Grade testGrade;

    @BeforeEach
    void setUp() {
        testAssignment = new Assignment();
        testAssignment.setId(1L);
        testAssignment.setTitle("Mergesort");
        testAssignment.setDescription("Toteuta mergesort");
        testAssignment.setTeacherId(1L);

        testSubmission = new Submission();
        testSubmission.setId(1L);
        testSubmission.setAssignmentId(1L);
        testSubmission.setStudentId(1L);
        testSubmission.setCodeZip("mock zip content".getBytes());

        testGrade = new Grade();
        testGrade.setId(1L);
        testGrade.setSubmissionId(1L);
        testGrade.setPoints(85);
        testGrade.setGeminiAnalysis("Good implementation");
    }

    @Test
    void testGradeSubmission() throws Exception {
        String mockResponse = "Koodi on hyvää. Antaa 85 pistettä.";

        when(submissionRepository.findById(1L)).thenReturn(Optional.of(testSubmission));
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));
        when(geminiClient.generateAnalysis(any(String.class))).thenReturn(mockResponse);
        when(gradeRepository.save(any(Grade.class))).thenReturn(testGrade);

        Grade result = gradeService.gradeSubmission(1L);

        assertNotNull(result);
        assertEquals(1L, result.getSubmissionId());
        verify(geminiClient, times(1)).generateAnalysis(any(String.class));
        verify(gradeRepository, times(1)).save(any(Grade.class));
    }

    @Test
    void testGradeSubmissionNotFound() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            gradeService.gradeSubmission(999L);
        });
    }

    // Poistetaan extractPointsFromAnalysis-testi koska metodi on private
    // @Test
    // void testExtractPoints() {
    //     assertEquals(100, gradeService.extractPointsFromAnalysis("100 pistettä"));
    // }

    @Test
    void testGetGrade() {
        when(gradeRepository.findBySubmissionId(1L)).thenReturn(Optional.of(testGrade));

        Optional<Grade> found = gradeService.getGrade(1L);

        assertTrue(found.isPresent());
        assertEquals(85, found.get().getPoints());
    }
}
