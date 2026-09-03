(function () {
    const api = window.AppApi;
    if (!api) {
        return;
    }

    const state = {
        teacher: null,
        courses: [],
        selectedCourseId: null,
        students: []
    };

    const page = document.body.dataset.teacherPage || pageName();

    function pageName() {
        const file = window.location.pathname.split('/').pop() || 'index.html';
        if (file === 'course-management.html') {
            return 'courses';
        }
        if (file === 'student-management.html') {
            return 'students';
        }
        if (file === 'grade-management.html') {
            return 'grades';
        }
        return 'dashboard';
    }

    document.addEventListener('DOMContentLoaded', async () => {
        try {
            await boot();
        } catch (error) {
            api.notify('error', '加载失败', error.message || '教师端数据加载失败');
        }
    });

    async function boot() {
        await loadCurrentTeacher();
        applyTeacherProfile();
        markActiveNav();

        if (page === 'dashboard') {
            await renderDashboard();
        } else if (page === 'courses') {
            await renderCoursesPage();
        } else if (page === 'students') {
            await renderStudentsPage();
        } else if (page === 'grades') {
            await renderGradesPage();
        }
    }

    async function loadCurrentTeacher() {
        const current = await api.get('/api/v1/auth/current-user');
        // 合并 localStorage 中的 userInfo 作为补充
        try {
            const stored = JSON.parse(localStorage.getItem('userInfo') || '{}');
            if (stored && Object.keys(stored).length) {
                current.user = stored;
                if (!current.username) current.username = stored.username || stored.studentNo || stored.teacherNo;
            }
        } catch (e) { /* ignore */ }
        if (!current || current.role !== 'teacher') {
            window.location.href = '/login.html';
            return;
        }
        state.teacher = current.user || {
            id: current.userId,
            teacherNo: current.username,
            name: current.username
        };
    }

    async function loadCourses(force) {
        if (!force && state.courses.length) {
            return state.courses;
        }
        const teacherId = state.teacher?.id || state.teacher?.teacherId || state.teacher?.userId;
        if (!teacherId) {
            state.courses = [];
            state.selectedCourseId = null;
            throw new Error('无法读取当前教师ID');
        }
        state.courses = await api.get(`/api/v1/courses/teacher/${encodeURIComponent(teacherId)}`) || [];
        const requestedCourseId = new URLSearchParams(window.location.search).get('courseId');
        const requested = requestedCourseId ? Number(requestedCourseId) : null;
        const firstCourse = state.courses[0] ? Number(state.courses[0].id) : null;
        state.selectedCourseId = requested || state.selectedCourseId || firstCourse;
        return state.courses;
    }

    async function loadStudents(courseId, status) {
        if (!courseId) {
            state.students = [];
            return [];
        }
        const teacherId = currentTeacherId();
        state.students = await api.get(`/api/v1/course-selections/teacher/course/${encodeURIComponent(courseId)}/students`, {
            teacherId,
            status: status === undefined ? 1 : status
        }) || [];
        return state.students;
    }

    async function renderDashboard() {
        const dashboard = await api.get('/api/v1/course-selections/teacher/dashboard', {
            teacherId: currentTeacherId()
        });
        state.courses = dashboard.courses || [];
        renderStats([
            ['我的课程', dashboard.courseCount || 0, 'fa-book-open', '本账号授课课程'],
            ['已选学生', dashboard.studentCount || 0, 'fa-users', '当前有效选课人数'],
            ['已录成绩', dashboard.gradedCount || 0, 'fa-pen-to-square', '已保存总评成绩'],
            ['平均分', dashboard.averageScore ?? '-', 'fa-chart-line', '仅统计已录成绩']
        ]);
        renderDashboardCourses(state.courses);
        renderRecentSelections(dashboard.recentSelections || []);
    }

    async function renderCoursesPage() {
        await loadCourses(true);
        renderCourseFilters();
        renderCourseCards(state.courses);
        bindCourseSearch();
    }

    async function renderStudentsPage() {
        await loadCourses(true);
        renderCourseSelect('course-select', state.selectedCourseId);
        await refreshStudentsTable();
        bindStudentFilters();
    }

    async function renderGradesPage() {
        await loadCourses(true);
        renderCourseSelect('course-select', state.selectedCourseId);
        await refreshGradeTable();
        bindGradeFilters();
        bindBatchSave();
    }

    function applyTeacherProfile() {
        const teacher = state.teacher || {};
        const name = teacher.name || teacher.teacherNo || '教师';
        document.querySelectorAll('[data-teacher-name]').forEach((el) => {
            el.textContent = name;
        });
        document.querySelectorAll('[data-teacher-role]').forEach((el) => {
            el.textContent = teacher.title || '教师';
        });
        document.querySelectorAll('[data-teacher-avatar]').forEach((el) => {
            el.textContent = firstChar(name);
        });
    }

    function markActiveNav() {
        const path = window.location.pathname.split('/').pop() || 'index.html';
        document.querySelectorAll('.sidebar-nav .nav-link').forEach((link) => {
            const href = link.getAttribute('href') || '';
            link.classList.toggle('active', href.endsWith(path));
        });
    }

    function renderStats(items) {
        const container = document.querySelector('[data-teacher-stats]');
        if (!container) {
            return;
        }
        container.innerHTML = items.map(([title, value, icon, hint]) => `
            <div class="stat-card">
                <div class="stat-icon"><i class="fas ${icon}"></i></div>
                <div class="stat-value">${api.escapeHtml(value)}</div>
                <div class="stat-title">${api.escapeHtml(title)}</div>
                <div class="teacher-kpi-subtle">${api.escapeHtml(hint)}</div>
            </div>
        `).join('');
    }

    function renderDashboardCourses(courses) {
        const container = document.querySelector('[data-dashboard-courses]');
        if (!container) {
            return;
        }
        const visibleCourses = courses.slice(0, 6);
        if (!visibleCourses.length) {
            container.innerHTML = empty('暂无授课课程');
            return;
        }
        container.innerHTML = visibleCourses.map(courseCardHtml).join('');
        bindCourseActionButtons(container);
    }

    function renderRecentSelections(rows) {
        const tbody = document.querySelector('[data-recent-students]');
        if (!tbody) {
            return;
        }
        if (!rows.length) {
            tbody.innerHTML = tableEmpty(5, '暂无学生选课记录');
            return;
        }
        tbody.innerHTML = rows.map((row) => `
            <tr>
                <td>${api.escapeHtml(row.courseName)}</td>
                <td>${api.escapeHtml(row.studentNo || row.studentId)}</td>
                <td>${api.escapeHtml(row.studentName)}</td>
                <td>${api.escapeHtml(row.className || '-')}</td>
                <td>${api.formatDate(row.selectionTime)}</td>
            </tr>
        `).join('');
    }

    function renderCourseFilters() {
        const count = document.querySelector('[data-course-count]');
        if (count) {
            count.textContent = state.courses.length;
        }
    }

    function renderCourseCards(courses) {
        const container = document.querySelector('[data-course-list]');
        if (!container) {
            return;
        }
        if (!courses.length) {
            container.innerHTML = empty('暂无授课课程');
            return;
        }
        container.innerHTML = courses.map(courseCardHtml).join('');
        bindCourseActionButtons(container);
    }

    function courseCardHtml(course) {
        const selected = numberValue(course.selectedCount);
        const capacity = numberValue(course.availableSlots);
        const percent = capacity > 0 ? Math.min(100, Math.round((selected / capacity) * 100)) : 0;
        const fillClass = percent >= 95 ? 'danger' : percent >= 75 ? 'warning' : '';
        return `
            <article class="teacher-course-card" data-course-id="${api.escapeHtml(course.id)}">
                <div class="teacher-course-head">
                    <div>
                        <h3 class="teacher-course-title">${api.escapeHtml(course.courseName)}</h3>
                        <div class="teacher-course-code">${api.escapeHtml(course.courseCode)}</div>
                    </div>
                    <span class="badge ${course.status === 1 ? 'badge-success' : 'badge-muted'}">${course.status === 1 ? '开放' : '关闭'}</span>
                </div>
                <div class="teacher-course-meta">
                    <div class="teacher-meta-item"><i class="fas fa-award"></i><span>${api.escapeHtml(course.credit ?? '-')} 学分</span></div>
                    <div class="teacher-meta-item"><i class="fas fa-clock"></i><span>${api.escapeHtml(course.totalHours ?? '-')} 学时</span></div>
                    <div class="teacher-meta-item"><i class="fas fa-location-dot"></i><span>${api.escapeHtml(course.classroom || '-')}</span></div>
                    <div class="teacher-meta-item"><i class="fas fa-calendar-days"></i><span>${api.escapeHtml(course.schedule || '-')}</span></div>
                </div>
                <p class="teacher-course-desc">${api.escapeHtml(course.description || '暂无课程说明')}</p>
                <div class="teacher-progress-line">
                    <div class="teacher-progress"><div class="teacher-progress-fill ${fillClass}" style="width:${percent}%"></div></div>
                    <span class="teacher-table-note">${selected}/${capacity || '-'}</span>
                </div>
                <div class="teacher-actions">
                    <button class="btn btn-secondary btn-sm" type="button" data-action="students" data-course-id="${api.escapeHtml(course.id)}"><i class="fas fa-users"></i>学生</button>
                    <button class="btn btn-primary btn-sm" type="button" data-action="grades" data-course-id="${api.escapeHtml(course.id)}"><i class="fas fa-pen"></i>成绩</button>
                    <button class="btn btn-info btn-sm" type="button" data-action="applyEdit" data-course-id="${api.escapeHtml(course.id)}"><i class="fas fa-edit"></i>编辑</button>
                    <button class="btn btn-danger btn-sm" type="button" data-action="applyDelete" data-course-id="${api.escapeHtml(course.id)}"><i class="fas fa-trash"></i>删除</button>
                </div>
            </article>
        `;
    }

    function bindCourseActionButtons(root) {
        root.querySelectorAll('[data-action="students"]').forEach((button) => {
            button.addEventListener('click', () => {
                window.location.href = `/teacher/student-management.html?courseId=${encodeURIComponent(button.dataset.courseId)}`;
            });
        });
        root.querySelectorAll('[data-action="grades"]').forEach((button) => {
            button.addEventListener('click', () => {
                window.location.href = `/teacher/grade-management.html?courseId=${encodeURIComponent(button.dataset.courseId)}`;
            });
        });
        root.querySelectorAll('[data-action="applyEdit"]').forEach((button) => {
            button.addEventListener('click', () => showApplyEditCourseModal(button.dataset.courseId));
        });
        root.querySelectorAll('[data-action="applyDelete"]').forEach((button) => {
            button.addEventListener('click', () => showApplyDeleteModal(button.dataset.courseId));
        });
    }

    function bindCourseSearch() {
        const searchInput = document.querySelector('[data-course-search]');
        const statusSelect = document.querySelector('[data-course-status]');
        const apply = () => {
            const keyword = (searchInput?.value || '').trim().toLowerCase();
            const status = statusSelect?.value || '';
            const filtered = state.courses.filter((course) => {
                const matchesKeyword = !keyword
                    || String(course.courseName || '').toLowerCase().includes(keyword)
                    || String(course.courseCode || '').toLowerCase().includes(keyword);
                const matchesStatus = !status || String(course.status) === status;
                return matchesKeyword && matchesStatus;
            });
            const count = document.querySelector('[data-course-count]');
            if (count) {
                count.textContent = filtered.length;
            }
            renderCourseCards(filtered);
        };
        document.querySelector('[data-course-filter-button]')?.addEventListener('click', apply);
        searchInput?.addEventListener('input', debounce(apply, 180));
        statusSelect?.addEventListener('change', apply);
    }

    function renderCourseSelect(id, selectedId) {
        const select = document.getElementById(id);
        if (!select) {
            return;
        }
        if (!state.courses.length) {
            select.innerHTML = '<option value="">暂无授课课程</option>';
            return;
        }
        select.innerHTML = state.courses.map((course) => `
            <option value="${api.escapeHtml(course.id)}" ${Number(course.id) === Number(selectedId) ? 'selected' : ''}>
                ${api.escapeHtml(course.courseName)} (${api.escapeHtml(course.courseCode)})
            </option>
        `).join('');
        select.addEventListener('change', async () => {
            state.selectedCourseId = Number(select.value);
            if (page === 'students') {
                await refreshStudentsTable();
            } else if (page === 'grades') {
                await refreshGradeTable();
            }
        });
    }

    async function refreshStudentsTable() {
        const courseId = selectedCourseId();
        await loadStudents(courseId, 1);
        renderStudentSummary();
        renderStudentsTable(filterStudents());
    }

    function bindStudentFilters() {
        const input = document.querySelector('[data-student-search]');
        const button = document.querySelector('[data-student-filter-button]');
        const apply = () => renderStudentsTable(filterStudents());
        input?.addEventListener('input', debounce(apply, 180));
        button?.addEventListener('click', apply);
    }

    function filterStudents() {
        const keyword = (document.querySelector('[data-student-search]')?.value || '').trim().toLowerCase();
        if (!keyword) {
            return state.students;
        }
        return state.students.filter((student) => {
            return String(student.studentName || '').toLowerCase().includes(keyword)
                || String(student.studentNo || '').toLowerCase().includes(keyword)
                || String(student.className || '').toLowerCase().includes(keyword);
        });
    }

    function renderStudentSummary() {
        const count = document.querySelector('[data-student-count]');
        if (count) {
            count.textContent = state.students.length;
        }
        const course = selectedCourse();
        const title = document.querySelector('[data-selected-course-title]');
        if (title) {
            title.textContent = course ? `${course.courseName} 学生名单` : '学生名单';
        }
    }

    function renderStudentsTable(students) {
        const tbody = document.querySelector('[data-student-list]');
        if (!tbody) {
            return;
        }
        if (!students.length) {
            tbody.innerHTML = tableEmpty(9, '暂无学生记录');
            return;
        }
        tbody.innerHTML = students.map((student) => `
            <tr>
                <td>${api.escapeHtml(student.studentNo || student.studentId)}</td>
                <td>
                    <div class="d-flex align-items-center">
                        <div class="user-avatar">${api.escapeHtml(firstChar(student.studentName))}</div>
                        <span class="ml-2">${api.escapeHtml(student.studentName)}</span>
                    </div>
                </td>
                <td>${api.escapeHtml(student.gender || '-')}</td>
                <td>${api.escapeHtml(student.className || '-')}</td>
                <td>${api.escapeHtml(student.phone || '-')}</td>
                <td>${api.escapeHtml(student.email || '-')}</td>
                <td>${api.formatDate(student.selectionTime)}</td>
                <td>${gradeBadge(student.score, student.scoreLevel)}</td>
                <td>
                    <button class="btn btn-primary btn-sm" type="button" data-grade-link="${api.escapeHtml(student.selectionId)}">
                        <i class="fas fa-pen"></i>录入
                    </button>
                </td>
            </tr>
        `).join('');
        tbody.querySelectorAll('[data-grade-link]').forEach((button) => {
            button.addEventListener('click', () => {
                const courseId = selectedCourseId();
                window.location.href = `/teacher/grade-management.html?courseId=${encodeURIComponent(courseId)}&selectionId=${encodeURIComponent(button.dataset.gradeLink)}`;
            });
        });
    }

    async function refreshGradeTable() {
        const courseId = selectedCourseId();
        await loadStudents(courseId, 1);
        renderGradeSummary();
        renderGradeTable(filterGradeRows());
        renderGradeDistribution(state.students);
    }

    function bindGradeFilters() {
        const input = document.querySelector('[data-grade-search]');
        const type = document.querySelector('[data-grade-status]');
        const apply = () => {
            renderGradeTable(filterGradeRows());
            renderGradeDistribution(filterGradeRows());
        };
        input?.addEventListener('input', debounce(apply, 180));
        type?.addEventListener('change', apply);
    }

    function filterGradeRows() {
        const keyword = (document.querySelector('[data-grade-search]')?.value || '').trim().toLowerCase();
        const status = document.querySelector('[data-grade-status]')?.value || '';
        return state.students.filter((student) => {
            const matchesKeyword = !keyword
                || String(student.studentName || '').toLowerCase().includes(keyword)
                || String(student.studentNo || '').toLowerCase().includes(keyword);
            const hasScore = student.score !== null && student.score !== undefined;
            const matchesStatus = !status
                || (status === 'graded' && hasScore)
                || (status === 'ungraded' && !hasScore);
            return matchesKeyword && matchesStatus;
        });
    }

    function renderGradeSummary() {
        const course = selectedCourse();
        const title = document.querySelector('[data-grade-course-title]');
        if (title) {
            title.textContent = course ? `${course.courseName} 成绩录入` : '成绩录入';
        }
        const count = document.querySelector('[data-grade-count]');
        if (count) {
            count.textContent = state.students.length;
        }
    }

    function renderGradeTable(rows) {
        const tbody = document.querySelector('[data-grade-list]');
        if (!tbody) {
            return;
        }
        if (!rows.length) {
            tbody.innerHTML = tableEmpty(8, '暂无成绩记录');
            return;
        }
        tbody.innerHTML = rows.map((row) => gradeRowHtml(row)).join('');
        bindGradeRows(tbody);
        focusRequestedSelection();
    }

    function gradeRowHtml(row) {
        const score = row.score ?? calculateScore(row.dailyGrade, row.labGrade, row.examGrade);
        return `
            <tr data-selection-id="${api.escapeHtml(row.selectionId)}">
                <td>
                    <strong>${api.escapeHtml(row.studentName)}</strong>
                    <div class="teacher-table-note">${api.escapeHtml(row.studentNo || row.studentId)} · ${api.escapeHtml(row.className || '-')}</div>
                </td>
                <td><input class="form-control teacher-grade-input" type="number" min="0" max="100" value="${scoreValue(row.dailyGrade)}" data-grade-field="dailyGrade"></td>
                <td><input class="form-control teacher-grade-input" type="number" min="0" max="100" value="${scoreValue(row.labGrade)}" data-grade-field="labGrade"></td>
                <td><input class="form-control teacher-grade-input" type="number" min="0" max="100" value="${scoreValue(row.examGrade)}" data-grade-field="examGrade"></td>
                <td class="teacher-score-strong" data-final-score>${scoreValue(score)}</td>
                <td data-score-level>${gradeBadge(score, scoreLevel(score))}</td>
                <td><input class="form-control teacher-remark-input" type="text" value="${api.escapeHtml(row.remark || '')}" data-grade-field="remark" placeholder="备注"></td>
                <td>
                    <button class="btn btn-primary btn-sm" type="button" data-save-grade><i class="fas fa-save"></i>保存</button>
                </td>
            </tr>
        `;
    }

    function bindGradeRows(tbody) {
        tbody.querySelectorAll('[data-grade-field]').forEach((input) => {
            input.addEventListener('input', () => {
                const row = input.closest('tr');
                updateFinalScore(row);
            });
        });
        tbody.querySelectorAll('[data-save-grade]').forEach((button) => {
            button.addEventListener('click', async () => {
                await saveGradeRow(button.closest('tr'));
            });
        });
    }

    function focusRequestedSelection() {
        const selectionId = new URLSearchParams(window.location.search).get('selectionId');
        if (!selectionId) {
            return;
        }
        const row = document.querySelector(`tr[data-selection-id="${CSS.escape(selectionId)}"]`);
        if (row) {
            row.scrollIntoView({ block: 'center', behavior: 'smooth' });
            row.style.boxShadow = 'inset 4px 0 0 var(--primary-color)';
        }
    }

    function updateFinalScore(row) {
        const payload = gradePayload(row);
        const score = calculateScore(payload.dailyGrade, payload.labGrade, payload.examGrade);
        row.querySelector('[data-final-score]').textContent = scoreValue(score);
        row.querySelector('[data-score-level]').innerHTML = gradeBadge(score, scoreLevel(score));
    }

    async function saveGradeRow(row) {
        const button = row.querySelector('[data-save-grade]');
        const payload = gradePayload(row);
        const teacherId = currentTeacherId();
        button.disabled = true;
        try {
            const saved = await api.post(
                `/api/v1/course-selections/${encodeURIComponent(payload.selectionId)}/grade`,
                payload,
                { teacherId }
            );
            const index = state.students.findIndex((item) => Number(item.selectionId) === Number(saved.selectionId));
            const merged = index >= 0 ? { ...state.students[index], ...saved } : saved;
            if (index >= 0) {
                state.students[index] = merged;
            }
            api.notify('success', '保存成功', `${merged.studentName || '学生'} 的成绩已保存`);
            renderGradeDistribution(state.students);
        } finally {
            button.disabled = false;
        }
    }

    function bindBatchSave() {
        document.querySelector('[data-batch-save]')?.addEventListener('click', async () => {
            const rows = Array.from(document.querySelectorAll('[data-grade-list] tr[data-selection-id]'));
            for (const row of rows) {
                await saveGradeRow(row);
            }
            api.notify('success', '批量保存完成', `已保存 ${rows.length} 条成绩`);
        });
    }

    function gradePayload(row) {
        const field = (name) => row.querySelector(`[data-grade-field="${name}"]`);
        const dailyGrade = numberOrNull(field('dailyGrade')?.value);
        const labGrade = numberOrNull(field('labGrade')?.value);
        const examGrade = numberOrNull(field('examGrade')?.value);
        const calculatedScore = calculateScore(dailyGrade, labGrade, examGrade);
        const displayedScore = numberOrNull(row.querySelector('[data-final-score]')?.textContent);
        return {
            selectionId: Number(row.dataset.selectionId),
            dailyGrade,
            labGrade,
            examGrade,
            score: calculatedScore ?? displayedScore,
            remark: field('remark')?.value || ''
        };
    }

    function renderGradeDistribution(rows) {
        const container = document.querySelector('[data-grade-distribution]');
        if (!container) {
            return;
        }
        const buckets = [
            ['优秀', 90, 100],
            ['良好', 80, 89.99],
            ['中等', 70, 79.99],
            ['及格', 60, 69.99],
            ['不及格', 0, 59.99]
        ];
        const scores = rows.map((row) => numberOrNull(row.score)).filter((score) => score !== null);
        const total = Math.max(scores.length, 1);
        container.innerHTML = buckets.map(([label, min, max]) => {
            const count = scores.filter((score) => score >= min && score <= max).length;
            const percent = Math.round((count / total) * 100);
            return `
                <div class="teacher-distribution-row">
                    <span>${label}</span>
                    <div class="teacher-distribution-bar"><div class="teacher-distribution-fill" style="width:${percent}%"></div></div>
                    <strong>${count}</strong>
                </div>
            `;
        }).join('');

        const avg = scores.length
            ? Math.round((scores.reduce((sum, score) => sum + score, 0) / scores.length) * 10) / 10
            : '-';
        const avgEl = document.querySelector('[data-grade-average]');
        if (avgEl) {
            avgEl.textContent = avg;
        }
    }

    function selectedCourseId() {
        const select = document.getElementById('course-select');
        if (select?.value) {
            state.selectedCourseId = Number(select.value);
        }
        return state.selectedCourseId;
    }

    function selectedCourse() {
        const id = selectedCourseId();
        return state.courses.find((course) => Number(course.id) === Number(id));
    }

    function currentTeacherId() {
        const teacherId = state.teacher?.id || state.teacher?.teacherId || state.teacher?.userId;
        if (!teacherId) {
            throw new Error('无法读取当前教师ID');
        }
        return teacherId;
    }

    function gradeBadge(score, level) {
        const value = numberOrNull(score);
        const text = level || scoreLevel(value);
        let cls = 'badge-muted';
        if (value !== null) {
            if (value >= 90) {
                cls = 'badge-primary';
            } else if (value >= 80) {
                cls = 'badge-success';
            } else if (value >= 60) {
                cls = 'badge-warning';
            } else {
                cls = 'badge-danger';
            }
        }
        return `<span class="badge ${cls}">${api.escapeHtml(text)}</span>`;
    }

    function calculateScore(dailyGrade, labGrade, examGrade) {
        const hasAny = [dailyGrade, labGrade, examGrade].some((value) => value !== null && value !== undefined);
        if (!hasAny) {
            return null;
        }
        const daily = numberOrNull(dailyGrade) ?? 0;
        const lab = numberOrNull(labGrade) ?? 0;
        const exam = numberOrNull(examGrade) ?? 0;
        return Math.round((daily * 0.4 + lab * 0.2 + exam * 0.4) * 10) / 10;
    }

    function scoreLevel(score) {
        const value = numberOrNull(score);
        if (value === null) {
            return '未录入';
        }
        if (value >= 90) {
            return '优秀';
        }
        if (value >= 80) {
            return '良好';
        }
        if (value >= 70) {
            return '中等';
        }
        if (value >= 60) {
            return '及格';
        }
        return '不及格';
    }

    function scoreValue(value) {
        const number = numberOrNull(value);
        return number === null ? '' : String(number);
    }

    function numberOrNull(value) {
        if (value === null || value === undefined || value === '') {
            return null;
        }
        const number = Number(value);
        return Number.isFinite(number) ? number : null;
    }

    function numberValue(value) {
        const number = numberOrNull(value);
        return number === null ? 0 : number;
    }

    function firstChar(value) {
        return String(value || '师').trim().charAt(0) || '师';
    }

    function empty(message) {
        return `<div class="teacher-empty">${api.escapeHtml(message)}</div>`;
    }

    function tableEmpty(colspan, message) {
        return `<tr><td colspan="${colspan}" class="text-center text-secondary">${api.escapeHtml(message)}</td></tr>`;
    }

    function debounce(fn, wait) {
        let timer;
        return (...args) => {
            window.clearTimeout(timer);
            timer = window.setTimeout(() => fn(...args), wait);
        };
    }

    // ===== 课程申请（教师提交新增/编辑/删除，由管理员审批） =====
    function currentApplicant() {
        return {
            id: currentTeacherId(),
            name: state.teacher?.name || state.teacher?.teacherNo || state.teacher?.username || '教师'
        };
    }

    function showApplyAddCourseModal() {
        const form = document.getElementById('applyCourseForm');
        if (form) form.reset();
        document.getElementById('applyType').value = 'add';
        document.getElementById('applyCourseId').value = '';
        document.getElementById('applyCourseCode').value = '';
        document.getElementById('applyCourseCode').disabled = false;
        const hint = document.getElementById('applyModalHint');
        if (hint) hint.textContent = '提交后需管理员审批，通过后课程才会开放可选。';
        openModal(document.getElementById('applyCourseModal'));
    }

    function showApplyEditCourseModal(courseId) {
        const course = state.courses.find((c) => Number(c.id) === Number(courseId));
        if (!course) return;
        document.getElementById('applyType').value = 'edit';
        document.getElementById('applyCourseId').value = course.id;
        document.getElementById('applyCourseCode').value = course.courseCode || '';
        document.getElementById('applyCourseCode').disabled = true;
        document.getElementById('applyCourseName').value = course.courseName || '';
        document.getElementById('applyCourseCredits').value = course.credit ?? '';
        document.getElementById('applyCourseCategory').value = course.courseType || '选修课';
        document.getElementById('applyCourseTotal').value = course.availableSlots || 40;
        document.getElementById('applyCourseSemester').value = course.semester || '2025-2026-1';
        document.getElementById('applyCourseTime').value = course.schedule || '';
        document.getElementById('applyCourseLocation').value = course.classroom || '';
        document.getElementById('applyCourseStatus').value = course.status === 0 ? 'closed' : 'normal';
        document.getElementById('applyCourseDescription').value = course.description || '';
        const hint = document.getElementById('applyModalHint');
        if (hint) hint.textContent = '修改课程内容不影响学生选课；只有改为"关闭"状态才影响选课。';
        openModal(document.getElementById('applyCourseModal'));
    }

    function showApplyDeleteModal(courseId) {
        document.getElementById('applyDeleteId').value = courseId;
        const course = state.courses.find((c) => Number(c.id) === Number(courseId));
        document.getElementById('applyDeleteName').textContent = course ? (course.courseName || course.courseCode || '') : '';
        document.getElementById('applyDeleteReason').value = '';
        openModal(document.getElementById('applyDeleteModal'));
    }

    async function submitCourseApplication() {
        const type = document.getElementById('applyType').value;
        const payload = {
            courseCode: document.getElementById('applyCourseCode').value.trim(),
            courseName: document.getElementById('applyCourseName').value.trim(),
            courseType: document.getElementById('applyCourseCategory').value,
            credit: parseFloat(document.getElementById('applyCourseCredits').value) || 0,
            totalHours: (parseFloat(document.getElementById('applyCourseCredits').value) || 2) * 16,
            teacherId: currentTeacherId(),
            availableSlots: parseInt(document.getElementById('applyCourseTotal').value) || 40,
            semester: document.getElementById('applyCourseSemester').value,
            schedule: document.getElementById('applyCourseTime').value.trim(),
            classroom: document.getElementById('applyCourseLocation').value.trim(),
            description: document.getElementById('applyCourseDescription').value.trim(),
            status: document.getElementById('applyCourseStatus').value === 'closed' ? 0 : 1
        };
        if (!payload.courseName) { api.notify('warning', '提示', '请填写课程名称'); return; }
        if (type === 'add' && !payload.courseCode) { api.notify('warning', '提示', '请填写课程编号'); return; }
        const body = {
            applicantId: currentApplicant().id,
            applicantName: currentApplicant().name,
            type: type,
            courseId: type === 'edit' ? document.getElementById('applyCourseId').value : null,
            courseCode: payload.courseCode,
            courseName: payload.courseName,
            reason: type === 'add' ? '申请新增课程' : '申请修改课程信息',
            payload: JSON.stringify(payload)
        };
        try {
            await api.post('/api/v1/course-applications', body);
            closeModal(document.getElementById('applyCourseModal'));
            api.notify('success', '申请已提交', '请等待管理员审批');
            loadMyApplications(1);
        } catch (e) { api.notify('error', '提交失败', e.message); }
    }

    async function submitDeleteApplication() {
        const id = document.getElementById('applyDeleteId').value;
        const reason = document.getElementById('applyDeleteReason').value.trim();
        if (!reason) { api.notify('warning', '提示', '请填写删除理由'); return; }
        const body = {
            applicantId: currentApplicant().id,
            applicantName: currentApplicant().name,
            type: 'delete',
            courseId: Number(id),
            reason: reason
        };
        try {
            await api.post('/api/v1/course-applications', body);
            closeModal(document.getElementById('applyDeleteModal'));
            api.notify('success', '申请已提交', '请等待管理员审批');
            loadMyApplications(1);
        } catch (e) { api.notify('error', '提交失败', e.message); }
    }

    function showMyApplications() {
        openModal(document.getElementById('myApplicationsModal'));
        loadMyApplications(1);
    }

    async function loadMyApplications(page) {
        const body = document.getElementById('myApplicationTableBody');
        if (!body) return;
        body.innerHTML = '<tr><td colspan="5" class="text-center text-secondary"><span class="loading"></span></td></tr>';
        try {
            const data = await api.get(`/api/v1/course-applications/teacher/${encodeURIComponent(currentTeacherId())}`, { pageNum: page || 1, pageSize: 10 });
            const rows = api.pageItems(data);
            if (!rows || !rows.length) { body.innerHTML = tableEmpty(5, '暂无申请记录'); return; }
            const typeMap = { add: '新增课程', edit: '编辑课程', delete: '删除课程' };
            const statusMap = { 0: '<span class="badge badge-warning">待审批</span>', 1: '<span class="badge badge-success">已通过</span>', 2: '<span class="badge badge-danger">已驳回</span>' };
            body.innerHTML = rows.map((a) => `
                <tr>
                    <td>${api.escapeHtml(typeMap[a.type] || a.type)}</td>
                    <td>${api.escapeHtml(a.courseName || a.courseCode || '-')}</td>
                    <td style="max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">${api.escapeHtml(a.reason || '-')}</td>
                    <td>${statusMap[a.status] || api.escapeHtml(a.status)}</td>
                    <td>${api.formatDate(a.createTime)}</td>
                </tr>
            `).join('');
        } catch (e) { body.innerHTML = tableEmpty(5, '加载失败'); }
    }

    window.showApplyAddCourseModal = showApplyAddCourseModal;
    window.showApplyEditCourseModal = showApplyEditCourseModal;
    window.showApplyDeleteModal = showApplyDeleteModal;
    window.submitCourseApplication = submitCourseApplication;
    window.submitDeleteApplication = submitDeleteApplication;
    window.showMyApplications = showMyApplications;
    window.loadMyApplications = loadMyApplications;
})();
