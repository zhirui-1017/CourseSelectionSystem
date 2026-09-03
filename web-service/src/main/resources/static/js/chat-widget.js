/*
 * AI 智能助手 — 浮动对话面板
 * v3.0 — SSE 流式对话 + Markdown 渲染 + 会话历史管理
 */

(function () {
  'use strict';

  if (window.__aiChatWidgetInitialized) return;
  window.__aiChatWidgetInitialized = true;

  // ========== 配置 ==========
  var CONTEXT_PATH = ''; // 微服务网关挂载于根路径 /
  var CHAT_STREAM_API = CONTEXT_PATH + '/api/v1/ai/chat/stream';
  var SESSIONS_API = CONTEXT_PATH + '/api/v1/ai/sessions';
  var MARKED_CDN = CONTEXT_PATH + '/lib/marked/15.0.0/marked.min.js';

  // ========== 状态 ==========
  var isOpen = false;
  var isMaximized = false;
  var isThinking = false;
  var isHistoryOpen = false;
  var savedWidth = 480;
  var savedHeight = 680;
  var currentSessionId = null;    // 当前会话 UID
  var sessionList = [];           // 会话列表缓存
  var allSessionsLoaded = false;  // 是否已加载过会话列表

  // ========== 角色化快捷提示词 ==========
  function getQuickHints() {
    var path = window.location.pathname;
    if (path.indexOf('/student/') !== -1) {
      return [
        '📚 帮我推荐3门选修课',
        '📊 分析我的GPA',
        '📅 查看我的课表',
        '⭐ 课程评价最高的课'
      ];
    } else if (path.indexOf('/teacher/') !== -1) {
      return [
        '📊 查看我的课程学生成绩',
        '👥 分析班级成绩分布',
        '⚠️ 检测异常情况',
        '📝 帮我写课程公告'
      ];
    } else {
      return [
        '📈 系统运营数据概览',
        '🔥 热门课程TOP5',
        '⚠️ 检测系统异常',
        '📋 查看最近操作日志'
      ];
    }
  }

  // ========== DOM 结构 ==========
  function createWidget() {
    var container = document.createElement('div');
    container.id = 'ai-chat-container';
    container.innerHTML =
      // 历史记录侧边栏
      '<div id="ai-history-sidebar" class="ai-history-sidebar hidden">' +
      '  <div class="ai-history-header">' +
      '    <span>📋 对话历史</span>' +
      '    <button id="ai-history-close" class="ai-history-close-btn" title="关闭侧栏">&times;</button>' +
      '  </div>' +
      '  <div id="ai-history-list" class="ai-history-list">' +
      '    <div class="ai-history-empty">暂无历史会话</div>' +
      '  </div>' +
      '</div>' +

      // 浮动按钮
      '<button id="ai-chat-fab" class="ai-chat-fab" title="AI 智能助手 — 点击打开">💬</button>' +

      // 对话面板
      '<div id="ai-chat-panel" class="ai-chat-panel hidden">' +
      '  <div class="ai-chat-header" id="ai-chat-drag-handle">' +
      '    <span class="ai-chat-header-title">🤖 AI 智能助手</span>' +
      '    <div style="display:flex;gap:4px;align-items:center">' +
      '      <button id="ai-chat-history" class="ai-chat-header-action-btn" title="对话历史">📋</button>' +
      '      <button id="ai-chat-new" class="ai-chat-header-action-btn" title="新建会话">＋</button>' +
      '      <button id="ai-chat-clear" class="ai-chat-header-action-btn" title="清空当前会话">🗑</button>' +
      '      <button id="ai-chat-maximize" class="ai-chat-header-btn" title="放大窗口">🔲</button>' +
      '      <button id="ai-chat-close" class="ai-chat-header-btn" title="关闭">&times;</button>' +
      '    </div>' +
      '  </div>' +
      '  <div id="ai-chat-messages" class="ai-chat-messages">' +
      '  </div>' +
      '  <div id="ai-chat-hints" class="ai-chat-quick-hints"></div>' +
      '  <div class="ai-chat-input-area">' +
      '    <input id="ai-chat-input" class="ai-chat-input" type="text" placeholder="输入你的问题..." autocomplete="off">' +
      '    <button id="ai-chat-send" class="ai-chat-send">➤</button>' +
      '  </div>' +
      '  <div id="ai-chat-resize-handle" class="ai-chat-resize-handle" title="拖拽调整大小"></div>' +
      '</div>';

    // 历史侧栏遮罩
    var overlay = document.createElement('div');
    overlay.id = 'ai-history-overlay';
    overlay.className = 'ai-history-overlay hidden';
    container.appendChild(overlay);

    document.body.appendChild(container);
  }

  // ========== 工具函数 ==========
  /** 从 AI 回复中解析追问建议与干净文本 */
  function parseSuggestions(text) {
    var suggestions = [];
    var cleanText = text;
    var regex = /<!--SUGGESTIONS-->\s*([\s\S]*?)\s*<!--\/SUGGESTIONS-->/;
    var match = text.match(regex);
    if (match) {
      cleanText = text.replace(regex, '').trim();
      try {
        var parsed = JSON.parse(match[1].trim());
        if (parsed.suggestions && Array.isArray(parsed.suggestions)) {
          suggestions = parsed.suggestions.filter(function(s) { return s && s.trim(); });
        }
      } catch (e) {
        // LLM 输出格式错误时静默忽略
      }
    }
    return { cleanText: cleanText, suggestions: suggestions };
  }

  /** 后端未返回追问建议时的前端兜底策略 */
  function getFallbackSuggestions(userMessage) {
    var role = detectRole();
    var msg = (userMessage || '').toLowerCase();

    // 按用户消息关键词匹配
    if (role === 'student') {
      if (msg.indexOf('成绩') !== -1 || msg.indexOf('gpa') !== -1 || msg.indexOf('绩点') !== -1) {
        return ['我的薄弱科目有哪些？', '帮我推荐能提升绩点的选修课', '查看我的课表安排'];
      }
      if (msg.indexOf('推荐') !== -1 || msg.indexOf('选课') !== -1 || msg.indexOf('选修') !== -1) {
        return ['这些课程的评分怎么样？', '查看我的课表看有没有时间冲突', '分析我目前的GPA情况'];
      }
      if (msg.indexOf('课表') !== -1 || msg.indexOf('课程安排') !== -1) {
        return ['帮我推荐适合的选修课', '分析我的各科成绩', '本学期有什么实验课？'];
      }
      if (msg.indexOf('评价') !== -1 || msg.indexOf('评分') !== -1) {
        return ['帮我搜索同类型的其他课程', '推荐3门热门选修课', '查看我的GPA分析'];
      }
      if (msg.indexOf('搜索') !== -1 || msg.indexOf('查找') !== -1 || msg.indexOf('有什么') !== -1) {
        return ['哪些课程还有空余名额？', '推荐学分高且好评多的课', '分析我的薄弱科目'];
      }
    }
    if (role === 'teacher') {
      if (msg.indexOf('成绩') !== -1 || msg.indexOf('分数') !== -1 || msg.indexOf('分布') !== -1) {
        return ['查看我所有课程的学生名单', '有没有成绩异常的学生？', '各班级平均分对比'];
      }
      if (msg.indexOf('学生') !== -1 || msg.indexOf('学号') !== -1) {
        return ['查看该学生的所有课程成绩', '我教的课程选课情况如何？', '各班级成绩对比分析'];
      }
      if (msg.indexOf('课') !== -1 || msg.indexOf('选课') !== -1) {
        return ['查看班级成绩分布', '查询某位学生的成绩', '统计各分数段人数'];
      }
    }
    if (role === 'admin') {
      if (msg.indexOf('统计') !== -1 || msg.indexOf('数据') !== -1 || msg.indexOf('概况') !== -1) {
        return ['检测系统异常情况', '查看最近操作日志', '当前学期信息'];
      }
      if (msg.indexOf('异常') !== -1 || msg.indexOf('检测') !== -1) {
        return ['查看系统运营统计', '查看热门课程排行', '查询当前学期'];
      }
    }

    // 通用兜底 — 根据角色返回默认追问
    if (role === 'student') return ['帮我推荐几门选修课', '分析我的GPA情况', '我的薄弱科目有哪些？'];
    if (role === 'teacher') return ['查看我所授课程', '班级成绩分析', '查询某位学生成绩'];
    if (role === 'admin')  return ['查看系统运营统计', '检测异常情况', '热门课程 TOP5'];
    return ['还有什么可以帮你的？'];
  }

  function formatTime() {
    var now = new Date();
    var h = now.getHours();
    var m = now.getMinutes();
    return (h < 10 ? '0' + h : h) + ':' + (m < 10 ? '0' + m : m);
  }

  function formatDate(dtStr) {
    if (!dtStr) return '';
    var d = new Date(dtStr);
    var now = new Date();
    var month = d.getMonth() + 1;
    var day = d.getDate();
    var h = d.getHours();
    var min = d.getMinutes();
    var pad = function(n) { return n < 10 ? '0' + n : n; };
    var dateStr = month + '/' + day;
    // 如果是今天，只显示时间
    if (d.toDateString() === now.toDateString()) {
      return pad(h) + ':' + pad(min);
    }
    return dateStr + ' ' + pad(h) + ':' + pad(min);
  }

  function scrollToBottom() {
    var messages = document.getElementById('ai-chat-messages');
    if (messages) messages.scrollTop = messages.scrollHeight;
  }

  function assistantSvgAvatar() {
    return '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="18" height="18" rx="3"/><circle cx="9" cy="9" r="2"/><circle cx="15" cy="9" r="2"/><path d="M8 15c1 2 3 3 4 3s3-1 4-3"/></svg>';
  }

  function userSvgAvatar() {
    return '<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M5 21c0-4 3-7 7-7s7 3 7 7"/></svg>';
  }

  // ========== 消息渲染 ==========

  function addMessageRow(role, content, timestamp, skipSuggestions) {
    var messages = document.getElementById('ai-chat-messages');
    var isAssistant = role === 'assistant';

    // 解析追问建议
    var parsed = { cleanText: content, suggestions: [] };
    if (isAssistant && !skipSuggestions) {
      parsed = parseSuggestions(content);
    }

    var row = document.createElement('div');
    row.className = 'ai-chat-msg-row ' + role;

    // 头像
    var avatar = document.createElement('div');
    avatar.className = 'ai-chat-avatar ' + (isAssistant ? 'ai-chat-avatar-assistant' : 'ai-chat-avatar-user');
    avatar.innerHTML = isAssistant ? assistantSvgAvatar() : userSvgAvatar();

    // 消息体
    var body = document.createElement('div');
    body.className = 'ai-chat-msg-body';

    // 气泡包装器（用于放置复制按钮）
    var bubbleWrapper = document.createElement('div');
    bubbleWrapper.className = 'ai-chat-bubble-wrapper';

    var bubble = document.createElement('div');
    bubble.className = 'ai-chat-bubble ' + role;

    // 助手消息尝试 Markdown 渲染
    if (isAssistant && typeof marked !== 'undefined' && marked.parse) {
      try {
        bubble.innerHTML = marked.parse(parsed.cleanText);
        bubble.classList.add('markdown-body');
      } catch (e) {
        bubble.textContent = parsed.cleanText;
      }
    } else {
      bubble.textContent = parsed.cleanText;
    }

    bubbleWrapper.appendChild(bubble);

    // 助手气泡的复制按钮
    if (isAssistant) {
      var copyBtn = createCopyButton(parsed.cleanText);
      bubbleWrapper.appendChild(copyBtn);
    }

    var timeEl = document.createElement('div');
    timeEl.className = 'ai-chat-time';
    timeEl.textContent = timestamp || formatTime();

    if (isAssistant) {
      body.appendChild(bubbleWrapper);
      body.appendChild(timeEl);
      row.appendChild(avatar);
      row.appendChild(body);
    } else {
      body.appendChild(bubbleWrapper);
      body.appendChild(timeEl);
      row.appendChild(body);
      row.appendChild(avatar);
    }

    // 追加追问按钮
    if (isAssistant && parsed.suggestions.length > 0) {
      var sugDiv = document.createElement('div');
      sugDiv.className = 'ai-chat-suggestions';
      parsed.suggestions.forEach(function(sug) {
        var btn = document.createElement('button');
        btn.className = 'ai-chat-suggestion-btn';
        btn.textContent = sug;
        btn.addEventListener('click', function() {
          document.getElementById('ai-chat-input').value = sug;
          sendMessage();
        });
        sugDiv.appendChild(btn);
      });
      body.appendChild(sugDiv);
    }

    messages.appendChild(row);
    scrollToBottom();
    return row;
  }

  /** 创建复制按钮 */
  function createCopyButton(content) {
    var btn = document.createElement('button');
    btn.className = 'ai-chat-copy-btn';
    btn.innerHTML = '📋';
    btn.title = '复制内容';
    btn.setAttribute('data-copy-text', content || '');
    btn.addEventListener('click', function(e) {
      e.stopPropagation();
      var textToCopy = btn.getAttribute('data-copy-text') || '';
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(textToCopy).then(function() {
          showCopyFeedback(btn);
        }).catch(function() {
          fallbackCopy(textToCopy, btn);
        });
      } else {
        fallbackCopy(textToCopy, btn);
      }
    });
    return btn;
  }

  function fallbackCopy(text, btn) {
    var ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    ta.style.top = '-9999px';
    document.body.appendChild(ta);
    ta.focus();
    ta.select();
    try {
      document.execCommand('copy');
      showCopyFeedback(btn);
    } catch (e) {}
    document.body.removeChild(ta);
  }

  function showCopyFeedback(btn) {
    btn.classList.add('copied');
    btn.innerHTML = '✓';
    btn.title = '已复制';
    setTimeout(function() {
      btn.classList.remove('copied');
      btn.innerHTML = '📋';
      btn.title = '复制内容';
    }, 1500);
  }

  function addUserMessage(text) {
    addMessageRow('user', text, formatTime());
  }

  function addAssistantText(text) {
    var row = addMessageRow('assistant', text, formatTime());
    return row;
  }

  function createStreamingBubble() {
    var messages = document.getElementById('ai-chat-messages');
    var row = document.createElement('div');
    row.className = 'ai-chat-msg-row assistant';
    row.id = 'ai-streaming-row';

    var avatar = document.createElement('div');
    avatar.className = 'ai-chat-avatar ai-chat-avatar-assistant';
    avatar.innerHTML = assistantSvgAvatar();

    var body = document.createElement('div');
    body.className = 'ai-chat-msg-body';

    var bubbleWrapper = document.createElement('div');
    bubbleWrapper.className = 'ai-chat-bubble-wrapper';

    var bubble = document.createElement('div');
    bubble.className = 'ai-chat-bubble assistant markdown-body';

    bubbleWrapper.appendChild(bubble);

    // 流式过程中隐藏复制按钮，完成后追加
    var copyBtn = createCopyButton('');
    copyBtn.style.display = 'none';
    copyBtn.id = 'ai-streaming-copy-btn';
    bubbleWrapper.appendChild(copyBtn);

    var timeEl = document.createElement('div');
    timeEl.className = 'ai-chat-time';
    timeEl.textContent = formatTime();

    body.appendChild(bubbleWrapper);
    body.appendChild(timeEl);
    row.appendChild(avatar);
    row.appendChild(body);
    messages.appendChild(row);

    return { bubble: bubble, row: row, body: body, copyBtn: copyBtn };
  }

  function finalizeStream(streamObj, fullContent, userMessage) {
    if (!streamObj) return;
    streamObj.row.removeAttribute('id');

    // 解析追问建议
    var parsed = parseSuggestions(fullContent);
    if (typeof marked !== 'undefined' && marked.parse) {
      try {
        streamObj.bubble.innerHTML = marked.parse(parsed.cleanText);
      } catch (e) {
        streamObj.bubble.textContent = parsed.cleanText;
      }
    } else {
      streamObj.bubble.textContent = parsed.cleanText;
    }

    // 显示复制按钮并更新内容
    if (streamObj.copyBtn) {
      streamObj.copyBtn.style.display = '';
      streamObj.copyBtn.removeAttribute('id');
      updateCopyButtonTarget(streamObj.copyBtn, parsed.cleanText);
    }

    // 追问建议：优先用 LLM 返回的，没有则用前端兜底
    var finalSuggestions = parsed.suggestions;
    if (finalSuggestions.length === 0) {
      finalSuggestions = getFallbackSuggestions(userMessage);
    }

    // 追加追问按钮
    if (finalSuggestions.length > 0 && streamObj.body) {
      var sugDiv = document.createElement('div');
      sugDiv.className = 'ai-chat-suggestions';
      finalSuggestions.forEach(function(sug) {
        var btn = document.createElement('button');
        btn.className = 'ai-chat-suggestion-btn';
        btn.textContent = sug;
        btn.addEventListener('click', function() {
          document.getElementById('ai-chat-input').value = sug;
          sendMessage();
        });
        sugDiv.appendChild(btn);
      });
      streamObj.body.appendChild(sugDiv);
    }

    scrollToBottom();
  }

  /** 更新已有复制按钮的目标文本 */
  function updateCopyButtonTarget(btn, newContent) {
    btn.setAttribute('data-copy-text', newContent);
  }

  function renderMarkdown(text) {
    if (typeof marked !== 'undefined' && marked.parse) {
      try {
        return marked.parse(text);
      } catch (e) {}
    }
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML.replace(/\n/g, '<br>');
  }

  // ========== 思考动画 ==========
  function addThinking() {
    var messages = document.getElementById('ai-chat-messages');
    var row = document.createElement('div');
    row.className = 'ai-chat-msg-row assistant';
    row.id = 'ai-thinking-row';

    var avatar = document.createElement('div');
    avatar.className = 'ai-chat-avatar ai-chat-avatar-assistant';
    avatar.innerHTML = assistantSvgAvatar();

    var thinking = document.createElement('div');
    thinking.className = 'ai-chat-thinking';
    thinking.innerHTML = '<span></span><span></span><span></span>';

    row.appendChild(avatar);
    row.appendChild(thinking);
    messages.appendChild(row);
    scrollToBottom();
  }

  function removeThinking() {
    var row = document.getElementById('ai-thinking-row');
    if (row) row.remove();
  }

  // ========== 清空消息区 ==========
  function clearMessages() {
    var messages = document.getElementById('ai-chat-messages');
    if (messages) messages.innerHTML = '';
  }

  // ========== 骨架屏 ==========
  function showSkeleton() {
    var messages = document.getElementById('ai-chat-messages');
    if (!messages) return;
    messages.innerHTML = '' +
      '<div class="ai-chat-skeleton">' +
      '  <div class="ai-chat-skeleton-row">' +
      '    <div class="ai-chat-skeleton-avatar"></div>' +
      '    <div class="ai-chat-skeleton-bubble long"></div>' +
      '  </div>' +
      '  <div class="ai-chat-skeleton-row">' +
      '    <div class="ai-chat-skeleton-bubble medium"></div>' +
      '    <div class="ai-chat-skeleton-avatar"></div>' +
      '  </div>' +
      '  <div class="ai-chat-skeleton-row">' +
      '    <div class="ai-chat-skeleton-avatar"></div>' +
      '    <div class="ai-chat-skeleton-bubble short"></div>' +
      '  </div>' +
      '</div>';
  }

  function hideSkeleton() {
    var skeletons = document.querySelectorAll('.ai-chat-skeleton');
    skeletons.forEach(function(s) { s.remove(); });
  }

  // ========== 会话管理 API ==========

  /** 加载会话列表 */
  function fetchSessions(callback) {
    authFetch(SESSIONS_API)
      .then(function(r) { return r.json(); })
      .then(function(res) {
        if (res.code === 200 && res.data) {
          sessionList = res.data;
          allSessionsLoaded = true;
          if (callback) callback(sessionList);
        }
      })
      .catch(function(err) {
        console.error('Failed to load sessions:', err);
        if (callback) callback([]);
      });
  }

  /** 创建新会话（服务器端） */
  function createSession(callback) {
    authFetch(SESSIONS_API + '/new', { method: 'POST' })
      .then(function(r) { return r.json(); })
      .then(function(res) {
        if (res.code === 200 && res.data) {
          callback(res.data);
        } else {
          console.error('Failed to create session');
          callback(null);
        }
      })
      .catch(function(err) {
        console.error('Failed to create session:', err);
        callback(null);
      });
  }

  /** 删除会话（服务器端） */
  function deleteSession(sessionUid, callback) {
    authFetch(SESSIONS_API + '/' + sessionUid, { method: 'DELETE' })
      .then(function(r) { return r.json(); })
      .then(function(res) {
        if (callback) callback(res.code === 200);
      })
      .catch(function(err) {
        console.error('Failed to delete session:', err);
        if (callback) callback(false);
      });
  }

  /** 清空会话消息（服务器端） */
  function clearSessionMessages(sessionUid, callback) {
    authFetch(SESSIONS_API + '/' + sessionUid + '/messages', { method: 'DELETE' })
      .then(function(r) { return r.json(); })
      .then(function(res) {
        if (callback) callback(res.code === 200);
      })
      .catch(function(err) {
        console.error('Failed to clear messages:', err);
        if (callback) callback(false);
      });
  }

  /** 加载会话历史消息 */
  function loadSessionMessages(sessionUid, callback) {
    authFetch(SESSIONS_API + '/' + sessionUid + '/messages')
      .then(function(r) { return r.json(); })
      .then(function(res) {
        if (res.code === 200 && res.data) {
          callback(res.data);
        } else {
          callback([]);
        }
      })
      .catch(function(err) {
        console.error('Failed to load messages:', err);
        callback([]);
      });
  }

  // ========== 会话切换 ==========

  /** 切换到指定会话 */
  function switchToSession(sessionUid) {
    if (isThinking) return; // 正在思考中不能切换

    showSkeleton();
    currentSessionId = sessionUid;

    if (!sessionUid) {
      // 不指定则创建新会话
      createSession(function(session) {
        if (session) {
          currentSessionId = session.sessionUid;
          sessionList.unshift(session);
          renderHistoryList();
        }
        hideSkeleton();
        showGreeting();
      });
      return;
    }

    // 加载消息
    loadSessionMessages(sessionUid, function(messages) {
      hideSkeleton();
      clearMessages();
      messages.forEach(function(msg) {
        addMessageRow(msg.role, msg.content, formatDate(msg.createdAt));
      });
    });

    highlightActiveSession(sessionUid);
  }

  // ========== 清除当前会话 ==========

  function clearCurrentSession() {
    if (!currentSessionId) return;

    if (!confirm('确定要清空当前会话的所有消息吗？此操作不可撤销。')) return;

    clearSessionMessages(currentSessionId, function(success) {
      if (success) {
        showGreeting();
        // 更新列表中的消息计数
        for (var i = 0; i < sessionList.length; i++) {
          if (sessionList[i].sessionUid === currentSessionId) {
            sessionList[i].messageCount = 0;
            sessionList[i].title = '新对话';
            break;
          }
        }
        renderHistoryList();
      } else {
        alert('清空失败，请重试');
      }
    });
  }

  // ========== 新建会话 ==========

  function newSession() {
    if (isThinking) return;

    createSession(function(session) {
      if (session) {
        currentSessionId = session.sessionUid;
        showGreeting();
        // 插到列表最前面
        sessionList.unshift(session);
        renderHistoryList();
        document.getElementById('ai-chat-input').focus();
      }
    });
  }

  // ========== 删除历史会话 ==========

  function deleteHistorySession(sessionUid, e) {
    if (e) { e.stopPropagation(); e.preventDefault(); }

    if (!confirm('确定要删除这个会话吗？所有消息将被永久删除。')) return;

    deleteSession(sessionUid, function(success) {
      if (success) {
        // 从列表中移除
        sessionList = sessionList.filter(function(s) { return s.sessionUid !== sessionUid; });
        renderHistoryList();

        // 如果删除的是当前会话，切换到最新的
        if (currentSessionId === sessionUid) {
          if (sessionList.length > 0) {
            switchToSession(sessionList[0].sessionUid);
          } else {
            newSession();
          }
        }
      }
    });
  }

  // ========== 历史侧栏 ==========

  function toggleHistorySidebar() {
    isHistoryOpen = !isHistoryOpen;
    var sidebar = document.getElementById('ai-history-sidebar');
    var overlay = document.getElementById('ai-history-overlay');

    if (isHistoryOpen) {
      sidebar.classList.remove('hidden');
      overlay.classList.remove('hidden');
      fetchSessions(function() {
        renderHistoryList();
        highlightActiveSession(currentSessionId);
      });
    } else {
      sidebar.classList.add('hidden');
      overlay.classList.add('hidden');
    }
  }

  function highlightActiveSession(sessionUid) {
    var items = document.querySelectorAll('.ai-history-item');
    items.forEach(function(item) {
      if (item.getAttribute('data-session-uid') === sessionUid) {
        item.classList.add('active');
      } else {
        item.classList.remove('active');
      }
    });
  }

  function renderHistoryList() {
    var list = document.getElementById('ai-history-list');
    if (!list) return;

    if (sessionList.length === 0) {
      list.innerHTML = '<div class="ai-history-empty">暂无历史会话</div>';
      return;
    }

    var html = '';
    sessionList.forEach(function(session) {
      var date = formatDate(session.updatedAt || session.createdAt);
      var isActive = session.sessionUid === currentSessionId;
      html += '<div class="ai-history-item' + (isActive ? ' active' : '') + '" data-session-uid="' + session.sessionUid + '">';
      html += '  <div class="ai-history-item-main">';
      html += '    <div class="ai-history-item-title">' + escapeHtml(session.title || '新对话') + '</div>';
      html += '    <div class="ai-history-item-meta">';
      html += '      <span>' + (session.messageCount || 0) + ' 条消息</span>';
      html += '      <span>' + date + '</span>';
      html += '    </div>';
      html += '  </div>';
      html += '  <button class="ai-history-item-delete" title="删除会话" data-session-uid="' + session.sessionUid + '">✕</button>';
      html += '</div>';
    });

    list.innerHTML = html;

    // 绑定点击事件
    list.querySelectorAll('.ai-history-item-main').forEach(function(main) {
      main.addEventListener('click', function() {
        var uid = this.parentElement.getAttribute('data-session-uid');
        switchToSession(uid);
        toggleHistorySidebar(); // 选择后关闭侧栏
      });
    });

    // 绑定删除按钮
    list.querySelectorAll('.ai-history-item-delete').forEach(function(btn) {
      btn.addEventListener('click', function(e) {
        var uid = this.getAttribute('data-session-uid');
        deleteHistorySession(uid, e);
      });
    });
  }

  function escapeHtml(text) {
    var div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  // ========== SSE 流式对话核心 ==========
  function sendMessage() {
    if (isThinking) return;

    var input = document.getElementById('ai-chat-input');
    var message = input.value.trim();
    if (!message) return;

    // 确保有会话
    if (!currentSessionId) {
      createSession(function(session) {
        if (session) {
          currentSessionId = session.sessionUid;
          sessionList.unshift(session);
          renderHistoryList();
          doSend(message);
        }
      });
    } else {
      doSend(message);
    }

    function doSend(msg) {
      input.value = '';
      input.focus();

      // 用户消息
      addUserMessage(msg);

      // 进入思考态
      isThinking = true;
      var sendBtn = document.getElementById('ai-chat-send');
      sendBtn.disabled = true;
      addThinking();

      // 流式状态
      var streamObj = null;
      var fullContent = '';
      var firstTokenReceived = false;

      var safetyTimer = setTimeout(function () {
        if (!firstTokenReceived && isThinking) {
          removeThinking();
          addAssistantText('AI 助手响应超时，请稍后重试。如果持续超时，请检查 API Key 或网络连接。');
          finishSend(sendBtn);
          isThinking = false;
        }
      }, 30000);

      authFetch(CHAT_STREAM_API, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message: msg, sessionId: currentSessionId })
      }).then(function (response) {
        if (!response.ok) {
          throw new Error('HTTP ' + response.status);
        }

        var reader = response.body.getReader();
        var decoder = new TextDecoder();
        var buffer = '';

        function pump() {
          return reader.read().then(function (result) {
            if (result.done) {
              if (streamObj) finalizeStream(streamObj, fullContent, msg);
              finishSend(sendBtn);
              // 更新会话列表
              fetchSessions(function() {
                renderHistoryList();
                highlightActiveSession(currentSessionId);
              });
              return;
            }

            buffer += decoder.decode(result.value, { stream: true });
            var lines = buffer.split('\n');
            buffer = lines.pop();

            for (var i = 0; i < lines.length; i++) {
              var line = lines[i];
              if (line.indexOf('data:') !== 0) continue;

              var jsonStr = line.substring(5);
              try {
                var data = JSON.parse(jsonStr);

                if (data.error !== undefined) {
                  if (!streamObj) {
                    removeThinking();
                    streamObj = createStreamingBubble();
                  }
                  streamObj.bubble.innerHTML = renderMarkdown('> ⚠️ ' + data.error);
                } else if (data.done) {
                  clearTimeout(safetyTimer);
                  if (streamObj) {
                    finalizeStream(streamObj, fullContent, msg);
                  } else {
                    removeThinking();
                  }
                  // 服务端可能返回新的 sessionId
                  if (data.sessionId) {
                    currentSessionId = data.sessionId;
                  }
                  finishSend(sendBtn);
                  // 加载最新消息列表
                  fetchSessions(function() {
                    renderHistoryList();
                    highlightActiveSession(currentSessionId);
                  });
                  return;
                } else if (data.token !== undefined) {
                  if (!streamObj) {
                    firstTokenReceived = true;
                    clearTimeout(safetyTimer);
                    removeThinking();
                    streamObj = createStreamingBubble();
                  }
                  fullContent += data.token;
                  streamObj.bubble.innerHTML = renderMarkdown(fullContent);
                  scrollToBottom();
                }
              } catch (e) {}
            }

            return pump();
          });
        }

        return pump();
      }).catch(function (err) {
        clearTimeout(safetyTimer);
        removeThinking();
        if (streamObj) {
          streamObj.bubble.innerHTML = renderMarkdown(fullContent + '\n\n> ⚠️ 网络请求失败，请检查网络后重试。');
          finalizeStream(streamObj, fullContent + '\n\n> ⚠️ 网络请求失败，请检查网络后重试。', msg);
        } else {
          addAssistantText('网络请求失败，请检查网络后重试。');
        }
        finishSend(sendBtn);
        console.error('AI stream error:', err);
      });

      function finishSend(btn) {
        clearTimeout(safetyTimer);
        isThinking = false;
        if (btn) btn.disabled = false;
      }
    }
  }

  // ========== 快捷提示词 ==========
  function initQuickHints() {
    var hintsContainer = document.getElementById('ai-chat-hints');
    var hints = getQuickHints();
    hints.forEach(function (hint) {
      var btn = document.createElement('button');
      btn.className = 'ai-chat-quick-hint';
      btn.textContent = hint;
      btn.onclick = function () {
        document.getElementById('ai-chat-input').value = hint;
        sendMessage();
      };
      hintsContainer.appendChild(btn);
    });
  }

  // ========== 拖拽调整大小 ==========
  function initResize() {
    var panel = document.getElementById('ai-chat-panel');
    var handle = document.getElementById('ai-chat-resize-handle');
    if (!panel || !handle) return;

    var startX, startY, startWidth, startHeight;
    var minWidth = 360;
    var minHeight = 400;

    handle.addEventListener('mousedown', function(e) {
      if (isMaximized) return;
      e.preventDefault();
      e.stopPropagation();
      startX = e.clientX;
      startY = e.clientY;
      startWidth = panel.offsetWidth;
      startHeight = panel.offsetHeight;
      panel.style.transition = 'none';
      document.body.style.userSelect = 'none';
      document.body.style.cursor = 'nwse-resize';

      function onMouseMove(e) {
        var newWidth = startWidth + (e.clientX - startX);
        var newHeight = startHeight + (e.clientY - startY);
        newWidth = Math.max(minWidth, Math.min(newWidth, window.innerWidth - 24));
        newHeight = Math.max(minHeight, Math.min(newHeight, window.innerHeight - 96));
        panel.style.width = newWidth + 'px';
        panel.style.height = newHeight + 'px';
        savedWidth = newWidth;
        savedHeight = newHeight;
      }

      function onMouseUp() {
        panel.style.transition = '';
        document.body.style.userSelect = '';
        document.body.style.cursor = '';
        document.removeEventListener('mousemove', onMouseMove);
        document.removeEventListener('mouseup', onMouseUp);
      }

      document.addEventListener('mousemove', onMouseMove);
      document.addEventListener('mouseup', onMouseUp);
    });

    handle.addEventListener('touchstart', function(e) {
      if (isMaximized) return;
      e.preventDefault();
      e.stopPropagation();
      var touch = e.touches[0];
      startX = touch.clientX;
      startY = touch.clientY;
      startWidth = panel.offsetWidth;
      startHeight = panel.offsetHeight;
      panel.style.transition = 'none';

      function onTouchMove(e) {
        var touch = e.touches[0];
        var newWidth = startWidth + (touch.clientX - startX);
        var newHeight = startHeight + (touch.clientY - startY);
        newWidth = Math.max(minWidth, Math.min(newWidth, window.innerWidth - 24));
        newHeight = Math.max(minHeight, Math.min(newHeight, window.innerHeight - 96));
        panel.style.width = newWidth + 'px';
        panel.style.height = newHeight + 'px';
        savedWidth = newWidth;
        savedHeight = newHeight;
      }

      function onTouchEnd() {
        panel.style.transition = '';
        document.removeEventListener('touchmove', onTouchMove);
        document.removeEventListener('touchend', onTouchEnd);
      }

      document.addEventListener('touchmove', onTouchMove, { passive: false });
      document.addEventListener('touchend', onTouchEnd);
    });
  }

  // ========== 面板切换 ==========
  function togglePanel() {
    isOpen = !isOpen;
    var panel = document.getElementById('ai-chat-panel');
    var fab = document.getElementById('ai-chat-fab');
    if (isOpen) {
      panel.classList.remove('hidden');
      fab.classList.add('panel-open');
      document.getElementById('ai-chat-input').focus();
      // 首次打开时初始化会话
      if (!currentSessionId) {
        showSkeleton();
        initCurrentSession();
      }
    } else {
      panel.classList.add('hidden');
      fab.classList.remove('panel-open');
      // 收起面板时也关闭历史侧栏
      if (isHistoryOpen) toggleHistorySidebar();
    }
  }

  function toggleMaximize() {
    isMaximized = !isMaximized;
    var panel = document.getElementById('ai-chat-panel');
    var btn = document.getElementById('ai-chat-maximize');
    if (isMaximized) {
      savedWidth = parseInt(panel.style.width) || panel.offsetWidth;
      savedHeight = parseInt(panel.style.height) || panel.offsetHeight;
      panel.classList.add('maximized');
      panel.style.width = '';
      panel.style.height = '';
      btn.innerHTML = '🔳';
      btn.title = '还原窗口';
    } else {
      panel.classList.remove('maximized');
      panel.style.width = savedWidth + 'px';
      panel.style.height = savedHeight + 'px';
      btn.innerHTML = '🔲';
      btn.title = '放大窗口';
    }
    var fab = document.getElementById('ai-chat-fab');
    if (isOpen) fab.classList.add('panel-open');
  }

  /** 初始化当前会话：尝试加载最近的会话 */
  function initCurrentSession() {
    fetchSessions(function(sessions) {
      if (sessions.length > 0) {
        currentSessionId = sessions[0].sessionUid;
        loadSessionMessages(currentSessionId, function(messages) {
          hideSkeleton();
          if (messages.length > 0) {
            // 有历史消息，恢复显示
            messages.forEach(function(msg) {
              addMessageRow(msg.role, msg.content, formatDate(msg.createdAt));
            });
          } else {
            // 有空会话但没消息，显示问候
            showGreeting();
          }
          renderHistoryList();
          highlightActiveSession(currentSessionId);
        });
      } else {
        // 没有任何会话，显示问候语
        hideSkeleton();
        showGreeting();
        renderHistoryList();
      }
    });
  }

  /** 显示默认问候语 */
  function showGreeting() {
    clearMessages();
    var role = detectRole();
    var greeting = '';
    if (role === 'student') {
      greeting = '你好！我是你的**课程小助手** 🎓\n\n我可以帮你：\n- 📚 搜索和推荐选修课程\n- 📊 分析你的成绩和 GPA\n- 📅 查看本学期课表\n- ⭐ 查询课程评价\n\n试试下面的快捷提示，或者直接告诉我你的问题吧！';
    } else if (role === 'teacher') {
      greeting = '你好！我是你的**教学辅助助手** 👨‍🏫\n\n我可以帮你：\n- 📊 分析班级成绩分布\n- 👥 查看课程学生名单与搜索学生\n- 📝 查询学生个人成绩\n- ⚠️ 检测异常情况\n\n试试下面的快捷提示，或者直接告诉我你的问题吧！';
    } else {
      greeting = '你好！我是你的**运营分析助手** 📈\n\n我可以帮你：\n- 📊 查看系统运营统计\n- 🔥 分析热门课程\n- ⚠️ 检测系统异常\n- 📋 查看操作日志\n\n试试下面的快捷提示，或者直接告诉我你的问题吧！';
    }
    addMessageRow('assistant', greeting, formatTime());
  }

  function detectRole() {
    var path = window.location.pathname;
    if (path.indexOf('/student/') !== -1) return 'student';
    if (path.indexOf('/teacher/') !== -1) return 'teacher';
    return 'admin';
  }

  // ========== 事件绑定 ==========
  function bindEvents() {
    document.getElementById('ai-chat-fab').addEventListener('click', togglePanel);
    document.getElementById('ai-chat-close').addEventListener('click', togglePanel);
    document.getElementById('ai-chat-maximize').addEventListener('click', toggleMaximize);
    document.getElementById('ai-chat-send').addEventListener('click', sendMessage);
    document.getElementById('ai-chat-new').addEventListener('click', newSession);
    document.getElementById('ai-chat-clear').addEventListener('click', clearCurrentSession);
    document.getElementById('ai-chat-history').addEventListener('click', toggleHistorySidebar);
    document.getElementById('ai-history-close').addEventListener('click', toggleHistorySidebar);
    document.getElementById('ai-history-overlay').addEventListener('click', toggleHistorySidebar);

    var input = document.getElementById('ai-chat-input');
    input.addEventListener('keydown', function (e) {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault();
        sendMessage();
      }
    });
  }

  // ========== 加载 Markdown 解析库 ==========
  function loadMarked(callback) {
    if (typeof marked !== 'undefined') {
      callback();
      return;
    }

    var script = document.createElement('script');
    script.src = MARKED_CDN;
    script.onload = callback;
    script.onerror = function () {
      console.warn('marked.js CDN 加载失败，将使用纯文本模式');
      callback();
    };
    document.head.appendChild(script);
  }

  // ========== 启动 ==========
  function init() {
    createWidget();
    loadMarked(function () {
      initQuickHints();
      bindEvents();
      initResize();
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();

  // 微服务版：所有请求自动携带 JWT
  function authFetch(url, options) {
    options = options || {};
    options.headers = Object.assign({}, options.headers || {}, { 'Authorization': 'Bearer ' + (localStorage.getItem('token') || '') });
    return fetch(url, options);
  }
