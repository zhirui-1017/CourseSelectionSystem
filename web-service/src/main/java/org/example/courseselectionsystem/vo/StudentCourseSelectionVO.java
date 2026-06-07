package org.example.courseselectionsystem.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 学生选课信息视图
 */
@Data
public class StudentCourseSelectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 选课记录ID
     */
    private Long id;

    /**
     * 课程ID
     */
    private Long courseId;

    /**
     * 课程编号
     */
    private String courseCode;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 课程类型
     */
    private String courseType;

    /**
     * 学分
     */
    private BigDecimal credit;

    /**
     * 总课时
     */
    private Integer totalHours;

    /**
     * 教师ID
     */
    private Long teacherId;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 学期
     */
    private String semester;

    /**
     * 上课时间
     */
    private String courseTime;

    /**
     * 上课地点
     */
    private String courseLocation;

    /**
     * 选课时间
     */
    private Date selectionTime;

    /**
     * 成绩
     */
    private BigDecimal score;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 成绩等级
     */
    private String grade;
}
