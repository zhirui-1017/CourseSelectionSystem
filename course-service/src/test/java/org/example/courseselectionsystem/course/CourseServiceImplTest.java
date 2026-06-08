package org.example.courseselectionsystem.course;

import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.service.impl.CourseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Test
    void updateCourseFromMapPreservesExistingHiddenFieldsWhenOmitted() {
        CourseServiceImpl service = newService();
        Course course = new Course();
        course.setId(9L);
        course.setCourseCode("CS101");
        course.setCourseName("旧课程");
        course.setTeacherId(3L);
        course.setCredit(2D);
        course.setTotalHours(32);
        course.setAvailableSlots(40);
        course.setSelectedCount(12);
        course.setClassroom("A101");
        course.setSchedule("周一1-2节");
        course.setCourseType("必修课");
        course.setDescription("旧介绍");
        course.setStatus(1);
        when(courseRepository.findById(9L)).thenReturn(Optional.of(course));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Course updated = service.updateCourse(Map.of(
                "id", 9L,
                "courseName", "新课程",
                "credit", 3D,
                "availableSlots", 60,
                "description", "新介绍"
        ));

        assertThat(updated.getCourseName()).isEqualTo("新课程");
        assertThat(updated.getCredit()).isEqualTo(3D);
        assertThat(updated.getAvailableSlots()).isEqualTo(60);
        assertThat(updated.getDescription()).isEqualTo("新介绍");
        assertThat(updated.getCourseCode()).isEqualTo("CS101");
        assertThat(updated.getTeacherId()).isEqualTo(3L);
        assertThat(updated.getTotalHours()).isEqualTo(32);
        assertThat(updated.getSelectedCount()).isEqualTo(12);
        assertThat(updated.getClassroom()).isEqualTo("A101");
        assertThat(updated.getSchedule()).isEqualTo("周一1-2节");
        assertThat(updated.getCourseType()).isEqualTo("必修课");
        assertThat(updated.getStatus()).isEqualTo(1);
    }

    private CourseServiceImpl newService() {
        CourseServiceImpl service = new CourseServiceImpl();
        ReflectionTestUtils.setField(service, "courseRepository", courseRepository);
        return service;
    }
}
