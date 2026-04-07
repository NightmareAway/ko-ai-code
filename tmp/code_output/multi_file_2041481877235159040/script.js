// Task management application

// Task data structure: array of task objects
let tasks = [];

// DOM elements
const taskForm = document.getElementById('taskForm');
const taskTitleInput = document.getElementById('taskTitle');
const taskDescriptionInput = document.getElementById('taskDescription');
const taskDueDateInput = document.getElementById('taskDueDate');
const taskPrioritySelect = document.getElementById('taskPriority');
const filterPrioritySelect = document.getElementById('filterPriority');
const clearCompletedButton = document.getElementById('clearCompleted');
const tasksContainer = document.getElementById('tasksContainer');

// Initialize with a sample task for demonstration
window.addEventListener('DOMContentLoaded', () => {
    // Set default due date to today
    const today = new Date().toISOString().split('T')[0];
    taskDueDateInput.value = today;
    
    // Add a sample task
    addTask('Sample Task', 'This is an example task to get you started.', today, 'medium', false);
    renderTasks();
});

// Function to add a new task
function addTask(title, description, dueDate, priority, completed = false) {
    const task = {
        id: Date.now(), // Simple unique ID based on timestamp
        title,
        description,
        dueDate,
        priority,
        completed
    };
    tasks.push(task);
    saveTasksToLocalStorage();
}

// Function to render tasks based on current filter
function renderTasks() {
    const filterValue = filterPrioritySelect.value;
    let filteredTasks = tasks;
    
    if (filterValue !== 'all') {
        filteredTasks = tasks.filter(task => task.priority === filterValue);
    }
    
    tasksContainer.innerHTML = '';
    
    if (filteredTasks.length === 0) {
        tasksContainer.innerHTML = '<p>No tasks found. Add a new task to get started!</p>';
        return;
    }
    
    filteredTasks.forEach(task => {
        const taskElement = document.createElement('div');
        taskElement.className = `task-item ${task.completed ? 'completed' : ''}`;
        taskElement.dataset.id = task.id;
        
        const priorityClass = `task-priority priority-${task.priority}`;
        
        taskElement.innerHTML = `
            <div class="task-header">
                <span class="task-title">${task.title}</span>
                <span class="${priorityClass}">${task.priority.toUpperCase()}</span>
            </div>
            <p>${task.description || 'No description provided.'}</p>
            <div class="task-due-date">Due: ${task.dueDate}</div>
            <div class="task-actions">
                <button class="complete-btn">${task.completed ? 'Undo' : 'Complete'}</button>
                <button class="delete-btn">Delete</button>
            </div>
        `;
        
        tasksContainer.appendChild(taskElement);
    });
    
    // Attach event listeners to the new task buttons
    attachTaskEventListeners();
}

// Function to attach event listeners to task buttons
function attachTaskEventListeners() {
    document.querySelectorAll('.complete-btn').forEach(button => {
        button.addEventListener('click', function() {
            const taskId = parseInt(this.closest('.task-item').dataset.id);
            toggleTaskCompletion(taskId);
        });
    });
    
    document.querySelectorAll('.delete-btn').forEach(button => {
        button.addEventListener('click', function() {
            const taskId = parseInt(this.closest('.task-item').dataset.id);
            deleteTask(taskId);
        });
    });
}

// Function to toggle task completion status
function toggleTaskCompletion(taskId) {
    const taskIndex = tasks.findIndex(task => task.id === taskId);
    if (taskIndex !== -1) {
        tasks[taskIndex].completed = !tasks[taskIndex].completed;
        saveTasksToLocalStorage();
        renderTasks();
    }
}

// Function to delete a task
function deleteTask(taskId) {
    if (confirm('Are you sure you want to delete this task?')) {
        tasks = tasks.filter(task => task.id !== taskId);
        saveTasksToLocalStorage();
        renderTasks();
    }
}

// Function to save tasks to localStorage
function saveTasksToLocalStorage() {
    localStorage.setItem('tasks', JSON.stringify(tasks));
}

// Function to load tasks from localStorage (optional, for persistence across page reloads)
function loadTasksFromLocalStorage() {
    const storedTasks = localStorage.getItem('tasks');
    if (storedTasks) {
        tasks = JSON.parse(storedTasks);
    }
}

// Event listener for form submission
taskForm.addEventListener('submit', function(e) {
    e.preventDefault();
    
    const title = taskTitleInput.value.trim();
    const description = taskDescriptionInput.value.trim();
    const dueDate = taskDueDateInput.value;
    const priority = taskPrioritySelect.value;
    
    if (!title) {
        alert('Please enter a task title.');
        return;
    }
    
    addTask(title, description, dueDate, priority);
    renderTasks();
    
    // Reset form
    taskForm.reset();
    taskDueDateInput.value = new Date().toISOString().split('T')[0]; // Reset to today
    taskPrioritySelect.value = 'medium'; // Reset to default
});

// Event listener for filter change
filterPrioritySelect.addEventListener('change', renderTasks);

// Event listener for clearing completed tasks
clearCompletedButton.addEventListener('click', function() {
    if (confirm('Are you sure you want to delete all completed tasks?')) {
        tasks = tasks.filter(task => !task.completed);
        saveTasksToLocalStorage();
        renderTasks();
    }
});

// Load tasks from localStorage on page load
loadTasksFromLocalStorage();