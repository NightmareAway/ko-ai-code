// Task management application

// Task data structure
let tasks = [];

// DOM elements
const taskForm = document.getElementById('taskForm');
const taskTitleInput = document.getElementById('taskTitle');
const taskDescriptionInput = document.getElementById('taskDescription');
const tasksContainer = document.getElementById('tasksContainer');

// Initialize the app
function init() {
    loadTasksFromStorage();
    renderTasks();
    setupEventListeners();
}

// Load tasks from localStorage
function loadTasksFromStorage() {
    const storedTasks = localStorage.getItem('tasks');
    if (storedTasks) {
        tasks = JSON.parse(storedTasks);
    }
}

// Save tasks to localStorage
function saveTasksToStorage() {
    localStorage.setItem('tasks', JSON.stringify(tasks));
}

// Render all tasks to the DOM
function renderTasks() {
    tasksContainer.innerHTML = '';
    
    if (tasks.length === 0) {
        tasksContainer.innerHTML = '<p class="no-tasks">No tasks yet. Add one above!</p>';
        return;
    }
    
    tasks.forEach((task, index) => {
        const taskElement = createTaskElement(task, index);
        tasksContainer.appendChild(taskElement);
    });
}

// Create a DOM element for a single task
function createTaskElement(task, index) {
    const taskDiv = document.createElement('div');
    taskDiv.className = `task-item ${task.completed ? 'completed' : ''}`;
    taskDiv.dataset.index = index;
    
    const contentDiv = document.createElement('div');
    contentDiv.className = 'task-content';
    
    const title = document.createElement('h3');
    title.textContent = task.title;
    
    const description = document.createElement('p');
    description.textContent = task.description || 'No description provided.';
    
    contentDiv.appendChild(title);
    contentDiv.appendChild(description);
    
    const actionsDiv = document.createElement('div');
    actionsDiv.className = 'task-actions';
    
    const completeButton = document.createElement('button');
    completeButton.className = 'complete-btn';
    completeButton.textContent = task.completed ? 'Undo' : 'Complete';
    completeButton.addEventListener('click', () => toggleTaskCompletion(index));
    
    const deleteButton = document.createElement('button');
    deleteButton.className = 'delete-btn';
    deleteButton.textContent = 'Delete';
    deleteButton.addEventListener('click', () => deleteTask(index));
    
    actionsDiv.appendChild(completeButton);
    actionsDiv.appendChild(deleteButton);
    
    taskDiv.appendChild(contentDiv);
    taskDiv.appendChild(actionsDiv);
    
    return taskDiv;
}

// Add a new task
function addTask(title, description) {
    const newTask = {
        title: title.trim(),
        description: description.trim(),
        completed: false,
        createdAt: new Date().toISOString()
    };
    
    tasks.push(newTask);
    saveTasksToStorage();
    renderTasks();
}

// Toggle task completion status
function toggleTaskCompletion(index) {
    if (index >= 0 && index < tasks.length) {
        tasks[index].completed = !tasks[index].completed;
        saveTasksToStorage();
        renderTasks();
    }
}

// Delete a task
function deleteTask(index) {
    if (index >= 0 && index < tasks.length) {
        tasks.splice(index, 1);
        saveTasksToStorage();
        renderTasks();
    }
}

// Set up event listeners
function setupEventListeners() {
    // Form submission for adding new tasks
    taskForm.addEventListener('submit', (e) => {
        e.preventDefault();
        
        const title = taskTitleInput.value;
        const description = taskDescriptionInput.value;
        
        if (title) {
            addTask(title, description);
            taskTitleInput.value = '';
            taskDescriptionInput.value = '';
            taskTitleInput.focus();
        }
    });
}

// Initialize the application when the DOM is fully loaded
document.addEventListener('DOMContentLoaded', init);