package org.example.courseselectionsystem.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.entity.CourseApplication;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.repository.CourseApplicationRepository;
import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.service.CourseApplicationService;
import org.example.courseselectionsystem.service.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CourseApplicationServiceImpl implements CourseApplicationService {

    @Autowired
    private CourseApplicationRepository applicationRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseService courseService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseApplication apply(CourseApplication application) {
        if (application == null || application.getApplicantId() == null
                || !StringUtils.hasText(application.getType())) {
            throw new BusinessException(Result.PARAM_ERROR, "申请信息不完整");
        }
        String type = application.getType();
        if (!"add".equals(type) && !"edit".equals(type) && !"delete".equals(type)) {
            throw new BusinessException(Result.PARAM_ERROR, "申请类型不正确");
        }
        if (application.getStatus() == null) {
            application.setStatus(0);
        }
        normalizeApplicantName(application);
        validatePayloadNumbers(application);
        return applicationRepository.save(application);
    }

    @Override
    public CourseApplication getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "申请不存在"));
    }

    @Override
    public Page<CourseApplication> listPending(int pageNum, int pageSize) {
        int p = Math.max(1, pageNum);
        int s = Math.min(Math.max(1, pageSize), 1000);
        return applicationRepository.findByStatus(0,
                PageRequest.of(p - 1, s, Sort.by(Sort.Direction.DESC, "createTime")));
    }

    @Override
    public Page<CourseApplication> listByTeacher(Long applicantId, int pageNum, int pageSize) {
        int p = Math.max(1, pageNum);
        int s = Math.min(Math.max(1, pageSize), 1000);
        return applicationRepository.findByApplicantId(applicantId,
                PageRequest.of(p - 1, s, Sort.by(Sort.Direction.DESC, "createTime")));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseApplication approve(Long id, Long approverId) {
        CourseApplication app = getById(id);
        if (!Objects.equals(app.getStatus(), 0)) {
            throw new BusinessException(Result.PARAM_ERROR, "该申请已处理");
        }
        switch (app.getType()) {
            case "add" -> createCourse(app);
            case "edit" -> updateCourse(app);
            case "delete" -> courseService.deleteCourse(app.getCourseId(), app.getReason(),
                    approverId, app.getApplicantName() != null ? app.getApplicantName() : "教师");
            default -> throw new BusinessException(Result.PARAM_ERROR, "申请类型不正确");
        }
        app.setStatus(1);
        app.setApproverId(approverId);
        app.setUpdateTime(new Date());
        return applicationRepository.save(app);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CourseApplication reject(Long id, Long approverId, String comment) {
        CourseApplication app = getById(id);
        if (!Objects.equals(app.getStatus(), 0)) {
            throw new BusinessException(Result.PARAM_ERROR, "该申请已处理");
        }
        app.setStatus(2);
        app.setApproverId(approverId);
        app.setApproveComment(comment);
        app.setUpdateTime(new Date());
        return applicationRepository.save(app);
    }

    /** 审批通过新增：直接建课并开放选课（status=1） */
    private void createCourse(CourseApplication app) {
        validatePayloadNumbers(app);
        Course course = new Course();
        applyPayload(course, app.getPayload());
        if (!StringUtils.hasText(course.getCourseCode()) || !StringUtils.hasText(course.getCourseName())) {
            throw new BusinessException(Result.PARAM_ERROR, "课程编号或名称缺失");
        }
        if (courseRepository.findByCourseCode(course.getCourseCode()).isPresent()) {
            throw new BusinessException(Result.PARAM_ERROR, "课程编号已存在");
        }
        if (course.getStatus() == null) {
            course.setStatus(1);
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
        course.setCreateTime(new Date());
        course.setUpdateTime(new Date());
        courseRepository.save(course);
    }

    /** 审批通过编辑：更新课程字段；只有改为不可选状态才影响学生选课 */
    private void updateCourse(CourseApplication app) {
        validatePayloadNumbers(app);
        Course course = courseRepository.findById(app.getCourseId())
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
        applyPayload(course, app.getPayload());
        course.setUpdateTime(new Date());
        courseRepository.save(course);
    }

    /**
     * 申请人统一显示真实姓名：根据 applicant_id 从教师表取姓名并覆盖 applicantName，
     * 避免因前端取不到姓名而把工号（如 T001）当作申请人存入。
     */
    private void normalizeApplicantName(CourseApplication app) {
        if (app.getApplicantId() == null) {
            return;
        }
        try {
            List<String> names = jdbcTemplate.queryForList(
                    "SELECT name FROM teacher WHERE id = ?", String.class, app.getApplicantId());
            if (!names.isEmpty()) {
                app.setApplicantName(names.get(0));
            }
        } catch (Exception ignored) {
            // 教师表不可用时不阻断申请
        }
    }

    /**
     * 校验申请负载中的数值字段，避免越界数据（如学分超过 DECIMAL(3,1) 上限）
     * 导致审批/建课时报数据库错误。
     */
    private void validatePayloadNumbers(CourseApplication app) {
        String payload = app.getPayload();
        if (!StringUtils.hasText(payload)) {
            return;
        }
        try {
            Map<String, Object> m = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            if (m.containsKey("credit") && m.get("credit") != null) {
                double credit = doubleValue(m.get("credit"));
                if (credit < 0.5 || credit > 20) {
                    throw new BusinessException(Result.PARAM_ERROR, "课程学分须在 0.5~20 之间，无法审批");
                }
            }
            if (m.containsKey("totalHours") && m.get("totalHours") != null) {
                int totalHours = intValue(m.get("totalHours"));
                if (totalHours < 1 || totalHours > 500) {
                    throw new BusinessException(Result.PARAM_ERROR, "课程学时须在 1~500 之间，无法审批");
                }
            }
            if (m.containsKey("availableSlots") && m.get("availableSlots") != null) {
                int slots = intValue(m.get("availableSlots"));
                if (slots < 1 || slots > 999) {
                    throw new BusinessException(Result.PARAM_ERROR, "课程容量须在 1~999 之间，无法审批");
                }
            }
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception e) {
            throw new BusinessException(Result.PARAM_ERROR, "课程数据格式错误");
        }
    }

    private void applyPayload(Course course, String payload) {
        if (!StringUtils.hasText(payload)) {
            return;
        }
        try {
            Map<String, Object> m = objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {});
            if (m.containsKey("courseCode") && m.get("courseCode") != null) course.setCourseCode(String.valueOf(m.get("courseCode")));
            if (m.containsKey("courseName") && m.get("courseName") != null) course.setCourseName(String.valueOf(m.get("courseName")));
            if (m.containsKey("courseType") && m.get("courseType") != null) course.setCourseType(String.valueOf(m.get("courseType")));
            if (m.containsKey("credit") && m.get("credit") != null) course.setCredit(doubleValue(m.get("credit")));
            if (m.containsKey("totalHours") && m.get("totalHours") != null) course.setTotalHours(intValue(m.get("totalHours")));
            if (m.containsKey("teacherId") && m.get("teacherId") != null) course.setTeacherId(longValue(m.get("teacherId")));
            if (m.containsKey("semester") && m.get("semester") != null) course.setSemester(String.valueOf(m.get("semester")));
            if (m.containsKey("schedule") && m.get("schedule") != null) course.setSchedule(String.valueOf(m.get("schedule")));
            if (m.containsKey("classroom") && m.get("classroom") != null) course.setClassroom(String.valueOf(m.get("classroom")));
            if (m.containsKey("availableSlots") && m.get("availableSlots") != null) course.setAvailableSlots(intValue(m.get("availableSlots")));
            if (m.containsKey("description") && m.get("description") != null) course.setDescription(String.valueOf(m.get("description")));
            if (m.containsKey("status") && m.get("status") != null) course.setStatus(intValue(m.get("status")));
        } catch (Exception e) {
            throw new BusinessException(Result.PARAM_ERROR, "课程数据格式错误");
        }
    }

    private Integer intValue(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : Integer.parseInt(String.valueOf(o));
    }

    private Long longValue(Object o) {
        return o instanceof Number ? ((Number) o).longValue() : Long.parseLong(String.valueOf(o));
    }

    private Double doubleValue(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : Double.parseDouble(String.valueOf(o));
    }
}
