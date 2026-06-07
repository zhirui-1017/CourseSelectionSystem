package org.example.courseselectionsystem.controller;

import org.example.courseselectionsystem.common.Result;
import org.example.courseselectionsystem.service.CollegeService;
import org.example.courseselectionsystem.vo.CollegeRequest;
import org.example.courseselectionsystem.vo.CollegeVO;
import org.example.courseselectionsystem.vo.PageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 学院控制器类
 * 处理学院相关的HTTP请求
 */
@RestController
@RequestMapping("/api/v1/colleges")
public class CollegeController {

    @Autowired
    private CollegeService collegeService;

    /**
     * 添加学院接口
     * @param request 学院请求参数
     * @return 添加结果
     */
    @PostMapping
    public Result<Long> addCollege(@RequestBody CollegeRequest request) {
        Long id = collegeService.addCollege(request);
        return Result.success(id);
    }

    /**
     * 更新学院接口
     * @param id 学院ID
     * @param request 学院请求参数
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public Result<Boolean> updateCollege(@PathVariable Long id, @RequestBody CollegeRequest request) {
        collegeService.updateCollege(id, request);
        return Result.success(true);
    }

    /**
     * 删除学院接口
     * @param id 学院ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteCollege(@PathVariable Long id) {
        collegeService.deleteCollege(id);
        return Result.success(true);
    }

    /**
     * 根据ID获取学院接口
     * @param id 学院ID
     * @return 学院信息
     */
    @GetMapping("/{id}")
    public Result<CollegeVO> getCollegeById(@PathVariable Long id) {
        CollegeVO college = collegeService.getCollegeById(id);
        return Result.success(college);
    }

    /**
     * 获取所有学院接口
     * @return 学院列表
     */
    @GetMapping("/all")
    public Result<List<CollegeVO>> getAllColleges() {
        List<CollegeVO> collegeList = collegeService.getAllColleges();
        return Result.success(collegeList);
    }

    /**
     * 获取学院列表接口
     * @param pageRequest 分页请求参数
     * @return 学院列表
     */
    @GetMapping("/list")
    public Result<PageResult<CollegeVO>> getCollegeList(PageRequest pageRequest) {
        org.example.courseselectionsystem.service.CollegeService.PageResult<CollegeVO> collegeList = collegeService.getCollegeList(pageRequest);
        return Result.success(new PageResult<>(collegeList.getItems(), collegeList.getTotal(), collegeList.getPageNum(), collegeList.getPageSize()));
    }

    /**
     * 根据名称查询学院接口
     * @param name 学院名称
     * @return 学院列表
     */
    @GetMapping("/by-name/{name}")
    public Result<List<CollegeVO>> getCollegeByName(@PathVariable String name) {
        List<CollegeVO> collegeList = collegeService.getCollegeByName(name);
        return Result.success(collegeList);
    }

    /**
     * 根据代码查询学院接口
     * @param code 学院代码
     * @return 学院信息
     */
    @GetMapping("/by-code/{code}")
    public Result<CollegeVO> getCollegeByCode(@PathVariable String code) {
        CollegeVO college = collegeService.getCollegeByCode(code);
        return Result.success(college);
    }

    /**
     * 启用学院接口
     * @param id 学院ID
     * @return 启用结果
     */
    @PutMapping("/{id}/enable")
    public Result<Boolean> enableCollege(@PathVariable Long id) {
        collegeService.enableCollege(id);
        return Result.success(true);
    }

    /**
     * 禁用学院接口
     * @param id 学院ID
     * @return 禁用结果
     */
    @PutMapping("/{id}/disable")
    public Result<Boolean> disableCollege(@PathVariable Long id) {
        collegeService.disableCollege(id);
        return Result.success(true);
    }

    /**
     * 分页结果类
     * @param <T> 数据泛型
     */
    public static class PageResult<T> {
        private List<T> items;
        private long total;
        private int pageNum;
        private int pageSize;

        public PageResult(List<T> items, long total, int pageNum, int pageSize) {
            this.items = items;
            this.total = total;
            this.pageNum = pageNum;
            this.pageSize = pageSize;
        }

        public List<T> getItems() {
            return items;
        }

        public void setItems(List<T> items) {
            this.items = items;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public int getPageNum() {
            return pageNum;
        }

        public void setPageNum(int pageNum) {
            this.pageNum = pageNum;
        }

        public int getPageSize() {
            return pageSize;
        }

        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
    }
}
