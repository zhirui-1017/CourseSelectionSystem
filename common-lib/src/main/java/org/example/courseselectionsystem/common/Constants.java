package org.example.courseselectionsystem.common;

public final class Constants {

    private Constants() {
    }

    public static final int SUCCESS_CODE = 200;
    public static final int FAIL_CODE = 500;
    public static final int PARAM_ERROR_CODE = 400;
    public static final int NOT_FOUND_CODE = 404;
    public static final int UNAUTHORIZED_CODE = 401;
    public static final int FORBIDDEN_CODE = 403;
    public static final int DUPLICATE_CODE = 409;
    public static final int NOT_IMPLEMENTED_CODE = 501;
    public static final String ADMIN_USERNAME = "admin";

    public static final String SUCCESS_MESSAGE = "操作成功";
    public static final String FAIL_MESSAGE = "操作失败";
    public static final String PARAM_ERROR_MESSAGE = "参数错误";
    public static final String NOT_FOUND_MESSAGE = "未找到资源";
    public static final String SYSTEM_ERROR_MESSAGE = "系统内部错误";

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_STUDENT = "ROLE_STUDENT";
    public static final String ROLE_TEACHER = "ROLE_TEACHER";

    public static final int DEFAULT_PAGE_NUM = 1;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    public static final int COURSE_STATUS_NOT_STARTED = 0;
    public static final int COURSE_STATUS_IN_PROGRESS = 1;
    public static final int COURSE_STATUS_ENDED = 2;

    public static final int SELECTION_STATUS_SELECTED = 0;
    public static final int SELECTION_STATUS_DROPPED = 1;
    public static final int SELECTION_STATUS_COMPLETED = 2;

    public static final String STUDENT_NO_REGEX = "^[A-Za-z]?\\d{4,10}$";
    public static final String TEACHER_NO_REGEX = "^[A-Za-z]?\\d{3,10}$";
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{6,}$";
    public static final String EMAIL_REGEX = "^[\\w-\\.]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    public static final String PHONE_REGEX = "^1[3-9]\\d{9}$";
}
