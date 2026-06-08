package org.example.courseselectionsystem.course;

import org.example.courseselectionsystem.entity.Department;
import org.example.courseselectionsystem.repository.DepartmentRepository;
import org.example.courseselectionsystem.service.impl.DepartmentServiceImpl;
import org.example.courseselectionsystem.vo.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    @Mock
    private DepartmentRepository departmentRepository;

    @Test
    void getDepartmentListUsesRepositoryPagingFilteringAndSortAlias() {
        DepartmentServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(2);
        request.setPageSize(4);
        request.setOrderByColumn("name");
        request.setIsAsc("asc");
        when(departmentRepository.findDepartments(eq("计算机"), eq("CS"), eq(1),
                any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new Department()), org.springframework.data.domain.PageRequest.of(1, 4), 9));

        service.getDepartmentList(request, "计算机", "CS", 1);

        ArgumentCaptor<org.springframework.data.domain.PageRequest> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.PageRequest.class);
        verify(departmentRepository).findDepartments(eq("计算机"), eq("CS"), eq(1), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(4);
        assertThat(captor.getValue().getSort().getOrderFor("departmentName")).isNotNull();
    }

    @Test
    void getDepartmentListNormalizesInvalidPagingAndUnknownSort() {
        DepartmentServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(0);
        request.setPageSize(500);
        request.setOrderByColumn("unsafeField");
        when(departmentRepository.findDepartments(isNull(), isNull(), isNull(),
                any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 100), 0));

        service.getDepartmentList(request, " ", "", null);

        ArgumentCaptor<org.springframework.data.domain.PageRequest> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.PageRequest.class);
        verify(departmentRepository).findDepartments(isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    private DepartmentServiceImpl newService() {
        DepartmentServiceImpl service = new DepartmentServiceImpl();
        ReflectionTestUtils.setField(service, "departmentRepository", departmentRepository);
        return service;
    }
}
