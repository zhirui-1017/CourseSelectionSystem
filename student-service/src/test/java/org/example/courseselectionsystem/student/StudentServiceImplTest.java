package org.example.courseselectionsystem.student;

import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.mapper.StudentMapper;
import org.example.courseselectionsystem.service.impl.StudentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentMapper studentMapper;

    @Test
    void resetPasswordUsesLastSixDigitsOfStudentNo() {
        StudentServiceImpl service = newService();
        Student student = student(7L, "S20230088", "old");
        when(studentMapper.selectById(7L)).thenReturn(student);
        when(studentMapper.updateById(any(Student.class))).thenAnswer(invocation -> invocation.getArgument(0) == null ? 0 : 1);

        boolean result = service.resetPassword(7L);

        assertThat(result).isTrue();
        assertThat(student.getPassword()).isEqualTo("230088");
    }

    @Test
    void changePasswordChecksOldPasswordAndSavesNewPassword() {
        StudentServiceImpl service = newService();
        Student student = student(7L, "S20230088", "old123");
        when(studentMapper.selectById(7L)).thenReturn(student);
        when(studentMapper.updateById(any(Student.class))).thenReturn(1);

        boolean result = service.changePassword(7L, "old123", "new123");

        assertThat(result).isTrue();
        assertThat(student.getPassword()).isEqualTo("new123");
    }

    @Test
    void changePasswordRejectsWrongOldPassword() {
        StudentServiceImpl service = newService();
        when(studentMapper.selectById(7L)).thenReturn(student(7L, "S20230088", "old123"));

        assertThatThrownBy(() -> service.changePassword(7L, "bad", "new123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("旧密码不正确");
    }

    @Test
    void updateStudentFromMapPreservesExistingPasswordAndClassWhenOmitted() {
        StudentServiceImpl service = newService();
        Student student = student(7L, "S20230088", "old123");
        student.setClassName("软件一班");
        student.setMajorId(3L);
        student.setCollegeId(2L);
        when(studentMapper.selectById(7L)).thenReturn(student);
        when(studentMapper.countByStudentNo("S20230088", 7L)).thenReturn(0);
        when(studentMapper.updateById(any(Student.class))).thenReturn(1);

        boolean result = service.updateStudent(Map.of(
                "id", 7L,
                "name", "新姓名",
                "email", "new@example.edu.cn"
        ));

        assertThat(result).isTrue();
        assertThat(student.getName()).isEqualTo("新姓名");
        assertThat(student.getPassword()).isEqualTo("old123");
        assertThat(student.getClassName()).isEqualTo("软件一班");
    }

    private StudentServiceImpl newService() {
        StudentServiceImpl service = new StudentServiceImpl();
        ReflectionTestUtils.setField(service, "studentMapper", studentMapper);
        return service;
    }

    private Student student(Long id, String studentNo, String password) {
        Student student = new Student();
        student.setId(id);
        student.setStudentNo(studentNo);
        student.setName("学生");
        student.setGender("男");
        student.setPassword(password);
        student.setMajorId(1L);
        student.setCollegeId(1L);
        student.setClassName("未分班");
        student.setStatus(1);
        return student;
    }
}
