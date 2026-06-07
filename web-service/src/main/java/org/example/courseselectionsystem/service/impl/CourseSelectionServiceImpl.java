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

import java.util.*;

/**
 * 选课服务实现类
 */
@Service
public class CourseSelectionServiceImpl implements CourseSelectionService {

    @Autowired
    private CourseSelectionRepository courseSelectionRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> selectCourse(Long studentId, Long courseId) {
        // 验证学生和课程ID
        if (studentId == null || courseId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "学生ID和课程ID不能为空");
        }

        // 查询课程信息
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));

        // 检查课程状态
        if (course.getStatus() != 1) {
            throw new BusinessException(Result.PARAM_ERROR, "课程已关闭选课");
        }

        // 检查是否已选该课程
        Optional<CourseSelection> existingSelection = courseSelectionRepository.findByStudentIdAndCourseId(studentId, courseId);
        if (existingSelection.isPresent()) {
            CourseSelection selection = existingSelection.get();
            if (selection.getStatus() == 1) {
                throw new BusinessException(Result.PARAM_ERROR, "您已选该课程");
            } else if (selection.getStatus() == 2) {
                throw new BusinessException(Result.PARAM_ERROR, "您已退选该课程");
            }
        }

        // 检查课程是否已满
        long selectedCount = courseSelectionRepository.countByCourseIdAndStatus(courseId, 1);
        if (selectedCount >= course.getMaxCapacity()) {
            // 如果已满，则进入候补
            CourseSelection selection = new CourseSelection();
            selection.setStudentId(studentId);
            selection.setCourseId(courseId);
            selection.setCourseCode(course.getCourseCode());
            selection.setCourseName(course.getCourseName());
            selection.setCredit(course.getCredit());
            selection.setTeacherName(course.getTeacherName());
            selection.setSemester(course.getSemester());
            selection.setStatus(3); // 候补状态
            selection.setSelectionTime(new Date());
            selection.setCreateTime(new Date());
            selection.setUpdateTime(new Date());
            courseSelectionRepository.save(selection);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "课程已选满，您已进入候补队列");
            result.put("status", 3);
            return result;
        }

        // 正常选课
        CourseSelection selection = new CourseSelection();
        selection.setStudentId(studentId);
        selection.setCourseId(courseId);
        selection.setCourseCode(course.getCourseCode());
        selection.setCourseName(course.getCourseName());
        selection.setCredit(course.getCredit());
        selection.setTeacherName(course.getTeacherName());
        selection.setSemester(course.getSemester());
        selection.setStatus(1); // 正常状态
        selection.setSelectionTime(new Date());
        selection.setCreateTime(new Date());
        selection.setUpdateTime(new Date());
        courseSelectionRepository.save(selection);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "选课成功");
        result.put("status", 1);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dropCourse(Long selectionId, Long studentId) {
        // 验证选课记录ID和学生ID
        if (selectionId == null || studentId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "参数不能为空");
        }

        // 查询选课记录
        CourseSelection selection = courseSelectionRepository.findById(selectionId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "选课记录不存在"));

        // 验证是否是该学生的选课记录
        if (!selection.getStudentId().equals(studentId)) {
            throw new BusinessException(Result.PARAM_ERROR, "无权操作此选课记录");
        }

        // 检查状态
        if (selection.getStatus() == 2) {
            throw new BusinessException(Result.PARAM_ERROR, "您已退选该课程");
        }

        // 更新状态为退课
        selection.setStatus(2);
        selection.setUpdateTime(new Date());
        courseSelectionRepository.save(selection);

        // 自动处理候补学生补入
        List<CourseSelection> waitingList = courseSelectionRepository.findByCourseIdAndStatusOrderBySelectionTimeAsc(selection.getCourseId(), 3);
        if (!waitingList.isEmpty()) {
            Course course = courseRepository.findById(selection.getCourseId()).orElse(null);
            if (course != null) {
                long currentCount = courseSelectionRepository.countByCourseIdAndStatus(selection.getCourseId(), 1);
                for (CourseSelection waitingSelection : waitingList) {
                    if (currentCount < course.getMaxCapacity()) {
                        waitingSelection.setStatus(1); // 从候补转为正式选课
                        waitingSelection.setUpdateTime(new Date());
                        courseSelectionRepository.save(waitingSelection);
                        currentCount++;
                    } else {
                        break;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public CourseSelection getCourseSelectionById(Long selectionId) {
        // 查询选课记录
        return courseSelectionRepository.findById(selectionId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "选课记录不存在"));
    }

    @Override
    public Page<CourseSelection> getStudentCourseSelections(Long studentId, PageRequest pageRequest, String semester, Integer status) {
        // 构建排序规则
        Sort.Direction direction = pageRequest.getIsAsc() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, pageRequest.getOrderByColumn());

        // 构建分页请求
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(pageRequest.getPageNum() - 1, pageRequest.getPageSize(), sort);

        // 调用Repository方法实现分页查询
        if (semester != null && status != null) {
            return courseSelectionRepository.findByStudentIdAndSemesterAndStatus(studentId, semester, status, springPageRequest);
        } else if (semester != null) {
            return courseSelectionRepository.findByStudentIdAndSemester(studentId, semester, springPageRequest);
        } else if (status != null) {
            return courseSelectionRepository.findByStudentIdAndStatus(studentId, status, springPageRequest);
        } else {
            return courseSelectionRepository.findByStudentId(studentId, springPageRequest);
        }
    }

    @Override
    public Page<CourseSelection> getCourseStudentList(Long courseId, PageRequest pageRequest, Integer status) {
        // 构建排序规则
        Sort.Direction direction = pageRequest.getIsAsc() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, pageRequest.getOrderByColumn());

        // 构建分页请求
        org.springframework.data.domain.PageRequest springPageRequest =
                org.springframework.data.domain.PageRequest.of(pageRequest.getPageNum() - 1, pageRequest.getPageSize(), sort);

        // 调用Repository方法实现分页查询
        if (status != null) {
            return courseSelectionRepository.findByCourseIdAndStatus(courseId, status, springPageRequest);
        } else {
            return courseSelectionRepository.findByCourseId(courseId, springPageRequest);
        }
    }

    @Override
    public Double countSelectedCredits(Long studentId, String semester) {
        // 统计学生已选课程学分
        List<CourseSelection> selections = courseSelectionRepository.findByStudentIdAndSemesterAndStatus(studentId, semester, 1);
        return selections.stream().mapToDouble(CourseSelection::getCredit).sum();
    }

    @Override
    public boolean isCourseSelected(Long studentId, Long courseId) {
        // 检查学生是否已选某课程
        Optional<CourseSelection> selection = courseSelectionRepository.findByStudentIdAndCourseId(studentId, courseId);
        return selection.isPresent() && selection.get().getStatus() == 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchSelectCourses(List<Long> studentIds, Long courseId) {
        // 验证参数
        if (studentIds == null || studentIds.isEmpty() || courseId == null) {
            throw new BusinessException(Result.PARAM_ERROR, "参数不能为空");
        }

        // 查询课程信息
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new BusinessException(Result.NOT_FOUND, "课程不存在"));

        // 批量选课
        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();

        for (Long studentId : studentIds) {
            try {
                // 检查是否已选
                Optional<CourseSelection> existingSelection = courseSelectionRepository.findByStudentIdAndCourseId(studentId, courseId);
                if (existingSelection.isPresent()) {
                    if (existingSelection.get().getStatus() == 1) {
                        failCount++;
                        failReasons.add("学生ID: " + studentId + " 已选该课程");
                        continue;
                    } else if (existingSelection.get().getStatus() == 2) {
                        // 允许重新选已退选的课程
                        CourseSelection selection = existingSelection.get();
                        selection.setStatus(1);
                        selection.setSelectionTime(new Date());
                        selection.setUpdateTime(new Date());
                        courseSelectionRepository.save(selection);
                        successCount++;
                    }
                } else {
                    // 检查名额
                    long selectedCount = courseSelectionRepository.countByCourseIdAndStatus(courseId, 1);
                    int status = (selectedCount >= course.getMaxCapacity()) ? 3 : 1;
                    
                    // 创建选课记录
                    CourseSelection selection = new CourseSelection();
                    selection.setStudentId(studentId);
                    selection.setCourseId(courseId);
                    selection.setCourseCode(course.getCourseCode());
                    selection.setCourseName(course.getCourseName());
                    selection.setCredit(course.getCredit());
                    selection.setTeacherName(course.getTeacherName());
                    selection.setSemester(course.getSemester());
                    selection.setStatus(status);
                    selection.setSelectionTime(new Date());
                    selection.setCreateTime(new Date());
                    selection.setUpdateTime(new Date());
                    courseSelectionRepository.save(selection);
                    successCount++;
                }
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
        // 验证参数
        if (selectionIds == null || selectionIds.isEmpty()) {
            throw new BusinessException(Result.PARAM_ERROR, "选课记录ID列表不能为空");
        }
        
        // 批量退课
        List<CourseSelection> selections = courseSelectionRepository.findAllById(selectionIds);
        if (selections.size() != selectionIds.size()) {
            throw new BusinessException(Result.PARAM_ERROR, "部分选课记录不存在");
        }
        
        for (CourseSelection selection : selections) {
            if (selection.getStatus() == 2) {
                throw new BusinessException(Result.PARAM_ERROR, "存在已退选的课程记录");
            }
            selection.setStatus(2);
            selection.setUpdateTime(new Date());
        }
        
        courseSelectionRepository.saveAll(selections);
        
        // 处理每个课程的候补学生
        for (CourseSelection selection : selections) {
            // 复用单个退课的候补处理逻辑
            dropCourse(selection.getId(), selection.getStudentId());
        }
        
        return true;
    }

    @Override
    public List<CourseSelection> getStudentCurrentCourses(Long studentId, String semester) {
        // 获取学生当前学期的正常选课记录
        return courseSelectionRepository.findByStudentIdAndSemesterAndStatus(studentId, semester, 1);
    }

    @Override
    public long countCourseStudents(Long courseId) {
        // 统计课程的实际选修人数
        return courseSelectionRepository.countByCourseIdAndStatus(courseId, 1);
    }

    @Override
    public List<CourseSelection> queryCourseSelections(Long studentId, Long courseId, String semester, Integer status) {
        // 根据条件组合查询选课记录
        if (studentId != null && courseId != null && semester != null && status != null) {
            return courseSelectionRepository.findByStudentIdAndCourseIdAndSemesterAndStatus(studentId, courseId, semester, status);
        } else if (studentId != null && semester != null && status != null) {
            return courseSelectionRepository.findByStudentIdAndSemesterAndStatus(studentId, semester, status);
        } else if (courseId != null && status != null) {
            return courseSelectionRepository.findByCourseIdAndStatus(courseId, status);
        } else if (studentId != null && courseId != null) {
            return courseSelectionRepository.findByStudentIdAndCourseId(studentId, courseId)
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        } else if (studentId != null) {
            return courseSelectionRepository.findByStudentId(studentId);
        } else if (courseId != null) {
            return courseSelectionRepository.findByCourseId(courseId);
        } else {
            return Collections.emptyList();
        }
    }
}
