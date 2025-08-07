/**
 * 公告管理系统 - 管理员页面JavaScript
 * 
 * @author 杨宇帆
 * @create 2025-08-07
 */

// 全局变量
let currentPage = 0;
let pageSize = 10;
let totalPages = 0;
let currentSearchParams = {};

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    checkAdminAuth();
    loadStatistics();
    loadAnnouncements();
    setupEventListeners();
});

/**
 * 检查管理员权限
 */
async function checkAdminAuth() {
    try {
        const response = await fetch('/api/login-status');
        const data = await response.json();
        
        if (!data.loggedIn || data.remainingCount !== -1) {
            // 不是超级管理员，跳转到登录页面
            window.location.href = '/admin-login.html';
            return;
        }
    } catch (error) {
        console.error('检查权限失败:', error);
        window.location.href = '/admin-login.html';
    }
}

/**
 * 设置事件监听器
 */
function setupEventListeners() {
    // 搜索输入框回车事件
    document.getElementById('searchInput').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            searchAnnouncements();
        }
    });
    
    // 筛选器变化事件
    document.getElementById('visibilityFilter').addEventListener('change', searchAnnouncements);
    document.getElementById('pinnedFilter').addEventListener('change', searchAnnouncements);
    document.getElementById('typeFilter').addEventListener('change', searchAnnouncements);
}

/**
 * 加载统计信息
 */
async function loadStatistics() {
    try {
        const response = await fetch('/api/announcements/statistics');
        const data = await response.json();
        
        if (response.ok && data.statistics) {
            const stats = data.statistics;
            document.getElementById('totalCount').textContent = stats.totalCount || 0;
            document.getElementById('visibleCount').textContent = stats.visibleCount || 0;
            document.getElementById('hiddenCount').textContent = stats.hiddenCount || 0;
            document.getElementById('pinnedCount').textContent = stats.pinnedCount || 0;
        }
    } catch (error) {
        console.error('加载统计信息失败:', error);
    }
}

/**
 * 加载公告列表
 */
