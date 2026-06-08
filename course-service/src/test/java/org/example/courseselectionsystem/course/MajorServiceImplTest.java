package org.example.courseselectionsystem.course;

import org.example.courseselectionsystem.entity.Major;
import org.example.courseselectionsystem.repository.MajorRepository;
import org.example.courseselectionsystem.service.impl.MajorServiceImpl;
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
class MajorServiceImplTest {

    @Mock
    private MajorRepository majorRepository;

    @Test
    void getMajorListUsesRepositoryPagingFilteringAndSortAlias() {
        MajorServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(2);
        request.setPageSize(4);
        request.setOrderByColumn("name");
        request.setIsAsc("asc");
        when(majorRepository.findMajors(eq("软件"), eq("SE"), eq(3L), eq(1),
                any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(new Major()), org.springframework.data.domain.PageRequest.of(1, 4), 9));

        service.getMajorList(request, "软件", "SE", 3L, 1);

        ArgumentCaptor<org.springframework.data.domain.PageRequest> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.PageRequest.class);
        verify(majorRepository).findMajors(eq("软件"), eq("SE"), eq(3L), eq(1), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(4);
        assertThat(captor.getValue().getSort().getOrderFor("majorName")).isNotNull();
    }

    @Test
    void getMajorListNormalizesInvalidPagingAndUnknownSort() {
        MajorServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(0);
        request.setPageSize(500);
        request.setOrderByColumn("unsafeField");
        when(majorRepository.findMajors(isNull(), isNull(), isNull(), isNull(),
                any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 100), 0));

        service.getMajorList(request, " ", "", null, null);

        ArgumentCaptor<org.springframework.data.domain.PageRequest> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.PageRequest.class);
        verify(majorRepository).findMajors(isNull(), isNull(), isNull(), isNull(), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    private MajorServiceImpl newService() {
        MajorServiceImpl service = new MajorServiceImpl();
        ReflectionTestUtils.setField(service, "majorRepository", majorRepository);
        return service;
    }
}
