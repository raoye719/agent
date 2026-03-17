const TOOLS_API = 'http://localhost:8123/api';

window.addEventListener('scroll', () => {
  const n = document.getElementById('navbar');
  if (n) n.style.background = window.scrollY > 50 ? 'rgba(2,4,8,.95)' : 'rgba(2,4,8,.75)';
});

function quickManus(q) {
  document.getElementById('manus-input').value = q;
  runManus();
}

function clearManus() {
  document.getElementById('manus-input').value = '';
  document.getElementById('manus-output').style.display = 'none';
  document.getElementById('step-stream').innerHTML = '';
  document.getElementById('manus-status').textContent = '';
}

async function runManus() {
  const msg = document.getElementById('manus-input').value.trim();
  if (!msg) { alert('请输入任务描述'); return; }

  const btn = document.getElementById('btn-manus');
  const status = document.getElementById('manus-status');
  const output = document.getElementById('manus-output');
  const stream = document.getElementById('step-stream');

  btn.disabled = true;
  btn._t = btn.innerHTML;
  btn.innerHTML = '<span class="spinner"></span> 执行中...';
  status.textContent = 'AI 正在思考...';
  output.style.display = 'block';
  stream.innerHTML = '';

  let currentThink = null;
  let rawBuffer = '';

  function addThink(text) {
    if (!currentThink) {
      currentThink = document.createElement('div');
      currentThink.className = 'think-block';
      currentThink.innerHTML = '<div class="think-label">💭 推理过程</div><span></span>';
      stream.appendChild(currentThink);
    }
    currentThink.querySelector('span').textContent += text;
    stream.scrollTop = stream.scrollHeight;
  }

  function addToolCall(text) {
    currentThink = null;
    const d = document.createElement('div');
    d.className = 'tool-call';
    d.textContent = '🔧 工具调用：' + text;
    stream.appendChild(d);
    stream.scrollTop = stream.scrollHeight;
  }

  function addToolResult(text) {
    const d = document.createElement('div');
    d.className = 'tool-result';
    d.textContent = '✅ 执行结果：' + text.slice(0, 200) + (text.length > 200 ? '...' : '');
    stream.appendChild(d);
    stream.scrollTop = stream.scrollHeight;
  }

  try {
    const url = TOOLS_API + '/ai/manus/chat?message=' + encodeURIComponent(msg);
    const resp = await fetch(url);
    const reader = resp.body.getReader();
    const dec = new TextDecoder();
    let buf = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buf += dec.decode(value, { stream: true });
      const lines = buf.split('\n');
      buf = lines.pop();
      for (const line of lines) {
        if (!line.startsWith('data:')) continue;
        const chunk = line.slice(5).trim();
        if (!chunk || chunk === '[DONE]') continue;
        rawBuffer += chunk;
        // 根据内容特征分类显示
        if (chunk.includes('Invoking') || chunk.includes('Tool:') || chunk.includes('Action:')) {
          addToolCall(chunk);
        } else if (chunk.includes('Result:') || chunk.includes('Observation:') || chunk.includes('Output:')) {
          addToolResult(chunk);
        } else {
          addThink(chunk);
        }
      }
    }
    status.textContent = '✅ 任务完成';
  } catch (e) {
    const d = document.createElement('div');
    d.className = 'think-block';
    d.innerHTML = '<div class="think-label">❌ 错误</div><span>' + e.message + '</span>';
    stream.appendChild(d);
    status.textContent = '❌ 执行失败';
  } finally {
    btn.disabled = false;
    btn.innerHTML = btn._t;
  }
}
