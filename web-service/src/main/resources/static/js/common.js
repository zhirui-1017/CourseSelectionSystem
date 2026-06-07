// 公共JavaScript工具函数和交互逻辑

// DOM 加载完成后执行
document.addEventListener('DOMContentLoaded', function() {
    // 初始化侧边栏切换
    initSidebarToggle();
    
    // 初始化用户下拉菜单
    initUserDropdown();
    
    // 初始化模态框
    initModals();
    
    // 初始化标签页
    initTabs();
    
    // 初始化数据表格
    initDataTables();
    
    // 初始化表单验证
    initFormValidation();
    
    // 初始化通知系统
    initNotifications();
    
    // 初始化移动端响应式
    initMobileResponsive();
    
    // 初始化导航功能
    initNavigation();
});

// 侧边栏切换功能
function initSidebarToggle() {
    const sidebarToggle = document.querySelector('.sidebar-toggle');
    const sidebar = document.querySelector('.sidebar');
    
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', function() {
            sidebar.classList.toggle('collapsed');
            
            // 保存状态到本地存储
            localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed'));
        });
        
        // 从本地存储恢复状态
        const isCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
        if (isCollapsed) {
            sidebar.classList.add('collapsed');
        }
    }
    
    // 移动端侧边栏切换
    const mobileToggle = document.querySelector('.mobile-sidebar-toggle');
    if (mobileToggle && sidebar) {
        mobileToggle.addEventListener('click', function() {
            sidebar.classList.toggle('active');
        });
        
        // 点击主内容区关闭侧边栏
        const mainContent = document.querySelector('.main-content');
        if (mainContent) {
            mainContent.addEventListener('click', function(e) {
                if (!sidebar.contains(e.target) && !mobileToggle.contains(e.target)) {
                    sidebar.classList.remove('active');
                }
            });
        }
    }
}

// 用户下拉菜单
function initUserDropdown() {
    const dropdownToggles = document.querySelectorAll('.dropdown-toggle');
    
    dropdownToggles.forEach(toggle => {
        toggle.addEventListener('click', function(e) {
            e.stopPropagation();
            const dropdown = this.closest('.dropdown');
            const menu = dropdown.querySelector('.dropdown-menu');
            
            menu.classList.toggle('show');
            
            // 关闭其他下拉菜单
            document.querySelectorAll('.dropdown-menu.show').forEach(otherMenu => {
                if (otherMenu !== menu) {
                    otherMenu.classList.remove('show');
                }
            });
        });
    });
    
    // 点击页面其他地方关闭下拉菜单
    document.addEventListener('click', function() {
        document.querySelectorAll('.dropdown-menu.show').forEach(menu => {
            menu.classList.remove('show');
        });
    });
    
    // 阻止下拉菜单内部点击传播
    document.querySelectorAll('.dropdown-menu').forEach(menu => {
        menu.addEventListener('click', function(e) {
            e.stopPropagation();
        });
    });
}

// 模态框功能
function initModals() {
    const modalTriggers = document.querySelectorAll('[data-toggle="modal"]');
    
    modalTriggers.forEach(trigger => {
        trigger.addEventListener('click', function() {
            const modalId = this.getAttribute('data-target');
            const modal = document.querySelector(modalId);
            
            if (modal) {
                openModal(modal);
            }
        });
    });
    
    // 关闭模态框按钮
    document.querySelectorAll('.close, [data-dismiss="modal"]').forEach(closeBtn => {
        closeBtn.addEventListener('click', function() {
            const modal = this.closest('.modal');
            closeModal(modal);
        });
    });
    
    // 点击模态框外部关闭
    document.querySelectorAll('.modal').forEach(modal => {
        modal.addEventListener('click', function(e) {
            if (e.target === modal) {
                closeModal(modal);
            }
        });
    });
    
    // ESC 键关闭模态框
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') {
            const openModal = document.querySelector('.modal.show');
            if (openModal) {
                closeModal(openModal);
            }
        }
    });
}

