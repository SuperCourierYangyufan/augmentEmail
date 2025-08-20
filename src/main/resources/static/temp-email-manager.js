/**
 * 临时邮箱管理系统前端交互脚本
 * 实现邮箱列表管理、创建、封禁、获取验证码等功能
 * 
 * @author 杨宇帆
 * @create 2025-08-02
 */

// 全局变量
let emailList = [];
let currentVerificationCode = '';
let verificationCodeTask = null; // 当前验证码获取任务
let currentVerificationUrl = '';
let verificationUrlTask = null; // 当前校验地址获取任务

// 分页相关变量
let currentPage = 1;
let pageSize = 10;
let totalEmails = 0;

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    console.log('临时邮箱管理系统初始化...');

    // 检查登录状态
    checkLoginStatus().then(loginInfo => {
        if (loginInfo.isLoggedIn) {
            loadEmailStatistics();
            loadEmailList();
            loadAnnouncements(); // 加载公告

            // 检查是否为超级管理员，显示公告管理按钮
            checkSuperAdminStatus();

            // 设置定时刷新（每30秒）
            setInterval(function() {
                loadEmailStatistics();
                loadEmailList();
                loadAnnouncements(); // 定时刷新公告
            }, 30000);
        } else {
            // 未登录，重定向到登录页面
            window.location.href = '/login.html';
        }
    });
});

/**
 * 加载邮箱统计信息
 */
async function loadEmailStatistics() {
    try {
        const response = await fetch('/api/temp-email/statistics');

        // 检查是否未授权
        if (handleUnauthorizedResponse(response)) {
            return;
        }

        const result = await response.json();

        if (result.success) {
            const stats = result.data;
            document.getElementById('totalEmails').textContent = stats.totalEmails || 0;
            document.getElementById('activeEmails').textContent = stats.activeEmails || 0;
            document.getElementById('bannedEmails').textContent = stats.bannedEmails || 0;
        } else {
            console.error('获取统计信息失败:', result.message);
        }
    } catch (error) {
        console.error('获取统计信息异常:', error);
    }
}

/**
 * 加载邮箱列表
 */
async function loadEmailList(page = currentPage) {
    try {
        const response = await fetch(`/api/temp-email/list?page=${page}&size=${pageSize}`);

        // 检查是否未授权
        if (handleUnauthorizedResponse(response)) {
            return;
        }

        const result = await response.json();

        if (result.success) {
            emailList = result.data.content || [];
            totalEmails = result.data.totalElements || 0;
            currentPage = result.data.number + 1; // Spring Data JPA页码从0开始
            renderEmailList();
            renderPagination();
        } else {
            showError('获取邮箱列表失败: ' + result.message);
        }
    } catch (error) {
        console.error('获取邮箱列表异常:', error);
        showError('网络错误，请检查连接');
    }
}

/**
 * 渲染邮箱列表 - 全新科技感设计
 */
