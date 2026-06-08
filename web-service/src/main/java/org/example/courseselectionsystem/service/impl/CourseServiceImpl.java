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

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course addCourse(Course course) {
        if (course == null || !StringUtils.hasText(course.getCourseName())
                || !StringUtils.hasText(course.getCourseCode())) {
            throw new BusinessException(Result.PARAM_ERROR, "课程信息不完整");
        }

        Optional<Course> existingCourse = courseRepository.findByCourseCode(course.getCourseCode());
        if (existingCourse.isPresent()) {
            throw new BusinessException(Result.PARAM_ERROR, "课程编号已存在");
        }

        if (course.getAvailableSlots() == null) {
            course.setAvailableSlots(40);
        }
        if (course.getSelectedCount() == null) {
            course.setSelectedCount(0);
        }
        if (!StringUtils.hasText(course.getCourseType())) {
            course.setCourseType("选修课");
        }
        if (!StringUtils.hasText(course.getClassroom())) {
            course.setClassroom("待安排");
        }
        if (!StringUtils.hasText(course.getSchedule())) {
            course.setSchedule("待安排");
        }
        if (course.getStatus() == null) {
            course.setStatus(1);
        }
        course.setCreateTime(new Date());
        course.setUpdateTime(new Date());

        return courseRepository.save(course);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course updateCourse(Course course) {
        if (course.getId() == null) {
            throw new BusinessException(Result.PARAM_ERROR, "课程ID不能为空");
        }

        Course existingCourse = courseRepository.findById(course.getId())
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));

        if (StringUtils.hasText(course.getCourseName())) {
            existingCourse.setCourseName(course.getCourseName());
        }
        if (course.getCredit() != null) {
            existingCourse.setCredit(course.getCredit());
        }
        if (StringUtils.hasText(course.getCourseType())) {
            existingCourse.setCourseType(course.getCourseType());
        }
        if (course.getTotalHours() != null) {
            existingCourse.setTotalHours(course.getTotalHours());
        }
        if (StringUtils.hasText(course.getClassroom())) {
            existingCourse.setClassroom(course.getClassroom());
        }
        if (StringUtils.hasText(course.getSchedule())) {
            existingCourse.setSchedule(course.getSchedule());
        }
        if (course.getMaxCapacity() != null) {
            existingCourse.setMaxCapacity(course.getMaxCapacity());
        }
        if (course.getTeacherId() != null) {
            existingCourse.setTeacherId(course.getTeacherId());
        }
        if (StringUtils.hasText(course.getDescription())) {
            existingCourse.setDescription(course.getDescription());
        }
        if (course.getStatus() != null) {
            existingCourse.setStatus(course.getStatus());
        }
        existingCourse.setUpdateTime(new Date());

        return courseRepository.save(existingCourse);
    }

    @Override
    public Course getCourseById(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
    }

    @Override
    public Course getCourseByCode(String courseCode) {
        return courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new BusinessException(Result.NOT_FOUND, "课程不存在");
        }
        courseRepository.deleteById(courseId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteCourses(List<Long> courseIds) {
        courseRepository.deleteAllById(courseIds);
        return true;
    }

    @Override
    public Page<Course> getCourseList(PageRequest pageRequestParam, String courseName, String courseCode,
                                      Long teacherId, Long departmentId, String courseType, Integer status) {
        PageRequest request = pageRequestParam == null ? new PageRequest() : pageRequestParam;
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : Math.min(request.getPageSize(), 100);
        org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(pageNum - 1, pageSize, courseSort(request));
        return courseRepository.findCourses(blankToNull(courseName), blankToNull(courseCode), teacherId,
                normalizeCourseType(courseType), status, pageRequest);
    }

    @Override
    public List<Course> getActiveCourses() {
        return courseRepository.findByStatus(1);
    }

    @Override
    public List<Course> getCoursesByDepartment(Long departmentId) {
        return getActiveCourses();
    }

    @Override
    public List<Course> getCoursesByTeacher(Long teacherId) {
        return courseRepository.findByTeacherId(teacherId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeCourseStatus(Long courseId, Integer status) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
        course.setStatus(status);
        course.setUpdateTime(new Date());
        courseRepository.save(course);
        return true;
    }

    @Override
    public List<Course> searchCourses(String keyword, Long departmentId, Integer courseType, Integer credit) {
        String type = normalizeCourseType(courseType);
        Double creditValue = credit == null ? null : credit.doubleValue();
        return courseRepository.searchCourses(blankToNull(keyword), type, creditValue, 1);
    }

    private String normalizeCourseType(Integer courseType) {
        if (courseType == null) {
            return null;
        }
        switch (courseType) {
            case 1:
                return "必修课";
            case 2:
                return "选修课";
            case 3:
                return "通识课";
            default:
                return String.valueOf(courseType);
        }
    }

    private String normalizeCourseType(String courseType) {
        if (!StringUtils.hasText(courseType) || "all".equalsIgnoreCase(courseType)) {
            return null;
        }
        String normalized = courseType.trim();
        if ("1".equals(normalized) || "required".equalsIgnoreCase(normalized)) {
            return "必修课";
        }
        if ("2".equals(normalized) || "elective".equalsIgnoreCase(normalized)) {
            return "选修课";
        }
        if ("3".equals(normalized) || "general".equalsIgnoreCase(normalized)) {
            return "通识课";
        }
        if ("4".equals(normalized) || "professional".equalsIgnoreCase(normalized)) {
            return "专业课";
        }
        return normalized;
    }

    private Sort courseSort(PageRequest request) {
        String sortField = request.getSortField();
        String property = StringUtils.hasText(sortField) ? courseSortProperty(sortField) : "id";
        Sort.Direction direction = "desc".equalsIgnoreCase(request.getSortOrder()) ? Sort.Direction.DESC : Sort.Direction.ASC;
        if (!StringUtils.hasText(sortField)) {
            direction = Sort.Direction.DESC;
        }
        return Sort.by(new Sort.Order(direction, property));
    }

    private String courseSortProperty(String field) {
        String normalized = field.trim();
        if ("code".equalsIgnoreCase(normalized)) {
            return "courseCode";
        }
        if ("name".equalsIgnoreCase(normalized)) {
            return "courseName";
        }
        if ("type".equalsIgnoreCase(normalized) || "category".equalsIgnoreCase(normalized)) {
            return "courseType";
        }
        if ("credits".equalsIgnoreCase(normalized)) {
            return "credit";
        }
        if ("capacity".equalsIgnoreCase(normalized) || "maxCapacity".equalsIgnoreCase(normalized)) {
            return "availableSlots";
        }
        if ("enrolled".equalsIgnoreCase(normalized) || "currentStudents".equalsIgnoreCase(normalized)) {
            return "selectedCount";
        }
        if ("courseCode".equalsIgnoreCase(normalized)
                || "courseName".equalsIgnoreCase(normalized)
                || "courseType".equalsIgnoreCase(normalized)
                || "credit".equalsIgnoreCase(normalized)
                || "totalHours".equalsIgnoreCase(normalized)
                || "teacherId".equalsIgnoreCase(normalized)
                || "availableSlots".equalsIgnoreCase(normalized)
                || "selectedCount".equalsIgnoreCase(normalized)
                || "status".equalsIgnoreCase(normalized)
                || "createTime".equalsIgnoreCase(normalized)
                || "updateTime".equalsIgnoreCase(normalized)
                || "id".equalsIgnoreCase(normalized)) {
            return normalized;
        }
        return "id";
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value : null;
    }
}
