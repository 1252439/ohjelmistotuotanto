const API_URL = '/api';

// Tab switching
function switchTab(tabName) {
    const tabs = document.querySelectorAll('.tab-content');
    const buttons = document.querySelectorAll('.tab-button');
    
    tabs.forEach(tab => tab.classList.remove('active'));
    buttons.forEach(btn => btn.classList.remove('active'));
    
    document.getElementById(tabName).classList.add('active');
    event.target.classList.add('active');
    
    if (tabName === 'teacher') {
        loadTeacherAssignments();
    } else if (tabName === 'student') {
        loadStudentAssignments();
    } else if (tabName === 'results') {
        loadSubmissions();
    }
}

// OPETTAJAN FUNKTIOT
async function createAssignment() {
    const title = document.getElementById('assignmentTitle').value;
    const description = document.getElementById('assignmentDescription').value;
    const messageDiv = document.getElementById('assignmentMessage');
    
    if (!title || !description) {
        messageDiv.innerHTML = '<div class="error">Täytä kaikki kentät.</div>';
        return;
    }
    
    try {
        const response = await fetch(`${API_URL}/assignments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-Teacher-ID': '1'
            },
            body: JSON.stringify({
                title: title,
                description: description
            })
        });
        
        if (response.ok) {
            messageDiv.innerHTML = '<div class="success">Tehtävä julkaistu.</div>';
            document.getElementById('assignmentTitle').value = '';
            document.getElementById('assignmentDescription').value = '';
            loadTeacherAssignments();
        } else {
            messageDiv.innerHTML = '<div class="error">Virhe tehtävän luomisessa.</div>';
        }
    } catch (error) {
        messageDiv.innerHTML = '<div class="error">' + error.message + '</div>';
    }
}

async function loadTeacherAssignments() {
    try {
        const response = await fetch(`${API_URL}/assignments/teacher/1`);
        const assignments = await response.json();
        
        let html = '';
        assignments.forEach(a => {
            html += `
                <div class="assignment-card">
                    <h3>${a.title}</h3>
                    <p>${a.description}</p>
                    <small>Luotu: ${new Date(a.createdAt).toLocaleString('fi-FI')}</small>
                </div>
            `;
        });
        
        document.getElementById('teacherAssignmentsList').innerHTML = html || '<p>Ei tehtäviä</p>';
    } catch (error) {
        console.error('Virhe tehtävien lataamisessa:', error);
    }
}

// OPISKELIJAN FUNKTIOT
async function loadStudentAssignments() {
    try {
        const response = await fetch(`${API_URL}/assignments`);
        const assignments = await response.json();
        
        let html = '<option value="">-- Valitse Tehtävä --</option>';
        assignments.forEach(a => {
            html += `<option value="${a.id}">${a.title}</option>`;
        });
        
        document.getElementById('assignmentSelect').innerHTML = html;
    } catch (error) {
        console.error('Virhe tehtävien lataamisessa:', error);
    }
}

async function submitCode() {
    const assignmentId = document.getElementById('assignmentSelect').value;
    const fileInput = document.getElementById('codeFile');
    const file = fileInput.files[0];
    const messageDiv = document.getElementById('submissionMessage');
    
    if (!assignmentId || !file) {
        messageDiv.innerHTML = '<div class="error">Valitse tehtävä ja ZIP-tiedosto.</div>';
        return;
    }
    
    const formData = new FormData();
    formData.append('assignmentId', assignmentId);
    formData.append('studentId', 1);
    formData.append('file', file);
    
    messageDiv.innerHTML = '<div class="loading">Lähetetään...</div>';
    
    try {
        const response = await fetch(`${API_URL}/submissions`, {
            method: 'POST',
            body: formData
        });
        
        if (response.ok) {
            const submission = await response.json();
            messageDiv.innerHTML = '<div class="success">Koodi lähetetty. Palautus ID: ' + submission.id + '</div>';
            fileInput.value = '';
            document.getElementById('assignmentSelect').value = '';
        } else {
            messageDiv.innerHTML = '<div class="error">Virhe lähetyksessä.</div>';
        }
    } catch (error) {
        messageDiv.innerHTML = '<div class="error">' + error.message + '</div>';
    }
}

// TULOSTEN NÄYTTÄVÄT FUNKTIOT
async function loadSubmissions() {
    try {
        const response = await fetch(`${API_URL}/submissions`);
        const submissions = await response.json();
        
        let html = '<option value="">-- Valitse Palautus --</option>';
        submissions.forEach((s, index) => {
            html += `<option value="${s.id}">Palautus #${s.id} (${new Date(s.submittedAt).toLocaleString('fi-FI')})</option>`;
        });
        
        document.getElementById('submissionSelect').innerHTML = html;
    } catch (error) {
        console.error('Virhe palautusten lataamisessa:', error);
    }
}

async function fetchGrade() {
    const submissionId = document.getElementById('submissionSelect').value;
    const container = document.getElementById('gradeContainer');
    
    if (!submissionId) {
        container.innerHTML = '<div class="error">Valitse palautus.</div>';
        return;
    }
    
    container.innerHTML = '<div class="loading">Arvostellaan...</div>';
    
    try {
        // Ensin triggeri arvostelu
        const gradeResponse = await fetch(`${API_URL}/grades/${submissionId}`, {
            method: 'POST'
        });
        
        // Sitten hae tulokset
        if (gradeResponse.ok) {
            const grade = await gradeResponse.json();
            const feedbackText = escapeHtml(grade.feedback || grade.geminiAnalysis || 'Ei palautetta saatavilla.');
            container.innerHTML = `
                <div class="grade-result">
                    <h3>Arvostelu</h3>
                    <div class="grade-points">${grade.points}/100</div>
                    <h4>Palaute:</h4>
                    <pre style="white-space: pre-wrap; font-family: inherit; margin: 0;">${feedbackText}</pre>
                </div>
            `;
        } else {
            container.innerHTML = '<div class="error">Virhe arvostelun haussa.</div>';
        }
    } catch (error) {
        container.innerHTML = '<div class="error">' + error.message + '</div>';
    }
}

// Load initial data
window.onload = () => {
    loadStudentAssignments();
};

function escapeHtml(text) {
    return text
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
