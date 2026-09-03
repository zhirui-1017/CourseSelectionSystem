package org.example.courseselectionsystem.student;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.mapper.StudentMapper;
import org.example.courseselectionsystem.service.impl.StudentServiceImpl;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
        assertThat(student.getPassword()).isNotEqualTo("230088");
        assertThat(new BCryptPasswordEncoder().matches("230088", student.getPassword())).isTrue();
    }

    @Test
    void changePasswordChecksOldPasswordAndSavesNewPassword() {
        StudentServiceImpl service = newService();
        Student student = student(7L, "S20230088", "old123");
        when(studentMapper.selectById(7L)).thenReturn(student);
        when(studentMapper.updateById(any(Student.class))).thenReturn(1);

        boolean result = service.changePassword(7L, "old123", "new123");

        assertThat(result).isTrue();
        assertThat(student.getPassword()).isNotEqualTo("new123");
        assertThat(new BCryptPasswordEncoder().matches("new123", student.getPassword())).isTrue();
    }

    @Test
    void changePasswordWorksWithBcryptStoredPassword() {
        StudentServiceImpl service = newService();
        Student student = student(7L, "S20230088", new BCryptPasswordEncoder().encode("old123"));
        when(studentMapper.selectById(7L)).thenReturn(student);
        when(studentMapper.updateById(any(Student.class))).thenReturn(1);

        boolean result = service.changePassword(7L, "old123", "new456");

        assertThat(result).isTrue();
        assertThat(new BCryptPasswordEncoder().matches("new456", student.getPassword())).isTrue();
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

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getStudentsByPageNormalizesPagingAndFallsBackToSafeSort() {
        StudentServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(0);
        request.setPageSize(500);
        request.setOrderByColumn("unsafeField");
        when(studentMapper.selectPage(any(IPage.class), any(QueryWrapper.class))).thenAnswer(invocation -> {
            IPage<Student> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        PageResult<Student> result = service.getStudentsByPage(request);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        ArgumentCaptor<QueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(studentMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(500);
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("ORDER BY id ASC");
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(500);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getStudentsByPageAppliesSearchFiltersAndSortAlias() {
        StudentServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(2);
        request.setPageSize(5);
        request.setOrderByColumn("studentNo");
        request.setIsAsc("desc");
        request.setSearchField("studentName");
        request.setSearchValue("张");
        request.setParams(Map.of(
                "studentNo", "S2023",
                "departmentId", "6",
                "majorId", "2",
                "status", "1"
        ));
        when(studentMapper.selectPage(any(IPage.class), any(QueryWrapper.class))).thenAnswer(invocation -> {
            IPage<Student> page = invocation.getArgument(0);
            page.setRecords(List.of(student(7L, "S20230088", "old123")));
            page.setTotal(1);
            return page;
        });

        PageResult<Student> result = service.getStudentsByPage(request);

        ArgumentCaptor<QueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(studentMapper).selectPage(any(IPage.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("name LIKE");
        assertThat(sqlSegment).contains("student_no LIKE");
        assertThat(sqlSegment).contains("major_id =");
        assertThat(sqlSegment).contains("SELECT id FROM major WHERE department_id");
        assertThat(sqlSegment).contains("status =");
        assertThat(sqlSegment).contains("ORDER BY student_no DESC");
        assertThat(result.getItems()).extracting(Student::getStudentNo).containsExactly("S20230088");
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
