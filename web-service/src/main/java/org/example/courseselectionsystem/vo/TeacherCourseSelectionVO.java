package org.example.courseselectionsystem.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 教师选课信息视图
 */
@Data
public class TeacherCourseSelectionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 选课记录ID
     */
    private Long id;

    /**
     * 学生ID
     */
    private Long studentId;

    /**
     * 学号
     */
    private String studentNo;

    /**
     * 学生姓名
     */
    private String studentName;

    /**
     * 学院ID
     */
    private Long departmentId;

    /**
     * 学院名称
     */
    private String departmentName;

    /**
     * 专业ID
     */
    private Long majorId;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 选课时间
     */
    private Date selectionTime;

    /**
     * 成绩
     */
    private BigDecimal score;

    /**
     * 成绩等级
     */
    private String grade;

    /**
     * 备注
     */
    private String remark;
}