// 打开模态框
function openModal(modal) {
    modal.classList.add('show');
    document.body.classList.add('modal-open');
    
    // 防止背景滚动
    document.body.style.overflow = 'hidden';
}

// 关闭模态框
function closeModal(modal) {
    modal.classList.remove('show');
    document.body.classList.remove('modal-open');
    
    // 恢复背景滚动
    document.body.style.overflow = '';
    
    // 清除表单
    const form = modal.querySelector('form');
    if (form) {
        form.reset();
        clearFormErrors(form);
    }
}

// 标签页功能
function initTabs() {
    const tabLinks = document.querySelectorAll('.tab-link');
    
    tabLinks.forEach(link => {
        link.addEventListener('click', function(e) {
            e.preventDefault();
            
            const tabList = this.closest('.tab-list');
            const tabId = this.getAttribute('href');
            const tabPane = document.querySelector(tabId);
            
            // 移除所有激活状态
            tabList.querySelectorAll('.tab-link').forEach(tab => {
                tab.classList.remove('active');
            });
            
            document.querySelectorAll('.tab-pane').forEach(pane => {
                pane.classList.remove('active');
            });
            
            // 添加当前激活状态
            this.classList.add('active');
            tabPane.classList.add('active');
            
            // 触发标签页切换事件
            const tabChangeEvent = new CustomEvent('tabChange', {
                detail: {
                    tabId: tabId,
                    tabElement: tabPane
                }
            });
            document.dispatchEvent(tabChangeEvent);
        });
    });
}

// 数据表格功能
function initDataTables() {
    const tables = document.querySelectorAll('.data-table');
    
    tables.forEach(table => {
        // 表格排序功能
        const headers = table.querySelectorAll('th[data-sortable]');
        headers.forEach(header => {
            header.addEventListener('click', function() {
                const columnIndex = Array.from(header.parentElement.children).indexOf(header);
                const sortDirection = this.getAttribute('data-sort-direction') === 'asc' ? 'desc' : 'asc';
                
                // 更新排序方向属性
                headers.forEach(h => h.removeAttribute('data-sort-direction'));
                this.setAttribute('data-sort-direction', sortDirection);
                
                sortTable(table, columnIndex, sortDirection);
            });
        });
        
        // 行悬停效果
        const rows = table.querySelectorAll('tbody tr');
        rows.forEach(row => {
            row.addEventListener('mouseenter', function() {
                this.classList.add('table-hover');
            });
            
            row.addEventListener('mouseleave', function() {
                this.classList.remove('table-hover');
            });
        });
    });
}

// 表格排序
function sortTable(table, columnIndex, direction) {
    const tbody = table.querySelector('tbody');
    const rows = Array.from(tbody.querySelectorAll('tr'));
    
    rows.sort((a, b) => {
        const aValue = a.cells[columnIndex].textContent.trim();
        const bValue = b.cells[columnIndex].textContent.trim();
        
        // 尝试数字排序
        if (!isNaN(aValue) && !isNaN(bValue)) {
            return direction === 'asc' ? Number(aValue) - Number(bValue) : Number(bValue) - Number(aValue);
        }
        
        // 字符串排序
        return direction === 'asc' ? 
            aValue.localeCompare(bValue) : 
            bValue.localeCompare(aValue);
    });
    
    // 重新插入行
    rows.forEach(row => tbody.appendChild(row));
}

// 表单验证
function initFormValidation() {
    const forms = document.querySelectorAll('form');
    
    forms.forEach(form => {
        form.addEventListener('submit', function(e) {
            if (!validateForm(this)) {
                e.preventDefault();
                scrollToFirstError(this);
            }
        });
        
        // 实时验证
        const inputs = form.querySelectorAll('input, select, textarea');
        inputs.forEach(input => {
            input.addEventListener('blur', function() {
                validateInput(this);
            });
            
            input.addEventListener('input', function() {
                // 清除错误提示
                const errorElement = document.querySelector(`#${this.id}-error`);
                if (errorElement) {
                    errorElement.remove();
                }
                this.classList.remove('is-invalid');
            });
        });
    });
}

