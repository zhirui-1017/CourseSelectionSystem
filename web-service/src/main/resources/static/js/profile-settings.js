(function () {
    const api = window.AppApi;
    if (!api) {
        return;
    }

    const configByRole = {
        student: {
            resource: 'students',
            idField: 'studentNo',
            idLabel: '学号',
            roleText: '学生',
            editableFields: ['name', 'gender', 'phone', 'email']
        },
        teacher: {
            resource: 'teachers',
            idField: 'teacherNo',
            idLabel: '工号',
            roleText: '教师',
            editableFields: ['name', 'gender', 'phone', 'email', 'title']
        }
    };

    const state = {
        role: document.body.dataset.profileRole,
        current: null,
        profile: null
    };

    document.addEventListener('DOMContentLoaded', async () => {
        try {
            await boot();
        } catch (error) {
            api.notify('error', '加载失败', error.message || '个人资料加载失败');
        }
    });

    async function boot() {
        const config = currentConfig();
        state.current = await api.get('/login/current');
        if (!state.current || state.current.role !== state.role) {
            window.location.href = '/login';
            return;
        }

        await loadProfile();
        applyProfile();
        bindProfileForm(config);
        bindPasswordForm(config);
        bindHashFocus();
    }

    async function loadProfile() {
        const config = currentConfig();
        const id = currentUserId();
        if (id === undefined || id === null || id === '') {
            throw new Error('无法读取当前登录用户ID');
        }
        state.profile = await api.get(`/api/v1/${config.resource}/${encodeURIComponent(id)}`);
    }

    function applyProfile() {
        const profile = state.profile || {};
        const config = currentConfig();
        const displayName = profile.name || profile[config.idField] || state.current?.username || config.roleText;
        const identityNo = profile[config.idField] || state.current?.username || '-';
        const subtitle = state.role === 'teacher'
            ? [profile.title, profile.departmentId ? `系部 ${profile.departmentId}` : null].filter(Boolean).join(' / ')
            : [profile.className, profile.majorId ? `专业 ${profile.majorId}` : null].filter(Boolean).join(' / ');

        setAll('[data-profile-name], [data-user-name]', displayName);
        setAll('[data-profile-role-text], [data-user-role]', subtitle || config.roleText);
        setAll('[data-profile-avatar]', avatarText(displayName));
        setAll('[data-profile-id-label]', config.idLabel);
        setAll('[data-profile-id-value]', identityNo);
        setAll('[data-profile-status]', profile.status === 0 ? '停用' : '正常');

        setValue('idNo', identityNo);
        setValue('name', profile.name || '');
        setValue('gender', profile.gender || '男');
        setValue('phone', profile.phone || '');
        setValue('email', profile.email || '');
        setValue('title', profile.title || '');

        renderReadOnly(profile, config);
    }

    function renderReadOnly(profile, config) {
        const target = document.querySelector('[data-profile-readonly]');
        if (!target) {
            return;
        }
        const rows = state.role === 'teacher'
            ? [
                [config.idLabel, profile.teacherNo],
                ['姓名', profile.name],
                ['性别', profile.gender],
                ['职称', profile.title],
                ['系部ID', profile.departmentId],
                ['邮箱', profile.email]
            ]
            : [
                [config.idLabel, profile.studentNo],
                ['姓名', profile.name],
                ['性别', profile.gender],
                ['班级', profile.className],
                ['专业ID', profile.majorId],
                ['邮箱', profile.email]
            ];
        target.innerHTML = rows.map(([label, value]) => `
            <div class="profile-read-item">
                <span class="profile-read-label">${api.escapeHtml(label)}</span>
                <span class="profile-read-value">${api.escapeHtml(value || '-')}</span>
            </div>
        `).join('');
    }

    function bindProfileForm(config) {
        const form = document.querySelector('[data-profile-form]');
        if (!form) {
            return;
        }
        form.addEventListener('reset', () => {
            window.setTimeout(applyProfile, 0);
        });
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const body = {};
            const formData = new FormData(form);
            config.editableFields.forEach((field) => {
                if (formData.has(field)) {
                    body[field] = String(formData.get(field) || '').trim();
                }
            });
            if (!body.name) {
                api.notify('error', '保存失败', '姓名不能为空');
                return;
            }

            const button = form.querySelector('[type="submit"]');
            setBusy(button, true, '保存中');
            try {
                const id = currentUserId();
                await api.request(`/api/v1/${config.resource}/${encodeURIComponent(id)}/from-map`, {
                    method: 'PUT',
                    body
                });
                await loadProfile();
                applyProfile();
                api.notify('success', '保存成功', '个人资料已更新');
            } catch (error) {
                api.notify('error', '保存失败', error.message);
            } finally {
                setBusy(button, false);
            }
        });
    }

    function bindPasswordForm(config) {
        const form = document.querySelector('[data-password-form]');
        if (!form) {
            return;
        }
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            const oldPassword = form.querySelector('[name="oldPassword"]')?.value || '';
            const newPassword = form.querySelector('[name="newPassword"]')?.value || '';
            const confirmPassword = form.querySelector('[name="confirmPassword"]')?.value || '';

            if (newPassword !== confirmPassword) {
                api.notify('error', '修改失败', '两次输入的新密码不一致');
                return;
            }
            if (newPassword.length < 6) {
                api.notify('error', '修改失败', '新密码至少需要6位');
                return;
            }
            if (oldPassword === newPassword) {
                api.notify('warning', '无需修改', '新密码不能与当前密码相同');
                return;
            }

            const button = form.querySelector('[type="submit"]');
            setBusy(button, true, '修改中');
            try {
                const id = currentUserId();
                await api.request(`/api/v1/${config.resource}/${encodeURIComponent(id)}/change-password${api.toQuery({ oldPassword, newPassword })}`, {
                    method: 'PUT'
                });
                form.reset();
                api.notify('success', '修改成功', '下次登录请使用新密码');
            } catch (error) {
                api.notify('error', '修改失败', error.message);
            } finally {
                setBusy(button, false);
            }
        });
    }

    function bindHashFocus() {
        if (window.location.hash !== '#security') {
            return;
        }
        const panel = document.getElementById('security');
        panel?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        panel?.querySelector('input')?.focus();
    }

    function currentConfig() {
        const config = configByRole[state.role];
        if (!config) {
            throw new Error('未知个人资料页面类型');
        }
        return config;
    }

    function currentUserId() {
        return state.current?.userId ?? state.current?.user?.id;
    }

    function setValue(name, value) {
        document.querySelectorAll(`[name="${name}"]`).forEach((element) => {
            element.value = value;
        });
    }

    function setAll(selector, value) {
        document.querySelectorAll(selector).forEach((element) => {
            element.textContent = value;
        });
    }

    function avatarText(value) {
        const text = String(value || '').trim();
        return text ? text.slice(0, 1) : '用';
    }

    function setBusy(button, busy, text) {
        if (!button) {
            return;
        }
        if (busy) {
            button.dataset.originalHtml = button.innerHTML;
            button.disabled = true;
            button.textContent = text || '处理中';
        } else {
            button.disabled = false;
            button.innerHTML = button.dataset.originalHtml || button.innerHTML;
        }
    }
})();
