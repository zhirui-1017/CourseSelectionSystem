package org.example.courseselectionsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.courseselectionsystem.entity.College;
import org.example.courseselectionsystem.vo.CollegeVO;

/**
 * 学院Mapper接口
 * 定义学院相关的数据库操作
 */
@Mapper
public interface CollegeMapper extends BaseMapper<College> {
    // BaseMapper已包含基本的CRUD方法
    // 如需自定义查询方法，可以在此处添加
    default CollegeVO toVO(College college) {
        if (college == null) {
            return null;
        }
        CollegeVO vo = new CollegeVO();
        vo.setId(college.getId());
        vo.setName(college.getName());
        vo.setCode(college.getCode());
        vo.setDescription(college.getDescription());
        vo.setStatus(college.getStatus());
        return vo;
    }
}
