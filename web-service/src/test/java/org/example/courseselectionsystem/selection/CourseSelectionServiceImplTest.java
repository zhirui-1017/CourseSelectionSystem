package org.example.courseselectionsystem.selection;

import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.repository.CourseSelectionRepository;
import org.example.courseselectionsystem.service.impl.CourseSelectionServiceImpl;
import org.example.courseselectionsystem.vo.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseSelectionServiceImplTest {

    @Mock
    private CourseSelectionRepository courseSelectionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Test
    void getStudentCourseSelectionsNormalizesPagingAndUnknownSort() {
        CourseSelectionServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(0);
        request.setPageSize(500);
        request.setOrderByColumn("unsafeField");
        when(courseSelectionRepository.findByStudentId(eq(3L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getStudentCourseSelections(3L, request, null, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseSelectionRepository).findByStudentId(eq(3L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort().getOrderFor("selectionTime")).isNotNull();
    }

    @Test
    void getCourseStudentListUsesSortAliasAndStatusFilter() {
        CourseSelectionServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(2);
        request.setPageSize(5);
        request.setOrderByColumn("createdAt");
        request.setIsAsc("desc");
        when(courseSelectionRepository.findByCourseIdAndStatus(eq(7L), eq(1), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getCourseStudentList(7L, request, 1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(courseSelectionRepository).findByCourseIdAndStatus(eq(7L), eq(1), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort().getOrderFor("createTime")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createTime").isDescending()).isTrue();
    }

    private CourseSelectionServiceImpl newService() {
        CourseSelectionServiceImpl service = new CourseSelectionServiceImpl();
        ReflectionTestUtils.setField(service, "courseSelectionRepository", courseSelectionRepository);
        ReflectionTestUtils.setField(service, "courseRepository", courseRepository);
        return service;
    }
}
