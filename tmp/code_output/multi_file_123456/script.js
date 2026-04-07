// 任务管理器应用
document.addEventListener('DOMContentLoaded', function() {
    // DOM元素
    const taskInput = document.getElementById('taskInput');
    const addTaskBtn = document.getElementById('addTaskBtn');
    const tasksList = document.getElementById('tasksList');
    const totalTasksElement = document.getElementById('totalTasks');
    const completedTasksElement = document.getElementById('completedTasks');
    const pendingTasksElement = document.getElementById('pendingTasks');
    const clearCompletedBtn = document.getElementById('clearCompletedBtn');
    const clearAllBtn = document.getElementById('clearAllBtn');
    const filterButtons = document.querySelectorAll('.filter-btn');
    
    // 任务数组
    let tasks = [];
    let currentFilter = 'all';
    
    // 从本地存储加载任务
    loadTasksFromStorage();
    
    // 更新任务统计
    updateTaskStats();
    
    // 事件监听器
    addTaskBtn.addEventListener('click', addTask);
    taskInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            addTask();
        }
    });
    
    clearCompletedBtn.addEventListener('click', clearCompletedTasks);
    clearAllBtn.addEventListener('click', clearAllTasks);
    
    // 筛选按钮事件监听器
    filterButtons.forEach(button => {
        button.addEventListener('click', function() {
            // 移除所有按钮的active类
            filterButtons.forEach(btn => btn.classList.remove('active'));
            // 给当前点击的按钮添加active类
            this.classList.add('active');
            // 设置当前筛选器
            currentFilter = this.getAttribute('data-filter');
            // 重新渲染任务列表
            renderTasks();
        });
    });
    
    // 添加新任务
    function addTask() {
        const taskText = taskInput.value.trim();
        
        if (taskText === '') {
            alert('请输入任务内容！');
            taskInput.focus();
            return;
        }
        
        // 创建新任务对象
        const newTask = {
            id: Date.now(), // 使用时间戳作为唯一ID
            text: taskText,
            completed: false,
            createdAt: new Date().toISOString()
        };
        
        // 添加到任务数组
        tasks.push(newTask);
        
        // 清空输入框
        taskInput.value = '';
        taskInput.focus();
        
        // 保存到本地存储
        saveTasksToStorage();
        
        // 更新UI
        renderTasks();
        updateTaskStats();
        
        // 显示添加成功提示
        showNotification('任务添加成功！');
    }
    
    // 渲染任务列表
    function renderTasks() {
        // 清空当前列表
        tasksList.innerHTML = '';
        
        // 根据筛选器过滤任务
        let filteredTasks = tasks;
        
        if (currentFilter === 'pending') {
            filteredTasks = tasks.filter(task => !task.completed);
        } else if (currentFilter === 'completed') {
            filteredTasks = tasks.filter(task => task.completed);
        }
        
        // 如果没有任务，显示占位符
        if (filteredTasks.length === 0) {
            const placeholderItem = document.createElement('li');
            placeholderItem.className = 'task-item placeholder';
            
            let message = '';
            if (currentFilter === 'all') {
                message = '暂无任务。添加您的第一个任务吧！';
            } else if (currentFilter === 'pending') {
                message = '没有待完成的任务。';
            } else if (currentFilter === 'completed') {
                message = '还没有完成的任务。';
            }
            
            placeholderItem.innerHTML = `<p>${message}</p>`;
            tasksList.appendChild(placeholderItem);
            return;
        }
        
        // 渲染每个任务
        filteredTasks.forEach(task => {
            const taskItem = document.createElement('li');
            taskItem.className = 'task-item';
            if (task.completed) {
                taskItem.classList.add('completed');
            }
            
            taskItem.innerHTML = `
                <div class="task-content">
                    <input type="checkbox" class="task-checkbox" ${task.completed ? 'checked' : ''} data-id="${task.id}">
                    <span class="task-text">${task.text}</span>
                </div>
                <div class="task-actions">
                    <button class="delete-btn" data-id="${task.id}">
                        <i class="fas fa-trash"></i>
                    </button>
                </div>
            `;
            
            tasksList.appendChild(taskItem);
        });
        
        // 为复选框和删除按钮添加事件监听器
        document.querySelectorAll('.task-checkbox').forEach(checkbox => {
            checkbox.addEventListener('change', toggleTaskCompletion);
        });
        
        document.querySelectorAll('.delete-btn').forEach(button => {
            button.addEventListener('click', deleteTask);
        });
    }
    
    // 切换任务完成状态
    function toggleTaskCompletion(e) {
        const taskId = parseInt(e.target.getAttribute('data-id'));
        const taskIndex = tasks.findIndex(task => task.id === taskId);
        
        if (taskIndex !== -1) {
            tasks[taskIndex].completed = e.target.checked;
            saveTasksToStorage();
            renderTasks();
            updateTaskStats();
            
            // 显示状态变更提示
            const status = e.target.checked ? '已完成' : '待完成';
            showNotification(`任务标记为${status}`);
        }
    }
    
    // 删除单个任务
    function deleteTask(e) {
        const taskId = parseInt(e.currentTarget.getAttribute('data-id'));
        const taskText = tasks.find(task => task.id === taskId)?.text || '';
        
        if (confirm(`确定要删除任务"${taskText}"吗？`)) {
            tasks = tasks.filter(task => task.id !== taskId);
            saveTasksToStorage();
            renderTasks();
            updateTaskStats();
            showNotification('任务已删除');
        }
    }
    
    // 清除所有已完成任务
    function clearCompletedTasks() {
        const completedCount = tasks.filter(task => task.completed).length;
        
        if (completedCount === 0) {
            alert('没有已完成的任务可以清除。');
            return;
        }
        
        if (confirm(`确定要清除所有已完成的任务吗？共${completedCount}个任务将被删除。`)) {
            tasks = tasks.filter(task => !task.completed);
            saveTasksToStorage();
            renderTasks();
            updateTaskStats();
            showNotification(`已清除${completedCount}个已完成任务`);
        }
    }
    
    // 清除所有任务
    function clearAllTasks() {
        if (tasks.length === 0) {
            alert('任务列表已经是空的。');
            return;
        }
        
        if (confirm(`确定要清除所有任务吗？共${tasks.length}个任务将被删除。`)) {
            tasks = [];
            saveTasksToStorage();
            renderTasks();
            updateTaskStats();
            showNotification('所有任务已清除');
        }
    }
    
    // 更新任务统计
    function updateTaskStats() {
        const total = tasks.length;
        const completed = tasks.filter(task => task.completed).length;
        const pending = total - completed;
        
        totalTasksElement.textContent = `总任务: ${total}`;
        completedTasksElement.textContent = `已完成: ${completed}`;
        pendingTasksElement.textContent = `待完成: ${pending}`;
    }
    
    // 保存任务到本地存储
    function saveTasksToStorage() {
        localStorage.setItem('taskManagerTasks', JSON.stringify(tasks));
    }
    
    // 从本地存储加载任务
    function loadTasksFromStorage() {
        const storedTasks = localStorage.getItem('taskManagerTasks');
        
        if (storedTasks) {
            try {
                tasks = JSON.parse(storedTasks);
                renderTasks();
            } catch (e) {
                console.error('加载任务时出错:', e);
                tasks = [];
            }
        } else {
            // 如果没有存储的任务，添加一些示例任务
            tasks = [
                { id: 1, text: '学习JavaScript基础知识', completed: true, createdAt: new Date().toISOString() },
                { id: 2, text: '完成项目任务管理网站', completed: false, createdAt: new Date().toISOString() },
                { id: 3, text: '阅读一本技术书籍', completed: false, createdAt: new Date().toISOString() },
                { id: 4, text: '准备下周的会议材料', completed: true, createdAt: new Date().toISOString() }
            ];
            saveTasksToStorage();
            renderTasks();
        }
    }
    
    // 显示通知
    function showNotification(message) {
        // 创建通知元素
        const notification = document.createElement('div');
        notification.className = 'notification';
        notification.textContent = message;
        notification.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            background-color: #4CAF50;
            color: white;
            padding: 15px 25px;
            border-radius: 5px;
            box-shadow: 0 4px 8px rgba(0,0,0,0.2);
            z-index: 1000;
            opacity: 0;
            transform: translateY(-20px);
            transition: opacity 0.3s, transform 0.3s;
        `;
        
        document.body.appendChild(notification);
        
        // 显示通知
        setTimeout(() => {
            notification.style.opacity = '1';
            notification.style.transform = 'translateY(0)';
        }, 10);
        
        // 3秒后隐藏并移除通知
        setTimeout(() => {
            notification.style.opacity = '0';
            notification.style.transform = 'translateY(-20px)';
            
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }
});