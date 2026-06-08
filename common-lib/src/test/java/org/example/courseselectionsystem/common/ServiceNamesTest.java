package org.example.courseselectionsystem.common;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceNamesTest {

    @Test
    void serviceNamesMatchRegisteredSpringApplicationNames() {
        assertThat(Set.of(
                ServiceNames.WEB_SERVICE,
                ServiceNames.USER_SERVICE,
                ServiceNames.STUDENT_SERVICE,
                ServiceNames.TEACHER_SERVICE,
                ServiceNames.COURSE_SERVICE,
                ServiceNames.SELECTION_SERVICE
        )).containsExactlyInAnyOrder(
                "web-service",
                "user-service",
                "student-service",
                "teacher-service",
                "course-service",
                "selection-service"
        );
    }
}