async function loadAnnouncements(page = 0) {
    const container = document.getElementById('announcementsContainer');
    
    // 显示加载状态
    container.innerHTML = `
        <div class="loading">
            <div class="loading-spinner"></div>
            <div>加载中...</div>
        </div>
    `;
    
    try {
        // 构建查询参数
        const params = new URLSearchParams({
            page: page,
            size: pageSize,
            ...currentSearchParams
        });
        
        const response = await fetch(`/api/announcements/admin?${params}`);
        const data = await response.json();
        
        if (response.ok) {
            currentPage = page;
            totalPages = data.totalPages;
            renderAnnouncementsList(data.content);
            updatePagination(data);
        } else {
            throw new Error(data.error || '加载公告列表失败');
        }
    } catch (error) {
        console.error('加载公告列表失败:', error);
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">❌</div>
                <h3>加载失败</h3>
                <p>${error.message}</p>
                <button class="btn btn-primary" onclick="loadAnnouncements()">重试</button>
            </div>
        `;
    }
}

/**
 * 渲染公告列表
 */
function renderAnnouncementsList(announcements) {
    const container = document.getElementById('announcementsContainer');
    
    if (!announcements || announcements.length === 0) {
        container.innerHTML = `
            <div class="empty-state">
                <div class="empty-state-icon">📢</div>
                <h3>暂无公告</h3>
                <p>还没有创建任何公告，点击"新建公告"开始创建吧！</p>
                <a href="/announcement-edit.html" class="btn btn-primary">新建公告</a>
            </div>
        `;
        return;
    }
    
    const tableHTML = `
        <table class="announcements-table">
            <thead>
                <tr>
                    <th>标题</th>
                    <th>类型</th>
                    <th>状态</th>
                    <th>创建时间</th>
                    <th>创建者</th>
                    <th>操作</th>
                </tr>
            </thead>
            <tbody>
                ${announcements.map(announcement => `
                    <tr>
                        <td>
                            ${announcement.title}
                            ${announcement.isPinned ? '<span class="pinned-badge">置顶</span>' : ''}
                        </td>
                        <td>${getTypeLabel(announcement.type)}</td>
                        <td>
                            <span class="status-badge ${announcement.isVisible ? 'status-visible' : 'status-hidden'}">
                                ${announcement.isVisible ? '可见' : '隐藏'}
                            </span>
                        </td>
                        <td>${formatDateTime(announcement.createTime)}</td>
                        <td>${announcement.creator || '系统'}</td>
                        <td>
                            <div class="action-buttons">
                                <button class="btn btn-sm btn-edit" onclick="editAnnouncement(${announcement.id})">
                                    编辑
                                </button>
                                <button class="btn btn-sm btn-toggle" onclick="toggleVisibility(${announcement.id})">
                                    ${announcement.isVisible ? '隐藏' : '显示'}
                                </button>
                                <button class="btn btn-sm btn-toggle" onclick="togglePinned(${announcement.id})">
                                    ${announcement.isPinned ? '取消置顶' : '置顶'}
                                </button>
                                <button class="btn btn-sm btn-delete" onclick="deleteAnnouncement(${announcement.id})">
                                    删除
                                </button>
                            </div>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
    
    container.innerHTML = tableHTML;
}

/**
 * 更新分页信息
 */
function updatePagination(data) {
    const pagination = document.getElementById('pagination');
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const pageInfo = document.getElementById('pageInfo');
    
    if (data.totalPages <= 1) {
        pagination.style.display = 'none';
        return;
    }
    
    pagination.style.display = 'flex';
    prevBtn.disabled = !data.hasPrevious;
    nextBtn.disabled = !data.hasNext;
    pageInfo.textContent = `第 ${currentPage + 1} 页，共 ${data.totalPages} 页`;
}

/**
 * 搜索公告
 */
function searchAnnouncements() {
    const searchInput = document.getElementById('searchInput').value.trim();
    const visibilityFilter = document.getElementById('visibilityFilter').value;
    const pinnedFilter = document.getElementById('pinnedFilter').value;
    const typeFilter = document.getElementById('typeFilter').value;
    
    currentSearchParams = {};
    
    if (searchInput) {
        currentSearchParams.title = searchInput;
    }
    if (visibilityFilter) {
        currentSearchParams.isVisible = visibilityFilter;
    }
    if (pinnedFilter) {
        currentSearchParams.isPinned = pinnedFilter;
    }
    if (typeFilter) {
        currentSearchParams.type = typeFilter;
    }
    
    loadAnnouncements(0);
}

/**
 * 切换页面
 */
function changePage(direction) {
    const newPage = currentPage + direction;
    if (newPage >= 0 && newPage < totalPages) {
        loadAnnouncements(newPage);
    }
}

/**
 * 编辑公告
 */
function editAnnouncement(id) {
    window.location.href = `/announcement-edit.html?id=${id}`;
}

/**
 * 切换公告可见状态
 */
async function toggleVisibility(id) {
    if (!confirm('确定要切换此公告的可见状态吗？')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/announcements/${id}/visibility`, {
            method: 'PATCH'
        });
        
        const data = await response.json();
        
        if (response.ok) {
            showMessage('状态更新成功', 'success');
            loadAnnouncements(currentPage);
            loadStatistics();
        } else {
            throw new Error(data.error || '更新失败');
        }
    } catch (error) {
        console.error('切换可见状态失败:', error);
        showMessage('更新失败: ' + error.message, 'error');
    }
}

/**
 * 切换公告置顶状态
 */
async function togglePinned(id) {
    if (!confirm('确定要切换此公告的置顶状态吗？')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/announcements/${id}/pinned`, {
            method: 'PATCH'
        });
        
        const data = await response.json();
        
        if (response.ok) {
            showMessage('置顶状态更新成功', 'success');
            loadAnnouncements(currentPage);
            loadStatistics();
        } else {
            throw new Error(data.error || '更新失败');
        }
    } catch (error) {
        console.error('切换置顶状态失败:', error);
        showMessage('更新失败: ' + error.message, 'error');
    }
}

/**
 * 删除公告
 */
async function deleteAnnouncement(id) {
    if (!confirm('确定要删除此公告吗？此操作不可恢复！')) {
        return;
    }
    
    try {
        const response = await fetch(`/api/announcements/${id}`, {
            method: 'DELETE'
        });
        
        const data = await response.json();
        
        if (response.ok) {
            showMessage('公告删除成功', 'success');
            loadAnnouncements(currentPage);
            loadStatistics();
        } else {
            throw new Error(data.error || '删除失败');
        }
    } catch (error) {
        console.error('删除公告失败:', error);
        showMessage('删除失败: ' + error.message, 'error');
    }
}

/**
 * 刷新数据
 */
function refreshData() {
    loadStatistics();
    loadAnnouncements(currentPage);
    showMessage('数据已刷新', 'success');
}

/**
 * 退出登录
 */
async function logout() {
    if (!confirm('确定要退出登录吗？')) {
        return;
    }
    
    try {
        await fetch('/api/logout', { method: 'POST' });
        window.location.href = '/admin-login.html';
    } catch (error) {
        console.error('退出登录失败:', error);
        window.location.href = '/admin-login.html';
    }
}

/**
 * 获取公告类型标签
 */
function getTypeLabel(type) {
    const typeLabels = {
        'GENERAL': '普通公告',
        'IMPORTANT': '重要通知',
        'MAINTENANCE': '系统维护',
        'UPDATE': '功能更新'
    };
    return typeLabels[type] || type;
}

/**
 * 格式化日期时间
 */
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '-';
    const date = new Date(dateTimeStr);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}

/**
 * 显示消息提示
 */
function showMessage(message, type = 'info') {
    // 创建消息元素
    const messageEl = document.createElement('div');
    messageEl.className = `alert alert-${type}`;
    messageEl.textContent = message;
    messageEl.style.position = 'fixed';
    messageEl.style.top = '20px';
    messageEl.style.right = '20px';
    messageEl.style.zIndex = '9999';
    messageEl.style.minWidth = '300px';
    messageEl.style.animation = 'slideInRight 0.3s ease-out';
    
    document.body.appendChild(messageEl);
    
    // 3秒后自动移除
    setTimeout(() => {
        messageEl.style.animation = 'slideOutRight 0.3s ease-out';
        setTimeout(() => {
            if (messageEl.parentNode) {
                messageEl.parentNode.removeChild(messageEl);
            }
        }, 300);
    }, 3000);
}
