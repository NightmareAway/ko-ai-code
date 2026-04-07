// DOM elements
const taskForm = document.getElementById('taskForm');
const taskTitleInput = document.getElementById('taskTitle');
const taskDescriptionInput = document.getElementById('taskDescription');
const tasksContainer = document.getElementById('tasksContainer');

// Array to store tasks
let tasks = [];

// Function to add a new task
function addTask(title, description) {
    const task = {
        id: Date.now(), // Simple unique ID based on timestamp
        title: title,
        description: description,
        createdAt: new Date().toLocaleString()
    };
    tasks.push(task);
    renderTasks();
}

// Function to delete a task
function deleteTask(id) {
    tasks = tasks.filter(task => task.id !== id);
    renderTasks();
}

// Function to render all tasks
function renderTasks() {
    tasksContainer.innerHTML = ''; // Clear current tasks
    
    if (tasks.length === 0) {
        tasksContainer.innerHTML = '<p>No tasks yet. Add one above!</p>';
        return;
    }
    
    tasks.forEach(task => {
        const taskElement = document.createElement('div');
        taskElement.className = 'task-item';
        taskElement.innerHTML = `
            <div>
                <h3>${task.title}</h3>
                <p>${task.description || 'No description provided.'}</p>
                <small>Created: ${task.createdAt}</small>
            </div>
            <button onclick="deleteTask(${task.id})">Delete</button>
        `;
        tasksContainer.appendChild(taskElement);
    });
}

// Event listener for form submission
taskForm.addEventListener('submit', function(event) {
    event.preventDefault(); // Prevent page reload
    
    const title = taskTitleInput.value.trim();
    const description = taskDescriptionInput.value.trim();
    
    if (!title) {
        alert('Please enter a task title.');
        return;
    }
    
    addTask(title, description);
    
    // Clear form inputs
    taskTitleInput.value = '';
    taskDescriptionInput.value = '';
    taskTitleInput.focus(); // Focus back to title input
});

// Initialize by rendering any existing tasks (e.g., from localStorage in a real app)
// For now, just render empty state
renderTasks();