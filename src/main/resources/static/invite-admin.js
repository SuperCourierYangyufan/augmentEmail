/**
 * 邀请管理页面逻辑
 * @author 杨宇帆
 * @create 2025-08-20
 */

// 页面加载时获取数据
document.addEventListener('DOMContentLoaded', () => {
    loadInviteList();
});

// 加载邀请列表
async function loadInviteList() {
    try {
        const response = await fetch('/api/invite/list');

        if (response.status === 401) {
            window.location.href = '/login.html?redirect=/invite-admin.html';
            return;
        }

        const result = await response.json();

        if (result.success) {
            renderStatistics(result.statistics);
            renderInviteList(result.data);
        } else {
            showError(result.message);
        }

    } catch (error) {
        console.error('加载失败:', error);
        showError('网络错误，请稍后重试');
    }
}

// 渲染统计信息
function renderStatistics(stats) {
    document.getElementById('totalCount').textContent = stats.total || 0;
    document.getElementById('submittedCount').textContent = stats.submitted || 0;
    document.getElementById('invitedCount').textContent = stats.invited || 0;
    document.getElementById('pendingCount').textContent = stats.pending || 0;
    document.getElementById('cancelledCount').textContent = stats.cancelled || 0;
}

// 渲染邀请列表
function renderInviteList(invites) {
    const container = document.getElementById('inviteContainer');

    if (!invites || invites.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-icon">📭</div>
                <h3>暂无邀请记录</h3>
                <p style="margin-top: 10px;">点击"生成邀请链接"创建新的邀请</p>
            </div>
        `;
        return;
    }

    let tableHtml = `
        <table class="invite-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>邀请码</th>
                    <th>订单号</th>
                    <th>邮箱地址</th>
                    <th>状态</th>
                    <th>驳回原因</th>
                    <th>填写时间</th>
                    <th>创建时间</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
    `;

    invites.forEach(invite => {
        const statusClass = getStatusClass(invite.status);
        const statusText = getStatusText(invite.status);

        tableHtml += `
            <tr>
                <td>${invite.id}</td>
                <td>
                    <code class="invite-code">${escapeHtml(invite.inviteCode)}</code>
                    <button class="copy-btn" onclick="copyInviteLink('${invite.inviteCode}')" title="复制邀请链接">📋</button>
                </td>
                <td>${invite.orderNumber ? '<span class="order-number">' + escapeHtml(invite.orderNumber) + '</span>' : '<span style="color:#999">未填写</span>'}</td>
                <td class="email-cell">${invite.emailAddress ? escapeHtml(invite.emailAddress) : '<span style="color:#999">未填写</span>'}</td>
                <td><span class="status-badge ${statusClass}">${statusText}</span></td>
                <td class="reason-cell">${invite.rejectReason ? escapeHtml(invite.rejectReason) : '<span style="color:#999">-</span>'}</td>
                <td>${invite.fillTime || '-'}</td>
                <td>${invite.createTime || '-'}</td>
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

// 获取状态样式类
function getStatusClass(status) {
    const classMap = {
        'PENDING': 'status-pending',
        'SUBMITTED': 'status-submitted',
        'INVITED': 'status-invited',
        'CANCELLED': 'status-cancelled'
    };
    return classMap[status] || '';
}

// 获取状态文本
function getStatusText(status) {
    const textMap = {
        'PENDING': '待填写',
        'SUBMITTED': '待邀请',
        'INVITED': '已邀请',
        'CANCELLED': '已取消'
    };
    return textMap[status] || status;
}

// 渲染操作按钮
function renderActionButtons(invite) {
    let buttons = '';

    if (invite.status === 'SUBMITTED') {
        buttons += `
            <button class="btn btn-success btn-sm" onclick="confirmInvite(${invite.id})">
                ✅ 邀请成功
            </button>
            <button class="btn btn-warning btn-sm" onclick="showCancelModal(${invite.id})">
                ⛔ 驳回
            </button>
        `;
    } else if (invite.status === 'PENDING') {
        buttons += `
            <button class="btn btn-warning btn-sm" onclick="showCancelModal(${invite.id})">
                ⛔ 取消
            </button>
        `;
    }

    // 所有状态都可以删除
    buttons += `
        <button class="btn btn-danger btn-sm" onclick="deleteInvite(${invite.id})">
            🗑️ 删除
        </button>
    `;

    return buttons;
}

// 生成邀请链接
async function generateLink() {
    try {
        const response = await fetch('/api/invite/generate', {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            // 复制到剪贴板
            try {
                await navigator.clipboard.writeText(result.inviteLink);
                alert('邀请链接已生成并复制到剪贴板：\n' + result.inviteLink);
            } catch (clipboardError) {
                // 剪贴板复制失败，显示链接让用户手动复制
                prompt('邀请链接已生成，请手动复制：', result.inviteLink);
            }
            loadInviteList();
        } else {
            alert('生成失败：' + result.message);
        }

    } catch (error) {
        console.error('生成失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// 复制邀请链接
async function copyInviteLink(inviteCode) {
    const link = window.location.origin + '/invite/' + inviteCode;
    try {
        await navigator.clipboard.writeText(link);
        alert('邀请链接已复制：\n' + link);
    } catch (error) {
        prompt('请手动复制邀请链接：', link);
    }
}

// 确认邀请成功
async function confirmInvite(id) {
    if (!confirm('确认该邀请已成功发送？')) return;

    try {
        const response = await fetch(`/api/invite/${id}/confirm`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            alert('操作成功');
            loadInviteList();
        } else {
            alert('操作失败：' + result.message);
        }

    } catch (error) {
        console.error('操作失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// 显示取消/驳回模态框
let currentCancelId = null;
function showCancelModal(id) {
    currentCancelId = id;
    document.getElementById('cancelReasonInput').value = '';
    document.getElementById('cancelModal').style.display = 'flex';
}

// 隐藏模态框
function hideCancelModal() {
    currentCancelId = null;
    document.getElementById('cancelModal').style.display = 'none';
}

// 确认驳回
async function confirmCancel() {
    if (!currentCancelId) return;

    const reason = document.getElementById('cancelReasonInput').value.trim();

    try {
        const response = await fetch(`/api/invite/${currentCancelId}/reject`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ reason: reason || null })
        });

        const result = await response.json();

        if (result.success) {
            alert('驳回成功，用户可重新填写');
            hideCancelModal();
            loadInviteList();
        } else {
            alert('操作失败：' + result.message);
        }

    } catch (error) {
        console.error('操作失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// 删除邀请记录
async function deleteInvite(id) {
    if (!confirm('确认删除该邀请记录？此操作不可恢复！')) return;

    try {
        const response = await fetch(`/api/invite/${id}`, {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            alert('删除成功');
            loadInviteList();
        } else {
            alert('删除失败：' + result.message);
        }

    } catch (error) {
        console.error('删除失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// 刷新数据
function refreshData() {
    loadInviteList();
}

// 退出登录
function logout() {
    if (confirm('确认退出登录？')) {
        fetch('/api/logout', { method: 'POST' })
            .then(() => {
                window.location.href = '/login.html';
            });
    }
}

// 显示错误
function showError(message) {
    const container = document.getElementById('inviteContainer');
    container.innerHTML = `
        <div class="empty-state">
            <div class="empty-icon">😕</div>
            <h3>加载失败</h3>
            <p style="margin-top: 10px;">${escapeHtml(message)}</p>
        </div>
    `;
}

// HTML转义函数
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ==================== Q&A 管理功能 ====================

// Q&A 富文本编辑器（Quill）
let qaAnswerQuill = null;

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

    if (looksLikeHtml(value)) {
        qaAnswerQuill.setContents(qaAnswerQuill.clipboard.convert(value));
    } else {
        qaAnswerQuill.setText(value);
    }
}

function getQAAnswerValue() {
    if (!qaAnswerQuill) {
        return document.getElementById('qaAnswerInput').value.trim();
    }

    const plain = qaAnswerQuill.getText().trim();
    if (!plain) return '';
    return qaAnswerQuill.root.innerHTML;
}

// 显示 Q&A 管理模态框
function showQAManager() {
    document.getElementById('qaModal').style.display = 'flex';
    loadQAList();
}

// 隐藏 Q&A 模态框
function hideQAModal() {
    document.getElementById('qaModal').style.display = 'none';
}

// 加载 Q&A 列表
async function loadQAList() {
    const container = document.getElementById('qaListContainer');
    container.innerHTML = `
        <div class="loading">
            <div class="spinner"></div>
            <div>加载中...</div>
        </div>
    `;

    try {
        const response = await fetch('/api/invite/qa/list');
        const result = await response.json();

        if (result.success) {
            renderQAList(result.data);
        } else {
            container.innerHTML = `<p style="text-align:center;color:#999;">加载失败: ${escapeHtml(result.message)}</p>`;
        }
    } catch (error) {
        console.error('加载 Q&A 失败:', error);
        container.innerHTML = `<p style="text-align:center;color:#999;">网络错误，请稍后重试</p>`;
    }
}

// 全局存储 Q&A 数据用于编辑
let qaDataMap = {};

// 渲染 Q&A 列表
function renderQAList(qaList) {
    const container = document.getElementById('qaListContainer');

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

    // 存储数据供编辑使用
    qaDataMap = {};
    qaList.forEach(qa => {
        qaDataMap[qa.id] = qa;
    });

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

    // 富文本展示（Quill 只读渲染），失败时降级为纯文本
    qaList.forEach((qa) => {
        const host = document.getElementById(`qa-answer-${qa.id}`);
        if (!host) return;

        const value = (qa && qa.answer) ? String(qa.answer) : '';

        if (!window.Quill) {
            host.textContent = value;
            return;
        }

        const quill = new Quill(host, {
            theme: 'snow',
            readOnly: true,
            modules: { toolbar: false }
        });

        if (looksLikeHtml(value)) {
            quill.setContents(quill.clipboard.convert(value));
        } else {
            quill.setText(value);
        }
    });
}

// 显示添加 Q&A 表单
function showAddQAForm() {
    document.getElementById('qaEditTitle').textContent = '添加 Q&A';
    document.getElementById('qaEditId').value = '';
    document.getElementById('qaQuestionInput').value = '';
    ensureQAAnswerQuill();
    setQAAnswerValue('');
    document.getElementById('qaEditModal').style.display = 'flex';
}

// 通过 ID 显示编辑 Q&A 表单（从全局数据中获取）
function showEditQAFormById(id) {
    const qa = qaDataMap[id];
    if (!qa) {
        alert('未找到该 Q&A 数据，请刷新后重试');
        return;
    }
    document.getElementById('qaEditTitle').textContent = '编辑 Q&A';
    document.getElementById('qaEditId').value = id;
    document.getElementById('qaQuestionInput').value = qa.question || '';
    ensureQAAnswerQuill();
    setQAAnswerValue(qa.answer || '');
    document.getElementById('qaEditModal').style.display = 'flex';
}

// 显示编辑 Q&A 表单（兼容旧调用）
function showEditQAForm(id, question, answer) {
    document.getElementById('qaEditTitle').textContent = '编辑 Q&A';
    document.getElementById('qaEditId').value = id;
    document.getElementById('qaQuestionInput').value = question;
    ensureQAAnswerQuill();
    setQAAnswerValue(answer);
    document.getElementById('qaEditModal').style.display = 'flex';
}

// 隐藏 Q&A 编辑模态框
function hideQAEditModal() {
    document.getElementById('qaEditModal').style.display = 'none';
}

// 保存 Q&A
async function saveQA() {
    const id = document.getElementById('qaEditId').value;
    const question = document.getElementById('qaQuestionInput').value.trim();
    const answer = getQAAnswerValue();

    if (!question) {
        alert('请输入问题');
        return;
    }
    if (!answer) {
        alert('请输入答案');
        return;
    }

    try {
        let response;
        if (id) {
            // 更新
            response = await fetch(`/api/invite/qa/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ question, answer })
            });
        } else {
            // 添加
            response = await fetch('/api/invite/qa/add', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ question, answer })
            });
        }

        const result = await response.json();

        if (result.success) {
            alert(id ? '更新成功' : '添加成功');
            hideQAEditModal();
            loadQAList();
        } else {
            alert('操作失败: ' + result.message);
        }
    } catch (error) {
        console.error('保存 Q&A 失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// 切换 Q&A 启用状态
async function toggleQA(id) {
    try {
        const response = await fetch(`/api/invite/qa/${id}/toggle`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            loadQAList();
        } else {
            alert('操作失败: ' + result.message);
        }
    } catch (error) {
        console.error('切换 Q&A 状态失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// 删除 Q&A
async function deleteQA(id) {
    if (!confirm('确认删除该 Q&A？此操作不可恢复！')) return;

    try {
        const response = await fetch(`/api/invite/qa/${id}`, {
            method: 'DELETE'
        });

        const result = await response.json();

        if (result.success) {
            alert('删除成功');
            loadQAList();
        } else {
            alert('删除失败: ' + result.message);
        }
    } catch (error) {
        console.error('删除 Q&A 失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// 上移 Q&A
async function moveQAUp(id) {
    try {
        const response = await fetch(`/api/invite/qa/${id}/move-up`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            loadQAList();
        } else {
            alert('操作失败: ' + result.message);
        }
    } catch (error) {
        console.error('上移 Q&A 失败:', error);
        alert('网络错误，请稍后重试');
    }
}

// 下移 Q&A
async function moveQADown(id) {
    try {
        const response = await fetch(`/api/invite/qa/${id}/move-down`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            loadQAList();
        } else {
            alert('操作失败: ' + result.message);
        }
    } catch (error) {
        console.error('下移 Q&A 失败:', error);
        alert('网络错误，请稍后重试');
    }
}
