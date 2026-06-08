package org.example.courseselectionsystem.student;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.mapper.StudentMapper;
import org.example.courseselectionsystem.service.impl.StudentServiceImpl;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplTest {

    @Mock
    private StudentMapper studentMapper;

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
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("ORDER BY id ASC");
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(100);
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
            Student student = new Student();
            student.setId(7L);
            student.setStudentNo("S20230088");
            student.setName("学生");
            page.setRecords(List.of(student));
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
}
