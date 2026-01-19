/**
 * 谷歌在线邀请管理页面逻辑
 * - 在线邀请记录：/api/online-invite/*
 * - 在线邀请 Q&A：/api/invite/qa/online/*
 */

let currentRejectId = null;
let onlineInviteData = [];

// 页面加载
document.addEventListener('DOMContentLoaded', () => {
    bindTabs();
    bindFilters();
    loadOnlineInviteList();
});

// ==================== 标签页 ====================

function bindTabs() {
    const btnRecords = document.getElementById('tabBtnRecords');
    const btnQA = document.getElementById('tabBtnQA');

    if (btnRecords) btnRecords.addEventListener('click', () => showTab('records'));
    if (btnQA) btnQA.addEventListener('click', () => showTab('qa'));
}

function showTab(name) {
    const btnRecords = document.getElementById('tabBtnRecords');
    const btnQA = document.getElementById('tabBtnQA');
    const panelRecords = document.getElementById('tabRecords');
    const panelQA = document.getElementById('tabQA');

    const isQA = name === 'qa';

    if (btnRecords) {
        btnRecords.classList.toggle('active', !isQA);
        btnRecords.setAttribute('aria-selected', String(!isQA));
    }
    if (btnQA) {
        btnQA.classList.toggle('active', isQA);
        btnQA.setAttribute('aria-selected', String(isQA));
    }

    if (panelRecords) {
        panelRecords.classList.toggle('hidden', isQA);
        panelRecords.setAttribute('aria-hidden', String(isQA));
    }
    if (panelQA) {
        panelQA.classList.toggle('hidden', !isQA);
        panelQA.setAttribute('aria-hidden', String(!isQA));
    }

    if (isQA) {
        loadQAList();
    }
}

function bindFilters() {
    const statusFilter = document.getElementById('statusFilter');
    if (!statusFilter) return;
    statusFilter.addEventListener('change', () => {
        renderOnlineInviteList(onlineInviteData);
    });
}

// ==================== 在线邀请列表 ====================

async function loadOnlineInviteList() {
    try {
        const response = await fetch('/api/online-invite/list');

        if (response.status === 401) {
            window.location.href = '/login.html?redirect=/online-invite-admin.html';
            return;
        }

        const result = await response.json();

        if (result && result.success) {
            renderStatistics(result.statistics || {});
            onlineInviteData = Array.isArray(result.data) ? result.data : [];
            renderOnlineInviteList(onlineInviteData);
        } else {
            showError((result && result.message) ? result.message : '加载失败');
        }
    } catch (error) {
        console.error('加载失败:', error);
        showError('网络错误，请稍后重试');
    }
}

function renderStatistics(stats) {
    setText('totalCount', stats.total ?? 0);
    setText('pendingCount', stats.pending ?? 0);
    setText('submittedCount', stats.submitted ?? 0);
    setText('processedCount', stats.processed ?? 0);
    setText('rejectedCount', stats.rejected ?? 0);
}

function setText(id, value) {
    const node = document.getElementById(id);
    if (node) node.textContent = String(value);
}

