package org.example.courseselectionsystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.courseselectionsystem.entity.Student;

import java.util.List;

/**
 * 学生Mapper接口
 * 定义学生相关的数据库操作
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    // BaseMapper已包含基本的CRUD方法
    
    /**
     * 根据学号查询学生
     * @param studentNo 学号
     * @return 学生信息
     */
    Student selectByStudentNo(@Param("studentNo") String studentNo);

    /**
     * 根据姓名模糊查询学生
     * @param name 学生姓名
     * @return 学生列表
     */
    List<Student> selectByNameLike(@Param("name") String name);

    /**
     * 根据专业ID查询学生
     * @param majorId 专业ID
     * @return 学生列表
     */
    List<Student> selectByMajorId(@Param("majorId") Long majorId);

    /**
     * 根据系ID查询学生
     * @param departmentId 系ID
     * @return 学生列表
     */
    List<Student> selectByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 根据学院ID查询学生
     * @param collegeId 学院ID
     * @return 学生列表
     */
    List<Student> selectByCollegeId(@Param("collegeId") Long collegeId);

    /**
     * 检查学号是否已存在
     * @param studentNo 学号
     * @param id 学生ID（用于排除自身）
     * @return 存在数量
     */
    int countByStudentNo(@Param("studentNo") String studentNo, @Param("id") Long id);

    default List<Student> selectAll() {
        return selectList(null);
    }
}
