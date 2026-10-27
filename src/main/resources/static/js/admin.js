/**
 * Java10143 管理后台逻辑
 * 只有 role=admin 的账号能进：登录时后端返回 admin:true 才放行，
 * 所有 /admin/** 接口在服务端还有 AdminInterceptor 二次校验。
 */
function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

let toastTimer = null;
function toast(msg) {
  const el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('show'), 1800);
}

const Admin = {
  tabName: 'stats',
  page: 1,
  keyword: '',
  cache: {},

  /* ---------------- 登录 ---------------- */
  async doLogin() {
    const account = document.getElementById('acc').value.trim();
    const password = document.getElementById('pwd').value;
    const err = document.getElementById('gateErr');
    err.textContent = '';
    try {
      const user = await API.login(account, password);
      if (!user.admin) {
        err.textContent = '该账号不是管理员，无法进入后台';
        return;
      }
      API.setLogin(user);
      Admin.enter(user);
    } catch (e) {
      err.textContent = e.message;
    }
  },

  logout() {
    API.logout();
    location.reload();
  },

  enter(user) {
    document.getElementById('gate').style.display = 'none';
    document.getElementById('layout').classList.add('show');
    document.getElementById('whoami').textContent = user.nickname + '（' + user.account + '）';
    Admin.tab('stats');
  },

  /* ---------------- 选项卡 ---------------- */
  tab(name) {
    Admin.tabName = name;
    Admin.page = 1;
    document.querySelectorAll('aside .mi').forEach(el => {
      el.classList.toggle('on', el.dataset.tab === name);
    });
    if (name === 'stats') Admin.viewStats();
    else if (name === 'dashboard') Admin.viewDashboard();
    else if (name === 'goods') Admin.viewGoods();
    else if (name === 'video') Admin.viewVideo();
    else if (name === 'comment') Admin.viewComment();
    else if (name === 'user') Admin.viewUser();
    else if (name === 'order') Admin.viewOrder();
  },

  main(html) { document.getElementById('main').innerHTML = html; },

  async guard(fn) {
    try { await fn(); } catch (e) {
      if (/401/.test(e.message) || /登录/.test(e.message)) {
        toast('登录已失效，请重新登录');
        setTimeout(() => location.reload(), 1200);
        return;
      }
      Admin.main('<div class="panel"><div class="empty" style="color:#e1251b">' + esc(e.message) + '</div></div>');
    }
  },

  /* ---------------- 数据看板 ---------------- */
  viewStats() {
    Admin.guard(async () => {
      const s = await API.admin.stats();
      const cards = [
        ['商品总数', s.goodsCount], ['SKU 规格数', s.skuCount], ['注册用户', s.userCount],
        ['订单总数', s.orderCount], ['短视频', s.videoCount], ['评论', s.commentCount]
      ];
      Admin.main(`
        <div class="page-head"><h1>数据看板</h1><div class="grow"></div>
          <button class="btn" onclick="Admin.viewStats()">刷新</button></div>
        <div class="cards">
          ${cards.map(c => `<div class="stat"><div class="k">${c[0]}</div><div class="v">${c[1]}</div></div>`).join('')}
        </div>
        <div class="panel">
          <h3 style="font-size:15px;margin-bottom:10px">快捷入口</h3>
          <div style="display:flex;gap:10px;flex-wrap:wrap">
            <button class="btn primary" onclick="Admin.tab('goods');setTimeout(()=>Admin.goodsForm(),300)">+ 新增商品</button>
            <button class="btn" onclick="Admin.tab('video');setTimeout(()=>Admin.videoForm(),300)">+ 新增短视频</button>
            <button class="btn" onclick="Admin.tab('user');setTimeout(()=>Admin.userForm(),300)">+ 新增用户</button>
            <a class="btn" href="/video.html" target="_blank">打开短视频页</a>
            <a class="btn" href="/index.html" target="_blank">打开商城页</a>
          </div>
        </div>`);
    });
  },

  /* ---------------- 数据大屏 ---------------- */
  viewDashboard() {
    Admin.main(`
      <div class="page-head"><h1>数据大屏</h1><div class="grow"></div>
        <a class="btn" href="/admin-dashboard.html" target="_blank">新窗口全屏打开 ↗</a></div>
      <div class="panel" style="padding:0;overflow:hidden;border-radius:12px">
        <iframe src="/admin-dashboard.html" style="width:100%;height:calc(100vh - 150px);border:0;display:block"></iframe>
      </div>`);
  },

  /* ---------------- 商品 ---------------- */
  viewGoods() {
    Admin.guard(async () => {
      const data = await API.admin.goodsList(Admin.keyword, Admin.page, 10);
      const rows = data.rows || [];
      Admin.main(`
        <div class="page-head">
          <h1>商品管理</h1>
          <div class="grow"></div>
          <div class="searchbar">
            <input id="gkw" placeholder="搜索商品名 / 编码" value="${esc(Admin.keyword)}"
                   onkeydown="if(event.key==='Enter')Admin.doGoodsSearch()">
            <button class="btn" onclick="Admin.doGoodsSearch()">搜索</button>
          </div>
          <button class="btn primary" onclick="Admin.goodsForm()">+ 新增商品</button>
        </div>
        <div class="panel">
          <table>
            <thead><tr><th>图片</th><th>商品</th><th>价格</th><th>库存</th><th>销量</th><th>评论</th><th style="width:150px">操作</th></tr></thead>
            <tbody>
              ${rows.length ? rows.map(g => `
                <tr>
                  <td><img class="thumb" src="${esc(g.picture)}"></td>
                  <td>
                    <div class="ell"><b>${esc(g.name)}</b></div>
                    <div style="color:#999;font-size:12px">${esc(g.spuCode || '')} · ID ${g.id}</div>
                  </td>
                  <td style="color:#e1251b;font-weight:700">￥${esc(g.price)}</td>
                  <td>${g.inventory == null ? '-' : g.inventory}</td>
                  <td>${g.salesCount || 0}</td>
                  <td>${g.commentCount || 0}</td>
                  <td>
                    <button class="btn" onclick="Admin.goodsForm(${g.id})">编辑</button>
                    <button class="btn danger" onclick="Admin.delGoods(${g.id},'${esc(g.name)}')">删除</button>
                  </td>
                </tr>`).join('') : '<tr><td colspan="7" class="empty">没有商品数据</td></tr>'}
            </tbody>
          </table>
          <div class="pager">
            共 ${data.total} 件 · 第 ${data.page} 页
            <button class="btn" onclick="Admin.goodsPage(-1)">上一页</button>
            <button class="btn" onclick="Admin.goodsPage(1)">下一页</button>
          </div>
        </div>`);
    });
  },

  doGoodsSearch() {
    Admin.keyword = document.getElementById('gkw').value.trim();
    Admin.page = 1;
    Admin.viewGoods();
  },

  goodsPage(d) {
    Admin.page = Math.max(1, Admin.page + d);
    Admin.viewGoods();
  },

  async goodsForm(id) {
    let g = { mainPictures: [], detailPictures: [], inventory: 100, price: '', oldPrice: '' };
    if (id) {
      try { g = await API.admin.goodsDetail(id); } catch (e) { return toast(e.message); }
    }
    let cats = [];
    try { cats = await API.admin.categories(); } catch (e) { /* 忽略 */ }

    Admin.modal(id ? '编辑商品 #' + id : '新增商品', `
      <div class="row"><label>商品名称 *</label><input id="f_name" value="${esc(g.name)}"></div>
      <div class="row"><label style="color:#e1251b;cursor:pointer" onclick="Admin.aiCopy()">🤖 AI 生成文案（按上面名称自动填标题/描述）</label></div>
      <div class="row two">
        <div><label>现价（元）</label><input id="f_price" value="${esc(g.price)}"></div>
        <div><label>原价（元）</label><input id="f_oldPrice" value="${esc(g.oldPrice)}"></div>
      </div>
      <div class="row two">
        <div><label>库存</label><input id="f_inventory" value="${g.inventory == null ? 100 : g.inventory}"></div>
        <div><label>商品编码</label><input id="f_spuCode" value="${esc(g.spuCode)}"></div>
      </div>
      <div class="row two">
        <div><label>所属分类</label>
          <select id="f_categoryId">
            <option value="">（不选）</option>
            ${cats.map(c => `<option value="${c.id}" ${String(c.categoryId || g.categoryId) === String(c.id) ? 'selected' : ''}>${'　'.repeat(Math.max(0, (c.layer || 1) - 1))}${esc(c.name)}</option>`).join('')}
          </select>
        </div>
        <div><label>销量</label><input id="f_salesCount" value="${g.salesCount || 0}"></div>
      </div>
      <div class="row"><label>商品描述</label><input id="f_description" value="${esc(g.desc)}"></div>
      <div class="row"><label>主图地址（多张用逗号分隔，留空自动生成）</label>
        <input id="f_mainPictures" value="${esc((g.mainPictures || []).join(','))}" placeholder="/img/p/p1001.svg?t=华为"></div>
      `, async () => {
      const body = {
        name: document.getElementById('f_name').value.trim(),
        price: document.getElementById('f_price').value.trim(),
        oldPrice: document.getElementById('f_oldPrice').value.trim(),
        inventory: document.getElementById('f_inventory').value.trim(),
        spuCode: document.getElementById('f_spuCode').value.trim(),
        categoryId: document.getElementById('f_categoryId').value || null,
        salesCount: document.getElementById('f_salesCount').value.trim(),
        description: document.getElementById('f_description').value.trim(),
        mainPictures: document.getElementById('f_mainPictures').value.trim()
      };
      if (!body.name) return toast('商品名称不能为空');
      if (id) await API.admin.goodsUpdate(id, body);
      else await API.admin.goodsCreate(body);
      Admin.closeModal();
      toast(id ? '修改成功' : '新增成功');
      Admin.viewGoods();
    });
  },

  /**
   * AI 商品文案：调 /admin/ai/copy（后端转发 Python 本地生成）。
   * 名称留空时会用 AI 标题回填；详情写进描述框。AI 挂了走后端降级文案，不挡保存。
   */
  async aiCopy() {
    const nameEl = document.getElementById('f_name');
    const descEl = document.getElementById('f_description');
    try {
      const res = await API.request('/admin/ai/copy', {
        method: 'POST',
        body: { name: (nameEl && nameEl.value.trim()) || '', category: '' }
      });
      if (!res) return;
      if (nameEl && !nameEl.value.trim() && res.titles && res.titles.length) {
        nameEl.value = res.titles[0];
      }
      if (descEl) descEl.value = res.detail || descEl.value;
      if (res.sellingPoints) {
        toast('AI 文案已填入：' + res.sellingPoints.join(' / '));
      }
    } catch (e) { toast('AI 文案生成失败：' + e.message); }
  },

  async delGoods(id, name) {    if (!confirm('确定删除商品「' + name + '」吗？\n（会同时删除它的 SKU）')) return;
    try {
      await API.admin.goodsDelete(id);
      toast('已删除');
      Admin.viewGoods();
    } catch (e) { toast(e.message); }
  },

  /* ---------------- 短视频 ---------------- */
  viewVideo() {
    Admin.guard(async () => {
      const list = await API.admin.videoList();
      Admin.cache.videos = list;
      Admin.main(`
        <div class="page-head">
          <h1>短视频管理</h1><div class="grow"></div>
          <button class="btn" onclick="Admin.viewVideo()">刷新</button>
          <button class="btn" onclick="Admin.aiVideoForm()">🤖 AI 生成视频</button>
          <button class="btn primary" onclick="Admin.videoForm()">+ 新增短视频</button>
        </div>
        <div class="panel">
          <table>
            <thead><tr><th>封面</th><th>标题</th><th>作者</th><th>关联商品</th><th>点赞</th><th>评论</th><th>状态</th><th style="width:250px">操作</th></tr></thead>
            <tbody>
              ${list.length ? list.map(v => `
                <tr>
                  <td><img class="thumb" src="${esc(v.cover)}"></td>
                  <td><div class="ell">${esc(v.title)}</div>
                      <div style="color:#999;font-size:12px">${v.url ? '真实 mp4 · ' + (v.duration || '?') + 's' : '图片合成兜底'}</div></td>
                  <td>${esc(v.authorName || '')}</td>
                  <td>${v.goods ? esc(v.goods.name) : '<span style="color:#999">未关联</span>'}</td>
                  <td>${v.likeCount || 0}</td>
                  <td>${v.commentCount || 0}</td>
                  <td><span class="tag ${v.status ? 'on' : 'off'}">${v.status ? '上架' : '下架'}</span></td>
                  <td>
                    <button class="btn primary" onclick="Admin.videoPreview(${v.id})">▶ 预览</button>
                    <button class="btn" onclick="Admin.videoForm(${v.id})">编辑</button>
                    <button class="btn" onclick="Admin.toggleVideo(${v.id})">${v.status ? '下架' : '上架'}</button>
                    <button class="btn danger" onclick="Admin.delVideo(${v.id})">删除</button>
                  </td>
                </tr>`).join('') : '<tr><td colspan="8" class="empty">还没有短视频</td></tr>'}
            </tbody>
          </table>
        </div>`);
    });
  },

  /** 预览播放短视频：弹层 video 播放（AI 生成 / 真实 mp4 通用） */
  videoPreview(id) {
    const v = (Admin.cache.videos || []).find(x => String(x.id) === String(id));
    if (!v) return toast('找不到该视频');
    if (!v.url) return toast('该视频没有 mp4 文件（图片合成兜底类型）');
    let ov = document.getElementById('vpOverlay');
    if (!ov) {
      ov = document.createElement('div');
      ov.id = 'vpOverlay';
      ov.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.74);z-index:9999;display:flex;align-items:center;justify-content:center';
      ov.innerHTML = '<div style="text-align:center">' +
        '<video id="vpVideo" controls autoplay playsinline style="max-width:86vw;max-height:78vh;border-radius:12px;display:block;background:#000"></video>' +
        '<div style="margin-top:12px"><button onclick="Admin.videoPreviewClose()" style="padding:9px 28px;border:0;border-radius:8px;background:#e1251b;color:#fff;font-size:14px;cursor:pointer">关闭</button></div>' +
        '</div>';
      ov.addEventListener('click', e => { if (e.target === ov) Admin.videoPreviewClose(); });
      document.body.appendChild(ov);
    }
    document.getElementById('vpVideo').src = v.url;
  },

  videoPreviewClose() {
    const v = document.getElementById('vpVideo');
    if (v) { v.pause(); v.removeAttribute('src'); v.load(); }
    const ov = document.getElementById('vpOverlay');
    if (ov) ov.remove();
  },

  /** AI 生成短视频：商品直选/关键词 + 4 种风格运镜 + 时长 + 字幕预览（POST /admin/video/generate） */
  STYLE_NAMES: { seeds: '种草慢推', unbox: '开箱横移', flash: '快闪节奏', fresh: '清新淡出' },
  CAP_TPL: {
    seeds: ['{kw}｜真的会回购', '细节好评 直接种草', '无脑冲 不踩雷'],
    unbox: ['开箱！{kw} 到手', '第一眼就爱住', '这个质感真的绝'],
    flash: ['{kw} 三连拍', '闭眼入', '手慢无'],
    fresh: ['今日份 {kw}', '生活需要一点小美好', '慢下来 看看它']
  },
  aiStyleSel: 'seeds',

  aiStyleBtn(v) {
    return `<button type="button" class="btn" id="ai_st_${v}" style="font-size:12.5px" onclick="Admin.aiPickStyle('${v}')">${Admin.STYLE_NAMES[v]}</button>`;
  },

  aiPickStyle(v) {
    Admin.aiStyleSel = v;
    ['seeds', 'unbox', 'flash', 'fresh'].forEach(s => {
      const b = document.getElementById('ai_st_' + s);
      if (b) { b.style.background = s === v ? '#e1251b' : ''; b.style.borderColor = s === v ? '#e1251b' : ''; b.style.color = s === v ? '#fff' : ''; }
    });
    Admin.aiSync();
  },

  /** 同步字幕预览：优先用选中商品名，否则用关键词 */
  aiSync() {
    const kwEl = document.getElementById('ai_keyword');
    if (!kwEl) return;
    const sel = document.getElementById('ai_spu');
    const kw = (sel && sel.value) ? ((sel.options[sel.selectedIndex] || {}).text || '好物') : (kwEl.value.trim() || '好物');
    const style = Admin.aiStyleSel || 'seeds';
    const caps = (Admin.CAP_TPL[style] || []).map(t => t.replace('{kw}', kw));
    const box = document.getElementById('ai_caps');
    if (box) box.innerHTML = caps.map(c => `<span style="display:inline-block;background:#fff1f0;border:1px solid #ffd5d2;border-radius:6px;padding:4px 10px;margin:2px;font-size:12.5px;color:#c0392b">${esc(c)}</span>`).join('');
  },

  async aiVideoForm() {
    // 商品下拉数据（与编辑视频共用缓存）
    if (!Admin.cache.goodsOptions) {
      try { const g = await API.admin.goodsList('', 1, 100); Admin.cache.goodsOptions = g.rows || []; }
      catch (e) { Admin.cache.goodsOptions = []; }
    }
    const opts = (Admin.cache.goodsOptions || []).map(g => `<option value="${g.id}">${esc(g.name)}</option>`).join('');
    Admin.aiStyleSel = 'seeds';
    Admin.modal('🤖 AI 生成短视频', `
      <div class="row"><label>选择商品（直接用它的 AI 商品图和名称）</label>
        <select id="ai_spu" onchange="Admin.aiSync()"><option value="">— 不选，用关键词搜 —</option>${opts}</select></div>
      <div class="row"><label>或输入关键词（没选商品时按关键词模糊匹配）</label>
        <input id="ai_keyword" placeholder="如：口红 / 牛奶 / 空气炸锅 / 车厘子" oninput="Admin.aiSync()"></div>
      <div class="row"><label>视频风格（运镜 + 字幕语气）</label>
        <div style="display:flex;gap:8px;flex-wrap:wrap">
          ${Admin.aiStyleBtn('seeds')}${Admin.aiStyleBtn('unbox')}${Admin.aiStyleBtn('flash')}${Admin.aiStyleBtn('fresh')}
        </div></div>
      <div class="row"><label>时长</label>
        <select id="ai_duration">
          <option value="8">8 秒（快，适合快闪）</option>
          <option value="10" selected>10 秒（推荐）</option>
          <option value="15">15 秒（完整展示）</option>
        </select></div>
      <div class="row"><label>将烧录进视频的字幕（每段画面一句）</label>
        <div id="ai_caps"></div></div>
      <div style="color:#999;font-size:12.5px;line-height:1.7">
        说明：合成竖屏 720×1280 mp4 并自动入库上信息流；生成约需 15-60 秒（时长越长越久），请耐心等待。</div>
      `, async () => {
      const spu = (document.getElementById('ai_spu') || {}).value || '';
      const kw = ((document.getElementById('ai_keyword') || {}).value || '').trim();
      if (!spu && !kw) return toast('请选择商品或输入关键词');
      const style = Admin.aiStyleSel || 'seeds';
      const duration = Number((document.getElementById('ai_duration') || {}).value || 10);
      toast('正在生成「' + Admin.STYLE_NAMES[style] + ' · ' + duration + ' 秒」，请稍候…');
      const r = await API.admin.videoGenerate({ keyword: kw || '好物', spuId: spu || undefined, style: style, duration: duration });
      Admin.closeModal();
      toast('已生成：' + (r.title || r.videoId));
      Admin.cache.videos = null;
      Admin.viewVideo();
      setTimeout(() => { try { Admin.videoPreview(r.videoId); } catch (e) {} }, 1200);
    });
    Admin.aiPickStyle('seeds');
  },

  async videoForm(id) {
    let v = { status: 1, duration: 6, sort: 99, likeCount: 0, shareCount: 0, collectCount: 0 };
    // 关联商品下拉：一次取 100 条商品，管理员不用再记 ID
    if (!Admin.cache.goodsOptions) {
      try {
        const g = await API.admin.goodsList('', 1, 100);
        Admin.cache.goodsOptions = g.rows || [];
      } catch (e) { Admin.cache.goodsOptions = []; }
    }
    if (id) {
      const all = Admin.cache.videos || await API.admin.videoList();
      v = all.find(x => String(x.id) === String(id)) || v;
      v = Object.assign({}, v, { tagsText: (v.tags || []).join(',') });
    }
    Admin.modal(id ? '编辑短视频 #' + id : '新增短视频', `
      <div class="row"><label>标题 *</label><input id="v_title" value="${esc(v.title)}"></div>
      <div class="row"><label>描述</label><textarea id="v_description" rows="2">${esc(v.description)}</textarea></div>
      <div class="row two">
        <div><label>作者昵称</label><input id="v_authorName" value="${esc(v.authorName)}"></div>
        <div><label>时长（秒）</label><input id="v_duration" value="${v.duration || 6}"></div>
      </div>
      <div class="row"><label>话题标签（逗号分隔）</label><input id="v_tags" value="${esc(v.tagsText || '')}"></div>
      <div class="row two">
        <div><label>关联商品（视频同款，决定"去看看"跳到哪）</label>
          <select id="v_spuId">
            <option value="">— 不关联 —</option>
            ${(Admin.cache.goodsOptions || []).map(g =>
              `<option value="${g.id}" ${String(g.id) === String(v.spuId) ? 'selected' : ''}>${esc(g.name)}（ID ${g.id}）</option>`).join('')}
          </select>
        </div>
        <div><label>排序（越小越前）</label><input id="v_sort" value="${v.sort || 99}"></div>
      </div>
      <div class="row"><label>视频地址（留空则用封面图做动效兜底）</label>
        <input id="v_url" value="${esc(v.url)}" placeholder="/media/video/v1001.mp4"></div>
      <div class="row"><label>封面地址</label>
        <input id="v_cover" value="${esc(v.cover)}" placeholder="/img/p/p1001.svg?t=华为Mate70Pro"></div>
      <div class="row"><label>状态</label>
        <select id="v_status">
          <option value="1" ${v.status ? 'selected' : ''}>上架</option>
          <option value="0" ${!v.status ? 'selected' : ''}>下架</option>
        </select>
      </div>
      `, async () => {
      const body = {
        title: document.getElementById('v_title').value.trim(),
        description: document.getElementById('v_description').value.trim(),
        authorName: document.getElementById('v_authorName').value.trim(),
        duration: document.getElementById('v_duration').value.trim(),
        tags: document.getElementById('v_tags').value.trim(),
        spuId: document.getElementById('v_spuId').value.trim(),
        sort: document.getElementById('v_sort').value.trim(),
        url: document.getElementById('v_url').value.trim(),
        cover: document.getElementById('v_cover').value.trim(),
        status: document.getElementById('v_status').value
      };
      if (!body.title) return toast('标题不能为空');
      if (id) await API.admin.videoUpdate(id, body);
      else await API.admin.videoCreate(body);
      Admin.closeModal();
      toast(id ? '修改成功' : '新增成功');
      Admin.viewVideo();
    });
  },

  async toggleVideo(id) {
    const v = (Admin.cache.videos || []).find(x => String(x.id) === String(id));
    if (!v) return;
    try {
      await API.admin.videoUpdate(id, { status: v.status ? 0 : 1 });
      toast(v.status ? '已下架' : '已上架');
      Admin.viewVideo();
    } catch (e) { toast(e.message); }
  },

  async delVideo(id) {
    if (!confirm('确定删除这条短视频吗？（它的评论会一起删除）')) return;
    try {
      await API.admin.videoDelete(id);
      toast('已删除');
      Admin.viewVideo();
    } catch (e) { toast(e.message); }
  },

  /* ---------------- 评论 ---------------- */
  viewComment() {
    Admin.guard(async () => {
      const list = await API.admin.commentList(Admin.commentType || '', Admin.keyword);
      Admin.main(`
        <div class="page-head">
          <h1>评论管理</h1><div class="grow"></div>
          <select id="ck_type" onchange="Admin.doCommentFilter()" style="height:34px;border:1px solid #dcdfe6;border-radius:7px;padding:0 10px">
            <option value="">全部类型</option>
            <option value="goods" ${Admin.commentType === 'goods' ? 'selected' : ''}>商品评论</option>
            <option value="video" ${Admin.commentType === 'video' ? 'selected' : ''}>视频评论</option>
          </select>
          <div class="searchbar">
            <input id="ckw" placeholder="搜索评论内容" value="${esc(Admin.keyword)}"
                   onkeydown="if(event.key==='Enter')Admin.doCommentFilter()">
            <button class="btn" onclick="Admin.doCommentFilter()">搜索</button>
          </div>
        </div>
        <div class="panel">
          <table>
            <thead><tr><th>用户</th><th>类型</th><th>对象 ID</th><th>内容</th><th>评分</th><th>赞</th><th>时间</th><th style="width:150px">操作</th></tr></thead>
            <tbody>
              ${list.length ? list.map(c => `
                <tr>
                  <td>${esc(c.nickname || '')}</td>
                  <td><span class="tag ${c.targetType === 'video' ? 'adm' : 'on'}">${c.targetType === 'video' ? '视频' : '商品'}</span></td>
                  <td>${c.targetId}</td>
                  <td><div class="ell" title="${esc(c.content)}">${esc(c.content)}</div></td>
                  <td>${c.score == null ? '-' : c.score}</td>
                  <td>${c.likeCount || 0}</td>
                  <td style="font-size:12px;color:#999">${esc(c.createTime || '')}</td>
                  <td>
                    <button class="btn" onclick="Admin.commentForm(${c.id},'${esc(c.content).replace(/'/g, '')}')">编辑</button>
                    <button class="btn danger" onclick="Admin.delComment(${c.id})">删除</button>
                  </td>
                </tr>`).join('') : '<tr><td colspan="8" class="empty">没有评论</td></tr>'}
            </tbody>
          </table>
        </div>`);
    });
  },

  doCommentFilter() {
    Admin.commentType = document.getElementById('ck_type').value;
    Admin.keyword = document.getElementById('ckw').value.trim();
    Admin.viewComment();
  },

  commentForm(id, content) {
    Admin.modal('编辑评论 #' + id, `
      <div class="row"><label>评论内容</label><textarea id="c_content" rows="3">${esc(content)}</textarea></div>
      <div class="row"><label>评分（0-5）</label><input id="c_score" value="5"></div>
      `, async () => {
      await API.admin.commentUpdate(id, {
        content: document.getElementById('c_content').value.trim(),
        score: document.getElementById('c_score').value.trim()
      });
      Admin.closeModal();
      toast('修改成功');
      Admin.viewComment();
    });
  },

  async delComment(id) {
    if (!confirm('确定删除这条评论吗？')) return;
    try {
      await API.admin.commentDelete(id);
      toast('已删除');
      Admin.viewComment();
    } catch (e) { toast(e.message); }
  },

  /* ---------------- 用户 ---------------- */
  viewUser() {
    Admin.guard(async () => {
      const list = await API.admin.userList(Admin.keyword);
      Admin.main(`
        <div class="page-head">
          <h1>用户管理</h1><div class="grow"></div>
          <div class="searchbar">
            <input id="ukw" placeholder="搜索账号 / 昵称" value="${esc(Admin.keyword)}"
                   onkeydown="if(event.key==='Enter')Admin.doUserSearch()">
            <button class="btn" onclick="Admin.doUserSearch()">搜索</button>
          </div>
          <button class="btn primary" onclick="Admin.userForm()">+ 新增用户</button>
        </div>
        <div class="panel">
          <table>
            <thead><tr><th>头像</th><th>账号</th><th>昵称</th><th>手机号</th><th>角色</th><th>注册时间</th><th style="width:150px">操作</th></tr></thead>
            <tbody>
              ${list.length ? list.map(u => `
                <tr>
                  <td><img class="thumb" style="border-radius:50%" src="${esc(u.avatar)}"></td>
                  <td>${esc(u.account)}</td>
                  <td>${esc(u.nickname || '')}</td>
                  <td>${esc(u.mobile || '')}</td>
                  <td><span class="tag ${u.role === 'admin' ? 'adm' : 'on'}">${u.role === 'admin' ? '管理员' : '普通用户'}</span></td>
                  <td style="font-size:12px;color:#999">${esc(u.createTime || '')}</td>
                  <td>
                    <button class="btn" onclick="Admin.userForm(${u.id})">编辑</button>
                    <button class="btn danger" onclick="Admin.delUser(${u.id},'${esc(u.account)}')">删除</button>
                  </td>
                </tr>`).join('') : '<tr><td colspan="7" class="empty">没有用户</td></tr>'}
            </tbody>
          </table>
        </div>`);
    });
  },

  doUserSearch() {
    Admin.keyword = document.getElementById('ukw').value.trim();
    Admin.viewUser();
  },

  async userForm(id) {
    let u = { role: 'user' };
    if (id) {
      const list = await API.admin.userList('');
      u = list.find(x => String(x.id) === String(id)) || u;
    }
    Admin.modal(id ? '编辑用户 #' + id : '新增用户', `
      <div class="row two">
        <div><label>账号 ${id ? '（不可改）' : '*'}</label>
          <input id="u_account" value="${esc(u.account)}" ${id ? 'disabled' : ''}></div>
        <div><label>昵称</label><input id="u_nickname" value="${esc(u.nickname)}"></div>
      </div>
      <div class="row two">
        <div><label>手机号</label><input id="u_mobile" value="${esc(u.mobile)}"></div>
        <div><label>性别</label><input id="u_gender" value="${esc(u.gender)}"></div>
      </div>
      <div class="row two">
        <div><label>密码 ${id ? '（留空则不修改）' : '（留空默认 123456）'}</label>
          <input id="u_password" type="password" placeholder="••••••"></div>
        <div><label>角色</label>
          <select id="u_role">
            <option value="user" ${u.role !== 'admin' ? 'selected' : ''}>普通用户</option>
            <option value="admin" ${u.role === 'admin' ? 'selected' : ''}>管理员</option>
          </select>
        </div>
      </div>
      `, async () => {
      const body = {
        nickname: document.getElementById('u_nickname').value.trim(),
        mobile: document.getElementById('u_mobile').value.trim(),
        gender: document.getElementById('u_gender').value.trim(),
        password: document.getElementById('u_password').value,
        role: document.getElementById('u_role').value
      };
      if (id) await API.admin.userUpdate(id, body);
      else {
        body.account = document.getElementById('u_account').value.trim();
        if (!body.account) return toast('账号不能为空');
        await API.admin.userCreate(body);
      }
      Admin.closeModal();
      toast(id ? '修改成功' : '新增成功');
      Admin.viewUser();
    });
  },

  async delUser(id, account) {
    if (!confirm('确定删除用户「' + account + '」吗？')) return;
    try {
      await API.admin.userDelete(id);
      toast('已删除');
      Admin.viewUser();
    } catch (e) { toast(e.message); }
  },

  /* ---------------- 订单 ---------------- */
  viewOrder() {
    Admin.guard(async () => {
      const list = await API.admin.orderList();
      const st = { 1: '待付款', 2: '待发货', 3: '待收货', 4: '待评价', 5: '已完成', 6: '已取消' };
      Admin.main(`
        <div class="page-head"><h1>订单查看</h1><div class="grow"></div>
          <button class="btn" onclick="Admin.viewOrder()">刷新</button></div>
        <div class="panel">
          <table>
            <thead><tr><th>订单号</th><th>状态</th><th>实付</th><th>件数</th><th>收货人</th><th>联系方式</th><th>下单时间</th></tr></thead>
            <tbody>
              ${list.length ? list.map(o => `
                <tr>
                  <td>${o.id}</td>
                  <td>${st[o.orderState] || o.orderState}</td>
                  <td style="color:#e1251b;font-weight:700">￥${esc(o.payMoney)}</td>
                  <td>${o.totalNum || 0}</td>
                  <td>${esc(o.receiver || '')}</td>
                  <td>${esc(o.receiverContact || '')}</td>
                  <td style="font-size:12px;color:#999">${esc(o.createTime || '')}</td>
                </tr>`).join('') : '<tr><td colspan="7" class="empty">还没有订单</td></tr>'}
            </tbody>
          </table>
        </div>`);
    });
  },

  /* ---------------- 弹窗 ---------------- */
  modal(title, bodyHtml, onOk) {
    const old = document.getElementById('modalBox');
    if (old) old.remove();
    const wrap = document.createElement('div');
    wrap.className = 'modal show';
    wrap.id = 'modalBox';
    wrap.innerHTML = `
      <div class="box">
        <h3>${title}</h3>
        <div class="body">${bodyHtml}</div>
        <div class="acts">
          <button class="btn" onclick="Admin.closeModal()">取消</button>
          <button class="btn primary" id="modalOk">保存</button>
        </div>
      </div>`;
    document.body.appendChild(wrap);
    document.getElementById('modalOk').onclick = async () => {
      const btn = document.getElementById('modalOk');
      btn.disabled = true;
      try {
        await onOk();
      } catch (e) {
        toast(e.message);
      }
      btn.disabled = false;
    };
  },

  closeModal() {
    const el = document.getElementById('modalBox');
    if (el) el.remove();
  }
};

window.Admin = Admin;

/* 页面打开时：已有管理员会话就直接进，否则显示登录门 */
(function boot() {
  const u = API.user();
  if (u && u.admin && API.token()) Admin.enter(u);
})();