function renderOnlineInviteList(invites) {
    const container = document.getElementById('inviteContainer');
    if (!container) return;

    const statusFilter = document.getElementById('statusFilter');
    const filterValue = statusFilter ? statusFilter.value : '';
    const list = Array.isArray(invites) ? invites : [];
    const filtered = filterValue ? list.filter(item => item && item.status === filterValue) : list;

    if (filtered.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📭</div>
                <h3>暂无记录</h3>
                <p style="margin-top: 10px;">当前筛选条件下没有在线邀请申请</p>
            </div>
        `;
        return;
    }

    let tableHtml = `
        <table class="invite-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>订单号</th>
                    <th>验证地址</th>
                    <th>状态</th>
                    <th>驳回原因</th>
                    <th>提交时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
    `;

    filtered.forEach(invite => {
        const statusClass = getStatusClass(invite.status);
        const statusText = invite.statusDescription || getStatusText(invite.status);

        tableHtml += `
            <tr>
                <td>${invite.id}</td>
                <td>${invite.orderNumber ? '<span class="order-number">' + escapeHtml(invite.orderNumber) + '</span>' : '<span style="color:#999">-</span>'}</td>
                <td>${renderVerifyAddress(invite.verifyAddress)}</td>
                <td><span class="status-badge ${statusClass}">${escapeHtml(statusText)}</span></td>
                <td class="reason-cell">${invite.rejectReason ? escapeHtml(invite.rejectReason) : '<span style="color:#999">-</span>'}</td>
                <td class="time-cell">${invite.submitTime || '-'}</td>
                <td>
                    <div class="action-buttons">
                        ${renderActionButtons(invite)}
                    </div>
                </td>
            </tr>
        `;
    });

    tableHtml += '</tbody></table>';
    container.innerHTML = tableHtml;
}

function renderVerifyAddress(verifyAddress) {
    const value = (verifyAddress || '').trim();
    if (!value) return '<span style="color:#999">-</span>';

    const safe = escapeHtml(value);
    if (/^https?:\/\//i.test(value)) {
        return `<a class="verify-link" href="${safe}" target="_blank" rel="noopener noreferrer">${safe}</a>`;
    }
    return safe;
}

function getStatusClass(status) {
    const classMap = {
        'PENDING': 'status-pending',
        'SUBMITTED': 'status-submitted',
        'PROCESSED': 'status-processed',
        'REJECTED': 'status-rejected'
    };
    return classMap[status] || '';
}

function getStatusText(status) {
    const textMap = {
        'PENDING': '待提交',
        'SUBMITTED': '待处理',
        'PROCESSED': '已处理',
        'REJECTED': '已驳回'
    };
    return textMap[status] || (status || '-');
}

function renderActionButtons(invite) {
    let buttons = '';

    if (invite && invite.status === 'SUBMITTED') {
        buttons += `
            <button class="btn btn-success btn-sm" onclick="confirmProcess(${invite.id})">
                ✅ 确认处理
            </button>
        `;
    }

    buttons += `
        <button class="btn btn-warning btn-sm" onclick="showCancelModal(${invite.id})">
            ⛔ 驳回
        </button>
        <button class="btn btn-danger btn-sm" onclick="deleteRecord(${invite.id})">
            🗑️ 删除
        </button>
    `;

    return buttons;
}

async function confirmProcess(id) {
    if (!confirm('确认该申请已处理完成？')) return;

    try {
        const response = await fetch(`/api/online-invite/${id}/confirm`, { method: 'POST' });
        const result = await response.json();

        if (result && result.success) {
            alert('操作成功');
            loadOnlineInviteList();
        } else {
            alert('操作失败：' + ((result && result.message) ? result.message : '未知错误'));
        }
    } catch (error) {
        console.error('操作失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// ==================== 驳回模态框 ====================

function showCancelModal(id) {
    currentRejectId = id;
    const input = document.getElementById('cancelReasonInput');
    if (input) input.value = '';
    const modal = document.getElementById('cancelModal');
    if (modal) modal.style.display = 'flex';
}

function hideCancelModal() {
    currentRejectId = null;
    const modal = document.getElementById('cancelModal');
    if (modal) modal.style.display = 'none';
}

async function confirmCancel() {
    if (!currentRejectId) return;

    const input = document.getElementById('cancelReasonInput');
    const reason = input ? input.value.trim() : '';

    try {
        const response = await fetch(`/api/online-invite/${currentRejectId}/reject`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reason: reason || null })
        });

        const result = await response.json();

        if (result && result.success) {
            alert('驳回成功');
            hideCancelModal();
            loadOnlineInviteList();
        } else {
            alert('操作失败：' + ((result && result.message) ? result.message : '未知错误'));
        }
    } catch (error) {
        console.error('驳回失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// ==================== 删除记录 ====================

async function deleteRecord(id) {
    if (!confirm('确认删除该记录？此操作不可恢复！')) return;

    try {
        const response = await fetch(`/api/online-invite/${id}`, { method: 'DELETE' });
        const result = await response.json();

        if (result && result.success) {
            alert('删除成功');
            loadOnlineInviteList();
        } else {
            alert('删除失败：' + ((result && result.message) ? result.message : '未知错误'));
        }
    } catch (error) {
        console.error('删除失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// ==================== 通用操作 ====================

function refreshData() {
    loadOnlineInviteList();
}

function logout() {
    if (confirm('确认退出登录？')) {
        fetch('/api/logout', { method: 'POST' })
            .then(() => window.location.href = '/login.html');
    }
}

function showError(message) {
    const container = document.getElementById('inviteContainer');
    if (!container) return;

    container.innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">😕</div>
            <h3>加载失败</h3>
            <p style="margin-top: 10px;">${escapeHtml(message)}</p>
        </div>
    `;
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ==================== Q&A 管理（ONLINE） ====================

let qaAnswerQuill = null;
let qaDataMap = {};

function looksLikeHtml(str) {
    return typeof str === 'string' && /<\/?[a-z][\s\S]*>/i.test(str);
}

function ensureQAAnswerQuill() {
    const textarea = document.getElementById('qaAnswerInput');
    const editorHost = document.getElementById('qaAnswerEditor');

    if (!window.Quill) {
        if (editorHost) editorHost.style.display = 'none';
        if (textarea) {
            textarea.style.display = 'block';
            textarea.style.width = '100%';
            textarea.style.padding = '12px';
            textarea.style.border = '2px solid #e0e0e0';
            textarea.style.borderRadius = '10px';
            textarea.style.fontSize = '0.95rem';
            textarea.style.minHeight = '160px';
            textarea.style.resize = 'vertical';
        }
        return;
    }

    if (editorHost) editorHost.style.display = 'block';
    if (textarea) textarea.style.display = 'none';
    if (qaAnswerQuill) return;

    qaAnswerQuill = new Quill('#qaAnswerEditor', {
        theme: 'snow',
        placeholder: '请输入答案...',
        modules: {
            toolbar: [
                [{ header: [1, 2, false] }],
                ['bold', 'italic', 'underline', 'strike'],
                [{ color: [] }, { background: [] }],
                [{ list: 'ordered' }, { list: 'bullet' }],
                [{ align: [] }],
                ['link', 'code-block'],
                ['clean']
            ]
        }
    });
}

function setQAAnswerValue(value) {
    const textarea = document.getElementById('qaAnswerInput');
    if (textarea) textarea.value = value || '';

    if (!qaAnswerQuill) return;
    qaAnswerQuill.setContents([]);

    if (!value) {
        qaAnswerQuill.setText('');
        return;
    }

    if (looksLikeHtml(value)) qaAnswerQuill.setContents(qaAnswerQuill.clipboard.convert(value));
    else qaAnswerQuill.setText(value);
}

function getQAAnswerValue() {
    if (!qaAnswerQuill) {
        const t = document.getElementById('qaAnswerInput');
        return t ? t.value.trim() : '';
    }

    const plain = qaAnswerQuill.getText().trim();
    if (!plain) return '';
    return qaAnswerQuill.root.innerHTML;
}

async function loadQAList() {
    const container = document.getElementById('qaListContainer');
    if (!container) return;

    container.innerHTML = `
        <div class="loading">
            <div class="spinner"></div>
            <div>加载中...</div>
        </div>
    `;

    try {
        const response = await fetch('/api/invite/qa/online/list');
        const result = await response.json();

        if (result && result.success) {
            renderQAList(result.data);
        } else {
            container.innerHTML = `<p style="text-align:center;color:#999;">加载失败: ${escapeHtml((result && result.message) ? result.message : '未知错误')}</p>`;
        }
    } catch (error) {
        console.error('加载 Q&A 失败:', error);
        container.innerHTML = `<p style="text-align:center;color:#999;">网络错误，请稍后重试</p>`;
    }
}

function renderQAList(qaList) {
    const container = document.getElementById('qaListContainer');
    if (!container) return;

    if (!qaList || qaList.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; padding: 40px; color: #999;">
                <div style="font-size: 3rem; margin-bottom: 15px;">📝</div>
                <p>暂无 Q&A 记录</p>
                <p style="margin-top: 8px; font-size: 0.875rem;">点击上方 "添加 Q&A" 创建第一条问答</p>
            </div>
        `;
        qaDataMap = {};
        return;
    }

    qaDataMap = {};
    qaList.forEach(qa => { qaDataMap[qa.id] = qa; });

    let html = '<div class="qa-list">';
    qaList.forEach((qa, index) => {
        html += `
            <div class="qa-item ${qa.enabled ? '' : 'disabled'}" data-id="${qa.id}">
                <div class="qa-question">Q: ${escapeHtml(qa.question)}</div>
                <div class="qa-answer">
                    <div style="font-weight: 700; color:#333; margin-bottom: 6px;">A:</div>
                    <div class="qa-answer-editor" id="qa-answer-${qa.id}"></div>
                </div>
                <div class="qa-meta">
                    <span>排序: ${qa.sortOrder} | ${qa.enabled ? '✅ 启用中' : '⏸️ 已禁用'}</span>
                    <span>${qa.createTime ? qa.createTime.substring(0, 10) : ''}</span>
                </div>
                <div class="qa-actions">
                    <button class="btn btn-secondary btn-sm" onclick="moveQAUp(${qa.id})" ${index === 0 ? 'disabled' : ''}>
                        ⬆️ 上移
                    </button>
                    <button class="btn btn-secondary btn-sm" onclick="moveQADown(${qa.id})" ${index === qaList.length - 1 ? 'disabled' : ''}>
                        ⬇️ 下移
                    </button>
                    <button class="btn btn-primary btn-sm" onclick="showEditQAFormById(${qa.id})">
                        ✏️ 编辑
                    </button>
                    <button class="btn btn-${qa.enabled ? 'warning' : 'success'} btn-sm" onclick="toggleQA(${qa.id})">
                        ${qa.enabled ? '⏸️ 禁用' : '▶️ 启用'}
                    </button>
                    <button class="btn btn-danger btn-sm" onclick="deleteQA(${qa.id})">
                        🗑️ 删除
                    </button>
                </div>
            </div>
        `;
    });
    html += '</div>';
    container.innerHTML = html;

    // Quill 只读渲染，失败则降级为纯文本
    qaList.forEach((qa) => {
        const host = document.getElementById(`qa-answer-${qa.id}`);
        if (!host) return;
        const value = (qa && qa.answer) ? String(qa.answer) : '';

        if (!window.Quill) {
            host.textContent = value;
            return;
        }

        const quill = new Quill(host, { theme: 'snow', readOnly: true, modules: { toolbar: false } });
        if (looksLikeHtml(value)) quill.setContents(quill.clipboard.convert(value));
        else quill.setText(value);
    });
}

function showAddQAForm() {
    document.getElementById('qaEditTitle').textContent = '添加 Q&A';
    document.getElementById('qaEditId').value = '';
    document.getElementById('qaQuestionInput').value = '';
    ensureQAAnswerQuill();
    setQAAnswerValue('');
    document.getElementById('qaEditModal').style.display = 'flex';
}

function showEditQAFormById(id) {
    const qa = qaDataMap[id];
    if (!qa) { alert('未找到该 Q&A 数据，请刷新后重试'); return; }
    document.getElementById('qaEditTitle').textContent = '编辑 Q&A';
    document.getElementById('qaEditId').value = id;
    document.getElementById('qaQuestionInput').value = qa.question || '';
    ensureQAAnswerQuill();
    setQAAnswerValue(qa.answer || '');
    document.getElementById('qaEditModal').style.display = 'flex';
}

function hideQAEditModal() {
    document.getElementById('qaEditModal').style.display = 'none';
}

async function saveQA() {
    const id = document.getElementById('qaEditId').value;
    const question = document.getElementById('qaQuestionInput').value.trim();
    const answer = getQAAnswerValue();

    if (!question) { alert('请输入问题'); return; }
    if (!answer) { alert('请输入答案'); return; }

    try {
        let response;
        if (id) {
            response = await fetch(`/api/invite/qa/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ question, answer })
            });
        } else {
            response = await fetch('/api/invite/qa/online/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ question, answer })
            });
        }

        const result = await response.json();
        if (result && result.success) {
            alert(id ? '更新成功' : '添加成功');
            hideQAEditModal();
            loadQAList();
        } else {
            alert('操作失败: ' + ((result && result.message) ? result.message : '未知错误'));
        }
    } catch (error) {
        console.error('保存 Q&A 失败:', error);
        alert('网络错误，请稍后重试');
    }
}

