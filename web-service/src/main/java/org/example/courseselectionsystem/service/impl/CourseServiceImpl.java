package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.service.CourseService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * 课程服务实现类
 */
@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course addCourse(Course course) {
        // 验证课程信息
        if (course == null || !StringUtils.hasText(course.getCourseName()) 
                || !StringUtils.hasText(course.getCourseCode())) {
            throw new BusinessException(Result.PARAM_ERROR, "课程信息不完整");
        }

        // 检查课程编号是否已存在
        Optional<Course> existingCourse = courseRepository.findByCourseCode(course.getCourseCode());
        if (existingCourse.isPresent()) {
            throw new BusinessException(Result.PARAM_ERROR, "课程编号已存在");
        }

        // 设置创建和更新时间
        course.setCreateTime(new Date());
        course.setUpdateTime(new Date());

        // 保存课程
        return courseRepository.save(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course updateCourse(Course course) {
        // 验证课程ID
        if (course.getId() == null) {
            throw new BusinessException(Result.PARAM_ERROR, "课程ID不能为空");
        }

        // 验证课程是否存在
        Course existingCourse = courseRepository.findById(course.getId())
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));

        // 更新课程信息
        existingCourse.setCourseName(course.getCourseName());
        existingCourse.setCredit(course.getCredit());
        existingCourse.setCourseType(course.getCourseType());
        existingCourse.setTotalHours(course.getTotalHours());
        existingCourse.setClassroom(course.getClassroom());
        existingCourse.setSchedule(course.getSchedule());
        existingCourse.setMaxCapacity(course.getMaxCapacity());
        existingCourse.setTeacherId(course.getTeacherId());
        existingCourse.setTeacherName(course.getTeacherName());
        existingCourse.setSemester(course.getSemester());
        existingCourse.setDescription(course.getDescription());
        existingCourse.setStatus(course.getStatus());
        existingCourse.setDepartmentId(course.getDepartmentId());
        existingCourse.setUpdateTime(new Date());

        // 保存更新后的课程
        return courseRepository.save(existingCourse);
    }

    @Override
    public Course getCourseById(Long courseId) {
        // 根据ID查询课程
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
    }

    @Override
    public Course getCourseByCode(String courseCode) {
        // 根据课程编号查询课程
        return courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCourse(Long courseId) {
        // 验证课程是否存在
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(Result.NOT_FOUND, "课程不存在");
        }

        // 删除课程
        courseRepository.deleteById(courseId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteCourses(List<Long> courseIds) {
        // 批量删除课程
        courseRepository.deleteAllById(courseIds);
        return true;
    }

    @Override
    public Page<Course> getCourseList(PageRequest pageRequestParam, String courseName, String courseCode, 
                                    Long teacherId, Long departmentId, Integer status) {
        // 构建排序对象
        Sort sort = Sort.by(Sort.Direction.DESC, "createTime");
        // 创建分页请求
        org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(pageRequestParam.getPageNum() - 1, pageRequestParam.getPageSize(), sort);
        // 调用Repository方法实现多条件查询
        return courseRepository.findCourses(courseName, courseCode, null, departmentId, null, null, status, pageRequest);
    }

    @Override
    public List<Course> getActiveCourses() {
        // 查询状态为启用的课程
        // 注意：这里需要根据实际Repository实现情况调整
        return Collections.emptyList();
    }

    @Override
    public List<Course> getCoursesByDepartment(Long departmentId) {
        // 根据学院ID查询课程
        // 注意：这里需要根据实际Repository实现情况调整
        return Collections.emptyList();
    }

    @Override
    public List<Course> getCoursesByTeacher(Long teacherId) {
        // 查询当前学期的课程
        String currentSemester = getCurrentSemester();
        return courseRepository.findBySemesterAndTeacherId(currentSemester, teacherId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeCourseStatus(Long courseId, Integer status) {
        // 验证课程是否存在
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));

        // 更新课程状态
        course.setStatus(status);
        course.setUpdateTime(new Date());
        courseRepository.save(course);
        return true;
    }

    @Override
    public List<Course> searchCourses(String keyword, Long departmentId, Integer courseType, Integer credit) {
        // 根据条件搜索课程
        // 注意：这里需要根据实际Repository实现情况调整
        return Collections.emptyList();
    }

    /**
     * 获取当前学期
     * @return 当前学期字符串
     */
    private String getCurrentSemester() {
        // 简单实现，实际可以根据系统配置或时间计算
        Date now = new Date();
        int year = now.getYear() + 1900;
        int month = now.getMonth() + 1;
        
        if (month >= 9 || month <= 1) {
            return year + "-" + (year + 1) + "学年第一学期";
        } else {
            return year + "-" + (year + 1) + "学年第二学期";
        }
    }
}
