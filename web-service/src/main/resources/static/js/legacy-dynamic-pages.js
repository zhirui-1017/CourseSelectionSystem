(function () {
    const api = window.AppApi;
    if (!api) {
        document.documentElement.dataset.legacyDynamicPages = 'waiting-app-api';
        window.addEventListener('load', () => {
            if (!window.AppApi || window.__legacyDynamicPagesRetrying) return;
            window.__legacyDynamicPagesRetrying = true;
            const script = document.createElement('script');
            script.src = `/js/legacy-dynamic-pages.js?retry=${Date.now()}`;
            document.head.appendChild(script);
        }, { once: true });
        return;
    }
    if (window.__legacyDynamicPagesBooted) {
        return;
    }
    window.__legacyDynamicPagesBooted = true;
    document.documentElement.dataset.legacyDynamicPages = 'booted';

    const state = {
        current: null,
        classPage: 1,
        coursePage: 1,
        gradePage: 1,
        logPage: 1,
        peoplePage: { student: 1, teacher: 1 },
        selectedClassId: null,
        selectedCourseId: null,
        selectedPeople: { student: null, teacher: null },
        peopleOptionsLoaded: { student: false, teacher: false },
        gradeOptionsLoaded: false,
        messagePage: 1,
        messageFilter: { type: 'all', read: '', keyword: '', deleted: false },
        teacherDashboard: null,
        teacherCourseSelections: {},
        teacherCourseEvaluations: {},
        teacherScoreChartType: 'score-distribution',
        teacherChartRetryCount: 0,
        studentGrades: [],
        scheduleWeekOffset: 0,
        evaluations: []
    };

    document.addEventListener('DOMContentLoaded', () => {
        setTimeout(() => init().catch((error) => notify('error', '动态数据加载失败', error.message)), 0);
    });

    async function init() {
        const path = window.location.pathname;
        document.documentElement.dataset.legacyDynamicPage = path;
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

    async function renderAdminPeople(role, pageNum) {
        state.peoplePage[role] = Math.max(1, Number(pageNum || state.peoplePage[role] || 1));
        initAdminPeopleControls(role);
        await fillPeopleFormOptions(role);

        const tbody = document.querySelector('table.data-table tbody');
        if (!tbody) return;

        const pageSize = 20;
        const endpoint = role === 'teacher' ? '/api/v1/teachers/list' : '/api/v1/students/list';
        tbody.innerHTML = rowMessage(9, '\u6b63\u5728\u52a0\u8f7d\u6570\u636e\u5e93\u6570\u636e...');

        const page = await api.get(endpoint, {
            ...peopleListParams(role),
            pageNum: state.peoplePage[role],
            pageSize,
            orderByColumn: 'id',
            sortOrder: 'desc'
        });
        const rows = api.pageItems(page);
        if (!rows.length) {
            tbody.innerHTML = rowMessage(9, role === 'teacher' ? '\u6682\u65e0\u6559\u5e08\u6570\u636e' : '\u6682\u65e0\u5b66\u751f\u6570\u636e');
            renderPeoplePager(role, 1);
            return;
        }

        tbody.innerHTML = rows.map((item) => role === 'teacher' ? teacherRow(item) : studentRow(item)).join('');
        bindPeopleRowActions(tbody, role);
        renderPeoplePager(role, Math.ceil(api.pageTotal(page) / pageSize));
    }

    function initAdminPeopleControls(role) {
        const prefix = role === 'teacher' ? 'Teacher' : 'Student';
        const lower = role === 'teacher' ? 'teacher' : 'student';
        bindPeopleAction(`search${prefix}Btn`, () => renderAdminPeople(role, 1));
        bindPeopleAction(`filter${prefix}Btn`, () => renderAdminPeople(role, 1));
        bindPeopleAction(`submitAdd${prefix}`, () => addPerson(role));
        bindPeopleAction(`submitEdit${prefix}`, () => updatePerson(role));
        bindPeopleAction(`export${prefix}sBtn`, () => exportPeople(role));
        bindPeopleAction(`confirmImport${prefix}s`, () => {
            notify('error', '\u6279\u91cf\u5bfc\u5165\u672a\u63a5\u5165', '\u8bf7\u5148\u4f7f\u7528\u65b0\u589e\u3001\u7f16\u8f91\u548c\u5220\u9664\u5b8c\u6210\u771f\u5b9e CRUD');
        });

        const search = document.getElementById(`${lower}Search`);
        if (search && !search.dataset.dynamicPeopleSearch) {
            search.dataset.dynamicPeopleSearch = 'true';
            search.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    renderAdminPeople(role, 1);
                }
            });
        }

        const addForm = document.getElementById(`add${prefix}Form`);
        if (addForm && !addForm.dataset.dynamicPeopleSubmit) {
            addForm.dataset.dynamicPeopleSubmit = 'true';
            addForm.addEventListener('submit', (event) => {
                event.preventDefault();
                addPerson(role);
            });
        }

        const editForm = document.getElementById(`edit${prefix}Form`);
        if (editForm && !editForm.dataset.dynamicPeopleSubmit) {
            editForm.dataset.dynamicPeopleSubmit = 'true';
            editForm.addEventListener('submit', (event) => {
                event.preventDefault();
                updatePerson(role);
            });
        }
    }

    function bindPeopleAction(id, handler) {
        const button = document.getElementById(id);
        if (!button || button.dataset.dynamicPeopleBound) return;
        button.dataset.dynamicPeopleBound = 'true';
        button.addEventListener('click', async (event) => {
            event.preventDefault();
            event.stopImmediatePropagation();
            try {
                await handler(event);
            } catch (error) {
                notify('error', '\u64cd\u4f5c\u5931\u8d25', error.message);
            }
        }, true);
    }

    function peopleListParams(role) {
        const isTeacher = role === 'teacher';
        const search = value(isTeacher ? 'teacherSearch' : 'studentSearch');
        const params = {};
        if (search) {
            const numberLike = /^[A-Za-z]?\d+$/.test(search);
            params.searchField = isTeacher ? (numberLike ? 'teacherNo' : 'name') : (numberLike ? 'studentNo' : 'name');
            params.searchValue = search;
        }
        if (isTeacher) {
            const departmentId = numericValue('teacherCollegeFilter');
            if (departmentId) params.departmentId = departmentId;
            const title = titleText(value('teacherTitleFilter'));
            if (title) params.title = title;
        } else {
            const collegeId = numericValue('studentCollegeFilter');
            if (collegeId) params.collegeId = collegeId;
            const status = normalizePersonStatus(value('studentStatusFilter'));
            if (status !== '') params.status = status;
        }
        return params;
    }

    function bindPeopleRowActions(tbody, role) {
        tbody.querySelectorAll('[data-edit-person]').forEach((button) => {
            button.addEventListener('click', () => openPersonEdit(role, button.dataset.id));
        });
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
                <td>${badge(peopleStatusText(student.status), peopleStatusBadge(student.status))}</td>
                <td>${formatDate(student.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-primary" data-edit-person data-id="${student.id}" title="\u7f16\u8f91"><i class="fas fa-edit"></i></button>
                    <button class="btn btn-sm btn-warning" data-reset-person data-id="${student.id}" title="\u91cd\u7f6e\u5bc6\u7801"><i class="fas fa-key"></i></button>
                    <button class="btn btn-sm btn-danger" data-delete-person data-id="${student.id}" title="\u5220\u9664"><i class="fas fa-trash"></i></button>
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
                <td>${badge(teacher.title || '\u6559\u5e08', 'primary')}</td>
                <td>${escape(teacher.phone || '-')}</td>
                <td>${escape(teacher.email || '-')}</td>
                <td>${formatDate(teacher.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-primary" data-edit-person data-id="${teacher.id}" title="\u7f16\u8f91"><i class="fas fa-edit"></i></button>
                    <button class="btn btn-sm btn-warning" data-reset-person data-id="${teacher.id}" title="\u91cd\u7f6e\u5bc6\u7801"><i class="fas fa-key"></i></button>
                    <button class="btn btn-sm btn-danger" data-delete-person data-id="${teacher.id}" title="\u5220\u9664"><i class="fas fa-trash"></i></button>
                </td>
            </tr>
        `;
    }

    async function addPerson(role) {
        const body = role === 'teacher' ? teacherFormBody('') : studentFormBody('');
        await api.post(`/api/v1/${role === 'teacher' ? 'teachers' : 'students'}/from-map`, body);
        closeModal(role === 'teacher' ? 'addTeacherModal' : 'addStudentModal');
        resetForm(role === 'teacher' ? 'addTeacherForm' : 'addStudentForm');
        notify('success', '\u65b0\u589e\u6210\u529f', '\u8bb0\u5f55\u5df2\u5199\u5165\u6570\u636e\u5e93');
        await renderAdminPeople(role, 1);
    }

    async function openPersonEdit(role, id) {
        const isTeacher = role === 'teacher';
        const item = await api.get(`/api/v1/${isTeacher ? 'teachers' : 'students'}/${encodeURIComponent(id)}`);
        state.selectedPeople[role] = id;
        if (isTeacher) {
            setValue('editTeacherId', id);
            setValue('editTeacherName', item.name);
            setValue('editTeacherGender', reverseGender(item.gender));
            setValue('editTeacherCollege', item.departmentId);
            setValue('editTeacherTitle', reverseTeacherTitle(item.title));
            setValue('editTeacherPhone', item.phone || '');
            setValue('editTeacherEmail', item.email || '');
            setValue('editHireDate', dateInput(item.createdAt));
            openModal('editTeacherModal');
        } else {
            setValue('editStudentId', id);
            setValue('editStudentName', item.name);
            setValue('editStudentGender', reverseGender(item.gender));
            setValue('editStudentCollege', item.collegeId);
            await populateMajorOptions('editStudentMajor', item.collegeId, item.majorId);
            await populateClassOptions('editStudentClass', item.majorId, item.className);
            setValue('editStudentStatus', reversePersonStatus(item.status));
            setValue('editStudentEmail', item.email || '');
            setValue('editStudentPhone', item.phone || '');
            openModal('editStudentModal');
        }
    }

    async function updatePerson(role) {
        const id = state.selectedPeople[role] || value(role === 'teacher' ? 'editTeacherId' : 'editStudentId');
        if (!id) throw new Error('\u8bf7\u5148\u9009\u62e9\u8981\u7f16\u8f91\u7684\u8bb0\u5f55');
        const body = role === 'teacher' ? teacherFormBody('edit') : studentFormBody('edit');
        await api.request(`/api/v1/${role === 'teacher' ? 'teachers' : 'students'}/${encodeURIComponent(id)}/from-map`, { method: 'PUT', body });
        closeModal(role === 'teacher' ? 'editTeacherModal' : 'editStudentModal');
        notify('success', '\u66f4\u65b0\u6210\u529f', '\u8bb0\u5f55\u5df2\u4fdd\u5b58\u5230\u6570\u636e\u5e93');
        await renderAdminPeople(role);
    }

    async function deletePerson(role, id) {
        if (!confirm('\u786e\u5b9a\u5220\u9664\u8be5\u8bb0\u5f55\u5417\uff1f')) return;
        await api.del(`/api/v1/${role === 'teacher' ? 'teachers' : 'students'}/${encodeURIComponent(id)}`);
        notify('success', '\u5220\u9664\u6210\u529f', '\u6570\u636e\u5e93\u8bb0\u5f55\u5df2\u5220\u9664');
        await renderAdminPeople(role);
    }

    async function resetPersonPassword(role, id) {
        if (!confirm('\u786e\u5b9a\u91cd\u7f6e\u5bc6\u7801\u5417\uff1f')) return;
        await api.request(`/api/v1/${role === 'teacher' ? 'teachers' : 'students'}/${encodeURIComponent(id)}/reset-password`, { method: 'PUT' });
        notify('success', '\u91cd\u7f6e\u6210\u529f', '\u5bc6\u7801\u5df2\u6309\u8d26\u53f7\u89c4\u5219\u91cd\u7f6e');
    }

    function studentFormBody(prefix) {
        const p = prefix ? 'editStudent' : 'student';
        const body = {
            name: requiredValue(`${p}Name`, '\u8bf7\u586b\u5199\u59d3\u540d'),
            gender: normalizeGender(value(`${p}Gender`)),
            collegeId: numericValue(`${p}College`) || 1,
            majorId: numericValue(`${p}Major`) || 1,
            className: value(`${p}Class`) || '\u672a\u5206\u73ed',
            status: normalizePersonStatus(value(`${p}Status`)),
            email: value(`${p}Email`),
            phone: value(`${p}Phone`)
        };
        if (!prefix) {
            body.studentNo = requiredValue('studentId', '\u8bf7\u586b\u5199\u5b66\u53f7');
            body.password = '123456';
        }
        return body;
    }

    function teacherFormBody(prefix) {
        const p = prefix ? 'editTeacher' : 'teacher';
        const body = {
            name: requiredValue(`${p}Name`, '\u8bf7\u586b\u5199\u59d3\u540d'),
            gender: normalizeGender(value(`${p}Gender`)),
            departmentId: numericValue(`${p}College`) || 1,
            title: titleText(value(`${p}Title`)) || '\u8bb2\u5e08',
            email: value(`${p}Email`),
            phone: value(`${p}Phone`),
            status: 1
        };
        if (!prefix) {
            body.teacherNo = requiredValue('teacherId', '\u8bf7\u586b\u5199\u5de5\u53f7');
            body.password = '123456';
        }
        return body;
    }

    async function fillPeopleFormOptions(role) {
        if (state.peopleOptionsLoaded[role]) return;
        state.peopleOptionsLoaded[role] = true;
        if (role === 'teacher') {
            await fillTeacherDepartmentOptions();
            return;
        }
        await fillStudentAcademicOptions();
    }

    async function fillStudentAcademicOptions() {
        const [collegePage, majorPage, classPage] = await Promise.all([
            api.get('/api/v1/colleges/list', { pageNum: 1, pageSize: 100 }).catch(() => null),
            api.get('/api/v1/majors/list', { pageNum: 1, pageSize: 100 }).catch(() => null),
            api.get('/api/v1/classes/list', { pageNum: 1, pageSize: 100 }).catch(() => null)
        ]);
        state.studentColleges = api.pageItems(collegePage);
        state.studentMajors = api.pageItems(majorPage);
        state.studentClasses = api.pageItems(classPage);
        fillSelect('studentCollegeFilter', state.studentColleges, collegeLabel, { allText: '\u6240\u6709\u5b66\u9662' });
        fillSelect('studentCollege', state.studentColleges, collegeLabel, { placeholder: '\u8bf7\u9009\u62e9\u5b66\u9662' });
        fillSelect('editStudentCollege', state.studentColleges, collegeLabel, { placeholder: '\u8bf7\u9009\u62e9\u5b66\u9662' });
        await populateMajorOptions('studentMajor', value('studentCollege'));
        await populateMajorOptions('editStudentMajor', value('editStudentCollege'));
        await populateClassOptions('studentClass', value('studentMajor'));
        await populateClassOptions('editStudentClass', value('editStudentMajor'));
        bindAcademicCascade();
    }

    async function fillTeacherDepartmentOptions() {
        const departmentPage = await api.get('/api/v1/departments/list', { pageNum: 1, pageSize: 100 }).catch(() => null);
        state.teacherDepartments = api.pageItems(departmentPage);
        fillSelect('teacherCollegeFilter', state.teacherDepartments, departmentLabel, { allText: '\u6240\u6709\u9662\u7cfb' });
        fillSelect('teacherCollege', state.teacherDepartments, departmentLabel, { placeholder: '\u8bf7\u9009\u62e9\u9662\u7cfb' });
        fillSelect('editTeacherCollege', state.teacherDepartments, departmentLabel, { placeholder: '\u8bf7\u9009\u62e9\u9662\u7cfb' });
        fillTeacherTitleSelect('teacherTitle');
        fillTeacherTitleSelect('editTeacherTitle');
        fillTeacherTitleSelect('teacherTitleFilter', true);
    }

    function bindAcademicCascade() {
        const pairs = [
            ['studentCollege', 'studentMajor', 'studentClass'],
            ['editStudentCollege', 'editStudentMajor', 'editStudentClass']
        ];
        pairs.forEach(([collegeId, majorId, classId]) => {
            const college = document.getElementById(collegeId);
            const major = document.getElementById(majorId);
            if (college && !college.dataset.dynamicCascade) {
                college.dataset.dynamicCascade = 'true';
                college.addEventListener('change', async () => {
                    await populateMajorOptions(majorId, value(collegeId));
                    await populateClassOptions(classId, value(majorId));
                });
            }
            if (major && !major.dataset.dynamicCascade) {
                major.dataset.dynamicCascade = 'true';
                major.addEventListener('change', async () => populateClassOptions(classId, value(majorId)));
            }
        });
    }

    async function populateMajorOptions(selectId, collegeId, selectedId) {
        const allMajors = state.studentMajors || [];
        const filtered = allMajors.filter((major) => !collegeId || String(major.collegeId || major.departmentId || '') === String(collegeId));
        const majors = filtered.length ? filtered : allMajors;
        fillSelect(selectId, majors, majorLabel, { placeholder: '\u8bf7\u9009\u62e9\u4e13\u4e1a', selectedValue: selectedId });
    }

    async function populateClassOptions(selectId, majorId, selectedValue) {
        const allClasses = state.studentClasses || [];
        const filtered = allClasses.filter((item) => !majorId || String(item.majorId || '') === String(majorId));
        const classes = filtered.length ? filtered : allClasses;
        const select = document.getElementById(selectId);
        if (!select || !classes.length) return;
        select.innerHTML = `<option value="">\u8bf7\u9009\u62e9\u73ed\u7ea7</option>` + classes.map((item) => {
            const text = item.className || item.name || item.classCode || item.id;
            const valueText = item.className || item.name || text;
            return `<option value="${escape(valueText)}" ${String(valueText) === String(selectedValue || '') ? 'selected' : ''}>${escape(text)}</option>`;
        }).join('');
    }

    function fillSelect(id, rows, labelFn, options = {}) {
        const select = document.getElementById(id);
        if (!select || !Array.isArray(rows) || !rows.length) return;
        const first = options.allText || options.placeholder || '';
        select.innerHTML = `<option value="">${escape(first)}</option>` + rows.map((row) => {
            const selected = String(row.id) === String(options.selectedValue || '') ? 'selected' : '';
            return `<option value="${escape(row.id)}" ${selected}>${escape(labelFn(row))}</option>`;
        }).join('');
    }

    function fillTeacherTitleSelect(id, includeAll) {
        const select = document.getElementById(id);
        if (!select) return;
        const titles = [
            ['professor', '\u6559\u6388'],
            ['associate_professor', '\u526f\u6559\u6388'],
            ['lecturer', '\u8bb2\u5e08'],
            ['assistant', '\u52a9\u6559']
        ];
        select.innerHTML = (includeAll ? '<option value="">\u6240\u6709\u804c\u79f0</option>' : '') +
            titles.map(([valueText, text]) => `<option value="${valueText}">${text}</option>`).join('');
    }

    function renderPeoplePager(role, totalPages) {
        const pager = document.querySelector('.pagination');
        if (!pager) return;
        const pages = Math.max(1, Number(totalPages || 1));
        const current = Math.min(state.peoplePage[role], pages);
        state.peoplePage[role] = current;
        const button = (page, text, disabled) => `
            <button class="btn ${page === current ? 'btn-primary' : 'btn-secondary'} pagination-btn" data-people-page="${page}" ${disabled ? 'disabled' : ''}>${text}</button>
        `;
        const middle = Array.from({ length: pages }, (_, index) => index + 1)
            .filter((page) => pages <= 7 || page === 1 || page === pages || Math.abs(page - current) <= 1)
            .map((page, index, all) => {
                const gap = index > 0 && page - all[index - 1] > 1 ? '<span class="pagination-ellipsis">...</span>' : '';
                return `${gap}${button(page, page, false)}`;
            }).join('');
        pager.innerHTML = `${button(Math.max(1, current - 1), '<i class="fas fa-chevron-left"></i>', current <= 1)}${middle}${button(Math.min(pages, current + 1), '<i class="fas fa-chevron-right"></i>', current >= pages)}`;
        pager.querySelectorAll('[data-people-page]').forEach((item) => {
            item.addEventListener('click', () => renderAdminPeople(role, item.dataset.peoplePage));
        });
    }

    async function exportPeople(role) {
        const endpoint = role === 'teacher' ? '/api/v1/teachers/list' : '/api/v1/students/list';
        const data = await api.get(endpoint, { ...peopleListParams(role), pageNum: 1, pageSize: 1000, orderByColumn: 'id', sortOrder: 'desc' });
        const rows = api.pageItems(data);
        const header = role === 'teacher'
            ? ['teacherNo', 'name', 'gender', 'department', 'title', 'phone', 'email']
            : ['studentNo', 'name', 'gender', 'college', 'major', 'className', 'status', 'phone', 'email'];
        const csvRows = [header, ...rows.map((row) => role === 'teacher'
            ? [row.teacherNo, row.name, row.gender, row.departmentName || row.departmentId, row.title, row.phone, row.email]
            : [row.studentNo, row.name, row.gender, row.collegeName || row.collegeId, row.majorName || row.majorId, row.className, peopleStatusText(row.status), row.phone, row.email]
        )];
        downloadCsv(`${role}-records.csv`, csvRows);
    }

    function downloadCsv(filename, rows) {
        const csv = rows.map((row) => row.map((cell) => `"${String(cell ?? '').replace(/"/g, '""')}"`).join(',')).join('\r\n');
        const blob = new Blob(['\ufeff', csv], { type: 'text/csv;charset=utf-8;' });
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = filename;
        document.body.appendChild(link);
        link.click();
        URL.revokeObjectURL(link.href);
        link.remove();
    }

    function checkedValues(selector) {
        return Array.from(document.querySelectorAll(`${selector}:checked`))
            .map((item) => item.value)
            .filter(Boolean);
    }

    function toggleSelectAll(source) {
        const sourceElement = source || window.event?.target;
        const root = sourceElement?.closest('table') || document;
        root.querySelectorAll('tbody input[type="checkbox"]').forEach((checkbox) => {
            checkbox.checked = Boolean(sourceElement?.checked);
        });
    }

    function unsupportedImport() {
        notify('error', 'Import unavailable', 'Please use add, edit, delete, and export for database-backed operations.');
    }

    function resetForm(id) {
        const form = document.getElementById(id);
        if (form) form.reset();
    }

    function collegeLabel(item) {
        return item.collegeName || item.name || item.collegeCode || item.code || item.id;
    }

    function departmentLabel(item) {
        return item.departmentName || item.name || item.departmentCode || item.code || item.id;
    }

    function majorLabel(item) {
        return item.majorName || item.name || item.majorCode || item.code || item.id;
    }

    function numericValue(id) {
        const result = Number(value(id));
        return Number.isFinite(result) && result > 0 ? result : '';
    }

    function normalizeGender(raw) {
        if (raw === 'male' || raw === '\u7537') return '\u7537';
        if (raw === 'female' || raw === '\u5973') return '\u5973';
        return raw || '\u7537';
    }

    function reverseGender(raw) {
        if (raw === '\u5973' || raw === 'female') return 'female';
        return 'male';
    }

    function normalizePersonStatus(raw) {
        if (!raw || raw === 'all') return '';
        if (raw === 'active' || raw === 'normal' || raw === '1') return 1;
        if (raw === 'graduated' || raw === '2') return 2;
        if (raw === 'dropout' || raw === '3') return 3;
        return 0;
    }

    function reversePersonStatus(status) {
        if (Number(status) === 2) return 'graduated';
        if (Number(status) === 3) return 'dropout';
        if (Number(status) === 0) return 'suspended';
        return 'active';
    }

    function peopleStatusText(status) {
        if (Number(status) === 2) return '\u6bd5\u4e1a';
        if (Number(status) === 3) return '\u9000\u5b66';
        if (Number(status) === 0) return '\u4f11\u5b66';
        return '\u5728\u8bfb';
    }

    function peopleStatusBadge(status) {
        if (Number(status) === 1) return 'success';
        if (Number(status) === 2) return 'info';
        return 'warning';
    }

    function titleText(raw) {
        const map = {
            professor: '\u6559\u6388',
            associate_professor: '\u526f\u6559\u6388',
            lecturer: '\u8bb2\u5e08',
            assistant: '\u52a9\u6559'
        };
        return map[raw] || raw || '';
    }

    function reverseTeacherTitle(raw) {
        const map = {
            '\u6559\u6388': 'professor',
            '\u526f\u6559\u6388': 'associate_professor',
            '\u8bb2\u5e08': 'lecturer',
            '\u52a9\u6559': 'assistant'
        };
        return map[raw] || raw || 'lecturer';
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
        window.batchDeleteClasses = batchDeleteClasses;
        window.exportClassData = exportClassData;
        window.importClassData = unsupportedImport;
        window.toggleSelectAll = toggleSelectAll;
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

    async function batchDeleteClasses() {
        const ids = checkedValues('.class-checkbox');
        if (!ids.length) {
            notify('error', 'No selection', 'Please select classes first.');
            return;
        }
        if (!confirm(`Delete ${ids.length} selected classes?`)) return;
        await api.request('/api/v1/classes/batch', { method: 'DELETE', body: ids.map(Number) });
        notify('success', 'Deleted', 'Selected classes were removed from the database.');
        await loadClasses(state.classPage);
    }

    async function exportClassData() {
        const data = await api.get('/api/v1/classes/list', {
            pageNum: 1,
            pageSize: 1000,
            keyword: value('searchClassInput'),
            grade: filterValue('gradeFilter'),
            status: normalizeClassStatus(value('statusFilter')),
            orderByColumn: 'id',
            isAsc: true
        });
        const rows = api.pageItems(data).map((item) => [
            item.classCode,
            item.className,
            item.collegeName || item.collegeId,
            item.majorName || item.majorId,
            item.grade,
            item.headTeacherName || '',
            item.studentCount ?? 0,
            item.monitorName || '',
            classStatusText(item.status)
        ]);
        downloadCsv('classes.csv', [['classCode', 'className', 'college', 'major', 'grade', 'headTeacher', 'studentCount', 'monitor', 'status'], ...rows]);
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
        window.prepareBatchDelete = batchDeleteCourses;
        window.exportCourseData = exportCourseData;
        window.importCourseData = unsupportedImport;
        window.toggleSelectAll = toggleSelectAll;
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
                    <div class="panel-heading">
                        <label class="pull-right" title="Select course"><input type="checkbox" class="course-checkbox" value="${course.id}"></label>
                        <h3 class="panel-title">${escape(course.courseName)}</h3>
                    </div>
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

    async function batchDeleteCourses() {
        const ids = checkedValues('.course-checkbox');
        if (!ids.length) {
            notify('error', 'No selection', 'Please select courses first.');
            return;
        }
        if (!confirm(`Delete ${ids.length} selected courses?`)) return;
        await api.request('/api/v1/courses/batch', { method: 'DELETE', body: ids.map(Number) });
        notify('success', 'Deleted', 'Selected courses were removed from the database.');
        await loadAdminCourses(state.coursePage);
    }

    async function exportCourseData() {
        const data = await api.get('/api/v1/courses/list', {
            pageNum: 1,
            pageSize: 1000,
            courseName: value('searchCourseInput'),
            courseType: filterValue('categoryFilter'),
            teacherId: filterValue('teacherFilter'),
            orderByColumn: 'id',
            isAsc: false
        });
        const rows = api.pageItems(data).map((course) => [
            course.courseCode,
            course.courseName,
            course.teacherName || course.teacherId || '',
            course.courseType || '',
            course.credit ?? '',
            course.availableSlots ?? course.maxCapacity ?? '',
            course.selectedCount ?? course.currentStudents ?? 0,
            courseStatusText(course.status)
        ]);
        downloadCsv('courses.csv', [['courseCode', 'courseName', 'teacher', 'courseType', 'credit', 'capacity', 'selectedCount', 'status'], ...rows]);
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
        window.showGradeInputModal = showGradeInputModal;
        window.submitGrade = submitGrade;
        window.batchUpdateGrades = showBatchGradeModal;
        window.loadCourseStudents = loadCourseStudents;
        window.submitBatchGrades = submitBatchGrades;
        window.exportGradeData = exportGradeData;
        window.importGradeData = unsupportedImport;
        window.toggleSelectAll = toggleSelectAll;
        await fillGradeFormOptions();
        await loadAdminGrades(1);
    }

    async function loadAdminGrades(page) {
        state.gradePage = Math.max(1, Number(page || 1));
        const body = document.getElementById('gradeTableBody');
        if (!body) return;
        body.innerHTML = rowMessage(11, '正在加载成绩数据...');
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
                <td>${escape(g.courseCode || g.courseId)}</td>
                <td>${escape(g.courseName)}</td>
                <td>${escape(g.credit)}</td>
                <td>${escape(g.semester || '-')}</td>
                <td>${escape(g.score ?? '-')}</td>
                <td>${escape(scoreLevel(g.score))}</td>
                <td>${formatDate(g.updatedAt || g.selectionTime)}</td>
                <td>
                    <button class="btn btn-info" onclick="showGradeDetail('${g.selectionId}')">详情</button>
                    <button class="btn btn-primary" onclick="showEditGradeModal('${g.selectionId}')">编辑</button>
                    <button class="btn btn-danger" onclick="deleteGrade('${g.selectionId}')">清空</button>
                </td>
            </tr>
        `).join('') : rowMessage(11, '暂无成绩数据');
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
        setText('detailGPA', g.score == null ? '-' : Math.max(0, Math.round((Number(g.score) - 50) / 10 * 10) / 10));
        setText('detailTeacherComment', g.remark || '-');
        setText('detailUpdateTime', formatDate(g.updatedAt));
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

    async function fillGradeFormOptions(force) {
        if (state.gradeOptionsLoaded && !force) return;
        const [studentsPage, coursesPage, classesPage] = await Promise.all([
            api.get('/api/v1/students/list', { pageNum: 1, pageSize: 1000, orderByColumn: 'id', isAsc: true }).catch(() => null),
            api.get('/api/v1/courses/list', { pageNum: 1, pageSize: 1000, orderByColumn: 'id', isAsc: true }).catch(() => null),
            api.get('/api/v1/classes/list', { pageNum: 1, pageSize: 1000, orderByColumn: 'id', isAsc: true }).catch(() => null)
        ]);
        const students = api.pageItems(studentsPage);
        const courses = api.pageItems(coursesPage);
        const classes = api.pageItems(classesPage);
        fillSelect('studentId', students, (student) => `${student.name || student.studentNo || student.id} (${student.studentNo || student.id})`, { placeholder: 'Select student' });
        fillSelect('courseId', courses, (course) => `${course.courseName || course.courseCode || course.id} (${course.courseCode || course.id})`, { placeholder: 'Select course' });
        fillSelect('courseFilter', courses, (course) => `${course.courseName || course.courseCode || course.id}`, { allText: 'All courses' });
        fillSelect('batchCourseId', courses, (course) => `${course.courseName || course.courseCode || course.id} (${course.courseCode || course.id})`, { placeholder: 'Select course' });
        const classFilter = document.getElementById('classFilter');
        if (classFilter && classes.length) {
            classFilter.innerHTML = '<option value="all">All classes</option>' + classes.map((item) => {
                const text = item.className || item.classCode || item.id;
                return `<option value="${escape(text)}">${escape(text)}</option>`;
            }).join('');
        }
        state.gradeOptionsLoaded = true;
    }

    async function showGradeInputModal() {
        await fillGradeFormOptions();
        openModal('gradeInputModal');
    }

    async function submitGrade() {
        await api.post('/api/v1/grades', {
            studentId: Number(requiredValue('studentId', 'Please select a student.')),
            courseId: Number(requiredValue('courseId', 'Please select a course.')),
            score: requiredValue('score', 'Please enter a score.'),
            remark: value('teacherComment')
        });
        closeModal('gradeInputModal');
        resetForm('gradeInputForm');
        notify('success', 'Saved', 'Grade was written to the database.');
        await loadAdminGrades(1);
    }

    async function showBatchGradeModal() {
        await fillGradeFormOptions();
        const courseId = value('batchCourseId') || filterValue('courseFilter');
        if (courseId) {
            setValue('batchCourseId', courseId);
            await loadCourseStudents(courseId);
        }
        openModal('batchInputModal');
    }

    async function loadCourseStudents(courseId) {
        const body = document.getElementById('batchGradeTable');
        if (!body) return;
        if (!courseId || courseId === 'all') {
            body.innerHTML = rowMessage(5, 'Please select a course first.');
            return;
        }
        body.innerHTML = rowMessage(5, 'Loading students...');
        const data = await api.get(`/api/v1/course-selections/course/${encodeURIComponent(courseId)}`, {
            pageNum: 1,
            pageSize: 1000,
            status: 1,
            orderByColumn: 'id',
            isAsc: true
        });
        const rows = api.pageItems(data);
        body.innerHTML = rows.length ? rows.map((row, index) => `
            <tr data-selection-id="${escape(row.id)}" data-student-id="${escape(row.studentId)}">
                <td>${index + 1}</td>
                <td>${escape(row.studentCode || row.studentNo || row.studentId)}</td>
                <td>${escape(row.studentName || '-')}</td>
                <td>${escape(row.className || '-')}</td>
                <td><input type="number" class="form-control score-input" min="0" max="100" step="0.5" value="${escape(row.score ?? '')}"></td>
            </tr>
        `).join('') : rowMessage(5, 'No enrolled students.');
    }

    async function submitBatchGrades() {
        const rows = Array.from(document.querySelectorAll('#batchGradeTable tr[data-selection-id]'));
        let saved = 0;
        for (const row of rows) {
            const score = row.querySelector('.score-input')?.value;
            if (score === '') continue;
            await api.request(`/api/v1/grades/${encodeURIComponent(row.dataset.selectionId)}`, {
                method: 'PUT',
                body: { score, remark: '' }
            });
            saved++;
        }
        closeModal('batchInputModal');
        notify('success', 'Saved', `${saved} grades were saved.`);
        await loadAdminGrades(state.gradePage);
    }

    async function exportGradeData() {
        const data = await api.get('/api/v1/grades/list', {
            pageNum: 1,
            pageSize: 1000,
            keyword: value('searchGradeInput'),
            courseId: filterValue('courseFilter'),
            className: filterValue('classFilter'),
            graded: gradeStatusFilter(value('statusFilter')),
            orderByColumn: 'id',
            isAsc: false
        });
        const rows = api.pageItems(data).map((row) => [
            row.studentNo,
            row.studentName,
            row.className || '',
            row.courseCode,
            row.courseName,
            row.credit ?? '',
            row.score ?? '',
            scoreLevel(row.score),
            row.remark || ''
        ]);
        downloadCsv('grades.csv', [['studentNo', 'studentName', 'className', 'courseCode', 'courseName', 'credit', 'score', 'level', 'remark'], ...rows]);
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

    async function initStudentGrades() {
        const current = await currentUser();
        const data = await api.get(`/api/v1/course-selections/student/${current.userId}`, {
            pageNum: 1,
            pageSize: 1000,
            status: 1,
            orderByColumn: 'selectionTime',
            isAsc: false
        });
        state.studentGrades = api.pageItems(data);
        ensureStudentGradesLayout();
        bindStudentGradeControls();
        renderStudentGradeStats();
        renderStudentGradeRows();
    }

    function ensureStudentGradesLayout() {
        if (document.querySelector('.grades-table tbody') && document.getElementById('course-name-filter')) {
            return;
        }
        const wrapper = document.querySelector('.content-wrapper') || document.querySelector('.main-content') || document.body;
        wrapper.innerHTML = `
            <div class="stats-cards dynamic-student-grades">
                <div class="stat-card">
                    <div class="stat-icon"><i class="fas fa-chart-pie"></i></div>
                    <div class="stat-info">
                        <div class="stat-value">-</div>
                        <div class="stat-label">\u5f53\u524d\u5e73\u5747\u7ee9\u70b9</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon"><i class="fas fa-graduation-cap"></i></div>
                    <div class="stat-info">
                        <div class="stat-value">-</div>
                        <div class="stat-label">\u5e73\u5747\u5206</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon"><i class="fas fa-book"></i></div>
                    <div class="stat-info">
                        <div class="stat-value">0</div>
                        <div class="stat-label">\u5df2\u6709\u6210\u7ee9\u8bfe\u7a0b</div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-icon"><i class="fas fa-award"></i></div>
                    <div class="stat-info">
                        <div class="stat-value">0</div>
                        <div class="stat-label">A\u7b49\u7ea7\u8bfe\u7a0b</div>
                    </div>
                </div>
            </div>
            <div class="filter-bar">
                <div class="filter-item">
                    <div class="filter-label">\u5b66\u671f\u9009\u62e9</div>
                    <select class="filter-select" id="semester-filter">
                        <option value="all">\u5168\u90e8\u5b66\u671f</option>
                    </select>
                </div>
                <div class="filter-item">
                    <div class="filter-label">\u8bfe\u7a0b\u7c7b\u578b</div>
                    <select class="filter-select" id="course-type-filter">
                        <option value="all">\u5168\u90e8\u7c7b\u578b</option>
                        <option value="required">\u5fc5\u4fee\u8bfe</option>
                        <option value="elective">\u9009\u4fee\u8bfe</option>
                        <option value="general">\u901a\u8bc6\u8bfe</option>
                        <option value="professional">\u4e13\u4e1a\u8bfe</option>
                    </select>
                </div>
                <div class="filter-item">
                    <div class="filter-label">\u6210\u7ee9\u8303\u56f4</div>
                    <select class="filter-select" id="grade-range-filter">
                        <option value="all">\u5168\u90e8\u6210\u7ee9</option>
                        <option value="A">A</option>
                        <option value="B">B</option>
                        <option value="C">C</option>
                        <option value="D">D</option>
                        <option value="F">F</option>
                    </select>
                </div>
                <div class="filter-item">
                    <div class="filter-label">\u8bfe\u7a0b\u540d\u79f0</div>
                    <input type="text" class="filter-input" id="course-name-filter" placeholder="\u641c\u7d22\u8bfe\u7a0b\u540d\u79f0">
                </div>
                <div class="filter-actions">
                    <button class="btn btn-primary" type="button">\u67e5\u8be2</button>
                    <button class="btn btn-outline-secondary" type="button">\u91cd\u7f6e</button>
                </div>
            </div>
            <div class="grades-container">
                <div class="grades-header">
                    <h3 class="grades-title">\u6210\u7ee9\u660e\u7ec6</h3>
                    <div class="grades-actions">
                        <button class="btn btn-outline-secondary" type="button"><i class="fas fa-download"></i> \u5bfc\u51fa\u6210\u7ee9</button>
                        <button class="btn btn-outline-secondary" type="button"><i class="fas fa-print"></i> \u6253\u5370\u6210\u7ee9</button>
                    </div>
                </div>
                <div class="table-responsive">
                    <table class="grades-table">
                        <thead>
                            <tr>
                                <th>\u8bfe\u7a0b\u4ee3\u7801</th>
                                <th>\u8bfe\u7a0b\u540d\u79f0</th>
                                <th>\u8bfe\u7a0b\u7c7b\u578b</th>
                                <th>\u5b66\u5206</th>
                                <th>\u6210\u7ee9\u7b49\u7ea7</th>
                                <th>\u5206\u6570\u6210\u7ee9</th>
                                <th>\u5b66\u5206\u7ee9\u70b9</th>
                                <th>\u5b66\u671f</th>
                                <th>\u64cd\u4f5c</th>
                            </tr>
                        </thead>
                        <tbody></tbody>
                    </table>
                </div>
            </div>
            <div class="grade-details">
                <div class="grade-details-content">
                    <div class="grade-details-header">
                        <h2 class="grade-details-title">\u6210\u7ee9\u8be6\u60c5</h2>
                        <button class="grade-details-close" type="button"><i class="fas fa-times"></i></button>
                    </div>
                    <div class="grade-details-grid">
                        ${['\u8bfe\u7a0b\u4ee3\u7801', '\u8bfe\u7a0b\u540d\u79f0', '\u8bfe\u7a0b\u7c7b\u578b', '\u5b66\u5206', '\u6700\u7ec8\u6210\u7ee9', '\u5b66\u5206\u7ee9\u70b9', '\u6388\u8bfe\u6559\u5e08', '\u5b66\u671f', '\u5e73\u65f6\u6210\u7ee9', '\u5b9e\u9a8c\u6210\u7ee9', '\u8003\u8bd5\u6210\u7ee9', '\u66f4\u65b0\u65f6\u95f4', '\u5907\u6ce8'].map((label) => `
                            <div class="grade-detail-item">
                                <div class="grade-detail-label">${label}</div>
                                <div class="grade-detail-value">-</div>
                            </div>
                        `).join('')}
                    </div>
                </div>
            </div>
        `;
    }

    function renderStudentGradeRows() {
        const body = document.querySelector('.grades-table tbody');
        if (!body) return;
        const rows = filteredStudentGrades();
        body.innerHTML = rows.length ? rows.map((g) => {
            const level = scoreLevel(g.score);
            return `
                <tr class="grade-row">
                    <td>${escape(g.courseCode || g.courseId)}</td>
                    <td>${escape(g.courseName || '-')}</td>
                    <td>${escape(g.courseType || '-')}</td>
                    <td>${escape(g.credit || 0)}</td>
                    <td><span class="grade-badge ${escape(letterGrade(g.score))}">${escape(level)}</span></td>
                    <td>${escape(g.score ?? '-')}</td>
                    <td>${escape(gpaValue(g.score))}</td>
                    <td>${escape(g.semester || '-')}</td>
                    <td><button class="btn btn-sm btn-outline-primary" type="button" data-student-grade-detail="${escape(g.selectionId || g.id)}">Details</button></td>
                </tr>
            `;
        }).join('') : rowMessage(9, 'No grade data.');
        body.querySelectorAll('[data-student-grade-detail]').forEach((button) => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                showStudentGradeDetail(button.dataset.studentGradeDetail);
            }, true);
        });
    }

    function filteredStudentGrades() {
        const keyword = value('course-name-filter').toLowerCase();
        const typeFilter = value('course-type-filter') || 'all';
        const gradeFilter = value('grade-range-filter') || 'all';
        return state.studentGrades.filter((item) => {
            if (keyword && !String(`${item.courseName || ''} ${item.courseCode || ''}`).toLowerCase().includes(keyword)) return false;
            if (!evaluationCourseTypeMatches(item.courseType, typeFilter)) return false;
            return gradeRangeMatches(item.score, gradeFilter);
        });
    }

    function renderStudentGradeStats() {
        const values = document.querySelectorAll('.grade-stats .stat-value, .stats-container .stat-value, .stat-value');
        if (!values.length) return;
        const rows = state.studentGrades.filter((item) => item.score != null);
        const scores = rows.map((item) => Number(item.score)).filter((score) => !Number.isNaN(score));
        values[0].textContent = scores.length ? gpaValue(average(scores)) : '-';
        if (values[1]) values[1].textContent = scores.length ? average(scores) : '-';
        if (values[2]) values[2].textContent = String(rows.length);
        if (values[3]) values[3].textContent = String(scores.filter((score) => score >= 90).length);
    }

    function bindStudentGradeControls() {
        const searchButton = document.querySelector('.filter-actions .btn-primary');
        if (searchButton && !searchButton.dataset.dynamicGradesBound) {
            searchButton.dataset.dynamicGradesBound = 'true';
            searchButton.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                renderStudentGradeRows();
            }, true);
        }
        const resetButton = document.querySelector('.filter-actions .btn-outline-secondary');
        if (resetButton && !resetButton.dataset.dynamicGradesBound) {
            resetButton.dataset.dynamicGradesBound = 'true';
            resetButton.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                setValue('semester-filter', 'all');
                setValue('course-type-filter', 'all');
                setValue('grade-range-filter', 'all');
                setValue('course-name-filter', '');
                renderStudentGradeRows();
            }, true);
        }
        ['semester-filter', 'course-type-filter', 'grade-range-filter', 'course-name-filter'].forEach((id) => {
            const input = document.getElementById(id);
            if (!input || input.dataset.dynamicGradesBound) return;
            input.dataset.dynamicGradesBound = 'true';
            input.addEventListener(input.tagName === 'INPUT' ? 'input' : 'change', () => renderStudentGradeRows(), true);
        });
        const exportButton = document.querySelector('.grades-actions button:first-of-type');
        if (exportButton && !exportButton.dataset.dynamicGradesBound) {
            exportButton.dataset.dynamicGradesBound = 'true';
            exportButton.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                exportStudentGrades();
            }, true);
        }
        const printButton = document.querySelector('.grades-actions button:last-of-type');
        if (printButton && !printButton.dataset.dynamicGradesBound) {
            printButton.dataset.dynamicGradesBound = 'true';
            printButton.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                window.print();
            }, true);
        }
        document.querySelector('.grade-details-close')?.addEventListener('click', (event) => {
            event.preventDefault();
            event.stopImmediatePropagation();
            const modal = document.querySelector('.grade-details');
            if (modal) modal.style.display = 'none';
        }, true);
    }

    function showStudentGradeDetail(selectionId) {
        const item = state.studentGrades.find((row) => String(row.selectionId || row.id) === String(selectionId));
        if (!item) return;
        const modal = document.querySelector('.grade-details');
        if (!modal) {
            alert(`Score: ${item.score ?? '-'}`);
            return;
        }
        const title = modal.querySelector('.grade-details-title');
        if (title) title.textContent = `${item.courseName || '-'} grade detail`;
        const values = modal.querySelectorAll('.grade-detail-value');
        const details = [
            item.courseCode || item.courseId,
            item.courseName || '-',
            item.courseType || '-',
            item.credit || 0,
            `${item.score ?? '-'} (${scoreLevel(item.score)})`,
            gpaValue(item.score),
            item.teacherName || '-',
            item.semester || '-',
            item.dailyGrade ?? '-',
            item.labGrade ?? '-',
            item.examGrade ?? '-',
            formatDateTime(item.updatedAt || item.updateTime || item.selectionTime),
            item.remark || '-'
        ];
        values.forEach((element, index) => {
            element.textContent = details[index] == null ? '-' : String(details[index]);
        });
        modal.style.display = 'flex';
    }

    function exportStudentGrades() {
        const rows = filteredStudentGrades();
        downloadCsv('student-grades.csv', [
            ['courseCode', 'courseName', 'courseType', 'credit', 'score', 'level', 'gpa', 'semester', 'teacher', 'remark'],
            ...rows.map((row) => [
                row.courseCode || row.courseId,
                row.courseName || '',
                row.courseType || '',
                row.credit || 0,
                row.score ?? '',
                scoreLevel(row.score),
                gpaValue(row.score),
                row.semester || '',
                row.teacherName || '',
                row.remark || ''
            ])
        ]);
    }

    function gradeRangeMatches(score, range) {
        if (!range || range === 'all') return true;
        const valueText = Number(score);
        if (Number.isNaN(valueText)) return false;
        if (range === 'A') return valueText >= 90;
        if (range === 'B') return valueText >= 80 && valueText < 90;
        if (range === 'C') return valueText >= 70 && valueText < 80;
        if (range === 'D') return valueText >= 60 && valueText < 70;
        if (range === 'F') return valueText < 60;
        return true;
    }

    function letterGrade(score) {
        const valueText = Number(score);
        if (Number.isNaN(valueText)) return '';
        if (valueText >= 90) return 'A';
        if (valueText >= 80) return 'B';
        if (valueText >= 70) return 'C';
        if (valueText >= 60) return 'D';
        return 'F';
    }

    function gpaValue(score) {
        const valueText = Number(score);
        if (Number.isNaN(valueText)) return '-';
        return Math.max(0, Math.round((valueText - 50) / 10 * 10) / 10);
    }

    async function initStudentSchedule() {
        bindStudentScheduleControls();
        await renderStudentSchedule();
    }

    async function renderStudentSchedule() {
        const current = await currentUser();
        const data = await api.get(`/api/v1/course-selections/student/${current.userId}`, {
            pageNum: 1,
            pageSize: 1000,
            status: 1,
            orderByColumn: 'selectionTime',
            isAsc: false
        });
        const rows = api.pageItems(data);
        updateScheduleWeekLabel();
        const body = document.querySelector('.schedule-table tbody');
        if (!body) return;
        body.innerHTML = rows.length ? rows.map((course) => `
            <tr>
                <td class="time-column">${escape(course.schedule || 'TBD')}</td>
                <td colspan="5" class="time-slot">
                    <div class="course-block ${escape(scheduleCourseTypeClass(course.courseType))}" data-schedule-course="${escape(course.courseId)}">
                        <div class="course-block-title">${escape(course.courseName || '-')}</div>
                        <div class="course-block-location">${escape(course.classroom || '-')}</div>
                        <div class="course-block-info">${escape(course.courseCode || course.courseId)} | ${escape(course.credit || 0)} credits | ${escape(course.teacherName || '-')}</div>
                    </div>
                </td>
            </tr>
        `).join('') : '<tr><td colspan="6" class="text-center">No schedule data.</td></tr>';
        body.querySelectorAll('.course-block').forEach((block) => {
            block.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                const title = block.querySelector('.course-block-title')?.textContent || '-';
                const info = block.querySelector('.course-block-info')?.textContent || '';
                alert(`${title}\n${info}`);
            }, true);
        });
    }

    function bindStudentScheduleControls() {
        const previous = document.querySelector('.navigation-button:first-of-type');
        const next = document.querySelector('.navigation-button:last-of-type');
        const today = document.querySelector('.today-button');
        if (previous && !previous.dataset.dynamicScheduleBound) {
            previous.dataset.dynamicScheduleBound = 'true';
            previous.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                state.scheduleWeekOffset -= 1;
                await renderStudentSchedule();
            }, true);
        }
        if (next && !next.dataset.dynamicScheduleBound) {
            next.dataset.dynamicScheduleBound = 'true';
            next.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                state.scheduleWeekOffset += 1;
                await renderStudentSchedule();
            }, true);
        }
        if (today && !today.dataset.dynamicScheduleBound) {
            today.dataset.dynamicScheduleBound = 'true';
            today.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                state.scheduleWeekOffset = 0;
                await renderStudentSchedule();
            }, true);
        }
        document.querySelectorAll('.semester-button').forEach((button) => {
            if (button.dataset.dynamicScheduleBound) return;
            button.dataset.dynamicScheduleBound = 'true';
            button.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                document.querySelectorAll('.semester-button').forEach((item) => item.classList.remove('active'));
                button.classList.add('active');
                await renderStudentSchedule();
            }, true);
        });
    }

    function updateScheduleWeekLabel() {
        const label = document.querySelector('.current-week');
        if (!label) return;
        label.textContent = state.scheduleWeekOffset === 0
            ? 'Current week'
            : state.scheduleWeekOffset > 0
                ? `Week +${state.scheduleWeekOffset}`
                : `Week ${state.scheduleWeekOffset}`;
    }

    function scheduleCourseTypeClass(type) {
        const text = String(type || '').toLowerCase();
        if (text.includes('elective') || text.includes('\u9009\u4fee')) return 'elective';
        if (text.includes('general') || text.includes('\u901a\u8bc6')) return 'general';
        if (text.includes('professional') || text.includes('\u4e13\u4e1a')) return 'professional';
        return 'required';
    }

    async function initStudentMessages(pageNum) {
        const current = await currentUser();
        state.messagePage = Math.max(1, Number(pageNum || state.messagePage || 1));
        bindStudentMessageControls();
        const list = document.querySelector('.messages-list');
        if (!list) return;

        if (state.messageFilter.deleted) {
            list.innerHTML = '<div class="empty-message">Deleted messages are removed from the database.</div>';
            renderMessagePagination(0, 1);
            await updateMessageCounters(current.userId);
            return;
        }

        list.innerHTML = '<div class="empty-message">Loading database messages...</div>';
        const data = await api.get('/api/v1/messages/list', studentMessageParams(current.userId));
        const rows = api.pageItems(data);
        list.innerHTML = rows.length ? rows.map(messageCard).join('') : '<div class="empty-message">No messages.</div>';
        bindStudentMessageListActions(list, current.userId);
        renderMessagePagination(api.pageTotal(data), Math.ceil(api.pageTotal(data) / 10));
        await updateMessageCounters(current.userId);
    }

    function studentMessageParams(userId) {
        const params = {
            pageNum: state.messagePage,
            pageSize: 10,
            recipientId: userId,
            recipientType: 2
        };
        if (state.messageFilter.type && state.messageFilter.type !== 'all') params.messageType = state.messageFilter.type;
        if (state.messageFilter.read !== '') params.isRead = state.messageFilter.read;
        if (state.messageFilter.keyword) params.keyword = state.messageFilter.keyword;
        return params;
    }

    function bindStudentMessageListActions(list, userId) {
        list.querySelectorAll('[data-read-message]').forEach((button) => {
            button.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                try {
                    await api.request(`/api/v1/messages/${button.dataset.id}/read`, { method: 'PUT' });
                    notify('success', 'Saved', 'Message read status was updated in the database.');
                    await initStudentMessages();
                } catch (error) {
                    notify('error', 'Operation failed', error.message);
                }
            }, true);
        });
        list.querySelectorAll('[data-delete-message]').forEach((button) => {
            button.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                if (!confirm('Delete this message?')) return;
                try {
                    await api.del(`/api/v1/messages/${button.dataset.id}`);
                    notify('success', 'Deleted', 'Message was removed from the database.');
                    await initStudentMessages();
                } catch (error) {
                    notify('error', 'Delete failed', error.message);
                }
            }, true);
        });
        list.querySelectorAll('.message-card[data-message-id]').forEach((card) => {
            card.addEventListener('click', async (event) => {
                if (event.target.closest('.message-action-btn')) return;
                event.stopImmediatePropagation();
                openMessageDetail(card);
                if (card.classList.contains('unread')) {
                    await api.request(`/api/v1/messages/${card.dataset.messageId}/read`, { method: 'PUT' }).catch(() => null);
                    card.classList.remove('unread');
                    await updateMessageCounters(userId);
                }
            }, true);
        });
    }

    function messageCard(item) {
        const unread = Number(item.isRead) === 0;
        return `
            <div class="message-card ${unread ? 'unread' : ''}" data-message-id="${escape(item.id)}" data-message-type="${escape(item.messageType || 'system')}">
                <div class="message-header">
                    <div class="message-info">
                        <span class="message-category ${escape(item.messageType || 'system')}">${escape(messageTypeText(item.messageType))}</span>
                        <h3 class="message-title">${escape(item.title)}</h3>
                    </div>
                    <span class="message-time">${formatDateTime(item.createdAt)}</span>
                </div>
                <p class="message-summary">${escape(item.content)}</p>
                <div class="message-footer">
                    <span class="message-time">${escape(messageTypeText(item.messageType))}</span>
                    <div class="message-actions-footer">
                        <button class="message-action-btn" data-read-message data-id="${escape(item.id)}" aria-label="Mark read" ${unread ? '' : 'disabled'}><i class="fas ${unread ? 'fa-check' : 'fa-envelope-open'}"></i></button>
                        <button class="message-action-btn" data-delete-message data-id="${escape(item.id)}" aria-label="Delete"><i class="fas fa-trash"></i></button>
                    </div>
                </div>
            </div>
        `;
    }

    function bindStudentMessageControls() {
        const search = document.querySelector('.search-input');
        if (search && !search.dataset.dynamicMessagesBound) {
            search.dataset.dynamicMessagesBound = 'true';
            let timer;
            search.addEventListener('input', (event) => {
                event.stopImmediatePropagation();
                clearTimeout(timer);
                state.messageFilter.keyword = search.value.trim();
                timer = setTimeout(() => initStudentMessages(1), 250);
            }, true);
            search.addEventListener('keydown', (event) => {
                if (event.key === 'Enter') {
                    event.preventDefault();
                    event.stopImmediatePropagation();
                    state.messageFilter.keyword = search.value.trim();
                    initStudentMessages(1);
                }
            }, true);
        }

        document.querySelectorAll('.mark-all-read, .sidebar-actions .sidebar-icon-btn:first-child').forEach((button) => {
            if (button.dataset.dynamicMessagesBound) return;
            button.dataset.dynamicMessagesBound = 'true';
            button.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                await markAllStudentMessagesRead();
            }, true);
        });

        document.querySelectorAll('.sidebar-actions .sidebar-icon-btn:nth-child(2)').forEach((button) => {
            if (button.dataset.dynamicMessagesBound) return;
            button.dataset.dynamicMessagesBound = 'true';
            button.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                await initStudentMessages(1);
            }, true);
        });

        document.querySelectorAll('.category-item').forEach((item, index) => {
            if (item.dataset.dynamicMessagesBound) return;
            item.dataset.dynamicMessagesBound = 'true';
            item.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                document.querySelectorAll('.category-item').forEach((category) => category.classList.remove('active'));
                item.classList.add('active');
                applyMessageCategory(index);
                await initStudentMessages(1);
            }, true);
        });

        const filterPanel = document.querySelector('.filter-panel');
        const filterSelects = filterPanel ? filterPanel.querySelectorAll('.filter-select') : [];
        const filterKeyword = filterPanel?.querySelector('.filter-input');
        const applyButton = filterPanel?.querySelector('.filter-actions .btn-primary');
        const resetButton = filterPanel?.querySelector('.filter-actions .btn-outline-secondary');
        if (applyButton && !applyButton.dataset.dynamicMessagesBound) {
            applyButton.dataset.dynamicMessagesBound = 'true';
            applyButton.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                state.messageFilter.type = normalizeMessageType(filterSelects[0]?.value || 'all');
                state.messageFilter.read = normalizeMessageRead(filterSelects[1]?.value || 'all');
                state.messageFilter.keyword = filterKeyword?.value?.trim() || search?.value?.trim() || '';
                state.messageFilter.deleted = false;
                await initStudentMessages(1);
            }, true);
        }
        if (resetButton && !resetButton.dataset.dynamicMessagesBound) {
            resetButton.dataset.dynamicMessagesBound = 'true';
            resetButton.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                state.messageFilter = { type: 'all', read: '', keyword: '', deleted: false };
                if (search) search.value = '';
                if (filterKeyword) filterKeyword.value = '';
                if (filterSelects[0]) filterSelects[0].value = 'all';
                if (filterSelects[1]) filterSelects[1].value = 'all';
                document.querySelectorAll('.category-item').forEach((category, index) => category.classList.toggle('active', index === 0));
                await initStudentMessages(1);
            }, true);
        }
    }

    function applyMessageCategory(index) {
        const categories = [
            { type: 'all', read: '', deleted: false },
            { type: 'all', read: 0, deleted: false },
            { type: 'system', read: '', deleted: false },
            { type: 'course', read: '', deleted: false },
            { type: 'assignment', read: '', deleted: false },
            { type: 'announcement', read: '', deleted: false },
            { type: 'all', read: '', deleted: true }
        ];
        state.messageFilter = { ...state.messageFilter, ...(categories[index] || categories[0]) };
        updateMessageTitle();
    }

    function updateMessageTitle() {
        const title = document.querySelector('.messages-title');
        if (!title) return;
        title.textContent = state.messageFilter.deleted ? 'Deleted'
            : state.messageFilter.read === 0 ? 'Unread'
                : state.messageFilter.type === 'all' ? 'All messages'
                    : messageTypeText(state.messageFilter.type);
    }

    async function markAllStudentMessagesRead() {
        const current = await currentUser();
        const data = await api.get('/api/v1/messages/list', {
            pageNum: 1,
            pageSize: 100,
            recipientId: current.userId,
            recipientType: 2,
            isRead: 0
        });
        const ids = api.pageItems(data).map((item) => item.id).filter(Boolean);
        if (!ids.length) {
            notify('success', 'No unread messages', 'Current messages are already read.');
            return;
        }
        await Promise.all(ids.map((id) => api.request(`/api/v1/messages/${encodeURIComponent(id)}/read`, { method: 'PUT' })));
        notify('success', 'Saved', `${ids.length} messages were marked read.`);
        await initStudentMessages(1);
    }

    async function updateMessageCounters(userId) {
        const countParams = (extra) => ({ pageNum: 1, pageSize: 1, recipientId: userId, recipientType: 2, ...extra });
        const [all, unread, system, course, assignment, announcement] = await Promise.all([
            api.get('/api/v1/messages/list', countParams({})).catch(() => null),
            api.get('/api/v1/messages/list', countParams({ isRead: 0 })).catch(() => null),
            api.get('/api/v1/messages/list', countParams({ messageType: 'system' })).catch(() => null),
            api.get('/api/v1/messages/list', countParams({ messageType: 'course' })).catch(() => null),
            api.get('/api/v1/messages/list', countParams({ messageType: 'assignment' })).catch(() => null),
            api.get('/api/v1/messages/list', countParams({ messageType: 'announcement' })).catch(() => null)
        ]);
        const values = [all, unread, system, course, assignment, announcement, null].map((item) => api.pageTotal(item));
        document.querySelectorAll('.category-item .category-badge').forEach((badgeElement, index) => {
            badgeElement.textContent = String(values[index] || 0);
        });
        document.querySelectorAll('.nav-badge').forEach((badgeElement) => {
            badgeElement.textContent = String(values[1] || 0);
        });
        updateMessageTitle();
    }

    function renderMessagePagination(total, totalPages) {
        const pager = document.querySelector('.pagination');
        if (!pager) return;
        const pages = Math.max(1, Number(totalPages || 1));
        const current = Math.min(state.messagePage, pages);
        state.messagePage = current;
        if (!total) {
            pager.innerHTML = '';
            return;
        }
        const button = (page, label, disabled) => `<button class="page-btn ${page === current ? 'active' : ''}" data-message-page="${page}" ${disabled ? 'disabled' : ''}>${label}</button>`;
        const middle = Array.from({ length: pages }, (_, index) => index + 1)
            .filter((page) => pages <= 7 || page === 1 || page === pages || Math.abs(page - current) <= 1)
            .map((page, index, all) => {
                const gap = index > 0 && page - all[index - 1] > 1 ? '<span>...</span>' : '';
                return `${gap}${button(page, page, false)}`;
            }).join('');
        pager.innerHTML = `${button(Math.max(1, current - 1), '<i class="fas fa-chevron-left"></i>', current <= 1)}${middle}${button(Math.min(pages, current + 1), '<i class="fas fa-chevron-right"></i>', current >= pages)}`;
        pager.querySelectorAll('[data-message-page]').forEach((buttonElement) => {
            buttonElement.addEventListener('click', async (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                await initStudentMessages(buttonElement.dataset.messagePage);
            }, true);
        });
    }

    function openMessageDetail(card) {
        const modal = document.querySelector('.message-detail-modal-backdrop');
        if (!modal) return;
        const title = card.querySelector('.message-title')?.textContent || '';
        const time = card.querySelector('.message-header .message-time')?.textContent || '';
        const from = card.querySelector('.message-footer .message-time')?.textContent || '';
        const content = card.querySelector('.message-summary')?.textContent || '';
        if (modal.querySelector('.modal-title')) modal.querySelector('.modal-title').textContent = title;
        if (modal.querySelector('.date-time')) modal.querySelector('.date-time').textContent = time;
        if (modal.querySelector('.message-from')) modal.querySelector('.message-from').textContent = from ? ` - ${from}` : '';
        if (modal.querySelector('.message-content')) modal.querySelector('.message-content').textContent = content;
        modal.style.display = 'flex';
        bindMessageDetailClose(modal);
    }

    function bindMessageDetailClose(modal) {
        if (modal.dataset.dynamicMessagesBound) return;
        modal.dataset.dynamicMessagesBound = 'true';
        modal.querySelectorAll('.close-message-detail').forEach((button) => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                modal.style.display = 'none';
            }, true);
        });
        modal.addEventListener('click', (event) => {
            if (event.target === modal) {
                event.stopImmediatePropagation();
                modal.style.display = 'none';
            }
        }, true);
    }

    function normalizeMessageType(raw) {
        return ['system', 'course', 'assignment', 'announcement'].includes(raw) ? raw : 'all';
    }

    function normalizeMessageRead(raw) {
        if (raw === 'read') return 1;
        if (raw === 'unread') return 0;
        return '';
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

    async function initStudentEvaluations() {
        const current = await currentUser();
        const data = await api.get(`/api/v1/evaluations/student/${current.userId}/courses`);
        state.evaluations = Array.isArray(data) ? data : [];
        bindEvaluationControls();
        renderEvaluationStats();
        renderEvaluationCards();
        const form = document.getElementById('evaluation-form');
        if (form && !form.dataset.dynamicEvaluationBound) {
            form.dataset.dynamicEvaluationBound = 'true';
            form.addEventListener('submit', submitEvaluation, true);
        }
    }

    function renderEvaluationStats() {
        const values = document.querySelectorAll('.evaluation-stats .stat-value');
        if (!values.length) return;
        const total = state.evaluations.length;
        const completed = state.evaluations.filter((item) => item.evaluated).length;
        const scores = state.evaluations
            .map((item) => Number(item.evaluationScore))
            .filter((score) => !Number.isNaN(score) && score > 0);
        const average = scores.length ? Math.round(scores.reduce((sum, score) => sum + score, 0) / scores.length * 10) / 10 : 0;
        values[0].textContent = `${completed}/${total}`;
        if (values[1]) values[1].textContent = average || '-';
        if (values[2]) values[2].textContent = '-';
        if (values[3]) values[3].textContent = String(Math.max(0, total - completed));
    }

    function renderEvaluationCards() {
        const container = document.querySelector('.evaluation-cards');
        if (!container) return;
        const rows = filteredEvaluations();
        container.innerHTML = rows.length ? rows.map((item) => {
            const status = item.evaluated ? 'evaluated' : 'not-evaluated';
            return `
                <div class="evaluation-card" data-status="${status}" data-course-type="${escape(item.courseType || '')}">
                    <div class="course-header">
                        <div class="course-info">
                            <h4>${escape(item.courseName)}</h4>
                            <p>${escape(item.courseCode)} | ${escape(item.teacherName || '-')} | ${escape(item.courseType || '-')}</p>
                        </div>
                        <div class="course-status ${status}">${item.evaluated ? '\u5df2\u8bc4\u4ef7' : '\u672a\u8bc4\u4ef7'}</div>
                    </div>
                    ${item.evaluated ? `
                        <div class="evaluation-rating">
                            <div class="rating-stars">${evaluationStars(item.evaluationScore)}</div>
                            <div class="evaluation-date">\u8bc4\u4ef7\u65f6\u95f4: ${escape(formatDate(item.evaluationTime))}</div>
                        </div>
                        <div class="evaluation-content"><p>${escape(item.evaluationContent || '-')}</p></div>
                    ` : ''}
                    <div class="evaluation-actions">
                        <button class="btn btn-sm ${item.evaluated ? 'btn-outline-secondary' : 'btn-primary'} start-evaluation" data-course-id="${escape(item.courseId)}">
                            ${item.evaluated ? '\u4fee\u6539\u8bc4\u4ef7' : '\u5f00\u59cb\u8bc4\u4ef7'}
                        </button>
                    </div>
                </div>
            `;
        }).join('') : '<div class="empty-message">\u6682\u65e0\u7b26\u5408\u6761\u4ef6\u7684\u8bc4\u4ef7\u8bfe\u7a0b</div>';
        container.querySelectorAll('.start-evaluation').forEach((button) => {
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                openEvaluationForm(button.dataset.courseId);
            }, true);
        });
    }

    function filteredEvaluations() {
        const activeTab = document.querySelector('.tab-button.active')?.dataset.tab || 'all';
        const statusFilter = value('status-filter') || 'all';
        const typeFilter = value('course-type-filter') || 'all';
        return state.evaluations.filter((item) => {
            const status = item.evaluated ? 'evaluated' : 'not-evaluated';
            if (activeTab !== 'all' && status !== activeTab) return false;
            if (statusFilter !== 'all' && status !== statusFilter) return false;
            return evaluationCourseTypeMatches(item.courseType, typeFilter);
        });
    }

    function evaluationCourseTypeMatches(courseType, filter) {
        if (!filter || filter === 'all') return true;
        const text = String(courseType || '').toLowerCase();
        const keywords = {
            required: ['required', '\u5fc5\u4fee'],
            elective: ['elective', '\u9009\u4fee'],
            general: ['general', '\u901a\u8bc6'],
            professional: ['professional', '\u4e13\u4e1a']
        };
        return (keywords[filter] || [filter]).some((keyword) => text.includes(keyword));
    }

    function bindEvaluationControls() {
        document.querySelectorAll('.tab-button').forEach((button) => {
            if (button.dataset.dynamicEvaluationBound) return;
            button.dataset.dynamicEvaluationBound = 'true';
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                document.querySelectorAll('.tab-button').forEach((item) => item.classList.remove('active'));
                button.classList.add('active');
                renderEvaluationCards();
            }, true);
        });

        ['status-filter', 'course-type-filter', 'semester-filter'].forEach((id) => {
            const select = document.getElementById(id);
            if (!select || select.dataset.dynamicEvaluationBound) return;
            select.dataset.dynamicEvaluationBound = 'true';
            select.addEventListener('change', () => renderEvaluationCards());
        });

        const searchButton = document.getElementById('search-button');
        if (searchButton && !searchButton.dataset.dynamicEvaluationBound) {
            searchButton.dataset.dynamicEvaluationBound = 'true';
            searchButton.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                renderEvaluationCards();
            }, true);
        }

        const resetButton = document.getElementById('reset-button');
        if (resetButton && !resetButton.dataset.dynamicEvaluationBound) {
            resetButton.dataset.dynamicEvaluationBound = 'true';
            resetButton.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                setValue('semester-filter', 'all');
                setValue('status-filter', 'all');
                setValue('course-type-filter', 'all');
                document.querySelectorAll('.tab-button').forEach((button, index) => button.classList.toggle('active', index === 0));
                renderEvaluationCards();
            }, true);
        }

        ['close-form-button', 'cancel-evaluation'].forEach((id) => {
            const button = document.getElementById(id);
            if (!button || button.dataset.dynamicEvaluationBound) return;
            button.dataset.dynamicEvaluationBound = 'true';
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                closeEvaluationForm();
            }, true);
        });
    }

    function openEvaluationForm(courseId) {
        const item = state.evaluations.find((row) => String(row.courseId) === String(courseId));
        if (!item) return;
        setText('current-course-name', item.courseName);
        const form = document.getElementById('evaluation-form');
        if (form) {
            form.dataset.courseId = item.courseId;
            form.dataset.evaluationId = item.evaluationId || '';
        }
        const content = document.querySelector('textarea[name="evaluation-content"]');
        if (content) content.value = item.evaluationContent || '';
        const anonymous = document.getElementById('anonymous-evaluation');
        if (anonymous) anonymous.checked = Number(item.isAnonymous) === 1 || item.isAnonymous === true;
        updateEvaluationRatingInputs(Number(item.evaluationScore || 0));
        const container = document.getElementById('evaluation-form-container');
        if (container) {
            container.style.display = 'block';
            container.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
        }
    }

    function closeEvaluationForm() {
        const container = document.getElementById('evaluation-form-container');
        if (container) container.style.display = 'none';
        const form = document.getElementById('evaluation-form');
        if (form) {
            form.reset();
            delete form.dataset.courseId;
            delete form.dataset.evaluationId;
        }
        updateEvaluationRatingInputs(0);
    }

    function updateEvaluationRatingInputs(score) {
        document.querySelectorAll('.rating-stars-input').forEach((group) => {
            const stars = group.querySelectorAll('.star');
            const first = stars[0];
            const hiddenId = first?.dataset.category ? `${first.dataset.category}-rating` : 'overall-rating';
            const hidden = document.getElementById(hiddenId);
            const valueText = hiddenId === 'overall-rating' ? Number(score || 0) : 0;
            if (hidden) hidden.value = String(valueText);
            stars.forEach((star) => {
                const active = Number(star.dataset.rating) <= valueText;
                star.innerHTML = `<i class="${active ? 'fas' : 'far'} fa-star"></i>`;
                star.classList.toggle('active', active);
            });
        });
    }

    async function submitEvaluation(event) {
        event.preventDefault();
        event.stopImmediatePropagation();
        const current = await currentUser();
        const form = event.currentTarget;
        const score = Number(value('overall-rating'));
        if (!score) {
            alert('\u8bf7\u81f3\u5c11\u9009\u62e9\u603b\u4f53\u8bc4\u5206');
            return;
        }
        await api.post('/api/v1/evaluations', {
            studentId: current.userId,
            courseId: Number(form.dataset.courseId),
            score,
            content: document.querySelector('textarea[name="evaluation-content"]')?.value || '',
            isAnonymous: document.getElementById('anonymous-evaluation')?.checked,
            status: 1
        });
        notify('success', '\u63d0\u4ea4\u6210\u529f', '\u8bc4\u4ef7\u5df2\u5199\u5165\u6570\u636e\u5e93');
        closeEvaluationForm();
        await initStudentEvaluations();
    }

    function evaluationStars(score) {
        const valueText = Math.max(0, Math.min(5, Math.round(Number(score || 0))));
        return Array.from({ length: 5 }, (_, index) => `<i class="${index < valueText ? 'fas' : 'far'} fa-star"></i>`).join('');
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

    async function initTeacherStatistics() {
        const current = await currentUser();
        bindTeacherStatisticsControls();
        const dashboard = await api.get('/api/v1/course-selections/teacher/dashboard', { teacherId: current.userId });
        state.teacherDashboard = dashboard || {};
        await loadTeacherStatisticsDetails();
        renderTeacherStatistics();
    }

    async function loadTeacherStatisticsDetails() {
        const courses = state.teacherDashboard?.courses || [];
        const selections = {};
        const evaluations = {};
        await Promise.all(courses.map(async (course) => {
            const courseId = course.id;
            const [selectionPage, evaluationPage] = await Promise.all([
                api.get(`/api/v1/course-selections/course/${encodeURIComponent(courseId)}`, {
                    pageNum: 1,
                    pageSize: 1000,
                    status: 1,
                    orderByColumn: 'id',
                    isAsc: true
                }).catch(() => null),
                api.get('/api/v1/evaluations/list', {
                    pageNum: 1,
                    pageSize: 100,
                    courseId,
                    status: 1,
                    orderByColumn: 'evaluationTime',
                    isAsc: false
                }).catch(() => null)
            ]);
            selections[courseId] = api.pageItems(selectionPage);
            evaluations[courseId] = api.pageItems(evaluationPage);
        }));
        state.teacherCourseSelections = selections;
        state.teacherCourseEvaluations = evaluations;
    }

    function renderTeacherStatistics() {
        const courseSummaries = teacherCourseSummaries();
        renderTeacherCourseSelect(courseSummaries);
        const selectedCourse = value('course-select') || 'all';
        const visible = selectedCourse === 'all'
            ? courseSummaries
            : courseSummaries.filter((item) => String(item.courseId) === String(selectedCourse));
        renderTeacherStatisticsCards(visible);
        renderTeacherStatisticsTable(visible);
        renderTeacherStatisticsCharts(visible);
    }

    function teacherCourseSummaries() {
        const courses = state.teacherDashboard?.courses || [];
        return courses.map((course) => {
            const selections = state.teacherCourseSelections?.[course.id] || [];
            const evaluations = state.teacherCourseEvaluations?.[course.id] || [];
            const scores = selections.map((row) => Number(row.score)).filter((score) => !Number.isNaN(score));
            const evaluationScores = evaluations.map((row) => Number(row.score)).filter((score) => !Number.isNaN(score));
            return {
                courseId: course.id,
                courseCode: course.courseCode || course.id,
                courseName: course.courseName || '-',
                studentCount: selections.length,
                averageScore: average(scores),
                excellentRate: percent(scores.filter((score) => score >= 90).length, scores.length),
                passRate: percent(scores.filter((score) => score >= 60).length, scores.length),
                attendanceRate: 100,
                evaluationScore: average(evaluationScores),
                scores
            };
        });
    }

    function renderTeacherStatisticsCards(rows) {
        const cards = document.querySelectorAll('.stats-container .stat-info h3');
        if (!cards.length) return;
        const scores = rows.flatMap((row) => row.scores);
        const studentCount = rows.reduce((sum, row) => sum + row.studentCount, 0);
        const evaluationScores = rows.map((row) => row.evaluationScore).filter((score) => score !== '-');
        const values = [
            rows.length,
            studentCount,
            scores.length,
            evaluationScores.length ? average(evaluationScores) : '-'
        ];
        cards.forEach((card, index) => {
            card.textContent = values[index] ?? '-';
        });
    }

    function renderTeacherCourseSelect(rows) {
        const select = document.getElementById('course-select');
        if (!select || select.dataset.dynamicOptionsLoaded) return;
        const current = select.value || 'all';
        select.innerHTML = '<option value="all">All courses</option>' + rows.map((course) => (
            `<option value="${escape(course.courseId)}">${escape(course.courseName)} (${escape(course.courseCode)})</option>`
        )).join('');
        select.value = rows.some((row) => String(row.courseId) === String(current)) ? current : 'all';
        select.dataset.dynamicOptionsLoaded = 'true';
    }

    function renderTeacherStatisticsTable(rows) {
        const tbody = document.querySelector('.data-table tbody');
        if (!tbody) return;
        tbody.innerHTML = rows.length ? rows.map((row) => `
            <tr>
                <td>${escape(row.courseCode)}</td>
                <td>${escape(row.courseName)}</td>
                <td>${escape(row.studentCount)}</td>
                <td>${escape(row.averageScore)}</td>
                <td>${escape(row.excellentRate)}%</td>
                <td>${escape(row.passRate)}%</td>
                <td>${escape(row.attendanceRate)}%</td>
                <td>${escape(row.evaluationScore === '-' ? '-' : `${row.evaluationScore}/5.0`)}</td>
            </tr>
        `).join('') : rowMessage(8, 'No course statistics.');
    }

    function bindTeacherStatisticsControls() {
        const select = document.getElementById('course-select');
        if (select && !select.dataset.dynamicStatsBound) {
            select.dataset.dynamicStatsBound = 'true';
            select.addEventListener('change', (event) => {
                event.stopImmediatePropagation();
                renderTeacherStatistics();
            }, true);
        }
        document.querySelectorAll('#semester-select, #start-date, #end-date').forEach((item) => {
            if (item.dataset.dynamicStatsBound) return;
            item.dataset.dynamicStatsBound = 'true';
            item.addEventListener('change', (event) => {
                event.stopImmediatePropagation();
                renderTeacherStatistics();
            }, true);
        });
        const exportButton = document.getElementById('btn-export');
        if (exportButton && !exportButton.dataset.dynamicStatsBound) {
            exportButton.dataset.dynamicStatsBound = 'true';
            exportButton.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                exportTeacherStatistics();
            }, true);
        }
        document.querySelectorAll('.chart-selector .selector-btn').forEach((button) => {
            if (button.dataset.dynamicStatsBound) return;
            button.dataset.dynamicStatsBound = 'true';
            button.addEventListener('click', (event) => {
                event.preventDefault();
                event.stopImmediatePropagation();
                document.querySelectorAll('.chart-selector .selector-btn').forEach((item) => item.classList.remove('active'));
                button.classList.add('active');
                state.teacherScoreChartType = button.dataset.chart || 'score-distribution';
                renderTeacherStatistics();
            }, true);
        });
    }

    function exportTeacherStatistics() {
        const selectedCourse = value('course-select') || 'all';
        const rows = teacherCourseSummaries().filter((row) => selectedCourse === 'all' || String(row.courseId) === String(selectedCourse));
        const csvRows = [
            ['courseCode', 'courseName', 'studentCount', 'averageScore', 'excellentRate', 'passRate', 'attendanceRate', 'evaluationScore'],
            ...rows.map((row) => [row.courseCode, row.courseName, row.studentCount, row.averageScore, `${row.excellentRate}%`, `${row.passRate}%`, `${row.attendanceRate}%`, row.evaluationScore])
        ];
        downloadCsv('teacher-statistics.csv', csvRows);
    }

    function renderTeacherStatisticsCharts(rows) {
        if (!window.Chart) {
            if (state.teacherChartRetryCount < 10) {
                state.teacherChartRetryCount += 1;
                setTimeout(() => renderTeacherStatisticsCharts(rows), 200);
            }
            return;
        }
        state.teacherChartRetryCount = 0;
        renderTeacherScoreChart(rows);
        renderTeacherAttendanceChart(rows);
        renderTeacherRatingChart(rows);
        renderTeacherComparisonChart(rows);
    }

    function renderTeacherScoreChart(rows) {
        const labels = state.teacherScoreChartType === 'score-range'
            ? ['90-100', '80-89', '70-79', '60-69', '<60']
            : ['Excellent', 'Good', 'Average', 'Pass', 'Fail'];
        const distribution = scoreDistribution(rows.flatMap((row) => row.scores));
        createOrReplaceChart('scoreDistributionChart', state.teacherScoreChartType === 'score-range' ? 'bar' : 'pie', {
            labels,
            datasets: [{
                data: distribution,
                backgroundColor: ['#43a047', '#1e88e5', '#fb8c00', '#fdd835', '#e53935']
            }]
        }, { responsive: true, plugins: { legend: { position: 'bottom' } } });
    }

    function renderTeacherAttendanceChart(rows) {
        createOrReplaceChart('attendanceChart', 'line', {
            labels: rows.map((row) => row.courseName),
            datasets: [{
                label: 'Attendance rate',
                data: rows.map((row) => row.attendanceRate),
                borderColor: '#1e88e5',
                backgroundColor: 'rgba(30, 136, 229, 0.12)',
                tension: 0.3,
                fill: true
            }]
        }, { responsive: true, scales: { y: { min: 0, max: 100 } } });
    }

    function renderTeacherRatingChart(rows) {
        createOrReplaceChart('ratingTrendChart', 'bar', {
            labels: rows.map((row) => row.courseName),
            datasets: [{
                label: 'Evaluation score',
                data: rows.map((row) => row.evaluationScore === '-' ? 0 : row.evaluationScore),
                backgroundColor: '#43a047'
            }]
        }, { responsive: true, scales: { y: { min: 0, max: 5 } } });
    }

    function renderTeacherComparisonChart(rows) {
        createOrReplaceChart('gradeComparisonChart', 'bar', {
            labels: rows.map((row) => row.courseName),
            datasets: [{
                label: 'Average score',
                data: rows.map((row) => row.averageScore === '-' ? 0 : row.averageScore),
                backgroundColor: '#fb8c00'
            }]
        }, { responsive: true, scales: { y: { min: 0, max: 100 } } });
    }

    function createOrReplaceChart(canvasId, type, data, options) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || !window.Chart) return;
        const existing = typeof window.Chart.getChart === 'function' ? window.Chart.getChart(canvas) : null;
        if (existing) existing.destroy();
        new window.Chart(canvas.getContext('2d'), { type, data, options });
    }

    function scoreDistribution(scores) {
        return [
            scores.filter((score) => score >= 90).length,
            scores.filter((score) => score >= 80 && score < 90).length,
            scores.filter((score) => score >= 70 && score < 80).length,
            scores.filter((score) => score >= 60 && score < 70).length,
            scores.filter((score) => score < 60).length
        ];
    }

    function average(values) {
        const rows = values.map(Number).filter((valueText) => !Number.isNaN(valueText));
        if (!rows.length) return '-';
        return Math.round(rows.reduce((sum, valueText) => sum + valueText, 0) / rows.length * 10) / 10;
    }

    function percent(part, total) {
        return total ? Math.round(part / total * 1000) / 10 : 0;
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
