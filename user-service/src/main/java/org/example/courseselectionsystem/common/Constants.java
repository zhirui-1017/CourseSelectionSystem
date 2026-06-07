package org.example.courseselectionsystem.common;

/**
 * 系统常量类
 * 定义系统中使用的各种常量
 */
public class Constants {

    // ==================== 响应状态码 ====================
    /**
     * 成功状态码
     */
    public static final int SUCCESS_CODE = 200;
    
    /**
     * 失败状态码
     */
    public static final int FAIL_CODE = 500;
    
    /**
     * 参数错误状态码
     */
    public static final int PARAM_ERROR_CODE = 400;
    
    /**
     * 未找到资源状态码
     */
    public static final int NOT_FOUND_CODE = 404;
    
    /**
     * 未授权状态码
     */
    public static final int UNAUTHORIZED_CODE = 401;
    
    /**
     * 禁止访问状态码
     */
    public static final int FORBIDDEN_CODE = 403;
    public static final int DUPLICATE_CODE = 409;
    public static final int NOT_IMPLEMENTED_CODE = 501;
    public static final String ADMIN_USERNAME = "admin";
    
    // ==================== 响应消息 ====================
    /**
     * 成功消息
     */
    public static final String SUCCESS_MESSAGE = "操作成功";
    
    /**
     * 失败消息
     */
    public static final String FAIL_MESSAGE = "操作失败";
    
    /**
     * 参数错误消息
     */
    public static final String PARAM_ERROR_MESSAGE = "参数错误";
    
    /**
     * 未找到资源消息
     */
    public static final String NOT_FOUND_MESSAGE = "未找到资源";
    
    /**
     * 系统内部错误消息
     */
    public static final String SYSTEM_ERROR_MESSAGE = "系统内部错误";
    
    // ==================== 用户角色 ====================
    /**
     * 管理员角色
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    
    /**
     * 学生角色
     */
    public static final String ROLE_STUDENT = "ROLE_STUDENT";
    
    /**
     * 教师角色
     */
    public static final String ROLE_TEACHER = "ROLE_TEACHER";
    
    // ==================== 分页默认值 ====================
    /**
     * 默认页码
     */
    public static final int DEFAULT_PAGE_NUM = 1;
    
    /**
     * 默认每页条数
     */
    public static final int DEFAULT_PAGE_SIZE = 10;
    
    /**
     * 最大每页条数
     */
    public static final int MAX_PAGE_SIZE = 100;
    
    // ==================== 课程状态 ====================
    /**
     * 课程状态：未开始
     */
    public static final int COURSE_STATUS_NOT_STARTED = 0;
    
    /**
     * 课程状态：进行中
     */
    public static final int COURSE_STATUS_IN_PROGRESS = 1;
    
    /**
     * 课程状态：已结束
     */
    public static final int COURSE_STATUS_ENDED = 2;
    
    // ==================== 选课状态 ====================
    /**
     * 选课状态：已选
     */
    public static final int SELECTION_STATUS_SELECTED = 0;
    
    /**
     * 选课状态：已退课
     */
    public static final int SELECTION_STATUS_DROPPED = 1;
    
    /**
     * 选课状态：已完成
     */
    public static final int SELECTION_STATUS_COMPLETED = 2;
    
    // ==================== 正则表达式 ====================
    /**
     * 学号正则表达式（8位数字）
     */
    public static final String STUDENT_NO_REGEX = "^[A-Za-z]?\\d{4,10}$";
    
    /**
     * 工号正则表达式（6位数字）
     */
    public static final String TEACHER_NO_REGEX = "^[A-Za-z]?\\d{3,10}$";
    
    /**
     * 密码正则表达式（至少6位，包含字母和数字）
     */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{6,}$";
    
    /**
     * 邮箱正则表达式
     */
    public static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    
    /**
     * 手机号正则表达式
     */
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
}
