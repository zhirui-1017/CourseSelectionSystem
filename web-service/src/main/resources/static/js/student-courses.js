(function () {
    const api = window.AppApi;
    let currentUser = null;

    document.addEventListener('DOMContentLoaded', async () => {
        try {
            currentUser = await api.get('/login/current');
        } catch (error) {
            api.notify('error', '未登录', '请先登录学生账号');
            return;
        }

        updateUserChrome(currentUser);

        if (location.pathname.endsWith('/course-selection.html')) {
            await initCourseSelectionPage();
        }
        if (location.pathname.endsWith('/my-courses.html')) {
            await initMyCoursesPage();
        }
    });

    async function initCourseSelectionPage() {
        const list = ensureCourseList();
        list.innerHTML = messageCard('正在加载可选课程...');
        await loadSelectableCourses();
        document.querySelector('.search-button')?.addEventListener('click', loadSelectableCourses);
        document.getElementById('course-search')?.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                loadSelectableCourses();
            }
        });
    }

    async function initMyCoursesPage() {
        const list = ensureCourseList();
        list.innerHTML = messageCard('正在加载我的课程...');
        await loadMyCourses();
        document.querySelector('.filter-btn')?.addEventListener('click', loadMyCourses);
        document.querySelector('.search-btn')?.addEventListener('click', loadMyCourses);
    }

    async function loadSelectableCourses() {
        const list = ensureCourseList();
        const keyword = document.getElementById('course-search')?.value?.trim();
        list.innerHTML = messageCard('正在加载可选课程...');
        try {
            const data = keyword
                ? await api.get('/api/v1/courses/search', { keyword })
                : await api.get('/api/v1/courses/active');
            const courses = api.pageItems(data);
            const selectedIds = await selectedCourseIds();
            if (!courses.length) {
                list.innerHTML = messageCard('暂无可选课程');
                return;
            }
            list.innerHTML = courses.map((course) => renderCourseCard(course, selectedIds.has(Number(course.id)))).join('');
            list.querySelectorAll('.js-select-course').forEach((button) => {
                button.addEventListener('click', () => selectCourse(button.dataset.courseId));
            });
        } catch (error) {
            list.innerHTML = messageCard(error.message);
        }
    }

    async function loadMyCourses() {
        const list = ensureCourseList();
        const studentId = currentUser?.userId;
        const keyword = document.getElementById('my-course-search')?.value?.trim()?.toLowerCase();
        list.innerHTML = messageCard('正在加载我的课程...');
        try {
            const page = await api.get(`/api/v1/course-selections/student/${studentId}`, {
                pageNum: 1,
                pageSize: 50,
                orderByColumn: 'selectionTime',
                isAsc: false,
                status: 1
            });
            let selections = api.pageItems(page);
            if (keyword) {
                selections = selections.filter((item) => String(item.courseName || item.courseCode || '').toLowerCase().includes(keyword));
            }
            if (!selections.length) {
                list.innerHTML = messageCard('暂无已选课程');
                return;
            }
            list.innerHTML = selections.map(renderSelectionCard).join('');
            list.querySelectorAll('.js-drop-course').forEach((button) => {
                button.addEventListener('click', () => dropCourse(button.dataset.selectionId));
            });
        } catch (error) {
            list.innerHTML = messageCard(error.message);
        }
    }

    async function selectedCourseIds() {
        try {
            const page = await api.get(`/api/v1/course-selections/student/${currentUser.userId}`, {
                pageNum: 1,
                pageSize: 100,
                orderByColumn: 'selectionTime',
                isAsc: false,
                status: 1
            });
            return new Set(api.pageItems(page).map((item) => Number(item.courseId)));
        } catch (error) {
            return new Set();
        }
    }

    async function selectCourse(courseId) {
        try {
            await api.post('/api/v1/course-selections', null, {
                studentId: currentUser.userId,
                courseId
            });
            api.notify('success', '选课成功', '课程已加入我的课程');
            await loadSelectableCourses();
        } catch (error) {
            api.notify('error', '选课失败', error.message);
        }
    }

    async function dropCourse(selectionId) {
        if (!confirm('确定退选该课程吗？')) {
            return;
        }
        try {
            await api.del(`/api/v1/course-selections/${selectionId}`, {
                studentId: currentUser.userId
            });
            api.notify('success', '退课成功', '课程已从我的课程移除');
            await loadMyCourses();
        } catch (error) {
            api.notify('error', '退课失败', error.message);
        }
    }

    function renderCourseCard(course, selected) {
        const capacity = Number(course.availableSlots ?? course.maxCapacity ?? 0);
        const selectedCount = Number(course.selectedCount ?? course.currentStudents ?? 0);
        const percent = capacity > 0 ? Math.min(100, Math.round((selectedCount / capacity) * 100)) : 0;
        const full = capacity > 0 && selectedCount >= capacity;
        const disabled = selected || full;
        const buttonText = selected ? '已选' : (full ? '候补' : '选课');
        return `
            <div class="course-card">
                <div class="course-header">
                    <h4 class="course-title">${api.escapeHtml(course.courseName)}</h4>
                    <div class="course-header-right">
                        <span class="course-code">${api.escapeHtml(course.courseCode)}</span>
                        <span class="course-badge ml-2">${api.escapeHtml(course.credit)}学分</span>
                    </div>
                </div>
                <div class="course-body">
                    <div class="course-info">
                        <div class="course-info-item"><i class="course-info-icon fas fa-user"></i><span>教师 ${api.escapeHtml(course.teacherName || course.teacherId || '-')}</span></div>
                        <div class="course-info-item"><i class="course-info-icon fas fa-clock"></i><span>${api.escapeHtml(course.schedule || '-')}</span></div>
                        <div class="course-info-item"><i class="course-info-icon fas fa-map-marker-alt"></i><span>${api.escapeHtml(course.classroom || '-')}</span></div>
                        <div class="course-info-item"><i class="course-info-icon fas fa-tags"></i><span>${api.escapeHtml(course.courseType || '-')}</span></div>
                    </div>
                    <p class="course-description">${api.escapeHtml(course.description || '暂无课程简介')}</p>
                    <div class="course-footer">
                        <div class="course-stats">
                            <div class="course-stat"><i class="fas fa-users"></i><span>已选 ${selectedCount}/${capacity || '-'}</span></div>
                            <div class="course-stat"><div class="capacity-bar"><div class="capacity-fill ${percent >= 95 ? 'high-capacity' : percent >= 75 ? 'medium-capacity' : 'low-capacity'}" style="width: ${percent}%"></div></div></div>
                        </div>
                        <div class="course-actions">
                            <button class="btn ${disabled ? 'btn-secondary' : 'btn-primary'} js-select-course" data-course-id="${course.id}" ${disabled ? 'disabled' : ''}>
                                <i class="fas fa-plus"></i> ${buttonText}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    function renderSelectionCard(selection) {
        return `
            <div class="course-card">
                <div class="course-header">
                    <h4 class="course-title">${api.escapeHtml(selection.courseName || `课程 ${selection.courseId}`)}</h4>
                    <span class="course-code">${api.escapeHtml(selection.courseCode || selection.courseId)}</span>
                </div>
                <div class="course-body">
                    <div class="course-info">
                        <div class="course-info-item"><i class="course-info-icon fas fa-clock"></i><span>选课时间 ${api.formatDate(selection.selectionTime)}</span></div>
                        <div class="course-info-item"><i class="course-info-icon fas fa-star"></i><span>${api.escapeHtml(selection.credit || 0)}学分</span></div>
                        <div class="course-info-item"><i class="course-info-icon fas fa-check-circle"></i><span>${selection.status === 3 ? '候补中' : '已选'}</span></div>
                    </div>
                    <div class="course-footer">
                        <div class="course-stats"></div>
                        <div class="course-actions">
                            <button class="btn btn-danger js-drop-course" data-selection-id="${selection.id}">
                                <i class="fas fa-trash"></i> 退课
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    function ensureCourseList() {
        let list = document.querySelector('.courses-list');
        if (list) {
            return list;
        }
        const main = document.querySelector('.main-content') || document.body;
        const wrapper = document.createElement('div');
        wrapper.className = 'content-wrapper';
        wrapper.innerHTML = '<div class="card"><div class="card-header"><h3 class="card-title">课程列表</h3></div><div class="card-body"><div class="courses-list"></div></div></div>';
        main.appendChild(wrapper);
        return wrapper.querySelector('.courses-list');
    }

    function messageCard(message) {
        return `<div class="course-card"><p class="course-description">${api.escapeHtml(message)}</p></div>`;
    }

    function updateUserChrome(user) {
        document.querySelectorAll('.user-name').forEach((element) => {
            element.textContent = user?.user?.name || user?.username || '学生';
        });
    }
})();
