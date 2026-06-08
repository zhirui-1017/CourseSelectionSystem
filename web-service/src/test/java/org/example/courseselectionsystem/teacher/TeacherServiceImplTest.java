package org.example.courseselectionsystem.teacher;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.mapper.TeacherMapper;
import org.example.courseselectionsystem.service.impl.TeacherServiceImpl;
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
class TeacherServiceImplTest {

    @Mock
    private TeacherMapper teacherMapper;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getTeachersByPageNormalizesPagingAndFallsBackToSafeSort() {
        TeacherServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(0);
        request.setPageSize(500);
        request.setOrderByColumn("unsafeField");
        when(teacherMapper.selectPage(any(IPage.class), any(QueryWrapper.class))).thenAnswer(invocation -> {
            IPage<Teacher> page = invocation.getArgument(0);
            page.setRecords(List.of());
            page.setTotal(0);
            return page;
        });

        PageResult<Teacher> result = service.getTeachersByPage(request);

        ArgumentCaptor<IPage> pageCaptor = ArgumentCaptor.forClass(IPage.class);
        ArgumentCaptor<QueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(teacherMapper).selectPage(pageCaptor.capture(), wrapperCaptor.capture());
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(100);
        assertThat(wrapperCaptor.getValue().getSqlSegment()).contains("ORDER BY id ASC");
        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(100);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getTeachersByPageAppliesSearchFiltersAndSortAlias() {
        TeacherServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(2);
        request.setPageSize(5);
        request.setOrderByColumn("teacherNo");
        request.setIsAsc("desc");
        request.setSearchField("teacherName");
        request.setSearchValue("李");
        request.setParams(Map.of(
                "teacherNo", "T100",
                "departmentId", "5",
                "title", "教授",
                "status", "1"
        ));
        when(teacherMapper.selectPage(any(IPage.class), any(QueryWrapper.class))).thenAnswer(invocation -> {
            IPage<Teacher> page = invocation.getArgument(0);
            Teacher teacher = new Teacher();
            teacher.setId(3L);
            teacher.setTeacherNo("T1001");
            teacher.setName("教师");
            page.setRecords(List.of(teacher));
            page.setTotal(1);
            return page;
        });

        PageResult<Teacher> result = service.getTeachersByPage(request);

        ArgumentCaptor<QueryWrapper> wrapperCaptor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(teacherMapper).selectPage(any(IPage.class), wrapperCaptor.capture());
        String sqlSegment = wrapperCaptor.getValue().getSqlSegment();
        assertThat(sqlSegment).contains("name LIKE");
        assertThat(sqlSegment).contains("teacher_no LIKE");
        assertThat(sqlSegment).contains("department_id =");
        assertThat(sqlSegment).contains("title LIKE");
        assertThat(sqlSegment).contains("status =");
        assertThat(sqlSegment).contains("ORDER BY teacher_no DESC");
        assertThat(result.getItems()).extracting(Teacher::getTeacherNo).containsExactly("T1001");
    }

    private TeacherServiceImpl newService() {
        TeacherServiceImpl service = new TeacherServiceImpl();
        ReflectionTestUtils.setField(service, "teacherMapper", teacherMapper);
        return service;
    }
}
