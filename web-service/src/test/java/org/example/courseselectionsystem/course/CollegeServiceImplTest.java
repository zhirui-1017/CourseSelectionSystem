package org.example.courseselectionsystem.course;

import org.example.courseselectionsystem.entity.College;
import org.example.courseselectionsystem.mapper.CollegeMapper;
import org.example.courseselectionsystem.repository.CollegeRepository;
import org.example.courseselectionsystem.service.CollegeService;
import org.example.courseselectionsystem.service.impl.CollegeServiceImpl;
import org.example.courseselectionsystem.vo.CollegeVO;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollegeServiceImplTest {

    @Mock
    private CollegeRepository collegeRepository;

    @Mock
    private CollegeMapper collegeMapper;

    @Test
    void getCollegeListUsesRepositoryPagingAndSortsByName() {
        CollegeServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(2);
        request.setPageSize(3);
        request.setOrderByColumn("name");
        request.setIsAsc("asc");
        when(collegeRepository.findAll(any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(college(5L, "CS", "Computer")), org.springframework.data.domain.PageRequest.of(1, 3), 8));
        when(collegeMapper.toVO(any(College.class))).thenAnswer(invocation -> toVO(invocation.getArgument(0)));

        CollegeService.PageResult<CollegeVO> result = service.getCollegeList(request);

        ArgumentCaptor<org.springframework.data.domain.PageRequest> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.PageRequest.class);
        verify(collegeRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(3);
        assertThat(captor.getValue().getSort().getOrderFor("name")).isNotNull();
        assertThat(result.getPageNum()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(3);
        assertThat(result.getTotal()).isEqualTo(8);
        assertThat(result.getItems()).extracting(CollegeVO::getCode).containsExactly("CS");
    }

    @Test
    void getCollegeListNormalizesInvalidPagingAndUnknownSort() {
        CollegeServiceImpl service = newService();
        PageRequest request = new PageRequest();
        request.setPageNum(0);
        request.setPageSize(500);
        request.setOrderByColumn("unsafeField");
        when(collegeRepository.findAll(any(org.springframework.data.domain.PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(), org.springframework.data.domain.PageRequest.of(0, 100), 0));

        service.getCollegeList(request);

        ArgumentCaptor<org.springframework.data.domain.PageRequest> captor =
                ArgumentCaptor.forClass(org.springframework.data.domain.PageRequest.class);
        verify(collegeRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(captor.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    private CollegeServiceImpl newService() {
        CollegeServiceImpl service = new CollegeServiceImpl();
        ReflectionTestUtils.setField(service, "collegeRepository", collegeRepository);
        ReflectionTestUtils.setField(service, "collegeMapper", collegeMapper);
        return service;
    }

    private CollegeVO toVO(College college) {
        CollegeVO vo = new CollegeVO();
        vo.setId(college.getId());
        vo.setCode(college.getCode());
        vo.setName(college.getName());
        vo.setStatus(college.getStatus());
        return vo;
    }

    private College college(Long id, String code, String name) {
        College college = new College();
        college.setId(id);
        college.setCode(code);
        college.setName(name);
        college.setStatus(1);
        return college;
    }
}