// 验证表单
function validateForm(form) {
    let isValid = true;
    const inputs = form.querySelectorAll('input, select, textarea');
    
    inputs.forEach(input => {
        if (!validateInput(input)) {
            isValid = false;
        }
    });
    
    return isValid;
}

// 验证单个输入
function validateInput(input) {
    const required = input.hasAttribute('required');
    const type = input.getAttribute('type');
    const minLength = input.getAttribute('minlength');
    const maxLength = input.getAttribute('maxlength');
    const pattern = input.getAttribute('pattern');
    const value = input.value.trim();
    
    let errorMessage = '';
    
    // 必填验证
    if (required && value === '') {
        errorMessage = '此字段不能为空';
    }
    // 最小长度验证
    else if (minLength && value.length < parseInt(minLength)) {
        errorMessage = `最少需要${minLength}个字符`;
    }
    // 最大长度验证
    else if (maxLength && value.length > parseInt(maxLength)) {
        errorMessage = `最多只能${maxLength}个字符`;
    }
    // 邮箱验证
    else if (type === 'email' && value !== '') {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(value)) {
            errorMessage = '请输入有效的邮箱地址';
        }
    }
    // 手机号码验证（中国）
    else if (type === 'tel' && value !== '') {
        const telRegex = /^1[3-9]\d{9}$/;
        if (!telRegex.test(value)) {
            errorMessage = '请输入有效的手机号码';
        }
    }
    // 数字验证
    else if (type === 'number' && value !== '') {
        if (isNaN(value)) {
            errorMessage = '请输入有效的数字';
        }
    }
    // 自定义正则验证
    else if (pattern && value !== '') {
        const regex = new RegExp(pattern);
        if (!regex.test(value)) {
            errorMessage = input.getAttribute('title') || '输入格式不正确';
        }
    }
    
    // 显示或清除错误信息
    showInputError(input, errorMessage);
    
    return errorMessage === '';
}

// 显示输入错误
function showInputError(input, errorMessage) {
    // 移除旧的错误信息
    const oldError = document.querySelector(`#${input.id}-error`);
    if (oldError) {
        oldError.remove();
    }
    
    input.classList.remove('is-invalid');
    
    if (errorMessage) {
        input.classList.add('is-invalid');
        
        const errorElement = document.createElement('div');
        errorElement.id = `${input.id}-error`;
        errorElement.className = 'invalid-feedback';
        errorElement.textContent = errorMessage;
        
        // 插入到输入框后面
        input.parentNode.appendChild(errorElement);
    }
}

// 清除表单错误
function clearFormErrors(form) {
    const errors = form.querySelectorAll('.invalid-feedback');
    errors.forEach(error => error.remove());
    
    const invalidInputs = form.querySelectorAll('.is-invalid');
    invalidInputs.forEach(input => input.classList.remove('is-invalid'));
}

// 滚动到第一个错误
function scrollToFirstError(form) {
    const firstError = form.querySelector('.is-invalid');
    if (firstError) {
        firstError.scrollIntoView({ 
            behavior: 'smooth', 
            block: 'center' 
        });
        firstError.focus();
    }
}

