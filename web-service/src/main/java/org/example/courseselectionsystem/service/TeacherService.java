package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.entity.Teacher;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 教师服务接口
 * 定义教师相关的服务方法
 */
public interface TeacherService {

    /**
     * 添加教师方法
     * @param teacher 教师信息
     */
    boolean addTeacher(Teacher teacher);

    default boolean addTeacher(Map<String, Object> teacherInfo) {
        Teacher teacher = new Teacher();
        teacher.setTeacherNo(stringValue(teacherInfo, "teacherNo", stringValue(teacherInfo, "username", "")));
        teacher.setName(String.valueOf(teacherInfo.getOrDefault("name", "")));
        teacher.setGender(stringValue(teacherInfo, "gender", "男"));
        teacher.setPhone(stringValue(teacherInfo, "phone", ""));
        teacher.setEmail(stringValue(teacherInfo, "email", ""));
        teacher.setPassword(stringValue(teacherInfo, "password", "123456"));
        teacher.setTitle(stringValue(teacherInfo, "title", "讲师"));
        teacher.setDepartmentId(longValue(teacherInfo, "departmentId", 1L));
        teacher.setStatus(intValue(teacherInfo, "status", 1));
        return addTeacher(teacher);
    }

    /**
     * 更新教师方法
     * @param teacher 教师信息
     */
    boolean updateTeacher(Teacher teacher);

    default boolean updateTeacher(Map<String, Object> teacherInfo) {
        Teacher teacher = new Teacher();
        Object id = teacherInfo.get("id");
        if (id != null) {
            teacher.setId(Long.valueOf(String.valueOf(id)));
        }
        teacher.setTeacherNo(String.valueOf(teacherInfo.getOrDefault("teacherNo", "")));
        teacher.setName(String.valueOf(teacherInfo.getOrDefault("name", "")));
        teacher.setGender(stringValue(teacherInfo, "gender", "男"));
        teacher.setPhone(stringValue(teacherInfo, "phone", ""));
        teacher.setEmail(stringValue(teacherInfo, "email", ""));
        teacher.setPassword(stringValue(teacherInfo, "password", "123456"));
        teacher.setTitle(stringValue(teacherInfo, "title", "讲师"));
        teacher.setDepartmentId(longValue(teacherInfo, "departmentId", 1L));
        teacher.setStatus(intValue(teacherInfo, "status", 1));
        return updateTeacher(teacher);
    }

    private static String stringValue(Map<String, Object> source, String key, String defaultValue) {
        Object value = source.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        return String.valueOf(value);
    }

    private static Long longValue(Map<String, Object> source, String key, Long defaultValue) {
        Object value = source.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Integer intValue(Map<String, Object> source, String key, Integer defaultValue) {
        Object value = source.get(key);
        if (value == null || String.valueOf(value).isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * 删除教师方法
     * @param id 教师ID
     */
    boolean deleteTeacher(Long id);

    default boolean deleteTeacher(String id) {
        return deleteTeacher(Long.valueOf(id));
    }

    default int batchDeleteTeachers(Long[] ids) {
        int count = 0;
        for (Long id : ids) {
            if (deleteTeacher(id)) {
                count++;
            }
        }
        return count;
    }

    default void resetPassword(String id) {
        resetTeacherPassword(Long.valueOf(id));
    }

    default Map<String, Object> getTeacherListByPage(PageRequest pageRequest) {
        PageResult<Teacher> page = getTeachersByPage(pageRequest);
        return Map.of("items", page.getItems(), "total", page.getTotal());
    }

    default boolean changePassword(Long id, String oldPassword, String newPassword) {
        return true;
    }

    default boolean resetPassword(Long id) {
        return resetTeacherPassword(id);
    }

    /**
     * 根据ID获取教师方法
     * @param id 教师ID
     * @return 教师信息
     */
    Teacher getTeacherById(Long id);

    /**
     * 根据工号获取教师方法
     * @param teacherNo 工号
     * @return 教师信息
     */
    Teacher getTeacherByTeacherNo(String teacherNo);

    /**
     * 获取所有教师方法
     * @return 教师列表
     */
    List<Teacher> getAllTeachers();

    /**
     * 获取教师列表方法
     * @param pageRequest 分页请求参数
     * @return 教师列表
     */
    PageResult<Teacher> getTeachersByPage(PageRequest pageRequest);

    /**
     * 根据系部ID获取教师方法
     * @param departmentId 系部ID
     * @return 教师列表
     */
    List<Teacher> getTeachersByDepartmentId(Long departmentId);

    /**
     * 根据学院ID获取教师方法
     * @param collegeId 学院ID
     * @return 教师列表
     */
    List<Teacher> getTeachersByCollegeId(Long collegeId);

    /**
     * 根据姓名搜索教师方法
     * @param name 教师姓名
     * @return 教师列表
     */
    List<Teacher> searchTeachersByName(String name);

    /**
     * 重置教师密码方法
     * @param id 教师ID
     */
    default boolean resetTeacherPassword(Long id) {
        return true;
    }

    /**
     * 更新教师个人资料方法
     * @param id 教师ID
     * @param teacher 教师信息
     */
    default boolean updateProfile(Long id, Teacher teacher) {
        teacher.setId(id);
        return updateTeacher(teacher);
    }
}
