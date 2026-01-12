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