// 通知系统
const notifications = {
    container: null,
    
    init: function() {
        this.container = document.createElement('div');
        this.container.className = 'notifications-container';
        document.body.appendChild(this.container);
    },
    
    show: function(type, title, message, duration = 5000) {
        if (!this.container) {
            this.init();
        }
        
        const notification = document.createElement('div');
        notification.className = `notification notification-${type}`;
        notification.innerHTML = `
            <div class="notification-content">
                <div class="notification-title">${title}</div>
                <div class="notification-message">${message}</div>
            </div>
            <button class="notification-close" aria-label="关闭">×</button>
        `;
        
        this.container.appendChild(notification);
        
        // 自动移除
        setTimeout(() => {
            this.remove(notification);
        }, duration);
        
        // 关闭按钮
        const closeBtn = notification.querySelector('.notification-close');
        closeBtn.addEventListener('click', () => {
            this.remove(notification);
        });
        
        return notification;
    },
    
    success: function(title, message, duration) {
        return this.show('success', title, message, duration);
    },
    
    error: function(title, message, duration) {
        return this.show('error', title, message, duration);
    },
    
    warning: function(title, message, duration) {
        return this.show('warning', title, message, duration);
    },
    
    info: function(title, message, duration) {
        return this.show('info', title, message, duration);
    },
    
    remove: function(notification) {
        notification.style.opacity = '0';
        notification.style.transform = 'translateX(100%)';
        notification.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
        
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    },
    
    clearAll: function() {
        if (this.container) {
            this.container.innerHTML = '';
        }
    }
};

// 初始化通知系统
function initNotifications() {
    window.notifications = notifications;
    notifications.init();
}

// 移动端响应式
function initMobileResponsive() {
    // 监听窗口大小变化
    window.addEventListener('resize', function() {
        handleResponsiveLayout();
    });
    
    // 初始化时执行一次
    handleResponsiveLayout();
}

// 处理响应式布局
function handleResponsiveLayout() {
    const sidebar = document.querySelector('.sidebar');
    const windowWidth = window.innerWidth;
    
    if (sidebar) {
        // 移动端自动隐藏侧边栏
        if (windowWidth <= 768) {
            sidebar.classList.remove('collapsed');
            sidebar.classList.remove('active');
        } else {
            // 桌面端恢复保存的状态
            const isCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
            sidebar.classList.toggle('collapsed', isCollapsed);
            sidebar.classList.remove('active');
        }
    }
}

// AJAX 请求封装
function ajax(options) {
    const defaults = {
        url: '',
        method: 'GET',
        data: null,
        headers: {
            'Content-Type': 'application/json'
        },
        success: function() {},
        error: function() {},
        complete: function() {}
    };
    
    const settings = { ...defaults, ...options };
    
    // 显示加载状态
    if (settings.showLoading) {
        showLoading();
    }
    
    return fetch(settings.url, {
        method: settings.method,
        headers: settings.headers,
        body: settings.data ? JSON.stringify(settings.data) : null
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }
        return response.json();
    })
    .then(data => {
        settings.success(data);
        return data;
    })
    .catch(error => {
        settings.error(error);
        throw error;
    })
    .finally(() => {
        // 隐藏加载状态
        if (settings.showLoading) {
            hideLoading();
        }
        settings.complete();
    });
}

// 加载指示器
function showLoading() {
    let loadingElement = document.getElementById('global-loading');
    
    if (!loadingElement) {
        loadingElement = document.createElement('div');
        loadingElement.id = 'global-loading';
        loadingElement.className = 'loading-overlay';
        loadingElement.innerHTML = `
            <div class="loading-spinner">
                <div class="loading"></div>
                <div class="loading-text">加载中...</div>
            </div>
        `;
        
        document.body.appendChild(loadingElement);
    }
    
    loadingElement.style.display = 'flex';
    document.body.style.pointerEvents = 'none';
}

function hideLoading() {
    const loadingElement = document.getElementById('global-loading');
    if (loadingElement) {
        loadingElement.style.display = 'none';
    }
    document.body.style.pointerEvents = '';
}

