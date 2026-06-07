package org.example.courseselectionsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.courseselectionsystem.entity.Major;

/**
 * 专业Mapper接口
 * 定义专业相关的数据库操作
 */
@Mapper
public interface MajorMapper extends BaseMapper<Major> {
    // BaseMapper已包含基本的CRUD方法
    // 如需自定义查询方法，可以在此处添加
}