async function toggleQA(id) {
    try {
        const response = await fetch(`/api/invite/qa/${id}/toggle`, { method: 'POST' });
        const result = await response.json();
        if (result && result.success) loadQAList();
        else alert('操作失败: ' + ((result && result.message) ? result.message : '未知错误'));
    } catch (error) {
        console.error('切换 Q&A 状态失败:', error);
        alert('网络错误，请稍后重试');
    }
}

async function deleteQA(id) {
    if (!confirm('确认删除该 Q&A？此操作不可恢复！')) return;
    try {
        const response = await fetch(`/api/invite/qa/${id}`, { method: 'DELETE' });
        const result = await response.json();
        if (result && result.success) { alert('删除成功'); loadQAList(); }
        else alert('删除失败: ' + ((result && result.message) ? result.message : '未知错误'));
    } catch (error) {
        console.error('删除 Q&A 失败:', error);
        alert('网络错误，请稍后重试');
    }
}

async function moveQAUp(id) {
    try {
        const response = await fetch(`/api/invite/qa/${id}/move-up`, { method: 'POST' });
        const result = await response.json();
        if (result && result.success) loadQAList();
        else alert('操作失败: ' + ((result && result.message) ? result.message : '未知错误'));
    } catch (error) {
        console.error('上移 Q&A 失败:', error);
        alert('网络错误，请稍后重试');
    }
}

async function moveQADown(id) {
    try {
        const response = await fetch(`/api/invite/qa/${id}/move-down`, { method: 'POST' });
        const result = await response.json();
        if (result && result.success) loadQAList();
        else alert('操作失败: ' + ((result && result.message) ? result.message : '未知错误'));
    } catch (error) {
        console.error('下移 Q&A 失败:', error);
        alert('网络错误，请稍后重试');
    }
}

