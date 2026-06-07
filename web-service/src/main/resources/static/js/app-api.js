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

        const response = await fetch(path, {
            method,
            headers,
            body,
            credentials: 'same-origin'
        });

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

    window.AppApi = {
        request,
        get,
        post,
        del,
        escapeHtml,
        formatDate,
        notify,
        pageItems,
        toQuery
    };
})();
