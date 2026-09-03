package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.service.CourseService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CourseServiceImpl implements CourseService {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @CacheEvict(value = {"course:active", "course:detail"}, allEntries = true)
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

    @CacheEvict(value = {"course:active", "course:detail"}, allEntries = true)
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

    @Cacheable(value = "course:detail", key = "#courseId")
    @Override
    public Course getCourseById(Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
        enrichCourses(List.of(course));
        return course;
    }

    @Override
    public Course getCourseByCode(String courseCode) {
        Course course = courseRepository.findByCourseCode(courseCode)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
        enrichCourses(List.of(course));
        return course;
    }

    /**
     * 用 course_selection 表中 status=1（已选）的真实记录数覆盖课程表硬编码的 selectedCount。
     * 统计失败（如 JDBC 不可用、表缺失）时优雅降级，保留原值，不阻断课程列表。
     */
    private void applyRealSelectedCount(List<Course> courses) {
        if (jdbcTemplate == null || courses == null || courses.isEmpty()) {
            return;
        }
        try {
            List<Long> ids = courses.stream()
                    .map(Course::getId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (ids.isEmpty()) {
                return;
            }
            String placeholders = ids.stream().map(i -> "?").collect(Collectors.joining(","));
            Map<Long, Long> counts = new HashMap<>();
            jdbcTemplate.query(
                    "SELECT course_id, COUNT(*) FROM course_selection WHERE status = 1 AND course_id IN (" + placeholders + ") GROUP BY course_id",
                    rs -> {
                        while (rs.next()) {
                            counts.put(rs.getLong("course_id"), rs.getLong(2));
                        }
                        return null;
                    },
                    ids.toArray());
            courses.forEach(c -> {
                if (c.getId() != null) {
                    c.setSelectedCount(counts.getOrDefault(c.getId(), 0L).intValue());
                }
            });
        } catch (Exception ignored) {
            // 统计失败时保留 course 表原有 selectedCount
        }
    }

    /**
     * 批量填充课程教师姓名（teacherName 为 @Transient，需查询 teacher 表）。
     * 填充失败时优雅降级，保留 teacherId。
     */
    private void applyTeacherName(List<Course> courses) {
        if (jdbcTemplate == null || courses == null || courses.isEmpty()) {
            return;
        }
        try {
            List<Long> teacherIds = courses.stream()
                    .map(Course::getTeacherId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (teacherIds.isEmpty()) {
                return;
            }
            String placeholders = teacherIds.stream().map(i -> "?").collect(Collectors.joining(","));
            Map<Long, String> names = new HashMap<>();
            jdbcTemplate.query(
                    "SELECT id, name FROM teacher WHERE id IN (" + placeholders + ")",
                    rs -> {
                        while (rs.next()) {
                            names.put(rs.getLong("id"), rs.getString("name"));
                        }
                        return null;
                    },
                    teacherIds.toArray());
            courses.forEach(c -> {
                if (c.getTeacherId() != null) {
                    c.setTeacherName(names.get(c.getTeacherId()));
                }
            });
        } catch (Exception ignored) {
            // 填充失败时保留 teacherId
        }
    }

    /** 统一丰富课程展示字段：真实选课人数 + 教师姓名 */
    private void enrichCourses(List<Course> courses) {
        applyRealSelectedCount(courses);
        applyTeacherName(courses);
    }

    @CacheEvict(value = {"course:active", "course:detail"}, allEntries = true)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCourse(Long courseId) {
        return deleteCourse(courseId, null, null, "系统");
    }

    @CacheEvict(value = {"course:active", "course:detail"}, allEntries = true)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteCourse(Long courseId, String reason, Long operatorId, String operatorName) {
        deleteCourseWithNotice(courseId, reason, operatorId, operatorName);
        return true;
    }

    /**
     * 删除课程并处理选课：若课程已有学生选课，发布停开通知并清理选课记录，让学生重新选课。
     * 通知/清理失败不阻断删除。
     */
    private void deleteCourseWithNotice(Long courseId, String reason, Long operatorId, String operatorName) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
        if (jdbcTemplate != null) {
            try {
                Long selected = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM course_selection WHERE course_id = ? AND status = 1", Long.class, courseId);
                Long any = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM course_selection WHERE course_id = ?", Long.class, courseId);
                boolean hasSelection = (selected != null && selected > 0) || (any != null && any > 0);
                if (hasSelection) {
                    String reasonText = StringUtils.hasText(reason) ? reason : "未填写";
                    jdbcTemplate.update(
                            "INSERT INTO course_notice (title, content, course_id, course_name, publisher_id, publisher_name) VALUES (?,?,?,?,?,?)",
                            "课程停开通知",
                            "课程【" + course.getCourseName() + "】（"
                                    + (course.getCourseCode() == null ? "" : course.getCourseCode())
                                    + "）已停开并删除，删除原因：" + reasonText + "。请相关学生及时重新选课。",
                            courseId, course.getCourseName(), operatorId, operatorName);
                    jdbcTemplate.update("DELETE FROM course_selection WHERE course_id = ?", courseId);
                    jdbcTemplate.update("DELETE FROM course_evaluation WHERE course_id = ?", courseId);
                }
            } catch (Exception ignored) {
                // 通知/清理失败不阻断删除
            }
        }
        courseRepository.deleteById(courseId);
    }

    @CacheEvict(value = {"course:active", "course:detail"}, allEntries = true)
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteCourses(List<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return true;
        }
        for (Long courseId : courseIds) {
            if (courseId != null && courseRepository.existsById(courseId)) {
                deleteCourseWithNotice(courseId, "批量删除", null, "管理员");
            }
        }
        return true;
    }

    @Override
    public Page<Course> getCourseList(PageRequest pageRequestParam, String courseName, String courseCode,
                                      Long teacherId, Long departmentId, String courseType, Integer status) {
        PageRequest request = pageRequestParam == null ? new PageRequest() : pageRequestParam;
        int pageNum = request.getPageNum() == null || request.getPageNum() < 1 ? 1 : request.getPageNum();
        int pageSize = request.getPageSize() == null || request.getPageSize() < 1 ? 10 : Math.min(request.getPageSize(), 1000);
        org.springframework.data.domain.PageRequest pageRequest =
                org.springframework.data.domain.PageRequest.of(pageNum - 1, pageSize, courseSort(request));
        Page<Course> coursePage = courseRepository.findCourses(blankToNull(courseName), blankToNull(courseCode), teacherId,
                normalizeCourseType(courseType), status, pageRequest);
        enrichCourses(coursePage.getContent());
        return coursePage;
    }

    @Cacheable(value = "course:active", key = "'list'")
    @Override
    public List<Course> getActiveCourses() {
        List<Course> courses = courseRepository.findByStatus(1);
        enrichCourses(courses);
        return courses;
    }

    @Cacheable(value = "course:active", key = "#semester == null || #semester.isBlank() ? 'list' : #semester")
    @Override
    public List<Course> getActiveCoursesBySemester(String semester) {
        if (semester == null || semester.isBlank()) {
            return getActiveCourses();
        }
        return getActiveCourses().stream()
                .filter(course -> semester.equals(course.getSemester()))
                .toList();
    }

    @Override
    public List<Course> getPopularCourses(Integer limit) {
        int n = limit == null || limit < 1 ? 10 : limit;
        return getActiveCourses().stream()
                .sorted(Comparator.comparingInt((Course c) -> c.getSelectedCount() == null ? 0 : c.getSelectedCount()).reversed())
                .limit(n)
                .toList();
    }

    @Override
    public List<Course> getCoursesByDepartment(Long departmentId) {
        if (departmentId == null) {
            return getActiveCourses();
        }
        List<Course> courses = courseRepository.findByDepartmentIdAndStatus(departmentId, 1);
        enrichCourses(courses);
        return courses;
    }

    @Override
    public List<Course> getCoursesByTeacher(Long teacherId) {
        List<Course> courses = courseRepository.findByTeacherId(teacherId);
        enrichCourses(courses);
        return courses;
    }

    @CacheEvict(value = {"course:active", "course:detail"}, allEntries = true)
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
        List<Course> courses = courseRepository.searchCourses(blankToNull(keyword), departmentId, type, creditValue, 1);
        enrichCourses(courses);
        return courses;
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

    @Override
    public long count() {
        return courseRepository.count();
    }

    @Override
    public long countRecent(int days) {
        return courseRepository.countByCreateTimeGreaterThanEqual(LocalDateTime.now().minusDays(Math.max(days, 1)));
    }
}
