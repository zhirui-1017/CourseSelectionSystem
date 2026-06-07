package org.example.courseselectionsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.courseselectionsystem.entity.Teacher;

import java.util.List;

/**
 * 教师Mapper接口
 * 定义教师相关的数据库操作
 */
@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {
    // BaseMapper已包含基本的CRUD方法
    
    /**
     * 根据工号查询教师
     * @param teacherNo 工号
     * @return 教师信息
     */
    Teacher selectByTeacherNo(@Param("teacherNo") String teacherNo);

    /**
     * 根据姓名模糊查询教师
     * @param name 教师姓名
     * @return 教师列表
     */
    List<Teacher> selectByNameLike(@Param("name") String name);

    /**
     * 根据系ID查询教师
     * @param departmentId 系ID
     * @return 教师列表
     */
    List<Teacher> selectByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 根据学院ID查询教师
     * @param collegeId 学院ID
     * @return 教师列表
     */
    List<Teacher> selectByCollegeId(@Param("collegeId") Long collegeId);

    /**
     * 检查工号是否已存在
     * @param teacherNo 工号
     * @param id 教师ID（用于排除自身）
     * @return 存在数量
     */
    int countByTeacherNo(@Param("teacherNo") String teacherNo, @Param("id") Long id);

    /**
     * 查询特定课程的授课教师
     * @param courseId 课程ID
     * @return 教师列表
     */
    List<Teacher> selectByCourseId(@Param("courseId") Long courseId);

    default List<Teacher> selectAll() {
        return selectList(null);
    }
}
