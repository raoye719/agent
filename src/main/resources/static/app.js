// 动态获取后端 API 地址，支持本地和云端部署
const API = `${window.location.protocol}//${window.location.host}/api`;
const CHAT_ID = 'user_' + Math.random().toString(36).slice(2, 9);

document.addEventListener('DOMContentLoaded', () => {
  const today = new Date().toISOString().slice(0, 10);
  const nextWeek = new Date(Date.now() + 6 * 86400000).toISOString().slice(0, 10);
  const ps = document.getElementById('plan-start');
  const pe = document.getElementById('plan-end');
  const md = document.getElementById('morning-date');
  const ed = document.getElementById('evening-date');
  if (ps) ps.value = today;
  if (pe) pe.value = nextWeek;
  if (md) md.value = today;
  if (ed) ed.value = today;
});

function setLoading(btnId, loading) {
  const btn = document.getElementById(btnId);
  if (!btn) return;
  btn.disabled = loading;
  if (loading) { btn._text = btn.innerHTML; btn.innerHTML = '<span class="spinner"></span> 处理中...'; }
  else { btn.innerHTML = btn._text || btn.innerHTML; }
}

function showResult(boxId, tagId, ok) {
  const box = document.getElementById(boxId);
  const tag = document.getElementById(tagId);
  if (!box) return;
  box.classList.add('show');
  tag.className = 'tag ' + (ok ? 'tag-ok' : 'tag-err');
  tag.textContent = ok ? '✓ 成功' : '✗ 失败';
}

function renderMd(el, text) {
  if (!el || !text) return;
  let h = text
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/^### (.+)$/gm, '<h3>$1</h3>')
    .replace(/^## (.+)$/gm, '<h2>$1</h2>')
    .replace(/^# (.+)$/gm, '<h1>$1</h1>')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/^---$/gm, '<hr/>')
    .replace(/^&gt; (.+)$/gm, '<blockquote>$1</blockquote>')
    .replace(/^[-*] (.+)$/gm, '<li>$1</li>')
    .replace(/^\d+\. (.+)$/gm, '<li>$1</li>')
    .replace(/\n/g, '<br/>');
  el.innerHTML = h;
}

// ── 周计划 ──
async function createWeeklyPlan() {
  const desc = document.getElementById('plan-desc').value.trim();
  const start = document.getElementById('plan-start').value;
  const end = document.getElementById('plan-end').value;
  if (!desc) { alert('请输入周计划描述'); return; }
  setLoading('btn-create-plan', true);
  try {
    const res = await fetch(API + '/ai/plan/v2/weekly/create', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ weekDescription: desc, startDate: start, endDate: end })
    });
    const data = await res.json();
    showResult('result-plan', 'tag-plan', data.success);
    renderMd(document.getElementById('pre-plan'),
      data.success ? (data.weeklyPlanContent || data.message || '周计划已生成并保存') : (data.message || '请求失败'));
  } catch (e) {
    showResult('result-plan', 'tag-plan', false);
    document.getElementById('pre-plan').textContent = '请求失败：' + e.message;
  } finally { setLoading('btn-create-plan', false); }
}

async function readWeeklyPlan() {
  const date = document.getElementById('plan-start') ? document.getElementById('plan-start').value : '';
  try {
    const res = await fetch(API + '/ai/plan/v2/weekly/read' + (date ? '?date=' + date : ''));
    const data = await res.json();
    const el = document.getElementById('weekly-plan-preview');
    if (data.success) renderMd(el, data.weeklyPlan);
    else el.textContent = data.message || '未找到周计划文件';
  } catch (e) {
    document.getElementById('weekly-plan-preview').textContent = '读取失败：' + e.message;
  }
}

// ── 早上 ──
async function morningRoutine() {
  const date = document.getElementById('morning-date').value;
  setLoading('btn-morning', true);
  try {
    const res = await fetch(API + '/ai/plan/v2/daily/morning' + (date ? '?date=' + date : ''));
    const data = await res.json();
    showResult('result-morning', 'tag-morning', data.success);
    const hint = document.getElementById('morning-file-hint');
    if (hint) hint.textContent = data.success
      ? '已保存：study_records/' + (date || new Date().toISOString().slice(0, 10)) + '_学习计划完成情况.md'
      : '执行结果';
    renderMd(document.getElementById('pre-morning'),
      data.success ? (data.message || '计划已生成') : (data.message || '请求失败'));
  } catch (e) {
    showResult('result-morning', 'tag-morning', false);
    document.getElementById('pre-morning').textContent = '请求失败：' + e.message;
  } finally { setLoading('btn-morning', false); }
}

// ── 晚上 ──
async function eveningRoutine() {
  const date = document.getElementById('evening-date').value;
  const report = document.getElementById('evening-report').value.trim();
  if (!report) { alert('请输入今日完成情况'); return; }
  setLoading('btn-evening', true);
  try {
    const params = new URLSearchParams();
    if (date) params.append('date', date);
    params.append('progressReport', report);
    const res = await fetch(API + '/ai/plan/v2/daily/evening', { method: 'POST', body: params });
    const data = await res.json();
    showResult('result-evening', 'tag-evening', data.success);
    renderMd(document.getElementById('pre-evening'),
      data.success ? (data.feedback || data.message || '进度已记录') : (data.message || '请求失败'));
  } catch (e) {
    showResult('result-evening', 'tag-evening', false);
    document.getElementById('pre-evening').textContent = '请求失败：' + e.message;
  } finally { setLoading('btn-evening', false); }
}

// ── 聊天 SSE ──
function chatKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); sendChat(); }
}

function appendBubble(cls, text) {
  const win = document.getElementById('chat-window');
  const div = document.createElement('div');
  div.className = 'bubble ' + cls;
  div.textContent = text;
  win.appendChild(div);
  win.scrollTop = win.scrollHeight;
  return div;
}

async function sendChat() {
  const input = document.getElementById('chat-input');
  const msg = input.value.trim();
  if (!msg) return;
  const btn = document.getElementById('btn-send');
  input.value = ''; input.disabled = true; btn.disabled = true;
  appendBubble('bubble-user', msg);
  const aiBubble = appendBubble('bubble-ai streaming', '');
  try {
    const url = API + '/ai/study_app/chat/sse?message=' + encodeURIComponent(msg) + '&chatId=' + CHAT_ID;
    const response = await fetch(url);
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = '';
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split('\n'); buffer = lines.pop();
      for (const line of lines) {
        if (!line.startsWith('data:')) continue;
        const chunk = line.slice(5).trim();
        if (chunk && chunk !== '[DONE]') {
          aiBubble.textContent += chunk;
          document.getElementById('chat-window').scrollTop = 99999;
        }
      }
    }
  } catch (e) {
    aiBubble.textContent = '请求失败：' + e.message;
  } finally {
    aiBubble.classList.remove('streaming');
    input.disabled = false; btn.disabled = false; input.focus();
  }
}
