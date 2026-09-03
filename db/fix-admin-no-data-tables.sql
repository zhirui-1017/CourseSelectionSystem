-- ============================================================
-- 补齐代码所引用但库中缺失的表/列（仅结构，不含任何示例数据）
-- 数据库：course_selection_system_cloud  （可重复执行）
-- 说明：这些新表初始为空；公告/日志/设置等页面在真实写入数据前显示“暂无”
-- ============================================================

-- 1) 学期管理：semester 表补充代码使用的 created_at / updated_at 列
ALTER TABLE semester
    ADD COLUMN created_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

-- 2) 系统日志：操作日志表 operation_log（user-service UserSupportController 使用）
CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_type TINYINT NOT NULL COMMENT '操作人类型：1学生 2教师 3管理员',
    operator_id BIGINT NOT NULL COMMENT '操作人ID',
    operator_name VARCHAR(50) NOT NULL COMMENT '操作人姓名',
    operation_type VARCHAR(50) NOT NULL COMMENT '类型分类：auth/operation/system/data',
    operation_desc VARCHAR(500) NULL COMMENT '操作描述',
    operation_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    ip_address VARCHAR(64) NULL COMMENT 'IP地址',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1成功 0失败',
    KEY idx_op_time (operation_time),
    KEY idx_op_type (operation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';

-- 3) 系统设置：system_setting（user-service UserSupportController 使用）
CREATE TABLE IF NOT EXISTS system_setting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE COMMENT '设置键',
    setting_value VARCHAR(500) NOT NULL COMMENT '设置值',
    description VARCHAR(200) NULL COMMENT '说明',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统设置';

-- 4) 消息通知表 message_notification（user-service /messages 使用）
CREATE TABLE IF NOT EXISTS message_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recipient_id BIGINT NOT NULL COMMENT '接收人ID',
    recipient_type TINYINT NOT NULL COMMENT '接收人类型：1学生 2教师 3管理员',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT NULL COMMENT '内容',
    message_type VARCHAR(50) NOT NULL DEFAULT 'system' COMMENT '类型：system/course/assignment/announcement',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读 1已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_recipient (recipient_id, recipient_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息通知';

-- 5) 公告管理：课程公告表 course_announcement（course-service CourseAnnouncementController 使用）
CREATE TABLE IF NOT EXISTS course_announcement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL COMMENT '课程ID',
    title VARCHAR(200) NOT NULL COMMENT '公告标题',
    content TEXT NULL COMMENT '公告内容',
    created_by BIGINT NULL COMMENT '发布人(教师ID)',
    publish_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_course (course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程公告';