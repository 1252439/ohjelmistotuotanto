package fi.seamk.kodera.service;

import fi.seamk.kodera.model.Submission;
import fi.seamk.kodera.repository.SubmissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private SubmissionService submissionService;

    private Submission testSubmission;

    @BeforeEach
    void setUp() {
        testSubmission = new Submission();
        testSubmission.setId(1L);
        testSubmission.setAssignmentId(1L);
        testSubmission.setStudentId(1L);
        testSubmission.setSubmittedAt(LocalDateTime.now());
    }

    @Test
    void testSubmitCode() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "solution.zip", 
            "application/zip", "mock zip content".getBytes());

        when(submissionRepository.save(any(Submission.class))).thenReturn(testSubmission);

        Submission submitted = submissionService.submitCode(1L, 1L, file);

        assertNotNull(submitted);
        assertEquals(1L, submitted.getAssignmentId());
        assertEquals(1L, submitted.getStudentId());
        verify(submissionRepository, times(1)).save(any(Submission.class));
    }

    @Test
    void testGetSubmission() {
        when(submissionRepository.findById(1L)).thenReturn(Optional.of(testSubmission));

        Optional<Submission> found = submissionService.getSubmission(1L);

        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getId());
    }

    @Test
    void testGetSubmissionNotFound() {
        when(submissionRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Submission> found = submissionService.getSubmission(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void testDeleteSubmission() {
        submissionService.deleteSubmission(1L);

        verify(submissionRepository, times(1)).deleteById(1L);
    }
}