function renderEmailList() {
    const container = document.getElementById('emailListContent');

    if (emailList.length === 0) {
        container.innerHTML = `
            <div class="empty-state-modern">
                <div class="empty-icon">
                    <svg width="80" height="80" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                        <path d="M3 8L10.89 13.26C11.2187 13.4793 11.6049 13.5963 12 13.5963C12.3951 13.5963 12.7813 13.4793 13.11 13.26L21 8M5 19H19C19.5304 19 20.0391 18.7893 20.4142 18.4142C20.7893 18.0391 21 17.5304 21 17V7C21 6.46957 20.7893 5.96086 20.4142 5.58579C20.0391 5.21071 19.5304 5 19 5H5C4.46957 5 3.96086 5.21071 3.58579 5.58579C3.21071 5.96086 3 6.46957 3 7V17C3 17.5304 3.21071 18.0391 3.58579 18.4142C3.96086 18.7893 4.46957 19 5 19Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                </div>
                <h3>暂无邮箱数据</h3>
                <p>点击"新建邮箱"按钮创建您的第一个临时邮箱</p>
                <div class="empty-glow"></div>
            </div>
        `;
        return;
    }

    let html = '';
    emailList.forEach((email, index) => {
        const statusClass = email.status === 'ACTIVE' ? 'status-active' : 'status-banned';
        const statusIcon = email.status === 'ACTIVE' ? '🟢' : '🔴';

        // 优化时间显示：显示天数和小时数
        let generatedText = '';
        if (email.generatedDays > 0) {
            const remainingHours = email.generatedHours % 24;
            generatedText = remainingHours > 0
                ? `${email.generatedDays}天 ${remainingHours}小时前`
                : `${email.generatedDays}天前`;
        } else {
            generatedText = `${email.generatedHours}小时前`;
        }

        html += `
            <div class="email-card-modern" style="animation-delay: ${index * 0.1}s" data-email-id="${email.id}">
                <div class="email-card-glow"></div>
                <div class="email-card-content">
                    <div class="email-header">
                        <div class="email-status-badge ${statusClass}">
                            <span class="status-icon">${statusIcon}</span>
                            <span class="status-text">${email.statusDescription}</span>
                        </div>
                        <div class="email-time">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                                <polyline points="12,6 12,12 16,14" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                            <span>${generatedText}</span>
                        </div>
                    </div>

                    <div class="email-address-section">
                        <div class="email-icon">
                            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M4 4H20C21.1 4 22 4.9 22 6V18C22 19.1 21.1 20 20 20H4C2.9 20 2 19.1 2 18V6C2 4.9 2.9 4 4 4Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                <polyline points="22,6 12,13 2,6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </div>
                        <div class="email-address-text">${email.emailAddress}</div>
                        <button class="copy-btn" onclick="copyEmailAddress('${email.emailAddress}')" title="复制邮箱地址">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <rect x="9" y="9" width="13" height="13" rx="2" ry="2" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                <path d="M5 15H4C3.46957 15 2.96086 14.7893 2.58579 14.4142C2.21071 14.0391 2 13.5304 2 13V4C2 3.46957 2.21071 2.96086 2.58579 2.58579C2.96086 2.21071 3.46957 2 4 2H13C13.5304 2 14.0391 2.21071 14.4142 2.58579C14.7893 2.96086 15 3.46957 15 4V5" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                        </button>
                    </div>

                    <div class="email-actions-modern">
                        ${email.status === 'ACTIVE' ? `
                            <button class="action-btn-modern btn-verify-modern" onclick="getVerificationCode('${email.emailAddress}')" title="获取验证码">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <path d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                                <span>获取验证码</span>
                                <div class="btn-glow"></div>
                            </button>
                            <button class="action-btn-modern btn-url-modern" onclick="getVerificationUrl('${email.emailAddress}')" title="获取校验地址">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <path d="M10 13A5 5 0 0 0 7.54 7.54L4.46 4.46A5 5 0 0 0 4.46 19.54L7.54 16.46A5 5 0 0 0 13 10L10 13Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                    <path d="M14 11A5 5 0 0 0 16.46 16.46L19.54 19.54A5 5 0 0 0 19.54 4.46L16.46 7.54A5 5 0 0 0 11 14L14 11Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                                <span>获取校验地址</span>
                                <div class="btn-glow"></div>
                            </button>
                            <button class="action-btn-modern btn-delete-modern" onclick="deleteAllEmails('${email.emailAddress}')" title="删除所有邮件">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <polyline points="3,6 5,6 21,6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                    <path d="M19 6V20C19 20.5304 18.7893 21.0391 18.4142 21.4142C18.0391 21.7893 17.5304 22 17 22H7C6.46957 22 5.96086 21.7893 5.58579 21.4142C5.21071 21.0391 5 20.5304 5 20V6M8 6V4C8 3.46957 8.21071 2.96086 8.58579 2.58579C8.96086 2.21071 9.46957 2 10 2H14C14.5304 2 15.0391 2.21071 15.4142 2.58579C15.7893 2.96086 16 3.46957 16 4V6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                                <span>删除所有邮件</span>
                                <div class="btn-glow"></div>
                            </button>
                            <button class="action-btn-modern btn-ban-modern" onclick="banEmail(${email.id})" title="封禁邮箱">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                                    <path d="M4.93 4.93L19.07 19.07" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                                <span>封禁</span>
                                <div class="btn-glow"></div>
                            </button>
                        ` : `
                            <div class="banned-notice-modern">
                                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                                    <path d="M4.93 4.93L19.07 19.07" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                                <span>邮箱已被封禁</span>
                            </div>
                        `}
                    </div>
                </div>
            </div>
        `;
    });

    container.innerHTML = html;
}

/**
 * 渲染分页组件
 */
