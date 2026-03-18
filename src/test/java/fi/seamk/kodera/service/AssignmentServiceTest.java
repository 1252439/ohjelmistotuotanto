package fi.seamk.kodera.service;

import fi.seamk.kodera.model.Assignment;
import fi.seamk.kodera.repository.AssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @InjectMocks
    private AssignmentService assignmentService;

    private Assignment testAssignment;

    @BeforeEach
    void setUp() {
        testAssignment = new Assignment();
        testAssignment.setId(1L);
        testAssignment.setTitle("Mergesort Algoritmi");
        testAssignment.setDescription("Toteuta mergesort algoritmi");
        testAssignment.setTeacherId(1L);
        testAssignment.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateAssignment() {
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(testAssignment);

        Assignment created = assignmentService.createAssignment(testAssignment, 1L);

        assertNotNull(created);
        assertEquals("Mergesort Algoritmi", created.getTitle());
        verify(assignmentRepository, times(1)).save(any(Assignment.class));
    }

    @Test
    void testGetAssignment() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));

        Optional<Assignment> found = assignmentService.getAssignment(1L);

        assertTrue(found.isPresent());
        assertEquals("Mergesort Algoritmi", found.get().getTitle());
        verify(assignmentRepository, times(1)).findById(1L);
    }

    @Test
    void testGetAssignmentNotFound() {
        when(assignmentRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Assignment> found = assignmentService.getAssignment(999L);

        assertFalse(found.isPresent());
    }

    @Test
    void testPublishAssignment() {
        when(assignmentRepository.findById(1L)).thenReturn(Optional.of(testAssignment));
        when(assignmentRepository.save(any(Assignment.class))).thenReturn(testAssignment);

        assignmentService.publishAssignment(1L);

        assertNotNull(testAssignment.getPublishedAt());
        verify(assignmentRepository, times(1)).save(testAssignment);
    }

    @Test
    void testDeleteAssignment() {
        assignmentService.deleteAssignment(1L);

        verify(assignmentRepository, times(1)).deleteById(1L);
    }
}
