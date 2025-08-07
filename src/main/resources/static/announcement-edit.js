/**
 * 公告编辑页面JavaScript
 * 
 * @author 杨宇帆
 * @create 2025-08-07
 */

// 全局变量
let isEditMode = false;
let currentAnnouncementId = null;

let tinymceEditor = null;

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    checkAdminAuth();
    initializePage();
    initializeTinyMCE();

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
            window.location.href = '/admin-login.html';
            return;
        }
    } catch (error) {
        console.error('检查权限失败:', error);
        window.location.href = '/admin-login.html';
    }
}

/**
 * 初始化页面
 */
function initializePage() {
    const urlParams = new URLSearchParams(window.location.search);
    const announcementId = urlParams.get('id');
    
    if (announcementId) {
        isEditMode = true;
        currentAnnouncementId = parseInt(announcementId);
        document.getElementById('pageTitle').textContent = '编辑公告';
        loadAnnouncementData(currentAnnouncementId);
    } else {
        isEditMode = false;
        document.getElementById('pageTitle').textContent = '新建公告';
    }
}

/**
 * 初始化TinyMCE富文本编辑器
 */
function initializeTinyMCE() {
    tinymce.init({
        selector: '#content',
        height: 400,
        language: 'zh_CN',
        api_key: 'p8zrol7nvr2zvlacfqeovn3gpyskfkrz3xz38ld17hn1kifl',
        plugins: [
            'advlist', 'autolink', 'lists', 'link', 'image', 'charmap', 'preview',
            'anchor', 'searchreplace', 'visualblocks', 'code', 'fullscreen',
            'insertdatetime', 'media', 'table', 'help', 'wordcount'
        ],
        toolbar: 'undo redo | blocks | bold italic forecolor backcolor | ' +
                'alignleft aligncenter alignright alignjustify | ' +
                'bullist numlist outdent indent | removeformat | help',
        content_style: 'body { font-family: -apple-system, BlinkMacSystemFont, San Francisco, Segoe UI, Roboto, Helvetica Neue, sans-serif; font-size: 14px; }',
        branding: false,
        menubar: false,
        statusbar: false,
        setup: function(editor) {
            tinymceEditor = editor;
            editor.on('change', function() {
                editor.save();
            });
        }
    });
}



/**
 * 设置事件监听器
 */
function setupEventListeners() {
    // 表单提交事件
    document.getElementById('announcementForm').addEventListener('submit', function(e) {
        e.preventDefault();
        saveAnnouncement();
    });
}





/**
 * 加载公告数据（编辑模式）
 */
async function loadAnnouncementData(id) {
    showLoading('加载公告数据...');
    
    try {
        const response = await fetch(`/api/announcements/${id}`);
        const data = await response.json();
        
        if (response.ok && data.announcement) {
            const announcement = data.announcement;
            
            // 填充表单数据
            document.getElementById('title').value = announcement.title || '';
            document.getElementById('type').value = announcement.type || 'GENERAL';
            document.getElementById('isVisible').checked = announcement.isVisible !== false;
            document.getElementById('isPinned').checked = announcement.isPinned === true;
            
            // 等待TinyMCE初始化完成后设置内容
            const checkEditor = setInterval(() => {
                if (tinymceEditor) {
                    tinymceEditor.setContent(announcement.content || '');
                    clearInterval(checkEditor);
                }
            }, 100);
            

            
        } else {
            throw new Error(data.error || '加载公告数据失败');
        }
    } catch (error) {
        console.error('加载公告数据失败:', error);
        showMessage('加载公告数据失败: ' + error.message, 'error');
        setTimeout(() => {
            window.location.href = '/announcement-admin.html';
        }, 2000);
    } finally {
        hideLoading();
    }
}

/**
 * 保存公告
 */
