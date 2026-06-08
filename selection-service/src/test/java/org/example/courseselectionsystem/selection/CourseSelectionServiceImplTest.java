package org.example.courseselectionsystem.selection;

import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.entity.CourseSelection;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.repository.CourseSelectionRepository;
import org.example.courseselectionsystem.repository.StudentRepository;
import org.example.courseselectionsystem.service.impl.CourseSelectionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseSelectionServiceImplTest {

    @Mock
    private CourseSelectionRepository courseSelectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private StudentRepository studentRepository;

    @Test
    void getSelectionStatsReturnsSelectionAndCourseCounts() {
        CourseSelectionServiceImpl service = newService();
        when(courseSelectionRepository.count()).thenReturn(12L);
        when(courseRepository.count()).thenReturn(5L);

        Map<String, Object> stats = service.getSelectionStats();

        assertThat(stats).containsEntry("selectionCount", 12L);
        assertThat(stats).containsEntry("courseCount", 5L);
    }

    @Test
    void updateGradeCalculatesScoreAndChecksTeacherOwnership() {
        CourseSelectionServiceImpl service = newService();
        CourseSelection selection = new CourseSelection();
        selection.setId(9L);
        selection.setStudentId(3L);
        selection.setCourseId(7L);
        selection.setStatus(1);
        Course course = new Course();
        course.setId(7L);
        course.setCourseCode("CS101");
        course.setCourseName("Java");
        course.setTeacherId(2L);
        when(courseSelectionRepository.findById(9L)).thenReturn(Optional.of(selection));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(courseSelectionRepository.save(any(CourseSelection.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, Object> payload = new HashMap<>();
        payload.put("dailyGrade", 90);
        payload.put("labGrade", 80);
        payload.put("examGrade", 70);
        payload.put("remark", "ok");

        Map<String, Object> row = service.updateGrade(9L, 2L, payload);

        assertThat(row).containsEntry("selectionId", 9L)
                .containsEntry("courseName", "Java")
                .containsEntry("score", 80D)
                .containsEntry("scoreLevel", "良好")
                .containsEntry("remark", "ok");
    }

    @Test
    void updateGradeRejectsOtherTeacherCourse() {
        CourseSelectionServiceImpl service = newService();
        CourseSelection selection = new CourseSelection();
        selection.setId(9L);
        selection.setCourseId(7L);
        Course course = new Course();
        course.setId(7L);
        course.setTeacherId(2L);
        when(courseSelectionRepository.findById(9L)).thenReturn(Optional.of(selection));
        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> service.updateGrade(9L, 3L, Map.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权访问该课程");
    }

    @Test
    void getTeacherCourseStudentsReturnsStudentDetailsAndChecksOwnership() {
        CourseSelectionServiceImpl service = newService();
        Course course = new Course();
        course.setId(7L);
        course.setCourseCode("CS101");
        course.setCourseName("Java");
        course.setTeacherId(2L);
        CourseSelection selection = new CourseSelection();
        selection.setId(9L);
        selection.setStudentId(3L);
        selection.setCourseId(7L);
        selection.setStatus(1);
        selection.setScore(88D);
        Student student = new Student();
        student.setId(3L);
        student.setStudentNo("S003");
        student.setName("Alice");
        student.setClassName("CS-1");
        student.setPhone("13800000000");

        when(courseRepository.findById(7L)).thenReturn(Optional.of(course));
        when(courseSelectionRepository.findByCourseIdAndStatus(7L, 1)).thenReturn(List.of(selection));
        when(studentRepository.findById(3L)).thenReturn(Optional.of(student));

        List<Map<String, Object>> rows = service.getTeacherCourseStudents(7L, 2L, 1);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("selectionId", 9L)
                .containsEntry("studentNo", "S003")
                .containsEntry("studentName", "Alice")
                .containsEntry("className", "CS-1")
                .containsEntry("courseName", "Java")
                .containsEntry("scoreLevel", "良好");
    }

    @Test
    void getTeacherDashboardAggregatesCoursesSelections() {
        CourseSelectionServiceImpl service = newService();
        Course course = new Course();
        course.setId(7L);
        course.setCourseCode("CS101");
        course.setCourseName("Java");
        course.setTeacherId(2L);
        CourseSelection selected = new CourseSelection();
        selected.setId(9L);
        selected.setStudentId(3L);
        selected.setCourseId(7L);
        selected.setStatus(1);
        selected.setScore(80D);
        CourseSelection waiting = new CourseSelection();
        waiting.setId(10L);
        waiting.setStudentId(4L);
        waiting.setCourseId(7L);
        waiting.setStatus(3);

        when(courseRepository.findByTeacherId(2L)).thenReturn(List.of(course));
        when(courseSelectionRepository.findByCourseId(7L)).thenReturn(List.of(selected, waiting));
        when(studentRepository.findById(3L)).thenReturn(Optional.empty());

        Map<String, Object> dashboard = service.getTeacherDashboard(2L);

        assertThat(dashboard).containsEntry("courseCount", 1)
                .containsEntry("studentCount", 1L)
                .containsEntry("gradedCount", 1L)
                .containsEntry("waitingCount", 1L)
                .containsEntry("averageScore", 80D);
        assertThat((List<?>) dashboard.get("recentSelections")).hasSize(1);
    }

    private CourseSelectionServiceImpl newService() {
        CourseSelectionServiceImpl service = new CourseSelectionServiceImpl();
        ReflectionTestUtils.setField(service, "courseSelectionRepository", courseSelectionRepository);
        ReflectionTestUtils.setField(service, "courseRepository", courseRepository);
        ReflectionTestUtils.setField(service, "studentRepository", studentRepository);
        return service;
    }
}