// 工具函数
const utils = {
    // 格式化日期
    formatDate: function(date, format = 'YYYY-MM-DD') {
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        const hours = String(d.getHours()).padStart(2, '0');
        const minutes = String(d.getMinutes()).padStart(2, '0');
        const seconds = String(d.getSeconds()).padStart(2, '0');
        
        return format
            .replace('YYYY', year)
            .replace('MM', month)
            .replace('DD', day)
            .replace('HH', hours)
            .replace('mm', minutes)
            .replace('ss', seconds);
    },
    
    // 获取URL参数
    getUrlParam: function(name) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(name);
    },
    
    // 防抖函数
    debounce: function(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },
    
    // 节流函数
    throttle: function(func, limit) {
        let inThrottle;
        return function(...args) {
            if (!inThrottle) {
                func.apply(this, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    },
    
    // 深拷贝
    deepClone: function(obj) {
        if (obj === null || typeof obj !== 'object') {
            return obj;
        }
        
        if (obj instanceof Date) {
            return new Date(obj.getTime());
        }
        
        if (obj instanceof Array) {
            const cloneArr = [];
            for (let i = 0; i < obj.length; i++) {
                cloneArr[i] = this.deepClone(obj[i]);
            }
            return cloneArr;
        }
        
        const cloneObj = {};
        for (let key in obj) {
            if (obj.hasOwnProperty(key)) {
                cloneObj[key] = this.deepClone(obj[key]);
            }
        }
        
        return cloneObj;
    },
    
    // 数字格式化
    formatNumber: function(num) {
        return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
    },
    
    // 计算两个日期之间的天数
    daysBetween: function(date1, date2) {
        const oneDay = 24 * 60 * 60 * 1000;
        const d1 = new Date(date1);
        const d2 = new Date(date2);
        return Math.round(Math.abs((d1 - d2) / oneDay));
    }
};

// 导出工具函数到全局
window.utils = utils;
window.ajax = ajax;

// 初始化导航功能
function initNavigation() {
    // 高亮当前活动页面的导航项
    highlightActiveNavItem();
    
    // 初始化面包屑导航
    initBreadcrumb();
    
    // 初始化平滑滚动
    initSmoothScroll();
    
    // 保存页面状态
    savePageState();
    
    // 处理导航事件
    handleNavigationEvents();
}

// 高亮当前活动页面的导航项
function highlightActiveNavItem() {
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.nav-link, .sidebar-nav-item');
    
    navLinks.forEach(link => {
        // 获取链接的完整URL
        const linkHref = link.getAttribute('href');
        if (!linkHref || linkHref === '#') return;
        
        // 创建完整的链接URL对象用于比较
        const linkUrl = new URL(linkHref, window.location.origin);
        const linkPath = linkUrl.pathname;
        
        // 检查当前路径是否匹配链接路径
        if (currentPath === linkPath || currentPath.endsWith('/' + linkPath)) {
            link.classList.add('active');
            // 如果是子菜单，展开父菜单
            const parentNav = link.closest('.nav-group');
            if (parentNav) {
                parentNav.classList.add('expanded');
            }
        }
    });
}

// 初始化面包屑导航
function initBreadcrumb() {
    // 获取当前页面信息
    const currentPage = getCurrentPageInfo();
    
    // 找到面包屑容器
    const breadcrumb = document.querySelector('.breadcrumb');
    if (!breadcrumb) return;
    
    // 创建面包屑HTML
    let breadcrumbHTML = '<a href="../../student/index.html" class="breadcrumb-item">首页</a>';
    
    // 如果有父级页面，添加父级页面链接
    if (currentPage.parent) {
        breadcrumbHTML += `<span class="breadcrumb-separator">/</span>
        <a href="${currentPage.parent.url}" class="breadcrumb-item">${currentPage.parent.title}</a>`;
    }
    
    // 添加当前页面
    breadcrumbHTML += `
    <span class="breadcrumb-separator">/</span>
    <span class="breadcrumb-current">${currentPage.title}</span>`;
    
    // 更新面包屑内容
    breadcrumb.innerHTML = breadcrumbHTML;
}

// 获取当前页面信息
function getCurrentPageInfo() {
    const currentPath = window.location.pathname;
    const pageMap = {
        'index.html': { title: '首页', parent: null },
        'my-courses.html': { title: '我的课程', parent: null },
        'course-selection.html': { title: '课程选择', parent: null },
        'schedule.html': { title: '我的课表', parent: null },
        'grades.html': { title: '成绩查询', parent: null },
        'evaluations.html': { title: '课程评价', parent: null },
        'profile.html': { title: '个人信息', parent: null },
        'messages.html': { title: '消息通知', parent: null },
        'settings.html': { title: '设置', parent: null }
    };
    
    // 从路径中提取文件名
    const pathParts = currentPath.split('/');
    const fileName = pathParts[pathParts.length - 1];
    
    // 返回对应的页面信息，如果没有找到则返回默认值
    return pageMap[fileName] || { title: '选课系统', parent: null };
}

// 初始化平滑滚动
function initSmoothScroll() {
    // 为所有内部链接添加平滑滚动
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            const targetId = this.getAttribute('href');
            // 跳过导航项和下拉菜单的点击
            if (this.classList.contains('nav-link') || this.closest('.dropdown')) {
                return;
            }
            
            e.preventDefault();
            
            const targetElement = document.querySelector(targetId);
            if (targetElement) {
                // 平滑滚动到目标元素
                targetElement.scrollIntoView({ 
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
}

// 保存页面状态
function savePageState() {
    // 监听页面卸载事件，保存当前滚动位置
    window.addEventListener('beforeunload', function() {
        const scrollPosition = {
            x: window.scrollX,
            y: window.scrollY
        };
        localStorage.setItem('scrollPosition', JSON.stringify(scrollPosition));
    });
    
    // 尝试恢复滚动位置
    setTimeout(() => {
        const savedPosition = localStorage.getItem('scrollPosition');
        if (savedPosition) {
            try {
                const position = JSON.parse(savedPosition);
                window.scrollTo({ 
                    top: position.y, 
                    left: position.x,
                    behavior: 'auto'
                });
            } catch (e) {
                console.error('恢复滚动位置失败:', e);
            }
        }
    }, 100);
}

// 处理导航事件
function handleNavigationEvents() {
    // 为所有导航链接添加事件处理
    document.querySelectorAll('.nav-link, .sidebar-nav-item').forEach(link => {
        link.addEventListener('click', function(e) {
            // 获取链接地址
            const href = this.getAttribute('href');
            if (!href || href === '#' || href.startsWith('javascript:')) return;
            
            // 检查是否是外部链接
            if (href.startsWith('http://') || href.startsWith('https://') || href.startsWith('../')) {
                return; // 让浏览器默认处理外部链接和相对上一级的链接
            }
            
            // 这里可以添加页面切换前的逻辑，如保存表单数据等
            const hasUnsavedChanges = checkUnsavedChanges();
            if (hasUnsavedChanges && !confirm('您有未保存的更改，确定要离开吗？')) {
                e.preventDefault();
                return;
            }
            
            // 可以添加页面跳转前的动画效果
            // showPageTransition();
        });
    });
}

// 检查是否有未保存的更改
function checkUnsavedChanges() {
    // 检查所有表单是否有修改
    const forms = document.querySelectorAll('form');
    for (const form of forms) {
        // 检查表单是否被修改过
        const wasModified = form.hasAttribute('data-was-modified') && 
                           form.getAttribute('data-was-modified') === 'true';
        if (wasModified) {
            return true;
        }
    }
    return false;
}

// 标记表单为已修改
function markFormAsModified(form) {
    if (form && form.tagName === 'FORM') {
        form.setAttribute('data-was-modified', 'true');
    }
}

// 标记表单为未修改
function markFormAsUnmodified(form) {
    if (form && form.tagName === 'FORM') {
        form.removeAttribute('data-was-modified');
    }
}

// 为所有表单添加修改监听
function watchFormChanges() {
    document.querySelectorAll('form').forEach(form => {
        // 监听表单输入变化
        form.addEventListener('input', function() {
            markFormAsModified(this);
        });
        
        // 监听表单提交
        form.addEventListener('submit', function() {
            markFormAsUnmodified(this);
        });
        
        // 监听重置按钮
        form.addEventListener('reset', function() {
            markFormAsUnmodified(this);
        });
    });
}

// 暴露导航相关的函数到全局
window.navigation = {
    highlightActiveItem: highlightActiveNavItem,
    initBreadcrumb: initBreadcrumb,
    getCurrentPageInfo: getCurrentPageInfo,
    checkUnsavedChanges: checkUnsavedChanges
};

// 数据模拟函数
function mockData(type) {
    const mockDataMap = {
        courses: [
            {
                id: 'C001',
                name: '高等数学',
                teacher: '张教授',
                credit: 5,
                students: 120,
                status: 'ongoing',
                rating: 4.8
            },
            {
                id: 'C002',
                name: '程序设计基础',
                teacher: '李老师',
                credit: 4,
                students: 95,
                status: 'ongoing',
                rating: 4.5
            },
            {
                id: 'C003',
                name: '数据结构',
                teacher: '王教授',
                credit: 4,
                students: 88,
                status: 'ongoing',
                rating: 4.7
            },
            {
                id: 'C004',
                name: '数据库原理',
                teacher: '陈老师',
                credit: 3,
                students: 75,
                status: 'upcoming',
                rating: 4.6
            }
        ],
        students: [
            {
                id: 'S001',
                name: '张三',
                major: '计算机科学',
                grade: '大三',
                status: 'active'
            },
            {
                id: 'S002',
                name: '李四',
                major: '软件工程',
                grade: '大二',
                status: 'active'
            },
            {
                id: 'S003',
                name: '王五',
                major: '数据科学',
                grade: '大四',
                status: 'active'
            },
            {
                id: 'S004',
                name: '赵六',
                major: '人工智能',
                grade: '大一',
                status: 'active'
            }
        ],
        teachers: [
            {
                id: 'T001',
                name: '张教授',
                department: '计算机系',
                title: '教授',
                courses: 5
            },
            {
                id: 'T002',
                name: '李老师',
                department: '计算机系',
                title: '讲师',
                courses: 3
            },
            {
                id: 'T003',
                name: '王教授',
                department: '软件工程系',
                title: '教授',
                courses: 4
            }
        ],
        statistics: {
            totalStudents: 2500,
            totalTeachers: 150,
            totalCourses: 200,
            totalSelections: 8500
        }
    };
    
    return mockDataMap[type] || null;
}

// 自动填充模拟数据
function fillMockData() {
    // 填充统计数据
    const statsElements = document.querySelectorAll('[data-mock-stats]');
    statsElements.forEach(element => {
        const statsType = element.getAttribute('data-mock-stats');
        const stats = mockData('statistics');
        if (stats && stats[statsType]) {
            element.textContent = utils.formatNumber(stats[statsType]);
        }
    });
}

// 导出为全局函数
window.mockData = mockData;
window.fillMockData = fillMockData;

// 加载完成后填充模拟数据
document.addEventListener('DOMContentLoaded', fillMockData);

// 添加全局样式
const style = document.createElement('style');
style.textContent = `
    /* 全局加载样式 */
    .loading-overlay {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background-color: rgba(255, 255, 255, 0.8);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 9999;
    }
    
    .loading-spinner {
        text-align: center;
    }
    
    .loading-text {
        margin-top: 10px;
        color: var(--text-secondary);
        font-size: var(--font-size-sm);
    }
    
    /* 通知容器样式 */
    .notifications-container {
        position: fixed;
        top: 70px;
        right: 20px;
        z-index: 1050;
        display: flex;
        flex-direction: column;
        gap: 10px;
    }
    
    /* 表格悬停效果 */
    .table-hover {
        background-color: var(--gray-100) !important;
    }
    
    /* 表单验证样式 */
    .is-invalid {
        border-color: var(--danger-color) !important;
    }
    
    .invalid-feedback {
        display: block;
        width: 100%;
        margin-top: 0.25rem;
        font-size: 0.875em;
        color: var(--danger-color);
    }
    
    /* 无障碍改进 */
    .focus-visible {
        outline: 2px solid var(--primary-color);
        outline-offset: 2px;
    }
`;
document.head.appendChild(style);