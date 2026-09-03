(function () {
    function toQuery(params) {
        const search = new URLSearchParams();
        Object.entries(params || {}).forEach(([key, value]) => {
            if (value !== undefined && value !== null && value !== '') {
                search.set(key, value);
            }
        });
        const text = search.toString();
        return text ? `?${text}` : '';
    }

    async function request(path, options = {}) {
        const method = options.method || 'GET';
        const headers = options.headers ? { ...options.headers } : {};
        let body = options.body;

        if (body && !(body instanceof FormData) && typeof body !== 'string') {
            headers['Content-Type'] = 'application/json';
            body = JSON.stringify(body);
        }

        // === JWT Token 自动携带 ===
        const token = localStorage.getItem('token');
        if (token) {
            headers['Authorization'] = 'Bearer ' + token;
        }

        const response = await fetch(path, {
            method,
            headers,
            body,
            credentials: 'same-origin'
        });

        // 401 未授权 → 清除 Token 并跳转登录页
        if (response.status === 401) {
            localStorage.removeItem('token');
            localStorage.removeItem('userInfo');
            const currentPath = window.location.pathname;
            if (currentPath !== '/login.html' && currentPath !== '/login') {
                window.location.href = '/login.html';
            }
            throw new Error('未登录或登录已过期');
        }

        const contentType = response.headers.get('content-type') || '';
        const payload = contentType.includes('application/json') ? await response.json() : await response.text();

        if (!response.ok) {
            throw new Error(typeof payload === 'string' ? payload : (payload.message || `HTTP ${response.status}`));
        }
        if (payload && typeof payload === 'object' && Object.prototype.hasOwnProperty.call(payload, 'success')) {
            if (!payload.success) {
                throw new Error(payload.message || '操作失败');
            }
            return payload.data;
        }
        return payload;
    }

    function get(path, params) {
        return request(`${path}${toQuery(params)}`);
    }

    function post(path, body, params) {
        return request(`${path}${toQuery(params)}`, { method: 'POST', body });
    }

    function put(path, body, params) {
        return request(`${path}${toQuery(params)}`, { method: 'PUT', body });
    }

    function del(path, params) {
        return request(`${path}${toQuery(params)}`, { method: 'DELETE' });
    }

    function escapeHtml(value) {
        return String(value ?? '').replace(/[&<>"']/g, (ch) => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            '"': '&quot;',
            "'": '&#39;'
        }[ch]));
    }

    function formatDate(value) {
        if (!value) {
            return '-';
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return String(value);
        }
        return date.toLocaleDateString('zh-CN');
    }

    function notify(type, title, message) {
        if (window.notifications && typeof window.notifications[type] === 'function') {
            window.notifications[type](title, message);
            return;
        }
        if (type === 'error') {
            alert(message || title);
        }
    }

    function pageItems(pageLike) {
        if (!pageLike) {
            return [];
        }
        if (Array.isArray(pageLike)) {
            return pageLike;
        }
        return pageLike.items || pageLike.content || pageLike.records || [];
    }

    function pageTotal(pageLike) {
        if (!pageLike) {
            return 0;
        }
        if (Array.isArray(pageLike)) {
            return pageLike.length;
        }
        return pageLike.total ?? pageLike.totalElements ?? pageLike.totalCount ?? pageItems(pageLike).length;
    }

    document.documentElement.dataset.appApi = 'ready';

    window.AppApi = {
        request,
        get,
        post,
        put,
        del,
        escapeHtml,
        formatDate,
        notify,
        pageItems,
        pageTotal,
        toQuery
    };
})();
