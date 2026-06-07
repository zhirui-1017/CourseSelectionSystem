package org.example.courseselectionsystem.repository;

import org.example.courseselectionsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户对象
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据工号/学号查询用户
     * @param userCode 工号/学号
     * @return 用户对象
     */
    Optional<User> findByUserCode(String userCode);

    /**
     * 检查用户名是否已存在
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查工号/学号是否已存在
     * @param userCode 工号/学号
     * @return 是否存在
     */
    boolean existsByUserCode(String userCode);
}
