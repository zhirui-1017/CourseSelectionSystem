package org.example.courseselectionsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.courseselectionsystem.entity.CourseSelection;

import java.util.List;

/**
 * 选课Mapper接口
 * 定义选课相关的数据库操作
 */
@Mapper
public interface CourseSelectionMapper extends BaseMapper<CourseSelection> {
    // BaseMapper已包含基本的CRUD方法
    
    /**
     * 根据学生ID查询已选课程
     * @param studentId 学生ID
     * @return 选课记录列表
     */
    List<CourseSelection> selectByStudentId(@Param("studentId") Long studentId);

    /**
     * 根据教师ID查询教授的课程选课记录
     * @param teacherId 教师ID
     * @return 选课记录列表
     */
    List<CourseSelection> selectByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 根据课程ID查询选课记录
     * @param courseId 课程ID
     * @return 选课记录列表
     */
    List<CourseSelection> selectByCourseId(@Param("courseId") Long courseId);

    /**
     * 查询学生是否已选该课程
     * @param studentId 学生ID
     * @param courseId 课程ID
     * @return 选课记录数量
     */
    int countByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