function renderPagination() {
    const totalPages = Math.ceil(totalEmails / pageSize);

    if (totalPages <= 1) {
        document.getElementById('pagination').innerHTML = '';
        return;
    }

    let paginationHtml = '<div class="pagination-container">';
    paginationHtml += '<div class="pagination-info">';
    paginationHtml += `共 ${totalEmails} 个邮箱，第 ${currentPage}/${totalPages} 页`;
    paginationHtml += '</div>';

    paginationHtml += '<div class="pagination-controls">';

    // 上一页按钮
    if (currentPage > 1) {
        paginationHtml += `<button class="btn btn-sm" onclick="changePage(${currentPage - 1})">上一页</button>`;
    } else {
        paginationHtml += '<button class="btn btn-sm" disabled>上一页</button>';
    }

    // 页码按钮
    const startPage = Math.max(1, currentPage - 2);
    const endPage = Math.min(totalPages, currentPage + 2);

    if (startPage > 1) {
        paginationHtml += '<button class="btn btn-sm" onclick="changePage(1)">1</button>';
        if (startPage > 2) {
            paginationHtml += '<span class="pagination-ellipsis">...</span>';
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        if (i === currentPage) {
            paginationHtml += `<button class="btn btn-sm active">${i}</button>`;
        } else {
            paginationHtml += `<button class="btn btn-sm" onclick="changePage(${i})">${i}</button>`;
        }
    }

    if (endPage < totalPages) {
        if (endPage < totalPages - 1) {
            paginationHtml += '<span class="pagination-ellipsis">...</span>';
        }
        paginationHtml += `<button class="btn btn-sm" onclick="changePage(${totalPages})">${totalPages}</button>`;
    }

    // 下一页按钮
    if (currentPage < totalPages) {
        paginationHtml += `<button class="btn btn-sm" onclick="changePage(${currentPage + 1})">下一页</button>`;
    } else {
        paginationHtml += '<button class="btn btn-sm" disabled>下一页</button>';
    }

    paginationHtml += '</div>';
    paginationHtml += '</div>';

    document.getElementById('pagination').innerHTML = paginationHtml;
}

/**
 * 切换页面
 */
function changePage(page) {
    if (page < 1 || page > Math.ceil(totalEmails / pageSize)) {
        return;
    }

    currentPage = page;
    loadEmailList(page);
}

/**
 * 刷新邮箱列表
 */
function refreshEmailList() {
    loadEmailStatistics();
    loadEmailList(currentPage);
    showSuccess('邮箱列表已刷新');
}

/**
 * 直接创建新邮箱（无需确认弹窗）
 */
async function createEmailDirectly(event) {
    const createBtn = event.target;

    // 显示加载状态
    createBtn.disabled = true;
    const originalText = createBtn.textContent;
    createBtn.textContent = '生成中...';

    try {
        // 直接调用生成邮箱的API
        const response = await fetch('/api/temp-email/generate', {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            showSuccess('邮箱生成成功并已自动加入邮箱库: ' + result.emailAddress);
            loadEmailStatistics();
            loadEmailList();
            // 直接使用返回结果中的用户信息更新剩余次数显示
            if (result.remainingCount !== undefined && result.maxCount !== undefined) {
                updateUserInfoDisplay(result.remainingCount, result.maxCount);
            } else {
                // 如果返回结果中没有用户信息，则调用API获取
                updateUserInfo();
            }
        } else {
            showError('生成邮箱失败: ' + result.message);
        }
    } catch (error) {
        console.error('生成邮箱异常:', error);
        showError('网络错误，请重试');
    } finally {
        createBtn.disabled = false;
        createBtn.textContent = originalText;
    }
}



/**
 * 创建新邮箱（仅生成邮箱地址，不保存到数据库）
 */
async function createEmail(event) {
    const createBtn = event.target;

    // 显示加载状态
    createBtn.disabled = true;
    createBtn.textContent = '生成中...';

    try {
        // 直接调用生成邮箱的API
        const response = await fetch('/api/temp-email/generate', {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            hideCreateModal();
            showSuccess('邮箱生成成功并已自动加入邮箱库: ' + result.emailAddress);
            loadEmailStatistics();
            loadEmailList();
            // 直接使用返回结果中的用户信息更新剩余次数显示
            if (result.remainingCount !== undefined && result.maxCount !== undefined) {
                updateUserInfoDisplay(result.remainingCount, result.maxCount);
            } else {
                // 如果返回结果中没有用户信息，则调用API获取
                updateUserInfo();
            }
        } else {
            showError('生成邮箱失败: ' + result.message);
        }
    } catch (error) {
        console.error('生成邮箱异常:', error);
        showError('网络错误，请重试');
    } finally {
        createBtn.disabled = false;
        createBtn.textContent = '生成邮箱';
    }
}







/**
 * 封禁邮箱
 */
async function banEmail(emailId) {
    if (!confirm('确定要封禁这个邮箱吗？封禁后将无法使用。')) {
        return;
    }

    try {
        const response = await fetch(`/api/temp-email/ban/${emailId}`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            showSuccess('邮箱已封禁');
            loadEmailStatistics();
            loadEmailList();
        } else {
            showError('封禁失败: ' + result.message);
        }
    } catch (error) {
        console.error('封禁邮箱异常:', error);
        showError('网络错误，请重试');
    }
}

/**
 * 删除指定邮箱的所有邮件
 */
async function deleteAllEmails(emailAddress) {
    if (!confirm(`确定要删除邮箱 ${emailAddress} 的所有邮件吗？此操作不可撤销！`)) {
        return;
    }

    try {
        showSuccess('正在删除邮件，请稍候...');

        const response = await fetch(`/api/temp-email/delete-all-emails?emailAddress=${encodeURIComponent(emailAddress)}`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            showSuccess(result.message);
            // 刷新列表以显示最新状态
            loadEmailStatistics();
            loadEmailList();
        } else {
            showError('删除失败: ' + result.message);
        }
    } catch (error) {
        console.error('删除邮件异常:', error);
        showError('网络错误，请重试');
    }
}

/**
 * 获取验证码（带重试机制）
 */
async function getVerificationCode(emailAddress) {
    // 如果已有任务在运行，先取消它
    if (verificationCodeTask) {
        verificationCodeTask.cancelled = true;
        verificationCodeTask = null;
    }

    showVerificationModal(emailAddress);

    // 创建新的任务对象
    verificationCodeTask = { cancelled: false };
    const currentTask = verificationCodeTask;

    try {
        const verificationCode = await getVerificationCodeWithRetry(emailAddress, 20, currentTask);

        // 检查任务是否被取消
        if (currentTask.cancelled) {
            console.log('验证码获取任务已被取消');
            return;
        }

        if (verificationCode) {
            currentVerificationCode = verificationCode;
            document.getElementById('verificationCode').textContent = verificationCode;

            // 更新使用统计
            loadEmailStatistics();
            loadEmailList();
        } else {
            document.getElementById('verificationCode').textContent = '获取失败';
            showError('获取验证码失败，已重试20次');
        }
    } catch (error) {
        // 检查任务是否被取消
        if (currentTask.cancelled) {
            console.log('验证码获取任务已被取消');
            return;
        }

        console.error('获取验证码异常:', error);
        document.getElementById('verificationCode').textContent = '网络错误';
        showError('网络错误，请重试');
    } finally {
        // 清理任务引用
        if (verificationCodeTask === currentTask) {
            verificationCodeTask = null;
        }
    }
}

/**
 * 获取校验地址（带重试机制）
 */
async function getVerificationUrl(emailAddress) {
    // 如果已有任务在运行，先取消它
    if (verificationUrlTask) {
        verificationUrlTask.cancelled = true;
        verificationUrlTask = null;
    }

    // 显示校验地址模态框
    showVerificationUrlModal(emailAddress);

    // 创建新的任务对象
    verificationUrlTask = { cancelled: false };
    const currentTask = verificationUrlTask;

    try {
        const verificationUrl = await getVerificationUrlWithRetry(emailAddress, 20, currentTask);

        // 检查任务是否被取消
        if (currentTask.cancelled) {
            console.log('校验地址获取任务已被取消');
            return;
        }

        if (verificationUrl) {
            currentVerificationUrl = verificationUrl;
            document.getElementById('verificationUrl').textContent = verificationUrl;

            // 更新使用统计
            loadEmailStatistics();
            loadEmailList();
        } else {
            document.getElementById('verificationUrl').textContent = '获取失败';
            showError('获取校验地址失败，已重试20次');
        }
    } catch (error) {
        // 检查任务是否被取消
        if (currentTask.cancelled) {
            console.log('校验地址获取任务已被取消');
            return;
        }

        console.error('获取校验地址异常:', error);
        document.getElementById('verificationUrl').textContent = '网络错误';
        showError('网络错误，请重试');
    } finally {
        // 清理任务引用
        if (verificationUrlTask === currentTask) {
            verificationUrlTask = null;
        }
    }
}

/**
 * 获取验证码重试机制
 * @param {string} emailAddress 邮箱地址
 * @param {number} maxRetries 最大重试次数
 * @param {object} task 任务对象，用于取消操作
 * @returns {Promise<string|null>} 验证码或null
 */
async function getVerificationCodeWithRetry(emailAddress, maxRetries = 20, task = null) {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
        // 检查任务是否被取消
        if (task && task.cancelled) {
            console.log('验证码获取任务被取消');
            return null;
        }

        try {
            console.log(`尝试获取验证码，第 ${attempt}/${maxRetries} 次`);

            // 更新验证码显示状态
            if (document.getElementById('verificationCode')) {
                document.getElementById('verificationCode').textContent = `正在获取... (${attempt}/${maxRetries})`;
            }

            const response = await fetch(`/api/temp-email/verification-code?emailAddress=${encodeURIComponent(emailAddress)}`);
            const result = await response.json();

            // 再次检查任务是否被取消
            if (task && task.cancelled) {
                console.log('验证码获取任务被取消');
                return null;
            }

            if (result.success && result.verificationCode) {
                console.log(`第 ${attempt} 次尝试成功获取验证码: ${result.verificationCode}`);
                return result.verificationCode;
            } else {
                console.warn(`第 ${attempt} 次尝试失败: ${result.message}`);

                // 如果不是最后一次尝试，等待5秒后重试
                if (attempt < maxRetries) {
                    await new Promise(resolve => {
                        const timeoutId = setTimeout(() => {
                            resolve();
                        }, 5000);

                        // 如果任务被取消，立即解决Promise
                        if (task) {
                            const checkCancellation = () => {
                                if (task.cancelled) {
                                    clearTimeout(timeoutId);
                                    resolve();
                                } else {
                                    setTimeout(checkCancellation, 100);
                                }
                            };
                            checkCancellation();
                        }
                    });
                }
            }
        } catch (error) {
            console.error(`第 ${attempt} 次尝试异常:`, error);

            // 检查任务是否被取消
            if (task && task.cancelled) {
                console.log('验证码获取任务被取消');
                return null;
            }

            // 如果不是最后一次尝试，等待5秒后重试
            if (attempt < maxRetries) {
                await new Promise(resolve => {
                    const timeoutId = setTimeout(() => {
                        resolve();
                    }, 5000);

                    // 如果任务被取消，立即解决Promise
                    if (task) {
                        const checkCancellation = () => {
                            if (task.cancelled) {
                                clearTimeout(timeoutId);
                                resolve();
                            } else {
                                setTimeout(checkCancellation, 100);
                            }
                        };
                        checkCancellation();
                    }
                });
            }
        }
    }

    console.error(`获取验证码失败，已重试 ${maxRetries} 次`);
    return null;
}

/**
 * 获取校验地址重试机制
 * @param {string} emailAddress 邮箱地址
 * @param {number} maxRetries 最大重试次数
 * @param {object} task 任务对象，用于取消操作
 * @returns {Promise<string|null>} 校验地址或null
 */
async function getVerificationUrlWithRetry(emailAddress, maxRetries = 20, task = null) {
    for (let attempt = 1; attempt <= maxRetries; attempt++) {
        // 检查任务是否被取消
        if (task && task.cancelled) {
            console.log('校验地址获取任务被取消');
            return null;
        }

        try {
            console.log(`尝试获取校验地址，第 ${attempt}/${maxRetries} 次`);

            // 更新校验地址显示状态
            if (document.getElementById('verificationUrl')) {
                document.getElementById('verificationUrl').textContent = `正在获取... (${attempt}/${maxRetries})`;
            }

            const response = await fetch(`/api/temp-email/verification-url?emailAddress=${encodeURIComponent(emailAddress)}`, {
                method: 'GET',
                credentials: 'include'
            });

            // 检查任务是否被取消
            if (task && task.cancelled) {
                console.log('校验地址获取任务被取消');
                return null;
            }

            if (response.ok) {
                const result = await response.json();

                // 再次检查任务是否被取消
                if (task && task.cancelled) {
                    console.log('校验地址获取任务被取消');
                    return null;
                }

                if (result.success && result.verificationUrl) {
                    console.log(`第 ${attempt} 次尝试成功获取校验地址: ${result.verificationUrl}`);
                    return result.verificationUrl;
                } else {
                    console.warn(`第 ${attempt} 次尝试失败: ${result.message}`);

                    // 如果不是最后一次尝试，等待5秒后重试
                    if (attempt < maxRetries) {
                        console.log(`等待5秒后进行第 ${attempt + 1} 次尝试...`);
                        await new Promise(resolve => setTimeout(resolve, 5000));
                    }
                }
            } else {
                console.error(`第 ${attempt} 次尝试HTTP错误: ${response.status}`);

                // 如果不是最后一次尝试，等待5秒后重试
                if (attempt < maxRetries) {
                    console.log(`等待5秒后进行第 ${attempt + 1} 次尝试...`);
                    await new Promise(resolve => setTimeout(resolve, 5000));
                }
            }
        } catch (error) {
            console.error(`第 ${attempt} 次尝试异常: ${error.message}`);

            // 检查任务是否被取消
            if (task && task.cancelled) {
                console.log('校验地址获取任务被取消');
                return null;
            }

            // 如果不是最后一次尝试，等待5秒后重试
            if (attempt < maxRetries) {
                console.log(`等待5秒后进行第 ${attempt + 1} 次尝试...`);
                await new Promise(resolve => setTimeout(resolve, 5000));
            }
        }
    }

    console.error(`获取校验地址失败，已重试 ${maxRetries} 次`);
    return null;
}

/**
 * 显示验证码模态框
 */
function showVerificationModal(emailAddress) {
    document.getElementById('verificationModal').style.display = 'block';
    document.getElementById('verificationEmailAddress').textContent = `邮箱: ${emailAddress}`;
    document.getElementById('verificationCode').textContent = '正在获取...';
    currentVerificationCode = '';
}

/**
 * 隐藏验证码模态框
 */
function hideVerificationModal() {
    // 取消正在进行的验证码获取任务
    if (verificationCodeTask) {
        verificationCodeTask.cancelled = true;
        verificationCodeTask = null;
        console.log('已取消验证码获取任务');
    }

    document.getElementById('verificationModal').style.display = 'none';
}

/**
 * 显示校验地址模态框
 */
function showVerificationUrlModal(emailAddress) {
    document.getElementById('verificationUrlEmailAddress').textContent = emailAddress;
    document.getElementById('verificationUrl').textContent = '正在获取...';
    document.getElementById('verificationUrlModal').style.display = 'block';
    currentVerificationUrl = '';
}

/**
 * 隐藏校验地址模态框
 */
function hideVerificationUrlModal() {
    // 取消正在进行的校验地址获取任务
    if (verificationUrlTask) {
        verificationUrlTask.cancelled = true;
        verificationUrlTask = null;
        console.log('已取消校验地址获取任务');
    }

    document.getElementById('verificationUrlModal').style.display = 'none';
}

/**
 * 复制验证码
 */
function copyVerificationCode() {
    if (!currentVerificationCode) {
        showError('没有可复制的验证码');
        return;
    }
    
    navigator.clipboard.writeText(currentVerificationCode).then(() => {
        showSuccess('验证码已复制到剪贴板');
    }).catch(() => {
        // 降级方案
        const textArea = document.createElement('textarea');
        textArea.value = currentVerificationCode;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showSuccess('验证码已复制到剪贴板');
    });
}

/**
 * 复制校验地址
 */
function copyVerificationUrl() {
    if (!currentVerificationUrl) {
        showError('没有可复制的校验地址');
        return;
    }

    navigator.clipboard.writeText(currentVerificationUrl).then(() => {
        showSuccess('校验地址已复制到剪贴板');
    }).catch(() => {
        // 降级方案
        const textArea = document.createElement('textarea');
        textArea.value = currentVerificationUrl;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showSuccess('校验地址已复制到剪贴板');
    });
}

/**
 * 刷新邮箱列表
 */
function refreshEmailList() {
    loadEmailStatistics();
    loadEmailList();
    showSuccess('列表已刷新');
}

/**
 * 复制邮箱地址到剪贴板
 */
async function copyEmailAddress(emailAddress) {
    try {
        if (navigator.clipboard && window.isSecureContext) {
            // 使用现代的 Clipboard API
            await navigator.clipboard.writeText(emailAddress);
        } else {
            // 降级到传统方法
            const textArea = document.createElement('textarea');
            textArea.value = emailAddress;
            textArea.style.position = 'fixed';
            textArea.style.left = '-999999px';
            textArea.style.top = '-999999px';
            document.body.appendChild(textArea);
            textArea.focus();
            textArea.select();
            document.execCommand('copy');
            document.body.removeChild(textArea);
        }
        showSuccess(`邮箱地址已复制: ${emailAddress}`);
    } catch (error) {
        console.error('复制失败:', error);
        showError('复制失败，请手动复制邮箱地址');
    }
}

/**
 * 显示成功消息
 */
function showSuccess(message) {
    showNotification(message, 'success');
}

/**
 * 显示错误消息
 */
function showError(message) {
    showNotification(message, 'error');
}

/**
 * 显示通知消息
 */
function showNotification(message, type = 'info') {
    // 创建通知元素
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 15px 20px;
        border-radius: 8px;
        color: white;
        font-weight: 600;
        z-index: 10000;
        animation: slideInRight 0.3s ease-out;
        max-width: 400px;
        box-shadow: 0 4px 20px rgba(0,0,0,0.2);
    `;
    
    // 根据类型设置样式
    switch (type) {
        case 'success':
            notification.style.background = 'linear-gradient(45deg, #51cf66 0%, #40c057 100%)';
            break;
        case 'error':
            notification.style.background = 'linear-gradient(45deg, #ff6b6b 0%, #ee5a52 100%)';
            break;
        default:
            notification.style.background = 'linear-gradient(45deg, #4facfe 0%, #00f2fe 100%)';
    }
    
    notification.textContent = message;
    document.body.appendChild(notification);
    
    // 3秒后自动移除
    setTimeout(() => {
        notification.style.animation = 'slideOutRight 0.3s ease-out';
        setTimeout(() => {
            if (notification.parentNode) {
                notification.parentNode.removeChild(notification);
            }
        }, 300);
    }, 3000);
}

// 点击模态框外部关闭
window.onclick = function(event) {
    const verificationModal = document.getElementById('verificationModal');

    if (event.target === verificationModal) {
        hideVerificationModal();
    }
}

// 添加CSS动画
const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            opacity: 0;
            transform: translateX(100%);
        }
        to {
            opacity: 1;
            transform: translateX(0);
        }
    }
    
    @keyframes slideOutRight {
        from {
            opacity: 1;
            transform: translateX(0);
        }
        to {
            opacity: 0;
            transform: translateX(100%);
        }
    }
`;
document.head.appendChild(style);

/**
 * 检查用户登录状态
 * @returns {Promise<Object>} 登录状态信息
 */
async function checkLoginStatus() {
    try {
        const response = await fetch('/api/login-status');
        const result = await response.json();

        if (result.loggedIn) {
            // 显示用户信息
            showUserInfo(result);
            return {
                isLoggedIn: true,
                remainingCount: result.remainingCount,
                isSuperAdmin: result.remainingCount === -1
            };
        } else {
            // 隐藏用户信息
            hideUserInfo();
            return {
                isLoggedIn: false,
                remainingCount: 0,
                isSuperAdmin: false
            };
        }
    } catch (error) {
        console.error('检查登录状态失败:', error);
        hideUserInfo();
        return {
            isLoggedIn: false,
            remainingCount: 0,
            isSuperAdmin: false
        };
    }
}

/**
 * 显示用户信息
 * @param {Object} userInfo 用户信息
 */
function showUserInfo(userInfo) {
    const userInfoElement = document.getElementById('userInfo');
    const userKeyInfoElement = document.getElementById('userKeyInfo');
    const logoutBtnElement = document.getElementById('logoutBtn');

    if (userInfoElement && userKeyInfoElement && logoutBtnElement) {
        userKeyInfoElement.textContent = `剩余次数: ${userInfo.remainingCount}/${userInfo.maxCount}`;
        userInfoElement.style.display = 'block';
        logoutBtnElement.style.display = 'block';
    }
}

/**
 * 更新用户信息（剩余次数等）
 */
async function updateUserInfo() {
    try {
        const response = await fetch('/api/login-status');
        const result = await response.json();

        if (result.loggedIn) {
            showUserInfo(result);
        }
    } catch (error) {
        console.error('更新用户信息失败:', error);
    }
}

/**
 * 直接更新用户剩余次数显示
 * @param {number} remainingCount 剩余次数
 * @param {number} maxCount 最大次数
 */
function updateUserInfoDisplay(remainingCount, maxCount) {
    const userKeyInfoElement = document.getElementById('userKeyInfo');
    if (userKeyInfoElement) {
        userKeyInfoElement.textContent = `剩余次数: ${remainingCount}/${maxCount}`;
    }
}

/**
 * 隐藏用户信息
 */
function hideUserInfo() {
    const userInfoElement = document.getElementById('userInfo');
    const logoutBtnElement = document.getElementById('logoutBtn');

    if (userInfoElement && logoutBtnElement) {
        userInfoElement.style.display = 'none';
        logoutBtnElement.style.display = 'none';
    }
}

/**
 * 退出登录
 */
async function logout() {
    try {
        const response = await fetch('/api/logout', {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            showNotification('退出登录成功', 'success');
            setTimeout(() => {
                window.location.href = '/login.html';
            }, 1000);
        } else {
            showNotification('退出登录失败: ' + result.message, 'error');
        }
    } catch (error) {
        console.error('退出登录失败:', error);
        showNotification('退出登录失败，网络错误', 'error');
    }
}

/**
 * 处理未授权的API响应
 * @param {Response} response HTTP响应对象
 */
function handleUnauthorizedResponse(response) {
    if (response.status === 401) {
        showNotification('登录已过期，请重新登录', 'error');
        setTimeout(() => {
            window.location.href = '/login.html';
        }, 2000);
        return true;
    }
    return false;
}

// ==================== 公告相关功能 ====================

/**
 * 加载公告列表
 */
async function loadAnnouncements() {
    try {
        const response = await fetch('/api/announcements/sidebar');

        if (response.ok) {
            const data = await response.json();
            renderAnnouncements(data.announcements || []);
        } else {
            console.warn('加载公告失败:', response.status);
            renderAnnouncements([]);
        }
    } catch (error) {
        console.error('加载公告失败:', error);
        renderAnnouncements([]);
    }
}

/**
 * 渲染公告列表
 */
function renderAnnouncements(announcements) {
    const container = document.getElementById('announcementsContent');

    if (!announcements || announcements.length === 0) {
        container.innerHTML = `
            <div class="no-announcements">
                <div class="no-announcements-icon">📭</div>
                <div>暂无公告</div>
            </div>
        `;
        return;
    }

    const announcementsHTML = announcements.map(announcement => `
        <div class="announcement-item ${announcement.isPinned ? 'pinned' : ''}"
             onclick="showAnnouncementDetail(${announcement.id})">
            <div class="announcement-title">${escapeHtml(announcement.title)}</div>
            <div class="announcement-meta">
                <span class="announcement-type ${announcement.type.toLowerCase()}">${getTypeLabel(announcement.type)}</span>
                <span class="announcement-date">${formatDate(announcement.createTime)}</span>
            </div>
        </div>
    `).join('');

    container.innerHTML = announcementsHTML;
}

/**
 * 切换公告展开/收起状态
 */
function toggleAnnouncements() {
    const content = document.getElementById('announcementsContent');
    const toggle = document.getElementById('announcementsToggle');

    if (content.classList.contains('collapsed')) {
        content.classList.remove('collapsed');
        toggle.classList.remove('collapsed');
        toggle.textContent = '🔽';
    } else {
        content.classList.add('collapsed');
        toggle.classList.add('collapsed');
        toggle.textContent = '▶️';
    }
}

/**
 * 显示公告详情
 */
async function showAnnouncementDetail(announcementId) {
    try {
        const response = await fetch(`/api/announcements/${announcementId}`);

        if (response.ok) {
            const data = await response.json();
            const announcement = data.announcement;

            // 填充模态框内容
            document.getElementById('announcementModalTitle').textContent = announcement.title;
            document.getElementById('announcementModalType').textContent = getTypeLabel(announcement.type);
            document.getElementById('announcementModalType').className = `announcement-type ${announcement.type.toLowerCase()}`;
            document.getElementById('announcementModalDate').textContent = formatDateTime(announcement.createTime);
            document.getElementById('announcementModalContent').innerHTML = announcement.content;

            // 显示模态框
            document.getElementById('announcementModal').style.display = 'block';
        } else {
            showNotification('加载公告详情失败', 'error');
        }
    } catch (error) {
        console.error('加载公告详情失败:', error);
        showNotification('加载公告详情失败', 'error');
    }
}

/**
 * 关闭公告详情模态框
 */
function closeAnnouncementModal() {
    document.getElementById('announcementModal').style.display = 'none';
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
 * 格式化日期（简短格式）
 */
function formatDate(dateTimeStr) {
    if (!dateTimeStr) return '';
    const date = new Date(dateTimeStr);
    const now = new Date();
    const diffTime = now - date;
    const diffDays = Math.floor(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays === 0) {
        return '今天';
    } else if (diffDays === 1) {
        return '昨天';
    } else if (diffDays < 7) {
        return `${diffDays}天前`;
    } else {
        return date.toLocaleDateString('zh-CN', {
            month: '2-digit',
            day: '2-digit'
        });
    }
}

/**
 * 格式化日期时间（完整格式）
 */
function formatDateTime(dateTimeStr) {
    if (!dateTimeStr) return '';
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
 * HTML转义
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// 点击模态框外部关闭
document.addEventListener('click', function(event) {
    const modal = document.getElementById('announcementModal');
    if (event.target === modal) {
        closeAnnouncementModal();
    }
});

// ==================== 超级管理员功能 ====================

/**
 * 检查超级管理员状态并显示公告管理按钮
 */
async function checkSuperAdminStatus() {
    try {
        const response = await fetch('/api/login-status');
        const result = await response.json();

        if (result.loggedIn && result.remainingCount === -1) {
            // 是超级管理员，显示公告管理按钮
            const announcementBtn = document.getElementById('announcementManageBtn');
            if (announcementBtn) {
                announcementBtn.style.display = 'inline-flex';
            }
        } else {
            // 不是超级管理员，隐藏公告管理按钮
            const announcementBtn = document.getElementById('announcementManageBtn');
            if (announcementBtn) {
                announcementBtn.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('检查超级管理员状态失败:', error);
        // 出错时隐藏按钮
        const announcementBtn = document.getElementById('announcementManageBtn');
        if (announcementBtn) {
            announcementBtn.style.display = 'none';
        }
    }
}

/**
 * 跳转到公告管理页面
 */
function goToAnnouncementManage() {
    // 先验证超级管理员权限
    checkSuperAdminStatus().then(() => {
        // 跳转到公告管理页面
        window.location.href = '/announcement-admin.html';
    }).catch(error => {
        console.error('权限验证失败:', error);
        showNotification('权限验证失败，无法访问公告管理', 'error');
    });
}
