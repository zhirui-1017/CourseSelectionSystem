-- 用确定的BCrypt哈希更新所有密码
-- 注意：这是用Java BCryptPasswordEncoder对"123456"生成的哈希
-- 如果登录仍有问题，说明是代码层面的BCrypt匹配问题

SET FOREIGN_KEY_CHECKS = 0;
USE course_selection_system_cloud;

UPDATE student SET password = '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W' WHERE 1=1;
UPDATE teacher SET password = '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W' WHERE 1=1;
UPDATE admin SET password = '$2a$10$N.zmdr9k7uOCQbF9SvOPe.XqKdJhG5HnTmqxY6uI6v1eAHsVbDp/W' WHERE 1=1;

SET FOREIGN_KEY_CHECKS = 1;
