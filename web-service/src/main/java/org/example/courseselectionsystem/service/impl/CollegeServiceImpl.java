package org.example.courseselectionsystem.service.impl;

import org.example.courseselectionsystem.exception.BusinessException;
import org.example.courseselectionsystem.mapper.CollegeMapper;
import org.example.courseselectionsystem.entity.College;
import org.example.courseselectionsystem.repository.CollegeRepository;
import org.example.courseselectionsystem.service.CollegeService;
import org.example.courseselectionsystem.vo.CollegeRequest;
import org.example.courseselectionsystem.vo.CollegeVO;
import org.example.courseselectionsystem.vo.PageRequest;
import org.example.courseselectionsystem.vo.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 学院服务实现类
 * 实现学院相关的业务逻辑
 */
@Service
public class CollegeServiceImpl implements CollegeService {

    private static final Logger logger = LoggerFactory.getLogger(CollegeServiceImpl.class);

    @Autowired
    private CollegeRepository collegeRepository;

    @Autowired
    private CollegeMapper collegeMapper;

    /**
     * 添加学院方法实现
     * @param request 学院请求参数
     * @return 学院ID
     */
    @Override
    @Transactional
    public Long addCollege(CollegeRequest request) {
        logger.info("添加学院，参数：{}", request);
        // 参数验证
        if (!StringUtils.hasText(request.getName())) {
            throw new BusinessException("学院名称不能为空");
        }
        if (!StringUtils.hasText(request.getCode())) {
            throw new BusinessException("学院代码不能为空");
        }

        // 检查学院代码是否已存在
        Optional<College> existingCollege = collegeRepository.findByCode(request.getCode());
        if (existingCollege.isPresent()) {
            throw new BusinessException("学院代码已存在：" + request.getCode());
        }

        // 检查学院名称是否已存在
        List<College> existingColleges = collegeRepository.findByName(request.getName());
        if (!existingColleges.isEmpty()) {
            throw new BusinessException("学院名称已存在：" + request.getName());
        }

        // 创建学院实体
        College college = new College();
        college.setName(request.getName());
        college.setCode(request.getCode());
        college.setDescription(request.getDescription());
        college.setStatus(1); // 默认启用

        // 保存学院
        college = collegeRepository.save(college);
        logger.info("添加学院成功，学院ID：{}", college.getId());
        return college.getId();
    }

    /**
     * 更新学院方法实现
     * @param id 学院ID
     * @param request 学院请求参数
     */
    @Override
    @Transactional
    public void updateCollege(Long id, CollegeRequest request) {
        logger.info("更新学院，学院ID：{}，参数：{}", id, request);
        // 检查学院是否存在
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学院不存在，ID：" + id));

        // 检查学院代码是否被其他学院使用
        if (StringUtils.hasText(request.getCode()) && !request.getCode().equals(college.getCode())) {
            Optional<College> existingCollege = collegeRepository.findByCode(request.getCode());
            if (existingCollege.isPresent() && !existingCollege.get().getId().equals(id)) {
                throw new BusinessException("学院代码已存在：" + request.getCode());
            }
            college.setCode(request.getCode());
        }

        // 检查学院名称是否被其他学院使用
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(college.getName())) {
            List<College> existingColleges = collegeRepository.findByName(request.getName());
            boolean nameExists = existingColleges.stream()
                    .anyMatch(c -> !c.getId().equals(id));
            if (nameExists) {
                throw new BusinessException("学院名称已存在：" + request.getName());
            }
            college.setName(request.getName());
        }

        // 更新其他字段
        if (request.getDescription() != null) {
            college.setDescription(request.getDescription());
        }

        // 保存更新
        collegeRepository.save(college);
        logger.info("更新学院成功，学院ID：{}", id);
    }

    /**
     * 删除学院方法实现
     * @param id 学院ID
     */
    @Override
    @Transactional
    public void deleteCollege(Long id) {
        logger.info("删除学院，学院ID：{}", id);
        // 检查学院是否存在
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学院不存在，ID：" + id));

        // 执行删除
        collegeRepository.delete(college);
        logger.info("删除学院成功，学院ID：{}", id);
    }

    /**
     * 根据ID获取学院方法实现
     * @param id 学院ID
     * @return 学院信息
     */
    @Override
    public CollegeVO getCollegeById(Long id) {
        logger.info("根据ID获取学院，学院ID：{}", id);
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学院不存在，ID：" + id));
        return collegeMapper.toVO(college);
    }

    /**
     * 获取所有学院方法实现
     * @return 学院列表
     */
    @Override
    public List<CollegeVO> getAllColleges() {
        logger.info("获取所有学院");
        List<College> colleges = collegeRepository.findAll();
        return colleges.stream()
                .map(collegeMapper::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 获取学院列表方法实现
     * @param pageRequest 分页请求参数
     * @return 学院列表
     */
    @Override
    public PageResult<CollegeVO> getCollegeList(PageRequest pageRequest) {
        logger.info("获取学院列表，分页参数：{}", pageRequest);
        // 这里应该调用带分页的查询方法
        // 暂时简化实现
        List<College> colleges = collegeRepository.findAll();
        List<CollegeVO> collegeVOs = colleges.stream()
                .map(collegeMapper::toVO)
                .collect(Collectors.toList());
        
        return new PageResult<CollegeVO>() {
            @Override
            public List<CollegeVO> getItems() {
                return collegeVOs;
            }

            @Override
            public long getTotal() {
                return collegeVOs.size();
            }

            @Override
            public int getPageNum() {
                return pageRequest.getPageNum();
            }

            @Override
            public int getPageSize() {
                return pageRequest.getPageSize();
            }
        };
    }

    /**
     * 根据名称查询学院方法实现
     * @param name 学院名称
     * @return 学院列表
     */
    @Override
    public List<CollegeVO> getCollegeByName(String name) {
        logger.info("根据名称查询学院，名称：{}", name);
        List<College> colleges = collegeRepository.findByNameContaining(name);
        return colleges.stream()
                .map(collegeMapper::toVO)
                .collect(Collectors.toList());
    }

    /**
     * 根据代码查询学院方法实现
     * @param code 学院代码
     * @return 学院信息
     */
    @Override
    public CollegeVO getCollegeByCode(String code) {
        logger.info("根据代码查询学院，代码：{}", code);
        College college = collegeRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("学院不存在，代码：" + code));
        return collegeMapper.toVO(college);
    }

    /**
     * 启用学院方法实现
     * @param id 学院ID
     */
    @Override
    @Transactional
    public void enableCollege(Long id) {
        logger.info("启用学院，学院ID：{}", id);
        updateCollegeStatus(id, 1);
    }

    /**
     * 禁用学院方法实现
     * @param id 学院ID
     */
    @Override
    @Transactional
    public void disableCollege(Long id) {
        logger.info("禁用学院，学院ID：{}", id);
        updateCollegeStatus(id, 0);
    }

    /**
     * 更新学院状态的私有方法
     * @param id 学院ID
     * @param status 状态值
     */
    private void updateCollegeStatus(Long id, Integer status) {
        College college = collegeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("学院不存在，ID：" + id));
        college.setStatus(status);
        collegeRepository.save(college);
    }
}
