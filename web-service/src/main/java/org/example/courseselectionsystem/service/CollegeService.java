package org.example.courseselectionsystem.service;

import org.example.courseselectionsystem.vo.CollegeRequest;
import org.example.courseselectionsystem.vo.CollegeVO;
import org.example.courseselectionsystem.vo.PageRequest;

import java.util.List;

/**
 * 学院服务接口
 * 定义学院相关的服务方法
 */
public interface CollegeService {

    /**
     * 添加学院方法
     * @param request 学院请求参数
     * @return 学院ID
     */
    Long addCollege(CollegeRequest request);

    /**
     * 更新学院方法
     * @param id 学院ID
     * @param request 学院请求参数
     */
    void updateCollege(Long id, CollegeRequest request);

    /**
     * 删除学院方法
     * @param id 学院ID
     */
    void deleteCollege(Long id);

    /**
     * 根据ID获取学院方法
     * @param id 学院ID
     * @return 学院信息
     */
    CollegeVO getCollegeById(Long id);

    /**
     * 获取所有学院方法
     * @return 学院列表
     */
    List<CollegeVO> getAllColleges();

    /**
     * 获取学院列表方法
     * @param pageRequest 分页请求参数
     * @return 学院列表
     */
    PageResult<CollegeVO> getCollegeList(PageRequest pageRequest);

    /**
     * 根据名称查询学院方法
     * @param name 学院名称
     * @return 学院列表
     */
    List<CollegeVO> getCollegeByName(String name);

    /**
     * 根据代码查询学院方法
     * @param code 学院代码
     * @return 学院信息
     */
    CollegeVO getCollegeByCode(String code);

    /**
     * 启用学院方法
     * @param id 学院ID
     */
    void enableCollege(Long id);

    /**
     * 禁用学院方法
     * @param id 学院ID
     */
    void disableCollege(Long id);

    /**
     * 分页结果接口
     * @param <T> 数据泛型
     */
    interface PageResult<T> {
        List<T> getItems();
        long getTotal();
        int getPageNum();
        int getPageSize();
    }
}
