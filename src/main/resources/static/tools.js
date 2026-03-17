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

  function addToolResult(text) {
    currentThink = null;
    const d = document.createElement('div');
    d.className = 'tool-result';
    d.textContent = '✅ ' + text.slice(0, 300) + (text.length > 300 ? '...' : '');
    stream.appendChild(d);
    stream.scrollTop = stream.scrollHeight;
  }

  function addFinalAnswer(text) {
    currentThink = null;
    const d = document.createElement('div');
    d.className = 'final-answer';
    d.innerHTML = '<div class="label">🎯 最终回答</div><div class="content"></div>';
    d.querySelector('.content').textContent = text;
    // 检测是否包含 PDF/TXT 文件路径，自动生成下载按钮
    const fileMatch = text.match(/([\w\u4e00-\u9fa5][\w\u4e00-\u9fa5\s.-]*\.(pdf|txt|md))/i);
    if (fileMatch) {
      const fileName = fileMatch[1].trim();
      const btn = document.createElement('a');
      btn.href = TOOLS_API + '/ai/file/download?fileName=' + encodeURIComponent(fileName);
      btn.target = '_blank';
      btn.download = fileName;
      btn.className = 'btn btn-primary';
      btn.style.cssText = 'margin-top:.8rem;display:inline-flex;font-size:.82rem;text-decoration:none';
      btn.textContent = '⬇ 下载 ' + fileName;
      d.appendChild(btn);
    }
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
        // 兼容两种格式：带 data: 前缀和不带
        let chunk = line.trim();
        if (chunk.startsWith('data:')) chunk = chunk.slice(5).trim();
        if (!chunk || chunk === '[DONE]') continue;

        // 解析 JSON 格式 {type, content}
        let type = 'answer', content = chunk;
        try {
          const obj = JSON.parse(chunk);
          type = obj.type || 'answer';
          content = obj.content || chunk;
        } catch (e) {
          // 非 JSON，当作普通文本处理
          const stepMatch = chunk.match(/^Step \d+: (.+)$/s);
          content = stepMatch ? stepMatch[1] : chunk;
          type = content.startsWith('工具 ') ? 'tool' : 'answer';
        }

        if (!content || content === '思考完成 - 无需行动' || content.startsWith('Terminated') || content.startsWith('执行结束')) continue;

        if (type === 'tool') {
          addToolResult(content);
        } else {
          addFinalAnswer(content);
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
