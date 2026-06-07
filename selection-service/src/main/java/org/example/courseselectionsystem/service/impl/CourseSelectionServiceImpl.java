package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.entity.Course;
import org.example.courseselectionsystem.entity.CourseSelection;
import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.repository.CourseRepository;
import org.example.courseselectionsystem.repository.CourseSelectionRepository;
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

    @Autowired
    private CourseSelectionRepository courseSelectionRepository;

    @Autowired
    private CourseRepository courseRepository;

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
        Sort sort = Sort.by(pageRequest.getIsAsc() ? Sort.Direction.ASC : Sort.Direction.DESC, sortColumn(pageRequest));
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(pageRequest.getPageNum() - 1, pageRequest.getPageSize(), sort);

        Page<CourseSelection> page = status != null
                ? courseSelectionRepository.findByStudentIdAndStatus(studentId, status, springPageRequest)
                : courseSelectionRepository.findByStudentId(studentId, springPageRequest);
        return page.map(this::enrich);
    }

    @Override
    public Page<CourseSelection> getCourseStudentList(Long courseId, PageRequest pageRequest, Integer status) {
        Sort sort = Sort.by(pageRequest.getIsAsc() ? Sort.Direction.ASC : Sort.Direction.DESC, sortColumn(pageRequest));
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(pageRequest.getPageNum() - 1, pageRequest.getPageSize(), sort);
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
        });
        return selection;
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

    private String sortColumn(PageRequest pageRequest) {
        return StringUtils.hasText(pageRequest.getOrderByColumn()) ? pageRequest.getOrderByColumn() : "selectionTime";
    }
}
