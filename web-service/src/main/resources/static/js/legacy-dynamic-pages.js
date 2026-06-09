(function () {
    const api = window.AppApi;
    if (!api) {
        return;
    }

    const state = {
        current: null,
        classPage: 1,
        coursePage: 1,
        gradePage: 1,
        logPage: 1,
        selectedClassId: null,
        selectedCourseId: null,
        evaluations: []
    };

    document.addEventListener('DOMContentLoaded', () => {
        setTimeout(() => init().catch((error) => notify('error', '动态数据加载失败', error.message)), 0);
    });

    async function init() {
        const path = window.location.pathname;
        if (path.endsWith('/admin/student-management.html')) return renderAdminPeople('student');
        if (path.endsWith('/admin/teacher-management.html')) return renderAdminPeople('teacher');
        if (path.endsWith('/admin/class-management.html')) return initAdminClasses();
        if (path.endsWith('/admin/course-management.html')) return initAdminCourses();
        if (path.endsWith('/admin/grade-management.html')) return initAdminGrades();
        if (path.endsWith('/admin/system-logs.html')) return initAdminLogs();
        if (path.endsWith('/admin/system-settings.html')) return initSystemSettings();
        if (path.endsWith('/student/grades.html')) return initStudentGrades();
        if (path.endsWith('/student/schedule.html')) return initStudentSchedule();
        if (path.endsWith('/student/messages.html')) return initStudentMessages();
        if (path.endsWith('/student/evaluations.html')) return initStudentEvaluations();
        if (path.endsWith('/teacher/teaching-statistics.html')) return initTeacherStatistics();
    }

    async function currentUser() {
        if (!state.current) {
            state.current = await api.get('/login/current');
        }
        return state.current;
    }

    async function renderAdminPeople(role) {
        const tbody = document.querySelector('table.data-table tbody');
        if (!tbody) return;
        const endpoint = role === 'teacher' ? '/api/v1/teachers/list' : '/api/v1/students/list';
        tbody.innerHTML = rowMessage(9, '正在加载数据库数据...');
        const page = await api.get(endpoint, { pageNum: 1, pageSize: 50, orderByColumn: 'id', isAsc: true });
        const rows = api.pageItems(page);
        if (!rows.length) {
            tbody.innerHTML = rowMessage(9, role === 'teacher' ? '暂无教师数据' : '暂无学生数据');
            return;
        }
        tbody.innerHTML = rows.map((item) => role === 'teacher' ? teacherRow(item) : studentRow(item)).join('');
        tbody.querySelectorAll('[data-delete-person]').forEach((button) => {
            button.addEventListener('click', () => deletePerson(role, button.dataset.id));
        });
        tbody.querySelectorAll('[data-reset-person]').forEach((button) => {
            button.addEventListener('click', () => resetPersonPassword(role, button.dataset.id));
        });
    }

    function studentRow(student) {
        return `
            <tr>
                <td>${escape(student.studentNo)}</td>
                <td>${escape(student.name)}</td>
                <td>${escape(student.gender)}</td>
                <td>${escape(student.collegeName || student.collegeId || '-')}</td>
                <td>${escape(student.majorName || student.majorId || '-')}</td>
                <td>${escape(student.className || '-')}</td>
                <td>${badge(Number(student.status) === 1 ? '在读' : '停用', Number(student.status) === 1 ? 'success' : 'warning')}</td>
                <td>${formatDate(student.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-warning" data-reset-person data-id="${student.id}" title="重置密码"><i class="fas fa-key"></i></button>
                    <button class="btn btn-sm btn-danger" data-delete-person data-id="${student.id}" title="删除"><i class="fas fa-trash"></i></button>
                </td>
            </tr>
        `;
    }

    function teacherRow(teacher) {
        return `
            <tr>
                <td>${escape(teacher.teacherNo)}</td>
                <td>${escape(teacher.name)}</td>
                <td>${escape(teacher.gender)}</td>
                <td>${escape(teacher.departmentName || teacher.departmentId || '-')}</td>
                <td>${badge(teacher.title || '教师', 'primary')}</td>
                <td>${escape(teacher.phone || '-')}</td>
                <td>${escape(teacher.email || '-')}</td>
                <td>${formatDate(teacher.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-warning" data-reset-person data-id="${teacher.id}" title="重置密码"><i class="fas fa-key"></i></button>
                    <button class="btn btn-sm btn-danger" data-delete-person data-id="${teacher.id}" title="删除"><i class="fas fa-trash"></i></button>
                </td>
            </tr>
        `;
    }

    async function deletePerson(role, id) {
        if (!confirm('确定删除该记录吗？')) return;
        await api.del(`/api/v1/${role === 'teacher' ? 'teachers' : 'students'}/${encodeURIComponent(id)}`);
        notify('success', '删除成功', '数据库记录已删除');
        renderAdminPeople(role);
    }

    async function resetPersonPassword(role, id) {
        if (!confirm('确定重置密码吗？')) return;
        await api.request(`/api/v1/${role === 'teacher' ? 'teachers' : 'students'}/${encodeURIComponent(id)}/reset-password`, { method: 'PUT' });
        notify('success', '重置成功', '密码已重置为账号后6位');
    }

    async function initAdminClasses() {
        window.searchClasses = () => loadClasses(1);
        window.filterClasses = () => loadClasses(1);
        window.goToPage = (page) => loadClasses(page);
        window.showClassDetail = showClassDetail;
        window.showEditClassModal = showEditClassModal;
        window.showClassStudents = showClassStudents;
        window.showClassCourses = showClassCourses;
        window.deleteClass = deleteClass;
        window.addClass = addClass;
        window.updateClass = updateClass;
        await loadClasses(1);
    }

    async function loadClasses(page) {
        state.classPage = Math.max(1, Number(page || 1));
        const body = document.getElementById('classTableBody');
        if (!body) return;
        body.innerHTML = rowMessage(11, '正在加载班级数据...');
        const data = await api.get('/api/v1/classes/list', {
            pageNum: state.classPage,
            pageSize: 10,
            keyword: value('searchClassInput'),
            grade: filterValue('gradeFilter'),
            status: normalizeClassStatus(value('statusFilter')),
            orderByColumn: 'id',
            isAsc: true
        });
        const rows = api.pageItems(data);
        if (!rows.length) {
            body.innerHTML = rowMessage(11, '暂无班级数据');
            renderPager('classPagination', state.classPage, 1, 'goToPage');
            return;
        }
        body.innerHTML = rows.map((item) => `
            <tr>
                <td><input type="checkbox" class="class-checkbox" value="${item.id}"></td>
                <td>${escape(item.classCode)}</td>
                <td>${escape(item.className)}</td>
                <td>${escape(item.collegeName || item.collegeId)}</td>
                <td>${escape(item.majorName || item.majorId)}</td>
                <td>${escape(item.grade)}</td>
                <td>${escape(item.headTeacherName || '-')}</td>
                <td>${escape(item.studentCount ?? 0)}</td>
                <td>${escape(item.monitorName || '-')}</td>
                <td>${classStatusBadge(item.status)}</td>
                <td>
                    <button class="btn btn-info" onclick="showClassDetail('${item.id}')">详情</button>
                    <button class="btn btn-primary" onclick="showEditClassModal('${item.id}')">编辑</button>
                    <button class="btn btn-danger" onclick="deleteClass('${item.id}')">删除</button>
                </td>
            </tr>
        `).join('');
        renderPager('classPagination', state.classPage, Math.ceil(api.pageTotal(data) / 10), 'goToPage');
    }

    async function addClass() {
        const defaults = await academicDefaults();
        const body = {
            classCode: requiredValue('classId', '请填写班级编号'),
            className: requiredValue('className', '请填写班级名称'),
            collegeId: defaults.collegeId,
            majorId: defaults.majorId,
            grade: value('grade') || '2024',
            contactPhone: value('contactPhone'),
            status: normalizeClassStatus(value('status')) || 1
        };
        await api.post('/api/v1/classes', body);
        closeModal('addClassModal');
        notify('success', '添加成功', '班级已写入数据库');
        await loadClasses(1);
    }

    async function updateClass() {
        const id = state.selectedClassId;
        if (!id) return;
        const old = await api.get(`/api/v1/classes/${id}`);
        const body = {
            classCode: old.classCode,
            className: requiredValue('editClassName', '请填写班级名称'),
            collegeId: old.collegeId,
            majorId: old.majorId,
            grade: value('editGrade') || old.grade,
            contactPhone: value('editContactPhone'),
            status: normalizeClassStatus(value('editStatus')) || old.status
        };
        await api.request(`/api/v1/classes/${id}`, { method: 'PUT', body });
        closeModal('editClassModal');
        notify('success', '更新成功', '班级已更新');
        await loadClasses(state.classPage);
    }

    async function showClassDetail(id) {
        const item = await api.get(`/api/v1/classes/${id}`);
        state.selectedClassId = id;
        setText('detailClassId', item.classCode);
        setText('detailClassName', item.className);
        setText('detailDepartment', item.collegeName || item.collegeId);
        setText('detailMajor', item.majorName || item.majorId);
        setText('detailGrade', item.grade);
        setText('detailHeadTeacher', item.headTeacherName || '-');
        setText('detailStudents', item.studentCount ?? 0);
        setText('detailMonitor', item.monitorName || '-');
        setText('detailContactPhone', item.contactPhone || '-');
        setText('detailCreateTime', formatDate(item.createTime));
        setText('detailStatus', classStatusText(item.status));
        setText('totalStudentsCount', item.studentCount ?? 0);
        setText('currentStudentsCount', item.studentCount ?? 0);
        openModal('classDetailModal');
    }

    async function showEditClassModal(id) {
        const item = await api.get(`/api/v1/classes/${id}`);
        state.selectedClassId = id;
        setValue('editClassId', item.classCode);
        setValue('editClassName', item.className);
        setValue('editGrade', item.grade);
        setValue('editContactPhone', item.contactPhone || '');
        setValue('editStatus', reverseClassStatus(item.status));
        openModal('editClassModal');
    }

    async function deleteClass(id) {
        if (!confirm('确定删除该班级吗？')) return;
        await api.del(`/api/v1/classes/${id}`);
        notify('success', '删除成功', '班级已从数据库删除');
        await loadClasses(state.classPage);
    }

    async function showClassStudents() {
        const id = state.selectedClassId;
        if (!id) return;
        const rows = await api.get(`/api/v1/classes/${id}/students`);
        const body = document.getElementById('classStudentsTable');
        body.innerHTML = rows.length ? rows.map((s) => `
            <tr><td>${escape(s.studentNo)}</td><td>${escape(s.name)}</td><td>${escape(s.gender)}</td><td>${escape(s.phone || '-')}</td><td>${escape(s.email || '-')}</td></tr>
        `).join('') : rowMessage(5, '暂无学生');
        openModal('classStudentsModal');
    }

    async function showClassCourses() {
        const id = state.selectedClassId;
        if (!id) return;
        const rows = await api.get(`/api/v1/classes/${id}/courses`);
        const body = document.getElementById('classCoursesTable');
        body.innerHTML = rows.length ? rows.map((c) => `
            <tr><td>${escape(c.courseCode)}</td><td>${escape(c.courseName)}</td><td>${escape(c.teacherName || '-')}</td><td>${escape(c.credit)}</td><td>${escape(c.schedule || '-')}</td></tr>
        `).join('') : rowMessage(5, '暂无课程');
        openModal('classCoursesModal');
    }

    async function initAdminCourses() {
        window.searchCourses = () => loadAdminCourses(1);
        window.filterCourses = () => loadAdminCourses(1);
        window.goToPage = (page) => loadAdminCourses(page);
        window.addCourse = addAdminCourse;
        window.updateCourse = updateAdminCourse;
        window.deleteCourse = deleteAdminCourse;
        window.showCourseDetail = showAdminCourseDetail;
        window.showEditCourseModal = showEditAdminCourseModal;
        window.showEnrolledStudents = showEnrolledStudents;
        await fillCourseFormOptions();
        await loadAdminCourses(1);
    }

    async function loadAdminCourses(page) {
        state.coursePage = Math.max(1, Number(page || 1));
        const container = document.getElementById('courseListContainer');
        if (!container) return;
        container.innerHTML = '<div class="col-md-12 text-center text-muted">正在加载课程数据...</div>';
        const data = await api.get('/api/v1/courses/list', {
            pageNum: state.coursePage,
            pageSize: 6,
            courseName: value('searchCourseInput'),
            courseType: filterValue('categoryFilter'),
            teacherId: filterValue('teacherFilter'),
            orderByColumn: 'id',
            isAsc: false
        });
        const rows = api.pageItems(data);
        if (!rows.length) {
            container.innerHTML = '<div class="col-md-12 text-center text-muted">暂无课程数据</div>';
            return;
        }
        container.innerHTML = rows.map(courseCard).join('');
        renderPager('coursePagination', state.coursePage, Math.ceil(api.pageTotal(data) / 6), 'goToPage');
    }

    function courseCard(course) {
        const selected = Number(course.selectedCount ?? course.currentStudents ?? 0);
        const capacity = Number(course.availableSlots ?? course.maxCapacity ?? 0);
        const percent = capacity ? Math.min(100, Math.round(selected / capacity * 100)) : 0;
        return `
            <div class="col-md-6 col-lg-4">
                <div class="panel panel-primary course-card">
                    <div class="panel-heading"><h3 class="panel-title">${escape(course.courseName)}</h3></div>
                    <div class="panel-body">
                        <p class="text-muted">${escape(course.courseCode)} | ${escape(course.courseType)} | ${escape(course.credit)}学分</p>
                        <p><strong>教师：</strong>${escape(course.teacherName || course.teacherId || '-')}</p>
                        <p><strong>时间：</strong>${escape(course.schedule || '-')}</p>
                        <p><strong>地点：</strong>${escape(course.classroom || '-')}</p>
                        <div class="capacity-bar"><div class="capacity-progress" style="width:${percent}%"></div></div>
                        <p>${selected}/${capacity || '-'}人 ${courseStatusBadge(course.status)}</p>
                    </div>
                    <div class="panel-footer">
                        <div class="btn-group btn-group-justified">
                            <button class="btn btn-sm btn-info" onclick="showCourseDetail('${course.id}')">详情</button>
                            <button class="btn btn-sm btn-primary" onclick="showEditCourseModal('${course.id}')">编辑</button>
                            <button class="btn btn-sm btn-danger" onclick="deleteCourse('${course.id}')">删除</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    async function addAdminCourse() {
        const body = courseFormBody('');
        await api.post('/api/v1/courses', body);
        closeModal('addCourseModal');
        notify('success', '添加成功', '课程已写入数据库');
        await loadAdminCourses(1);
    }

    async function updateAdminCourse() {
        const id = state.selectedCourseId || value('editCourseId');
        const body = courseFormBody('edit');
        await api.request(`/api/v1/courses/${id}`, { method: 'PUT', body });
        closeModal('editCourseModal');
        notify('success', '更新成功', '课程已更新');
        await loadAdminCourses(state.coursePage);
    }

    async function deleteAdminCourse(id) {
        if (!confirm('确定删除该课程吗？')) return;
        await api.del(`/api/v1/courses/${id}`);
        notify('success', '删除成功', '课程已删除');
        await loadAdminCourses(state.coursePage);
    }

    async function showAdminCourseDetail(id) {
        const course = await api.get(`/api/v1/courses/${id}`);
        state.selectedCourseId = id;
        setText('detailCourseId', course.courseCode);
        setText('detailCourseName', course.courseName);
        setText('detailCourseTeacher', course.teacherName || course.teacherId || '-');
        setText('detailCourseCredits', course.credit);
        setText('detailCourseCategory', course.courseType);
        setText('detailCourseSemester', course.semester || '-');
        setText('detailCourseTime', course.schedule || '-');
        setText('detailCourseLocation', course.classroom || '-');
        setText('detailCourseEnrollment', `${course.selectedCount ?? 0}/${course.availableSlots ?? '-'}`);
        setText('detailCourseStatus', courseStatusText(course.status));
        setText('detailCourseDescription', course.description || '暂无描述');
        openModal('courseDetailModal');
    }

    async function showEditAdminCourseModal(id) {
        const course = await api.get(`/api/v1/courses/${id}`);
        state.selectedCourseId = id;
        setValue('editCourseId', course.id);
        setValue('editCourseName', course.courseName);
        setValue('editCourseTeacher', course.teacherId);
        setValue('editCourseCredits', course.credit);
        setValue('editCourseCategory', course.courseType);
        setValue('editCourseTotal', course.availableSlots);
        setValue('editCourseTime', course.schedule);
        setValue('editCourseLocation', course.classroom);
        setValue('editCourseStatus', course.status);
        setValue('editCourseDescription', course.description || '');
        openModal('editCourseModal');
    }

    async function showEnrolledStudents() {
        if (!state.selectedCourseId) return;
        const data = await api.get(`/api/v1/course-selections/course/${state.selectedCourseId}`, { pageNum: 1, pageSize: 100, status: 1 });
        const body = document.getElementById('enrolledStudentsTable');
        const rows = api.pageItems(data);
        body.innerHTML = rows.length ? rows.map((row) => `
            <tr><td>${escape(row.studentCode || row.studentNo || row.studentId)}</td><td>${escape(row.studentName || '-')}</td><td>${escape(row.className || '-')}</td><td>${formatDate(row.selectionTime)}</td><td>-</td></tr>
        `).join('') : rowMessage(5, '暂无选课学生');
        openModal('enrolledStudentsModal');
    }

    function courseFormBody(prefix) {
        const p = prefix ? prefix + 'Course' : 'course';
        return {
            courseCode: prefix ? undefined : requiredValue('courseId', '请填写课程编号'),
            courseName: requiredValue(`${p}Name`, '请填写课程名称'),
            teacherId: Number(value(`${p}Teacher`) || 1),
            credit: Number(value(`${p}Credits`) || 2),
            totalHours: Number(value(`${p}Credits`) || 2) * 16,
            availableSlots: Number(value(`${p}Total`) || 40),
            courseType: value(`${p}Category`) || '选修课',
            schedule: value(`${p}Time`) || '待安排',
            classroom: value(`${p}Location`) || '待安排',
            status: normalizeCourseStatus(value(`${p}Status`)),
            description: value(`${p}Description`) || ''
        };
    }

    async function initAdminGrades() {
        window.searchGrades = () => loadAdminGrades(1);
        window.filterGrades = () => loadAdminGrades(1);
        window.goToPage = (page) => loadAdminGrades(page);
        window.showGradeDetail = showGradeDetail;
        window.showEditGradeModal = showEditGradeModal;
        window.updateGrade = updateAdminGrade;
        window.deleteGrade = clearAdminGrade;
        await loadAdminGrades(1);
    }

    async function loadAdminGrades(page) {
        state.gradePage = Math.max(1, Number(page || 1));
        const body = document.getElementById('gradeTableBody');
        if (!body) return;
        body.innerHTML = rowMessage(10, '正在加载成绩数据...');
        const data = await api.get('/api/v1/grades/list', {
            pageNum: state.gradePage,
            pageSize: 10,
            keyword: value('searchGradeInput'),
            courseId: filterValue('courseFilter'),
            className: filterValue('classFilter'),
            graded: gradeStatusFilter(value('statusFilter')),
            orderByColumn: 'id',
            isAsc: false
        });
        const rows = api.pageItems(data);
        updateGradeStats(rows, api.pageTotal(data));
        body.innerHTML = rows.length ? rows.map((g) => `
            <tr>
                <td><input type="checkbox" value="${g.selectionId}"></td>
                <td>${escape(g.studentNo)}</td>
                <td>${escape(g.studentName)}</td>
                <td>${escape(g.className || '-')}</td>
                <td>${escape(g.courseName)}</td>
                <td>${escape(g.credit)}</td>
                <td>${escape(g.score ?? '-')}</td>
                <td>${escape(scoreLevel(g.score))}</td>
                <td>${formatDate(g.updatedAt || g.selectionTime)}</td>
                <td>
                    <button class="btn btn-info" onclick="showGradeDetail('${g.selectionId}')">详情</button>
                    <button class="btn btn-primary" onclick="showEditGradeModal('${g.selectionId}')">编辑</button>
                    <button class="btn btn-danger" onclick="deleteGrade('${g.selectionId}')">清空</button>
                </td>
            </tr>
        `).join('') : rowMessage(10, '暂无成绩数据');
        renderPager('gradePagination', state.gradePage, Math.ceil(api.pageTotal(data) / 10), 'goToPage');
    }

    async function showGradeDetail(id) {
        const g = await api.get(`/api/v1/grades/${id}`);
        setText('detailStudentInfo', `${g.studentNo} / ${g.studentName}`);
        setText('detailClass', g.className || '-');
        setText('detailCourseInfo', `${g.courseCode} / ${g.courseName}`);
        setText('detailCredits', g.credit);
        setText('detailSemester', '-');
        setText('detailScore', g.score ?? '未录入');
        setText('detailGradeLevel', scoreLevel(g.score));
        setText('detailInputTime', formatDate(g.updatedAt));
        setText('detailComment', g.remark || '-');
        openModal('gradeDetailModal');
    }

    async function showEditGradeModal(id) {
        const g = await api.get(`/api/v1/grades/${id}`);
        setValue('editGradeId', id);
        setValue('editStudentInfo', `${g.studentNo} / ${g.studentName}`);
        setValue('editCourseInfo', `${g.courseCode} / ${g.courseName}`);
        setValue('editScore', g.score ?? '');
        setValue('editTeacherComment', g.remark || '');
        setValue('editInputTime', formatDate(g.updatedAt));
        openModal('editGradeModal');
    }

    async function updateAdminGrade() {
        const id = value('editGradeId');
        await api.request(`/api/v1/grades/${id}`, {
            method: 'PUT',
            body: { score: value('editScore'), remark: value('editTeacherComment') }
        });
        closeModal('editGradeModal');
        notify('success', '保存成功', '成绩已写入数据库');
        await loadAdminGrades(state.gradePage);
    }

    async function clearAdminGrade(id) {
        if (!confirm('确定清空该成绩吗？')) return;
        await api.del(`/api/v1/grades/${id}`);
        notify('success', '清空成功', '成绩已清空');
        await loadAdminGrades(state.gradePage);
    }

    async function initAdminLogs() {
        window.filterLogs = () => loadLogs(1);
        window.changePage = (page) => loadLogs(page);
        window.showLogDetail = showLogDetail;
        window.clearLogs = () => alert('批量清空日志暂未开放，请在数据库或后台任务中执行。');
        await loadLogs(1);
    }

    async function loadLogs(page) {
        state.logPage = Math.max(1, Number(page || 1));
        const body = document.getElementById('logsTableBody');
        if (!body) return;
        body.innerHTML = rowMessage(8, '正在加载日志数据...');
        const data = await api.get('/api/v1/operation-logs/list', {
            pageNum: state.logPage,
            pageSize: 10,
            keyword: value('logKeyword') || value('logUser'),
            operationType: value('logType'),
            status: logStatusValue(value('logLevel')),
            startDate: value('dateRangeStart'),
            endDate: value('dateRangeEnd')
        });
        const rows = api.pageItems(data);
        body.innerHTML = rows.length ? rows.map((log) => `
            <tr>
                <td>${log.id}</td>
                <td>${formatDateTime(log.operationTime)}</td>
                <td>${logStatusBadge(log.status)}</td>
                <td>${escape(log.operationType)}</td>
                <td>${escape(log.operatorName)}</td>
                <td>${escape(log.ipAddress || '-')}</td>
                <td>${escape(log.operationDesc)}</td>
                <td><button type="button" class="btn btn-xs btn-info" onclick="showLogDetail(${log.id})">详情</button></td>
            </tr>
        `).join('') : rowMessage(8, '暂无日志数据');
        setText('totalLogCount', api.pageTotal(data));
        setText('successLogCount', rows.filter((item) => Number(item.status) === 1).length);
        setText('errorLogCount', rows.filter((item) => Number(item.status) === 0).length);
        setText('warningLogCount', 0);
        renderPager('logsPagination', state.logPage, Math.ceil(api.pageTotal(data) / 10), 'changePage');
    }

    async function showLogDetail(id) {
        const log = await api.get(`/api/v1/operation-logs/${id}`);
        setText('detailLogId', log.id);
        setText('detailLogTime', formatDateTime(log.operationTime));
        setText('detailLogLevel', Number(log.status) === 1 ? '成功' : '失败');
        setText('detailLogType', log.operationType);
        setText('detailLogUser', log.operatorName);
        setText('detailLogIp', log.ipAddress || '-');
        setText('detailLogContent', log.operationDesc);
        setText('detailLogPath', '-');
        setText('detailLogBrowser', '-');
        openModal('logDetailModal');
    }

    async function initSystemSettings() {
        window.saveBasicSettings = saveBasicSettings;
        window.saveSystemSettings = saveSystemSettings;
        window.submitAddSemester = addSemester;
        window.submitEditSemester = updateSemester;
        window.deleteSemester = deleteSemester;
        window.showEditSemesterModal = showEditSemesterModal;
        window.submitAddDepartment = addDepartment;
        window.submitEditDepartment = updateDepartment;
        window.deleteDepartment = deleteDepartment;
        window.showEditDepartmentModal = showEditDepartmentModal;
        window.submitAddMajor = addMajor;
        window.submitEditMajor = updateMajor;
        window.deleteMajor = deleteMajor;
        window.showEditMajorModal = showEditMajorModal;
        await Promise.all([loadSystemSettings(), loadSemesters(), loadDepartmentsAndMajors()]);
    }

    async function loadSystemSettings() {
        const settings = await api.get('/api/v1/system-settings');
        const map = Object.fromEntries(settings.map((item) => [item.settingKey, item.settingValue]));
        setValue('systemName', map.system_name || '网上选课系统');
        setValue('contactEmail', map.contact_email || '');
        setValue('maxCourseSelection', map.max_selected_courses || '8');
        setValue('systemNotice', map.system_notice || '');
    }

    async function saveBasicSettings() {
        await saveSetting('system_name', value('systemName'), '系统名称');
        await saveSetting('contact_email', value('contactEmail'), '联系邮箱');
        notify('success', '保存成功', '基本设置已写入数据库');
    }

    async function saveSystemSettings() {
        await saveSetting('max_selected_courses', value('maxCourseSelection'), '学生最多可选课程数');
        await saveSetting('system_notice', value('systemNotice'), '系统公告');
        notify('success', '保存成功', '系统配置已写入数据库');
    }

    async function saveSetting(key, settingValue, description) {
        await api.request(`/api/v1/system-settings/key/${encodeURIComponent(key)}`, {
            method: 'PUT',
            body: { settingValue, description }
        });
    }

    async function loadSemesters() {
        const data = await api.get('/api/v1/semesters/list', { pageNum: 1, pageSize: 100, orderByColumn: 'startDate', isAsc: false });
        const body = document.getElementById('semesterTableBody');
        if (!body) return;
        body.innerHTML = api.pageItems(data).map((s) => `
            <tr>
                <td>${escape(s.semesterId)}</td>
                <td>${escape(s.semesterName)}</td>
                <td>${formatDate(s.startDate)}</td>
                <td>${formatDate(s.endDate)}</td>
                <td>${Number(s.isCurrent) === 1 ? badge('当前', 'success') : '-'}</td>
                <td>
                    <button class="btn btn-xs btn-primary" onclick="showEditSemesterModal('${s.id}')">编辑</button>
                    <button class="btn btn-xs btn-danger" onclick="deleteSemester('${s.id}')">删除</button>
                </td>
            </tr>
        `).join('');
    }

    async function addSemester() {
        await api.post('/api/v1/semesters', {
            semesterId: requiredValue('semesterId', '请填写学期标识'),
            semesterName: requiredValue('semesterName', '请填写学期名称'),
            startDate: requiredValue('startDate', '请选择开始日期'),
            endDate: requiredValue('endDate', '请选择结束日期'),
            isCurrent: document.getElementById('isCurrentSemester')?.checked,
            status: 1
        });
        closeModal('addSemesterModal');
        await loadSemesters();
    }

    async function showEditSemesterModal(id) {
        const s = await api.get(`/api/v1/semesters/${id}`);
        setValue('editSemesterId', s.id);
        setValue('editSemesterName', s.semesterName);
        setValue('editStartDate', dateInput(s.startDate));
        setValue('editEndDate', dateInput(s.endDate));
        const checkbox = document.getElementById('editIsCurrentSemester');
        if (checkbox) checkbox.checked = Number(s.isCurrent) === 1;
        openModal('editSemesterModal');
    }

    async function updateSemester() {
        const id = value('editSemesterId');
        const old = await api.get(`/api/v1/semesters/${id}`);
        await api.request(`/api/v1/semesters/${id}`, {
            method: 'PUT',
            body: {
                semesterId: old.semesterId,
                semesterName: requiredValue('editSemesterName', '请填写学期名称'),
                startDate: requiredValue('editStartDate', '请选择开始日期'),
                endDate: requiredValue('editEndDate', '请选择结束日期'),
                isCurrent: document.getElementById('editIsCurrentSemester')?.checked,
                status: old.status || 1
            }
        });
        closeModal('editSemesterModal');
        await loadSemesters();
    }

    async function deleteSemester(id) {
        if (!confirm('确定删除该学期吗？')) return;
        await api.del(`/api/v1/semesters/${id}`);
        await loadSemesters();
    }

    async function loadDepartmentsAndMajors() {
        const [departments, majors] = await Promise.all([
            api.get('/api/v1/departments/list', { pageNum: 1, pageSize: 100 }),
            api.get('/api/v1/majors/list', { pageNum: 1, pageSize: 100 })
        ]);
        const collegeBody = document.getElementById('departmentTableBody');
        if (collegeBody) {
            collegeBody.innerHTML = api.pageItems(departments).map((d) => `
                <tr>
                    <td>${escape(d.departmentCode)}</td>
                    <td>${escape(d.departmentName)}</td>
                    <td>${escape(d.collegeId || '-')}</td>
                    <td>${escape(d.description || '-')}</td>
                    <td>${statusBadge(d.status)}</td>
                    <td>
                        <button class="btn btn-xs btn-primary" onclick="showEditDepartmentModal('${d.id}')">编辑</button>
                        <button class="btn btn-xs btn-danger" onclick="deleteDepartment('${d.id}')">删除</button>
                    </td>
                </tr>
            `).join('');
        }
        const options = api.pageItems(departments).map((d) => `<option value="${d.id}">${escape(d.departmentName)}</option>`).join('');
        ['majorDepartment', 'editMajorDepartment'].forEach((id) => {
            const select = document.getElementById(id);
            if (select) select.innerHTML = options;
        });
        const majorBody = document.getElementById('majorTableBody');
        if (majorBody) {
            majorBody.innerHTML = api.pageItems(majors).map((m) => `
                <tr>
                    <td>${escape(m.majorCode)}</td>
                    <td>${escape(m.majorName)}</td>
                    <td>${escape(m.departmentId)}</td>
                    <td>${escape(m.description || '-')}</td>
                    <td>${statusBadge(m.status)}</td>
                    <td>
                        <button class="btn btn-xs btn-primary" onclick="showEditMajorModal('${m.id}')">编辑</button>
                        <button class="btn btn-xs btn-danger" onclick="deleteMajor('${m.id}')">删除</button>
                    </td>
                </tr>
            `).join('');
        }
    }

    async function addDepartment() {
        const collegeId = await defaultCollegeId();
        await api.post('/api/v1/departments', {
            departmentCode: requiredValue('departmentId', '请填写院系编号'),
            departmentName: requiredValue('departmentName', '请填写院系名称'),
            collegeId,
            description: value('departmentDescription'),
            status: 1
        });
        closeModal('addDepartmentModal');
        await loadDepartmentsAndMajors();
    }

    async function showEditDepartmentModal(id) {
        const item = await api.get(`/api/v1/departments/${id}`);
        setValue('editDepartmentId', item.id);
        setValue('editDepartmentName', item.departmentName);
        setValue('editDeanName', item.collegeId || '');
        setValue('editDepartmentDescription', item.description || '');
        openModal('editDepartmentModal');
    }

    async function updateDepartment() {
        const id = value('editDepartmentId');
        const old = await api.get(`/api/v1/departments/${id}`);
        await api.request(`/api/v1/departments/${id}`, {
            method: 'PUT',
            body: {
                departmentCode: old.departmentCode,
                departmentName: requiredValue('editDepartmentName', '请填写院系名称'),
                collegeId: old.collegeId,
                description: value('editDepartmentDescription'),
                status: old.status || 1
            }
        });
        closeModal('editDepartmentModal');
        await loadDepartmentsAndMajors();
    }

    async function deleteDepartment(id) {
        if (!confirm('确定删除该院系吗？')) return;
        await api.del(`/api/v1/departments/${id}`);
        await loadDepartmentsAndMajors();
    }

    async function addMajor() {
        await api.post('/api/v1/majors', {
            majorCode: requiredValue('majorId', '请填写专业编号'),
            majorName: requiredValue('majorName', '请填写专业名称'),
            departmentId: Number(requiredValue('majorDepartment', '请选择所属院系')),
            description: value('majorDescription'),
            status: 1
        });
        closeModal('addMajorModal');
        await loadDepartmentsAndMajors();
    }

    async function showEditMajorModal(id) {
        const item = await api.get(`/api/v1/majors/${id}`);
        setValue('editMajorId', item.id);
        setValue('editMajorName', item.majorName);
        setValue('editMajorDepartment', item.departmentId);
        setValue('editMajorDescription', item.description || '');
        openModal('editMajorModal');
    }

    async function updateMajor() {
        const id = value('editMajorId');
        const old = await api.get(`/api/v1/majors/${id}`);
        await api.request(`/api/v1/majors/${id}`, {
            method: 'PUT',
            body: {
                majorCode: old.majorCode,
                majorName: requiredValue('editMajorName', '请填写专业名称'),
                departmentId: Number(requiredValue('editMajorDepartment', '请选择所属院系')),
                description: value('editMajorDescription'),
                status: old.status || 1
            }
        });
        closeModal('editMajorModal');
        await loadDepartmentsAndMajors();
    }

    async function deleteMajor(id) {
        if (!confirm('确定删除该专业吗？')) return;
        await api.del(`/api/v1/majors/${id}`);
        await loadDepartmentsAndMajors();
    }

    async function defaultCollegeId() {
        const colleges = await api.get('/api/v1/colleges/list', { pageNum: 1, pageSize: 1 });
        return api.pageItems(colleges)[0]?.id || 1;
    }

    async function initStudentGrades() {
        const current = await currentUser();
        const data = await api.get(`/api/v1/course-selections/student/${current.userId}`, { pageNum: 1, pageSize: 100, status: 1 });
        const rows = api.pageItems(data);
        const body = document.querySelector('.grades-table tbody');
        if (!body) return;
        body.innerHTML = rows.length ? rows.map((g) => `
            <tr>
                <td>${escape(g.courseCode || g.courseId)}</td>
                <td>${escape(g.courseName || '-')}</td>
                <td>${escape(g.credit || 0)}</td>
                <td>${escape(g.score ?? '-')}</td>
                <td>${escape(scoreLevel(g.score))}</td>
                <td>${escape(g.remark || '-')}</td>
                <td><button class="btn btn-sm btn-primary" type="button" onclick="alert('总评成绩：${escape(g.score ?? '未录入')}')">详情</button></td>
            </tr>
        `).join('') : rowMessage(7, '暂无成绩数据');
    }

    async function initStudentSchedule() {
        const current = await currentUser();
        const data = await api.get(`/api/v1/course-selections/student/${current.userId}`, { pageNum: 1, pageSize: 100, status: 1 });
        const rows = api.pageItems(data);
        const body = document.querySelector('.schedule-table tbody');
        if (!body) return;
        body.innerHTML = rows.length ? rows.map((c) => `
            <tr>
                <td class="time-label">${escape(c.courseName || '-')}</td>
                <td colspan="5" class="time-slot">
                    <div class="course-block required">
                        <div class="course-block-title">${escape(c.courseName || '-')}</div>
                        <div class="course-block-info">${escape(c.courseCode || c.courseId)} | ${escape(c.credit || 0)}学分 | ${escape(c.schedule || '待安排')} | ${escape(c.classroom || '-')}</div>
                    </div>
                </td>
            </tr>
        `).join('') : '<tr><td colspan="6" class="text-center">暂无课表数据</td></tr>';
    }

    async function initStudentMessages() {
        const current = await currentUser();
        const data = await api.get('/api/v1/messages/list', { pageNum: 1, pageSize: 50, recipientId: current.userId, recipientType: 2 });
        const list = document.querySelector('.messages-list');
        if (!list) return;
        const rows = api.pageItems(data);
        list.innerHTML = rows.length ? rows.map(messageCard).join('') : '<div class="empty-message">暂无消息</div>';
        list.querySelectorAll('[data-read-message]').forEach((button) => {
            button.addEventListener('click', async () => {
                await api.request(`/api/v1/messages/${button.dataset.id}/read`, { method: 'PUT' });
                await initStudentMessages();
            });
        });
        list.querySelectorAll('[data-delete-message]').forEach((button) => {
            button.addEventListener('click', async () => {
                await api.del(`/api/v1/messages/${button.dataset.id}`);
                await initStudentMessages();
            });
        });
    }

    function messageCard(item) {
        return `
            <div class="message-card ${Number(item.isRead) === 0 ? 'unread' : ''}">
                <div class="message-header">
                    <div class="message-info">
                        <span class="message-category ${escape(item.messageType || 'system')}">${escape(messageTypeText(item.messageType))}</span>
                        <h3 class="message-title">${escape(item.title)}</h3>
                    </div>
                    <span class="message-time">${formatDateTime(item.createdAt)}</span>
                </div>
                <p class="message-summary">${escape(item.content)}</p>
                <div class="message-footer">
                    <span class="message-time">系统消息</span>
                    <div class="message-actions-footer">
                        <button class="message-action-btn" data-read-message data-id="${item.id}" aria-label="标为已读"><i class="fas fa-check"></i></button>
                        <button class="message-action-btn" data-delete-message data-id="${item.id}" aria-label="删除"><i class="fas fa-trash"></i></button>
                    </div>
                </div>
            </div>
        `;
    }

    async function initStudentEvaluations() {
        const current = await currentUser();
        state.evaluations = await api.get(`/api/v1/evaluations/student/${current.userId}/courses`);
        renderEvaluationCards();
        const form = document.getElementById('evaluation-form');
        if (form) {
            form.addEventListener('submit', submitEvaluation, true);
        }
    }

    function renderEvaluationCards() {
        const container = document.querySelector('.evaluation-cards');
        if (!container) return;
        container.innerHTML = state.evaluations.length ? state.evaluations.map((item) => `
            <div class="evaluation-card" data-status="${item.evaluated ? 'evaluated' : 'not-evaluated'}">
                <div class="course-info">
                    <h4>${escape(item.courseName)}</h4>
                    <p>${escape(item.courseCode)} | ${escape(item.teacherName || '-')} | ${escape(item.courseType || '-')}</p>
                    <p>${item.evaluated ? `已评分：${escape(item.evaluationScore)} 星，${escape(item.evaluationContent || '')}` : '尚未评价'}</p>
                </div>
                <div class="evaluation-actions">
                    <button class="btn btn-sm btn-primary start-evaluation" data-course-id="${item.courseId}">${item.evaluated ? '修改评价' : '开始评价'}</button>
                </div>
            </div>
        `).join('') : '<div class="empty-message">暂无可评价课程</div>';
        container.querySelectorAll('.start-evaluation').forEach((button) => {
            button.addEventListener('click', () => openEvaluationForm(button.dataset.courseId));
        });
    }

    function openEvaluationForm(courseId) {
        const item = state.evaluations.find((row) => String(row.courseId) === String(courseId));
        if (!item) return;
        setText('current-course-name', item.courseName);
        const form = document.getElementById('evaluation-form');
        if (form) form.dataset.courseId = item.courseId;
        setValue('overall-rating', item.evaluationScore || 0);
        setValue('anonymous-evaluation', item.isAnonymous || 0);
        const container = document.getElementById('evaluation-form-container');
        if (container) container.style.display = 'block';
    }

    async function submitEvaluation(event) {
        event.preventDefault();
        event.stopImmediatePropagation();
        const current = await currentUser();
        const form = event.currentTarget;
        const score = Number(value('overall-rating'));
        if (!score) {
            alert('请至少选择总体评分');
            return;
        }
        await api.post('/api/v1/evaluations', {
            studentId: current.userId,
            courseId: form.dataset.courseId,
            score,
            content: document.querySelector('textarea[name="evaluation-content"]')?.value || '',
            isAnonymous: document.getElementById('anonymous-evaluation')?.checked,
            status: 1
        });
        notify('success', '提交成功', '评价已写入数据库');
        document.getElementById('evaluation-form-container').style.display = 'none';
        await initStudentEvaluations();
    }

    async function initTeacherStatistics() {
        const current = await currentUser();
        const dashboard = await api.get('/api/v1/course-selections/teacher/dashboard', { teacherId: current.userId });
        const cards = document.querySelectorAll('.stats-container .stat-info h3');
        const values = [dashboard.courseCount || 0, dashboard.studentCount || 0, dashboard.gradedCount || 0, dashboard.averageScore ?? '-'];
        cards.forEach((card, index) => card.textContent = values[index]);
        const select = document.getElementById('course-select');
        if (select) {
            select.innerHTML = (dashboard.courses || []).map((c) => `<option value="${c.id}">${escape(c.courseName)}</option>`).join('');
        }
        const tbody = document.querySelector('.data-table tbody');
        if (tbody) {
            tbody.innerHTML = (dashboard.recentSelections || []).map((row) => `
                <tr><td>${escape(row.courseName)}</td><td>${escape(row.studentName)}</td><td>${escape(row.className || '-')}</td><td>${escape(row.score ?? '-')}</td><td>${formatDate(row.selectionTime)}</td></tr>
            `).join('');
        }
    }

    async function fillCourseFormOptions() {
        const teachers = await api.get('/api/v1/teachers/all').catch(() => []);
        ['courseTeacher', 'editCourseTeacher', 'teacherFilter'].forEach((id) => {
            const select = document.getElementById(id);
            if (select && Array.isArray(teachers)) {
                select.innerHTML = '<option value="">请选择教师</option>' + teachers.map((t) => `<option value="${t.id}">${escape(t.name || t.teacherNo)}</option>`).join('');
            }
        });
    }

    async function academicDefaults() {
        const [colleges, majors] = await Promise.all([
            api.get('/api/v1/colleges/list', { pageNum: 1, pageSize: 1 }),
            api.get('/api/v1/majors/list', { pageNum: 1, pageSize: 1 })
        ]);
        return {
            collegeId: api.pageItems(colleges)[0]?.id || 1,
            majorId: api.pageItems(majors)[0]?.id || 1
        };
    }

    function updateGradeStats(rows, total) {
        const scores = rows.map((row) => Number(row.score)).filter((score) => !Number.isNaN(score));
        const average = scores.length ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length * 10) / 10 : 0;
        setText('totalCount', total || rows.length);
        setText('averageScore', average);
        setText('passRate', scores.length ? `${Math.round(scores.filter((score) => score >= 60).length / scores.length * 100)}%` : '0%');
        setText('excellentRate', scores.length ? `${Math.round(scores.filter((score) => score >= 90).length / scores.length * 100)}%` : '0%');
    }

    function renderPager(id, currentPage, totalPages, fnName) {
        const pager = document.getElementById(id);
        if (!pager) return;
        const pages = Math.max(1, Number(totalPages || 1));
        pager.innerHTML = Array.from({ length: pages }, (_, index) => index + 1).map((page) => `
            <li class="${page === currentPage ? 'active' : ''}"><a href="javascript:void(0)" onclick="${fnName}(${page})">${page}</a></li>
        `).join('');
    }

    function rowMessage(colspan, message) {
        return `<tr><td colspan="${colspan}" class="text-center text-muted">${escape(message)}</td></tr>`;
    }

    function badge(text, type) {
        return `<span class="badge badge-${type}">${escape(text)}</span>`;
    }

    function statusBadge(status) {
        return Number(status) === 1 ? badge('启用', 'success') : badge('停用', 'warning');
    }

    function classStatusBadge(status) {
        return badge(classStatusText(status), Number(status) === 1 ? 'success' : Number(status) === 2 ? 'info' : 'danger');
    }

    function classStatusText(status) {
        if (Number(status) === 2) return '已毕业';
        if (Number(status) === 0) return '已关闭';
        return '正常';
    }

    function reverseClassStatus(status) {
        if (Number(status) === 2) return 'graduated';
        if (Number(status) === 0) return 'closed';
        return 'normal';
    }

    function normalizeClassStatus(raw) {
        if (!raw || raw === 'all') return '';
        if (raw === 'graduated') return 2;
        if (raw === 'closed') return 0;
        return 1;
    }

    function courseStatusBadge(status) {
        return Number(status) === 1 ? badge('开放', 'success') : Number(status) === 2 ? badge('结课', 'secondary') : badge('未开课', 'warning');
    }

    function courseStatusText(status) {
        if (Number(status) === 1) return '开放';
        if (Number(status) === 2) return '结课';
        return '未开课';
    }

    function normalizeCourseStatus(raw) {
        if (raw === 'closed' || raw === '0') return 0;
        if (raw === 'ended' || raw === '2') return 2;
        return 1;
    }

    function logStatusBadge(status) {
        return Number(status) === 1 ? '<span class="log-level-success">成功</span>' : '<span class="log-level-error">失败</span>';
    }

    function logStatusValue(level) {
        if (level === 'error') return 0;
        if (level === 'success') return 1;
        return '';
    }

    function scoreLevel(score) {
        const value = Number(score);
        if (Number.isNaN(value)) return '未录入';
        if (value >= 90) return '优秀';
        if (value >= 80) return '良好';
        if (value >= 70) return '中等';
        if (value >= 60) return '及格';
        return '不及格';
    }

    function messageTypeText(type) {
        const map = { system: '系统通知', course: '课程通知', assignment: '作业通知', announcement: '公告通知' };
        return map[type] || '系统通知';
    }

    function value(id) {
        return document.getElementById(id)?.value?.trim() || '';
    }

    function filterValue(id) {
        const result = value(id);
        return !result || result === 'all' ? '' : result;
    }

    function gradeStatusFilter(raw) {
        if (!raw || raw === 'all') return '';
        if (raw === 'graded' || raw === 'completed' || raw === '1') return true;
        if (raw === 'ungraded' || raw === 'pending' || raw === '0') return false;
        return raw;
    }

    function requiredValue(id, message) {
        const result = value(id);
        if (!result) throw new Error(message);
        return result;
    }

    function setValue(id, val) {
        const element = document.getElementById(id);
        if (element) element.value = val == null ? '' : String(val);
    }

    function setText(id, val) {
        const element = document.getElementById(id);
        if (element) element.textContent = val == null ? '' : String(val);
    }

    function openModal(id) {
        const el = document.getElementById(id);
        if (window.jQuery && el) {
            window.jQuery(el).modal('show');
        } else if (el) {
            el.style.display = 'block';
        }
    }

    function closeModal(id) {
        const el = document.getElementById(id);
        if (window.jQuery && el) {
            window.jQuery(el).modal('hide');
        } else if (el) {
            el.style.display = 'none';
        }
    }

    function notify(type, title, message) {
        api.notify(type, title, message);
    }

    function escape(value) {
        return api.escapeHtml(value);
    }

    function formatDate(value) {
        return api.formatDate(value);
    }

    function formatDateTime(value) {
        if (!value) return '-';
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString('zh-CN');
    }

    function dateInput(value) {
        if (!value) return '';
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? String(value).slice(0, 10) : date.toISOString().slice(0, 10);
    }
})();
