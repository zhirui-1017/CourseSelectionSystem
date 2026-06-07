(function () {
    const api = window.AppApi;

    document.addEventListener('DOMContentLoaded', () => {
        initHashTabs();
        loadAdminData();
        bindAdminActions();
    });

    function initHashTabs() {
        const links = document.querySelectorAll('.sidebar-nav .nav-link[href^="#"]');
        const panes = document.querySelectorAll('.tab-pane');

        function activate(hash) {
            const target = document.querySelector(hash || '#dashboard');
            if (!target) {
                return;
            }
            links.forEach((link) => link.classList.toggle('active', link.getAttribute('href') === `#${target.id}`));
            panes.forEach((pane) => pane.classList.toggle('active', pane.id === target.id));
        }

        links.forEach((link) => {
            link.addEventListener('click', (event) => {
                event.preventDefault();
                const hash = link.getAttribute('href');
                history.replaceState(null, '', hash);
                activate(hash);
            });
        });

        activate(window.location.hash || '#dashboard');
    }

    async function loadAdminData() {
        await Promise.allSettled([
            loadStats(),
            loadUsers(),
            loadCourses()
        ]);
    }

    async function loadStats() {
        try {
            const [stats, students, teachers, courses] = await Promise.all([
                api.get('/admin/stats'),
                api.get('/admin/students', { pageNum: 1, pageSize: 1, orderByColumn: 'id', isAsc: true }),
                api.get('/admin/teachers', { pageNum: 1, pageSize: 1, orderByColumn: 'id', isAsc: true }),
                api.get('/admin/adminCourses', { pageNum: 1, pageSize: 1, orderByColumn: 'id', isAsc: true })
            ]);

            const values = document.querySelectorAll('#dashboard .stat-value');
            const totals = [
                students.total ?? 0,
                teachers.total ?? 0,
                courses.total ?? stats.courseCount ?? 0,
                stats.selectionCount ?? 0
            ];
            values.forEach((element, index) => {
                element.textContent = Number(totals[index] || 0).toLocaleString('zh-CN');
            });
        } catch (error) {
            api.notify('error', '统计加载失败', error.message);
        }
    }

    async function loadUsers() {
        const tbody = document.querySelector('#user-management table.data-table tbody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = rowMessage(9, '正在加载用户数据...');
        try {
            const [students, teachers] = await Promise.all([
                api.get('/admin/students', { pageNum: 1, pageSize: 10, orderByColumn: 'id', isAsc: true }),
                api.get('/admin/teachers', { pageNum: 1, pageSize: 10, orderByColumn: 'id', isAsc: true })
            ]);
            const rows = [
                ...api.pageItems(students).map((item) => ({ ...item, role: 'student' })),
                ...api.pageItems(teachers).map((item) => ({ ...item, role: 'teacher' }))
            ];
            renderUsers(rows);
        } catch (error) {
            tbody.innerHTML = rowMessage(9, error.message);
        }
    }

    function renderUsers(rows) {
        const tbody = document.querySelector('#user-management table.data-table tbody');
        if (!rows.length) {
            tbody.innerHTML = rowMessage(9, '暂无用户数据');
            return;
        }
        tbody.innerHTML = rows.map((user) => `
            <tr>
                <td>${api.escapeHtml(user.id)}</td>
                <td>${api.escapeHtml(user.studentNo || user.teacherNo || user.username || '-')}</td>
                <td>${api.escapeHtml(user.name || '-')}</td>
                <td>${badge(user.role === 'student' ? '学生' : '教师', user.role === 'student' ? 'primary' : 'info')}</td>
                <td>${api.escapeHtml(user.collegeId || user.departmentId || '-')}</td>
                <td>${api.escapeHtml(user.majorId || user.title || '-')}</td>
                <td>${statusBadge(user.status)}</td>
                <td>${api.formatDate(user.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-danger js-delete-user" data-role="${user.role}" data-id="${user.id}" title="删除">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    async function loadCourses() {
        const tbody = document.querySelector('#course-management table.data-table tbody');
        if (!tbody) {
            return;
        }
        tbody.innerHTML = rowMessage(9, '正在加载课程数据...');
        try {
            const data = await api.get('/admin/adminCourses', { pageNum: 1, pageSize: 10, orderByColumn: 'id', isAsc: true });
            renderCourses(api.pageItems(data));
        } catch (error) {
            tbody.innerHTML = rowMessage(9, error.message);
        }
    }

    function renderCourses(rows) {
        const tbody = document.querySelector('#course-management table.data-table tbody');
        if (!rows.length) {
            tbody.innerHTML = rowMessage(9, '暂无课程数据');
            return;
        }
        tbody.innerHTML = rows.map((course) => `
            <tr>
                <td>${api.escapeHtml(course.courseCode)}</td>
                <td>${api.escapeHtml(course.courseName)}</td>
                <td>${api.escapeHtml(course.teacherName || course.teacherId || '-')}</td>
                <td>${api.escapeHtml(course.departmentId || '-')}</td>
                <td>${api.escapeHtml(course.credit)}</td>
                <td>${api.escapeHtml(course.availableSlots ?? course.maxCapacity ?? '-')}</td>
                <td>${api.escapeHtml(course.selectedCount ?? course.currentStudents ?? 0)}</td>
                <td>${courseStatusBadge(course.status)}</td>
                <td>
                    <button class="btn btn-sm btn-danger js-delete-course" data-id="${course.id}" title="删除">
                        <i class="fas fa-trash"></i>
                    </button>
                </td>
            </tr>
        `).join('');
    }

    function bindAdminActions() {
        document.getElementById('searchUserBtn')?.addEventListener('click', loadUsers);
        document.getElementById('filterUserBtn')?.addEventListener('click', loadUsers);
        document.getElementById('searchCourseBtn')?.addEventListener('click', loadCourses);
        document.getElementById('filterCourseBtn')?.addEventListener('click', loadCourses);

        document.getElementById('submitAddUser')?.addEventListener('click', addUser, true);
        document.getElementById('submitAddCourse')?.addEventListener('click', addCourse, true);

        document.addEventListener('click', async (event) => {
            const deleteUser = event.target.closest('.js-delete-user');
            if (deleteUser && confirm('确定删除该用户吗？')) {
                await handleDeleteUser(deleteUser.dataset.role, deleteUser.dataset.id);
            }

            const deleteCourse = event.target.closest('.js-delete-course');
            if (deleteCourse && confirm('确定删除该课程吗？')) {
                await handleDeleteCourse(deleteCourse.dataset.id);
            }
        });
    }

    async function addUser(event) {
        event?.preventDefault();
        event?.stopImmediatePropagation();
        const form = document.getElementById('addUserForm');
        if (!form) {
            return;
        }
        const data = Object.fromEntries(new FormData(form).entries());
        const role = data.role || 'student';
        const username = data.username || '';
        const body = {
            username,
            name: data.name,
            email: data.email,
            studentNo: username,
            teacherNo: username,
            gender: '男',
            majorId: 1,
            collegeId: 1,
            departmentId: 1,
            className: '未分班',
            status: 1
        };
        try {
            if (role === 'teacher') {
                await api.post('/admin/addTeacher', body);
            } else {
                await api.post('/admin/addStudent', body);
            }
            window.closeModal?.(document.getElementById('addUserModal'));
            form.reset();
            api.notify('success', '添加成功', '用户已写入数据库');
            await Promise.all([loadUsers(), loadStats()]);
        } catch (error) {
            api.notify('error', '添加失败', error.message);
        }
    }

    async function addCourse(event) {
        event?.preventDefault();
        event?.stopImmediatePropagation();
        const form = document.getElementById('addCourseForm');
        if (!form) {
            return;
        }
        const data = Object.fromEntries(new FormData(form).entries());
        const body = {
            courseCode: data.courseCode,
            courseName: data.courseName,
            teacherId: 1,
            credit: data.credits,
            totalHours: Number(data.credits || 2) * 16,
            availableSlots: data.capacity || 40,
            selectedCount: 0,
            classroom: '待安排',
            schedule: '待安排',
            courseType: '选修课',
            description: data.description || '',
            status: 1
        };
        try {
            await api.post('/admin/addCourse', body);
            window.closeModal?.(document.getElementById('addCourseModal'));
            form.reset();
            api.notify('success', '添加成功', '课程已写入数据库');
            await Promise.all([loadCourses(), loadStats()]);
        } catch (error) {
            api.notify('error', '添加失败', error.message);
        }
    }

    async function handleDeleteUser(role, id) {
        try {
            if (role === 'teacher') {
                await api.post('/admin/deleteTeacher', null, { teacherId: id });
            } else {
                await api.post('/admin/deleteStudent', null, { studentId: id });
            }
            api.notify('success', '删除成功', '用户已删除');
            await Promise.all([loadUsers(), loadStats()]);
        } catch (error) {
            api.notify('error', '删除失败', error.message);
        }
    }

    async function handleDeleteCourse(id) {
        try {
            await api.post('/admin/deleteCourse', null, { courseId: id });
            api.notify('success', '删除成功', '课程已删除');
            await Promise.all([loadCourses(), loadStats()]);
        } catch (error) {
            api.notify('error', '删除失败', error.message);
        }
    }

    function badge(text, type) {
        return `<span class="badge badge-${type}">${api.escapeHtml(text)}</span>`;
    }

    function statusBadge(status) {
        return Number(status) === 1 ? badge('启用', 'success') : badge('停用', 'warning');
    }

    function courseStatusBadge(status) {
        if (Number(status) === 1) {
            return badge('开放', 'success');
        }
        if (Number(status) === 2) {
            return badge('结束', 'secondary');
        }
        return badge('未开放', 'warning');
    }

    function rowMessage(colspan, message) {
        return `<tr><td colspan="${colspan}" class="text-center">${api.escapeHtml(message)}</td></tr>`;
    }
})();
