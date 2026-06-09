package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.entity.CourseSelection;
import org.example.courseselectionsystem.entity.Student;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.repository.CourseSelectionRepository;
import org.example.courseselectionsystem.repository.StudentRepository;
import org.example.courseselectionsystem.service.CourseSelectionService;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class CourseSelectionServiceImpl implements CourseSelectionService {

    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Map<String, String> SORT_COLUMNS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("studentId", "studentId"),
            Map.entry("student", "studentId"),
            Map.entry("courseId", "courseId"),
            Map.entry("course", "courseId"),
            Map.entry("status", "status"),
            Map.entry("score", "score"),
            Map.entry("dailyGrade", "dailyGrade"),
            Map.entry("labGrade", "labGrade"),
            Map.entry("examGrade", "examGrade"),
            Map.entry("selectionTime", "selectionTime"),
            Map.entry("selectedAt", "selectionTime"),
            Map.entry("dropTime", "dropTime"),
            Map.entry("createTime", "createTime"),
            Map.entry("createdAt", "createTime"),
            Map.entry("updateTime", "updateTime"),
            Map.entry("updatedAt", "updateTime")
    );

    @Autowired
    private CourseSelectionRepository courseSelectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> selectCourse(Long studentId, Long courseId) {
        if (studentId == null || courseId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "学生ID和课程ID不能为空");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));

        if (!Objects.equals(course.getStatus(), 1)) {
            throw new BusinessException(Result.PARAM_ERROR, "课程未开放选课");
        }

        Optional<CourseSelection> existingSelection = courseSelectionRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existingSelection.isPresent()) {
            CourseSelection selection = existingSelection.get();
            if (Objects.equals(selection.getStatus(), 1) || Objects.equals(selection.getStatus(), 3)) {
                throw new BusinessException(Result.PARAM_ERROR, "您已选择该课程");
            }
            selection.setStatus(1);
            selection.setDropTime(null);
            selection.setSelectionTime(new Date());
            selection.setUpdateTime(new Date());
            courseSelectionRepository.save(selection);
            refreshSelectedCount(course);
            return result("选课成功", 1);
        }

        long selectedCount = courseSelectionRepository.countByCourseIdAndStatus(courseId, 1);
        int status = selectedCount >= safeCapacity(course) ? 3 : 1;

        CourseSelection selection = new CourseSelection();
        selection.setStudentId(studentId);
        selection.setCourseId(courseId);
        selection.setStatus(status);
        selection.setSelectionTime(new Date());
        selection.setCreateTime(new Date());
        selection.setUpdateTime(new Date());
        courseSelectionRepository.save(selection);
        refreshSelectedCount(course);

        return result(status == 3 ? "课程已满，已进入候补队列" : "选课成功", status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dropCourse(Long selectionId, Long studentId) {
        if (selectionId == null || studentId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "参数不能为空");
        }

        CourseSelection selection = courseSelectionRepository.findById(selectionId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "选课记录不存在"));

        if (!selection.getStudentId().equals(studentId)) {
            throw new BusinessException(Result.PARAM_ERROR, "无权操作该选课记录");
        }
        if (Objects.equals(selection.getStatus(), 2)) {
            throw new BusinessException(Result.PARAM_ERROR, "该课程已退选");
        }

        selection.setStatus(2);
        selection.setDropTime(new Date());
        selection.setUpdateTime(new Date());
        courseSelectionRepository.save(selection);

        courseRepository.findById(selection.getCourseId()).ifPresent(this::refreshSelectedCount);
        promoteWaitingSelection(selection.getCourseId());
        return true;
    }

    @Override
    public CourseSelection getCourseSelectionById(Long selectionId) {
        return enrich(courseSelectionRepository.findById(selectionId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "选课记录不存在")));
    }

    @Override
    public Page<CourseSelection> getStudentCourseSelections(Long studentId, PageRequest pageRequest, String semester, Integer status) {
        PageRequest request = pageRequest == null ? new PageRequest() : pageRequest;
        Sort sort = Sort.by(sortDirection(request), sortColumn(request));
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(normalizePageNum(request.getPageNum()) - 1,
                        normalizePageSize(request.getPageSize()), sort);

        Page<CourseSelection> page = status != null
                ? courseSelectionRepository.findByStudentIdAndStatus(studentId, status, springPageRequest)
                : courseSelectionRepository.findByStudentId(studentId, springPageRequest);
        return page.map(this::enrich);
    }

    @Override
    public Page<CourseSelection> getCourseStudentList(Long courseId, PageRequest pageRequest, Integer status) {
        PageRequest request = pageRequest == null ? new PageRequest() : pageRequest;
        Sort sort = Sort.by(sortDirection(request), sortColumn(request));
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(normalizePageNum(request.getPageNum()) - 1,
                        normalizePageSize(request.getPageSize()), sort);
        return status != null
                ? courseSelectionRepository.findByCourseIdAndStatus(courseId, status, springPageRequest).map(this::enrich)
                : courseSelectionRepository.findByCourseId(courseId, springPageRequest).map(this::enrich);
    }

    @Override
    public Double countSelectedCredits(Long studentId, String semester) {
        return courseSelectionRepository.findByStudentIdAndStatus(studentId, 1)
                .stream()
                .map(this::enrich)
                .mapToDouble(selection -> selection.getCredit() == null ? 0D : selection.getCredit())
                .sum();
    }

    @Override
    public boolean isCourseSelected(Long studentId, Long courseId) {
        return courseSelectionRepository.findByStudentIdAndCourseId(studentId, courseId)
                .map(selection -> Objects.equals(selection.getStatus(), 1) || Objects.equals(selection.getStatus(), 3))
                .orElse(false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchSelectCourses(List<Long> studentIds, Long courseId) {
        if (studentIds == null || studentIds.isEmpty() || courseId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "参数不能为空");
        }
        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();
        for (Long studentId : studentIds) {
            try {
                selectCourse(studentId, courseId);
                successCount++;
            } catch (Exception e) {
                failCount++;
                failReasons.add("学生ID: " + studentId + " 选课失败: " + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("failReasons", failReasons);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDropCourses(List<Long> selectionIds) {
        if (selectionIds == null || selectionIds.isEmpty()) {
            throw new BusinessException(Result.PARAM_ERROR, "选课记录ID列表不能为空");
        }
        for (Long selectionId : selectionIds) {
            CourseSelection selection = courseSelectionRepository.findById(selectionId)
                    .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "选课记录不存在"));
            dropCourse(selectionId, selection.getStudentId());
        }
        return true;
    }

    @Override
    public List<CourseSelection> getStudentCurrentCourses(Long studentId, String semester) {
        List<CourseSelection> selections = courseSelectionRepository.findByStudentIdAndStatus(studentId, 1);
        selections.forEach(this::enrich);
        return selections;
    }

    @Override
    public long countCourseStudents(Long courseId) {
        return courseSelectionRepository.countByCourseIdAndStatus(courseId, 1);
    }

    @Override
    public List<CourseSelection> queryCourseSelections(Long studentId, Long courseId, String semester, Integer status) {
        List<CourseSelection> selections;
        if (studentId != null && courseId != null && status != null) {
            selections = courseSelectionRepository.findByStudentIdAndCourseIdAndStatus(studentId, courseId, status);
        } else if (studentId != null && status != null) {
            selections = courseSelectionRepository.findByStudentIdAndStatus(studentId, status);
        } else if (courseId != null && status != null) {
            selections = courseSelectionRepository.findByCourseIdAndStatus(courseId, status);
        } else if (studentId != null && courseId != null) {
            selections = courseSelectionRepository.findByStudentIdAndCourseId(studentId, courseId)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else if (studentId != null) {
            selections = courseSelectionRepository.findByStudentId(studentId);
        } else if (courseId != null) {
            selections = courseSelectionRepository.findByCourseId(courseId);
        } else {
            selections = Collections.emptyList();
        }
        selections.forEach(this::enrich);
        return selections;
    }

    @Override
    public Map<String, Object> getSelectionStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("selectionCount", courseSelectionRepository.count());
        stats.put("courseCount", courseRepository.count());
        return stats;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateGrade(Long selectionId, Long teacherId, Map<String, Object> gradeInfo) {
        if (selectionId == null || teacherId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "selectionId和teacherId不能为空");
        }

        CourseSelection selection = courseSelectionRepository.findById(selectionId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "选课记录不存在"));
        Course course = requireOwnedCourse(selection.getCourseId(), teacherId);

        Double dailyGrade = gradeValue(gradeInfo.get("dailyGrade"), "平时成绩");
        Double labGrade = gradeValue(gradeInfo.get("labGrade"), "实验成绩");
        Double examGrade = gradeValue(gradeInfo.get("examGrade"), "考试成绩");
        Double score = gradeValue(gradeInfo.get("score"), "总评成绩");
        if (score == null) {
            score = calculateScore(dailyGrade, labGrade, examGrade);
        }

        selection.setDailyGrade(dailyGrade);
        selection.setLabGrade(labGrade);
        selection.setExamGrade(examGrade);
        selection.setScore(score);
        selection.setRemark(stringValue(gradeInfo.get("remark")));
        selection.setUpdateTime(new Date());

        return toGradeRow(courseSelectionRepository.save(selection), course);
    }

    @Override
    public List<Map<String, Object>> getTeacherCourseStudents(Long courseId, Long teacherId, Integer status) {
        Course course = requireOwnedCourse(courseId, teacherId);
        List<CourseSelection> selections = status == null
                ? courseSelectionRepository.findByCourseId(courseId)
                : courseSelectionRepository.findByCourseIdAndStatus(courseId, status);

        List<Map<String, Object>> rows = new ArrayList<>();
        for (CourseSelection selection : selections) {
            rows.add(toStudentRow(selection, course));
        }
        return rows;
    }

    @Override
    public Map<String, Object> getTeacherDashboard(Long teacherId) {
        if (teacherId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "teacherId不能为空");
        }

        List<Course> courses = courseRepository.findByTeacherId(teacherId);
        if (courses == null) {
            courses = Collections.emptyList();
        }

        long selectedCount = 0L;
        long gradedCount = 0L;
        long waitingCount = 0L;
        double scoreTotal = 0D;
        long scoreCount = 0L;
        List<Map<String, Object>> recentSelections = new ArrayList<>();

        for (Course course : courses) {
            List<CourseSelection> selections = courseSelectionRepository.findByCourseId(course.getId());
            for (CourseSelection selection : selections) {
                if (Objects.equals(selection.getStatus(), 1)) {
                    selectedCount++;
                    if (selection.getScore() != null) {
                        gradedCount++;
                        scoreTotal += selection.getScore();
                        scoreCount++;
                    }
                    recentSelections.add(toStudentRow(selection, course));
                } else if (Objects.equals(selection.getStatus(), 3)) {
                    waitingCount++;
                }
            }
        }

        recentSelections.sort((left, right) -> {
            Date leftTime = (Date) left.get("selectionTime");
            Date rightTime = (Date) right.get("selectionTime");
            if (leftTime == null && rightTime == null) {
                return 0;
            }
            if (leftTime == null) {
                return 1;
            }
            if (rightTime == null) {
                return -1;
            }
            return rightTime.compareTo(leftTime);
        });
        if (recentSelections.size() > 8) {
            recentSelections = new ArrayList<>(recentSelections.subList(0, 8));
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("courseCount", courses.size());
        stats.put("studentCount", selectedCount);
        stats.put("gradedCount", gradedCount);
        stats.put("waitingCount", waitingCount);
        stats.put("averageScore", scoreCount == 0 ? null : Math.round((scoreTotal / scoreCount) * 10D) / 10D);
        stats.put("courses", courses);
        stats.put("recentSelections", recentSelections);
        return stats;
    }

    private void promoteWaitingSelection(Long courseId) {
        Course course = courseRepository.findById(courseId).orElse(null);
        if (course == null) {
            return;
        }
        long currentCount = courseSelectionRepository.countByCourseIdAndStatus(courseId, 1);
        if (currentCount >= safeCapacity(course)) {
            return;
        }
        List<CourseSelection> waitingList = courseSelectionRepository.findByCourseIdAndStatusOrderBySelectionTimeAsc(courseId, 3);
        if (!waitingList.isEmpty()) {
            CourseSelection next = waitingList.get(0);
            next.setStatus(1);
            next.setUpdateTime(new Date());
            courseSelectionRepository.save(next);
            refreshSelectedCount(course);
        }
    }

    private void refreshSelectedCount(Course course) {
        course.setSelectedCount((int) courseSelectionRepository.countByCourseIdAndStatus(course.getId(), 1));
        course.setUpdateTime(new Date());
        courseRepository.save(course);
    }

    private CourseSelection enrich(CourseSelection selection) {
        if (selection == null) {
            return null;
        }
        courseRepository.findById(selection.getCourseId()).ifPresent(course -> {
            selection.setCourseName(course.getCourseName());
            selection.setCourseCode(course.getCourseCode());
            selection.setCredit(course.getCredit());
            selection.setTeacherId(course.getTeacherId());
            selection.setTeacherName(course.getTeacherName());
            selection.setCourseType(course.getCourseType());
            selection.setSchedule(course.getSchedule());
            selection.setClassroom(course.getClassroom());
        });
        return selection;
    }

    private Course requireOwnedCourse(Long courseId, Long teacherId) {
        if (courseId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "课程ID不能为空");
        }
        if (teacherId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "teacherId不能为空");
        }
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));
        if (!Objects.equals(course.getTeacherId(), teacherId)) {
            throw new BusinessException(403, "无权访问该课程");
        }
        return course;
    }

    private Map<String, Object> toGradeRow(CourseSelection selection, Course course) {
        return toStudentRow(selection, course);
    }

    private Map<String, Object> toStudentRow(CourseSelection selection, Course course) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("selectionId", selection.getId());
        row.put("courseId", selection.getCourseId());
        row.put("courseCode", course.getCourseCode());
        row.put("courseName", course.getCourseName());
        row.put("teacherId", course.getTeacherId());
        row.put("teacherName", course.getTeacherName());
        row.put("courseType", course.getCourseType());
        row.put("credit", course.getCredit());
        row.put("schedule", course.getSchedule());
        row.put("classroom", course.getClassroom());
        row.put("studentId", selection.getStudentId());
        row.put("selectionTime", selection.getSelectionTime());
        row.put("dailyGrade", selection.getDailyGrade());
        row.put("labGrade", selection.getLabGrade());
        row.put("examGrade", selection.getExamGrade());
        row.put("score", selection.getScore());
        row.put("scoreLevel", scoreLevel(selection.getScore()));
        row.put("remark", selection.getRemark());
        row.put("status", selection.getStatus());
        row.put("statusText", selectionStatus(selection.getStatus()));
        appendStudentInfo(row, selection.getStudentId());
        return row;
    }

    private void appendStudentInfo(Map<String, Object> row, Long studentId) {
        Optional<Student> optionalStudent = studentId == null || studentRepository == null
                ? Optional.empty()
                : studentRepository.findById(studentId);
        if (optionalStudent.isPresent()) {
            Student student = optionalStudent.get();
            row.put("studentNo", student.getStudentNo());
            row.put("studentName", student.getName());
            row.put("gender", student.getGender());
            row.put("majorId", student.getMajorId());
            row.put("collegeId", student.getCollegeId());
            row.put("className", student.getClassName());
            row.put("phone", student.getPhone());
            row.put("email", student.getEmail());
            return;
        }

        row.put("studentNo", studentId);
        row.put("studentName", "学生" + studentId);
    }

    private Double calculateScore(Double dailyGrade, Double labGrade, Double examGrade) {
        if (dailyGrade == null && labGrade == null && examGrade == null) {
            return null;
        }
        double daily = dailyGrade == null ? 0D : dailyGrade;
        double lab = labGrade == null ? 0D : labGrade;
        double exam = examGrade == null ? 0D : examGrade;
        return Math.round((daily * 0.4D + lab * 0.2D + exam * 0.4D) * 10D) / 10D;
    }

    private String scoreLevel(Double score) {
        if (score == null) {
            return "未录入";
        }
        if (score >= 90D) {
            return "优秀";
        }
        if (score >= 80D) {
            return "良好";
        }
        if (score >= 70D) {
            return "中等";
        }
        if (score >= 60D) {
            return "及格";
        }
        return "不及格";
    }

    private String selectionStatus(Integer status) {
        if (Objects.equals(status, 1)) {
            return "已选";
        }
        if (Objects.equals(status, 2)) {
            return "已退课";
        }
        if (Objects.equals(status, 3)) {
            return "候补";
        }
        return "未知";
    }

    private Double gradeValue(Object value, String label) {
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            double number = Double.parseDouble(String.valueOf(value));
            if (number < 0D || number > 100D) {
                throw new BusinessException(Result.PARAM_ERROR, label + "必须在0到100之间");
            }
            return number;
        } catch (NumberFormatException e) {
            throw new BusinessException(Result.PARAM_ERROR, label + "必须是数字");
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer safeCapacity(Course course) {
        return course.getAvailableSlots() == null ? 0 : course.getAvailableSlots();
    }

    private Map<String, Object> result(String message, Integer status) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put("status", status);
        return result;
    }

    private Sort.Direction sortDirection(PageRequest request) {
        return Boolean.TRUE.equals(request.getIsAsc()) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String sortColumn(PageRequest pageRequest) {
        if (pageRequest == null || !StringUtils.hasText(pageRequest.getOrderByColumn())) {
            return "selectionTime";
        }
        return SORT_COLUMNS.getOrDefault(pageRequest.getOrderByColumn().trim(), "selectionTime");
    }
}
