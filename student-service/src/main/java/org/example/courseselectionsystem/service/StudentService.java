package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 学生服务接口
 * 定义学生相关的服务方法
 */
public interface StudentService {

    /**
     * 添加学生方法
     * @param student 学生信息
     */
    boolean addStudent(Student student);

    default boolean addStudent(Map<String, Object> studentInfo) {
        Student student = new Student();
        student.setStudentNo(stringValue(studentInfo, "studentNo", stringValue(studentInfo, "username", "")));
        student.setName(String.valueOf(studentInfo.getOrDefault("name", "")));
        student.setGender(stringValue(studentInfo, "gender", "男"));
        student.setPhone(stringValue(studentInfo, "phone", ""));
        student.setEmail(stringValue(studentInfo, "email", ""));
        student.setPassword(stringValue(studentInfo, "password", "123456"));
        student.setMajorId(longValue(studentInfo, "majorId", 1L));
        student.setCollegeId(longValue(studentInfo, "collegeId", 1L));
        student.setClassName(stringValue(studentInfo, "className", "未分班"));
        student.setStatus(intValue(studentInfo, "status", 1));
        return addStudent(student);
    }

    /**
     * 更新学生方法
     * @param student 学生信息
     */
    boolean updateStudent(Student student);

    default boolean updateStudent(Map<String, Object> studentInfo) {
        Object id = studentInfo.get("id");
        Student student = id == null ? new Student() : getStudentById(Long.valueOf(String.valueOf(id)));
        if (studentInfo.containsKey("studentNo") || studentInfo.containsKey("username")) {
            student.setStudentNo(stringValue(studentInfo, "studentNo", stringValue(studentInfo, "username", "")));
        }
        if (studentInfo.containsKey("name")) {
            student.setName(String.valueOf(studentInfo.getOrDefault("name", "")));
        }
        if (studentInfo.containsKey("gender")) {
            student.setGender(stringValue(studentInfo, "gender", "男"));
        }
        if (studentInfo.containsKey("phone")) {
            student.setPhone(stringValue(studentInfo, "phone", ""));
        }
        if (studentInfo.containsKey("email")) {
            student.setEmail(stringValue(studentInfo, "email", ""));
        }
        if (studentInfo.containsKey("password")) {
            student.setPassword(stringValue(studentInfo, "password", student.getPassword()));
        }
        if (studentInfo.containsKey("majorId")) {
            student.setMajorId(longValue(studentInfo, "majorId", 1L));
        }
        if (studentInfo.containsKey("collegeId")) {
            student.setCollegeId(longValue(studentInfo, "collegeId", 1L));
        }
        if (studentInfo.containsKey("className")) {
            student.setClassName(stringValue(studentInfo, "className", "未分班"));
        }
        if (studentInfo.containsKey("status")) {
            student.setStatus(intValue(studentInfo, "status", 1));
        }
        return updateStudent(student);
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
     * 删除学生方法
     * @param id 学生ID
     */
    boolean deleteStudent(Long id);

    default boolean deleteStudent(String id) {
        return deleteStudent(Long.valueOf(id));
    }

    default int batchDeleteStudents(Long[] ids) {
        int count = 0;
        for (Long id : ids) {
            if (deleteStudent(id)) {
                count++;
            }
        }
        return count;
    }

    default void resetPassword(String id) {
        resetPassword(Long.valueOf(id));
    }

    boolean resetPassword(Long id);

    boolean changePassword(Long id, String oldPassword, String newPassword);

    default Map<String, Object> getStudentListByPage(PageRequest pageRequest) {
        PageResult<Student> page = getStudentsByPage(pageRequest);
        return Map.of("items", page.getItems(), "total", page.getTotal());
    }

    /**
     * 根据ID获取学生方法
     * @param id 学生ID
     * @return 学生信息
     */
    Student getStudentById(Long id);

    /**
     * 根据学号获取学生方法
     * @param studentNo 学号
     * @return 学生信息
     */
    Student getStudentByStudentNo(String studentNo);

    /**
     * 获取所有学生方法
     * @return 学生列表
     */
    List<Student> getAllStudents();

    /**
     * 获取学生列表方法
     * @param pageRequest 分页请求参数
     * @return 学生列表
     */
    PageResult<Student> getStudentsByPage(PageRequest pageRequest);

    /**
     * 根据专业ID获取学生方法
     * @param majorId 专业ID
     * @return 学生列表
     */
    List<Student> getStudentsByMajorId(Long majorId);

    /**
     * 根据系部ID获取学生方法
     * @param departmentId 系部ID
     * @return 学生列表
     */
    List<Student> getStudentsByDepartmentId(Long departmentId);

    /**
     * 根据学院ID获取学生方法
     * @param collegeId 学院ID
     * @return 学生列表
     */
    List<Student> getStudentsByCollegeId(Long collegeId);

    /**
     * 根据姓名搜索学生方法
     * @param name 学生姓名
     * @return 学生列表
     */
    List<Student> searchStudentsByName(String name);
}
