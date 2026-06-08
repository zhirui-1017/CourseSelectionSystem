package org.example.courseselectionsystem.selection;

import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.repository.CourseSelectionRepository;
import org.example.courseselectionsystem.service.impl.CourseSelectionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseSelectionServiceImplTest {

    @Mock
    private CourseSelectionRepository courseSelectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Test
    void getSelectionStatsReturnsSelectionAndCourseCounts() {
        CourseSelectionServiceImpl service = new CourseSelectionServiceImpl();
        ReflectionTestUtils.setField(service, "courseSelectionRepository", courseSelectionRepository);
        ReflectionTestUtils.setField(service, "courseRepository", courseRepository);
        when(courseSelectionRepository.count()).thenReturn(12L);
        when(courseRepository.count()).thenReturn(5L);

        Map<String, Object> stats = service.getSelectionStats();

        assertThat(stats).containsEntry("selectionCount", 12L);
        assertThat(stats).containsEntry("courseCount", 5L);
    }
}
