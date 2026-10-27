/**
 * Java10143 · AI 智能客服「悬浮窗」通用挂件
 *
 * 用法（任何页面，两行搞定）：
 *   <script src="/js/api.js"></script>
 *   <script src="/js/chat.js"></script>
 *   <script src="/js/chat-widget.js"></script>
 *
 * 它会自己把 DOM 和样式注入进来，所以不依赖 app.css，短视频页（黑底全屏）
 * 和管理后台（白底 + 侧边栏）都能直接用。
 * 如果页面里已经有 #chatFab（比如首页 index.html 手写的那套），会自动跳过，不重复注入。
 */
(function () {
  if (document.getElementById('chatFab')) return; // 页面自带，直接复用

  // ---------- 样式（作用域锁在 #chatRoot 里，避免污染页面自身样式） ----------
  var CSS = [
    '#chatRoot,#chatRoot *{box-sizing:border-box;}',
    '#chatRoot .chat-fab{position:fixed;right:24px;bottom:28px;z-index:9998;',
    'width:58px;height:58px;border-radius:50%;background:linear-gradient(135deg,#e1251b,#ff5c4d);',
    'color:#fff;font-size:15px;font-weight:700;display:flex;align-items:center;justify-content:center;',
    'cursor:pointer;box-shadow:0 8px 22px rgba(225,37,27,.42);transition:transform .18s ease;',
    'user-select:none;font-family:inherit;}',
    '#chatRoot .chat-fab:hover{transform:scale(1.07);}',
    '#chatRoot .chat-dot{position:absolute;top:2px;right:2px;width:11px;height:11px;',
    'background:#52c41a;border:2px solid #fff;border-radius:50%;}',
    '#chatRoot .chat-panel{position:fixed;right:24px;bottom:98px;z-index:9999;',
    'width:372px;height:540px;max-height:calc(100vh - 140px);background:#fff;border-radius:14px;',
    'overflow:hidden;display:flex;flex-direction:column;box-shadow:0 16px 48px rgba(0,0,0,.22);',
    'font-family:-apple-system,BlinkMacSystemFont,"PingFang SC","Microsoft YaHei",sans-serif;}',
    '#chatRoot .chat-panel.hidden{display:none;}',
    '#chatRoot .chat-head{display:flex;align-items:center;gap:10px;padding:12px 14px;',
    'background:linear-gradient(135deg,#e1251b,#ff5c4d);color:#fff;}',
    '#chatRoot .chat-avatar{width:36px;height:36px;border-radius:50%;background:rgba(255,255,255,.22);',
    'display:flex;align-items:center;justify-content:center;font-weight:700;}',
    '#chatRoot .chat-title{flex:1;line-height:1.35;}',
    '#chatRoot .chat-title em{font-style:normal;font-size:11px;background:rgba(255,255,255,.25);',
    'padding:1px 7px;border-radius:9px;margin-left:4px;}',
    '#chatRoot .chat-title em.off{background:rgba(0,0,0,.25);}',
    '#chatRoot .chat-title small{font-size:11px;opacity:.88;}',
    '#chatRoot .chat-close{background:none;border:none;color:#fff;font-size:22px;',
    'cursor:pointer;line-height:1;padding:0 4px;}',
    '#chatRoot .chat-body{flex:1;overflow-y:auto;padding:14px;background:#f6f7f9;}',
    '#chatRoot .chat-msg{display:flex;flex-direction:column;margin-bottom:12px;}',
    '#chatRoot .chat-msg .bubble{max-width:84%;padding:9px 12px;border-radius:10px;',
    'font-size:13.5px;line-height:1.6;word-break:break-word;}',
    '#chatRoot .chat-msg.agent{align-items:flex-start;}',
    '#chatRoot .chat-msg.agent .bubble{background:#fff;border:1px solid #ececec;color:#333;}',
    '#chatRoot .chat-msg.me{align-items:flex-end;}',
    '#chatRoot .chat-msg.me .bubble{background:#e1251b;color:#fff;}',
    '#chatRoot .chat-msg .meta{font-size:11px;color:#aaa;margin-top:4px;padding:0 4px;}',
    '#chatRoot .chat-msg.loading .bubble{color:#999;font-style:italic;}',
    '#chatRoot .chat-quick{display:flex;flex-wrap:wrap;gap:6px;padding:8px 12px;',
    'border-top:1px solid #f0f0f0;background:#fff;}',
    '#chatRoot .chat-quick span{font-size:12px;color:#e1251b;background:#fff4f3;',
    'border:1px solid #ffd9d6;padding:3px 9px;border-radius:12px;cursor:pointer;white-space:nowrap;}',
    '#chatRoot .chat-quick span:hover{background:#ffe9e7;}',
    '#chatRoot .chat-input{display:flex;gap:8px;padding:10px 12px;background:#fff;border-top:1px solid #f0f0f0;}',
    '#chatRoot .chat-input input{flex:1;height:36px;padding:0 12px;border:1px solid #ddd;border-radius:18px;',
    'outline:none;font-size:13.5px;font-family:inherit;}',
    '#chatRoot .chat-input input:focus{border-color:#e1251b;}',
    '#chatRoot .chat-input .mic{width:36px;height:36px;border:1px solid #ddd;border-radius:50%;background:#fff;',
    'font-size:15px;cursor:pointer;line-height:1;flex:none;font-family:inherit;}',
    '#chatRoot .chat-input .mic:hover{border-color:#e1251b;}',
    '#chatRoot .chat-input .mic.rec{background:#e1251b;color:#fff;border-color:#e1251b;',
    'animation:chatMicPulse 1.1s ease-in-out infinite;}',
    '@keyframes chatMicPulse{0%,100%{transform:scale(1);}50%{transform:scale(1.12);}}',
    '#chatRoot .chat-input button{width:64px;height:36px;border:none;border-radius:18px;background:#e1251b;',
    'color:#fff;font-size:13.5px;cursor:pointer;font-family:inherit;}',
    '#chatRoot .chat-input button:hover{background:#c81d15;}',
    '@media (max-width:480px){#chatRoot .chat-panel{right:8px;left:8px;width:auto;bottom:88px;}',
    '#chatRoot .chat-fab{right:14px;bottom:18px;}}'
  ].join('');

  var style = document.createElement('style');
  style.id = 'chatWidgetStyle';
  style.appendChild(document.createTextNode(CSS));
  document.head.appendChild(style);

  var root = document.createElement('div');
  root.id = 'chatRoot';
  root.innerHTML = [
    '<div id="chatFab" class="chat-fab" title="联系客服">',
    '  <span>客服</span>',
    '  <i id="chatDot" class="chat-dot"></i>',
    '</div>',
    '<div id="chatPanel" class="chat-panel hidden">',
    '  <div class="chat-head">',
    '    <div class="chat-avatar">京</div>',
    '    <div class="chat-title">',
    '      <div>客服小京 <em id="chatMode">AI 在线</em></div>',
    '      <small id="chatOrderHint">正在读取您的订单…</small>',
    '    </div>',
    '    <button class="chat-close" type="button">\u00d7</button>',
    '  </div>',
    '  <div id="chatBody" class="chat-body"></div>',
    '  <div id="chatQuick" class="chat-quick"></div>',
    '  <div class="chat-input">',
    '    <input id="chatText" placeholder="说说您遇到什么问题…（回车发送）">',
    '    <button type="button" class="mic" id="chatMic" title="语音输入">\uD83C\uDFA4</button>',
    '    <button type="button">发送</button>',
    '  </div>',
    '</div>'
  ].join('');
  document.body.appendChild(root);

  // 事件绑定（不写内联 onclick，避免页面 CSP / 作用域问题）
  document.getElementById('chatFab').addEventListener('click', function () { window.Chat.toggle(); });
  root.querySelector('.chat-close').addEventListener('click', function () { window.Chat.toggle(); });
  root.querySelector('.chat-input button').addEventListener('click', function () { window.Chat.send(); });
  document.getElementById('chatText').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') window.Chat.send();
  });
  document.getElementById('chatMic').addEventListener('click', function () { window.Chat.voice(); });
})();