async function saveAnnouncement() {
    if (!validateForm()) {
        return;
    }
    
    showLoading('保存公告...');
    
    try {

        
        // 准备公告数据
        const announcementData = {
            title: document.getElementById('title').value.trim(),
            content: tinymceEditor.getContent(),
            type: document.getElementById('type').value,
            isVisible: document.getElementById('isVisible').checked,
            isPinned: document.getElementById('isPinned').checked,

        };
        
        // 发送保存请求
        const url = isEditMode ? `/api/announcements/${currentAnnouncementId}` : '/api/announcements';
        const method = isEditMode ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(announcementData)
        });
        
        const data = await response.json();
        
        if (response.ok) {
            showMessage(isEditMode ? '公告更新成功' : '公告创建成功', 'success');
            setTimeout(() => {
                window.location.href = '/announcement-admin.html';
            }, 1500);
        } else {
            throw new Error(data.error || '保存失败');
        }
        
    } catch (error) {
        console.error('保存公告失败:', error);
        showMessage('保存失败: ' + error.message, 'error');
    } finally {
        hideLoading();
    }
}



/**
 * 验证表单
 */
function validateForm() {
    const title = document.getElementById('title').value.trim();
    const content = tinymceEditor.getContent().trim();
    
    if (!title) {
        showMessage('请输入公告标题', 'error');
        document.getElementById('title').focus();
        return false;
    }
    
    if (!content) {
        showMessage('请输入公告内容', 'error');
        tinymceEditor.focus();
        return false;
    }
    
    return true;
}

/**
 * 预览公告
 */
function previewAnnouncement() {
    const title = document.getElementById('title').value.trim();
    const content = tinymceEditor.getContent();
    
    if (!title || !content) {
        showMessage('请先填写标题和内容', 'error');
        return;
    }
    
    // 创建预览窗口
    const previewWindow = window.open('', '_blank', 'width=800,height=600');
    previewWindow.document.write(`
        <!DOCTYPE html>
        <html>
        <head>
            <title>公告预览 - ${title}</title>
            <style>
                body { font-family: Arial, sans-serif; padding: 20px; line-height: 1.6; }
                h1 { color: #333; border-bottom: 2px solid #667eea; padding-bottom: 10px; }
                .content { margin-top: 20px; }
            </style>
        </head>
        <body>
            <h1>${title}</h1>
            <div class="content">${content}</div>
        </body>
        </html>
    `);
    previewWindow.document.close();
}



/**
 * 显示加载状态
 */
function showLoading(text = '处理中...') {
    document.getElementById('loadingText').textContent = text;
    document.getElementById('loadingOverlay').style.display = 'flex';
}

/**
 * 隐藏加载状态
 */
function hideLoading() {
    document.getElementById('loadingOverlay').style.display = 'none';
}

/**
 * 显示消息提示
 */
function showMessage(message, type = 'info') {
    const messageEl = document.createElement('div');
    messageEl.className = `alert alert-${type}`;
    messageEl.textContent = message;
    messageEl.style.position = 'fixed';
    messageEl.style.top = '20px';
    messageEl.style.right = '20px';
    messageEl.style.zIndex = '9999';
    messageEl.style.minWidth = '300px';
    messageEl.style.padding = '15px 20px';
    messageEl.style.borderRadius = '8px';
    messageEl.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.1)';
    messageEl.style.animation = 'slideInRight 0.3s ease-out';
    
    // 设置样式
    if (type === 'success') {
        messageEl.style.background = '#c6f6d5';
        messageEl.style.color = '#22543d';
        messageEl.style.borderLeft = '4px solid #38a169';
    } else if (type === 'error') {
        messageEl.style.background = '#fed7d7';
        messageEl.style.color = '#742a2a';
        messageEl.style.borderLeft = '4px solid #e53e3e';
    } else {
        messageEl.style.background = '#bee3f8';
        messageEl.style.color = '#2a4365';
        messageEl.style.borderLeft = '4px solid #3182ce';
    }
    
    document.body.appendChild(messageEl);
    
    setTimeout(() => {
        messageEl.style.animation = 'slideOutRight 0.3s ease-out';
        setTimeout(() => {
            if (messageEl.parentNode) {
                messageEl.parentNode.removeChild(messageEl);
            }
        }, 300);
    }, 3000);
}
