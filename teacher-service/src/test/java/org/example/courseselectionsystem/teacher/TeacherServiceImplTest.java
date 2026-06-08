package org.example.courseselectionsystem.teacher;

import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.mapper.TeacherMapper;
import org.example.courseselectionsystem.service.impl.TeacherServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceImplTest {

    @Mock
    private TeacherMapper teacherMapper;

    @Test
    void updateTeacherFromMapPreservesExistingPasswordAndTitleWhenOmitted() {
        TeacherServiceImpl service = newService();
        Teacher teacher = new Teacher();
        teacher.setId(3L);
        teacher.setTeacherNo("T1001");
        teacher.setName("教师");
        teacher.setGender("女");
        teacher.setPassword("old-pass");
        teacher.setTitle("教授");
        teacher.setDepartmentId(5L);
        teacher.setStatus(1);
        when(teacherMapper.selectById(3L)).thenReturn(teacher);
        when(teacherMapper.countByTeacherNo("T1001", 3L)).thenReturn(0);
        when(teacherMapper.updateById(any(Teacher.class))).thenReturn(1);

        boolean result = service.updateTeacher(Map.of(
                "id", 3L,
                "name", "新教师",
                "email", "teacher@example.edu.cn"
        ));

        assertThat(result).isTrue();
        assertThat(teacher.getName()).isEqualTo("新教师");
        assertThat(teacher.getPassword()).isEqualTo("old-pass");
        assertThat(teacher.getTitle()).isEqualTo("教授");
        assertThat(teacher.getDepartmentId()).isEqualTo(5L);
    }

    private TeacherServiceImpl newService() {
        TeacherServiceImpl service = new TeacherServiceImpl();
        ReflectionTestUtils.setField(service, "teacherMapper", teacherMapper);
        return service;
    }
}
