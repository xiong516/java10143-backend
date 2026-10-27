/**
 * Java10143 WEB 前端（原生 JS，无构建，改完刷新即可）
 * 页面用 hash 路由：#/home  #/category  #/goods/1001  #/cart  #/checkout  #/pay/1  #/my  #/order/1
 */
const state = {
  categories: [],
  activeCategoryId: null,
  goods: null,
  specChoose: {},
  chosenSku: null,
  cartSelection: new Set(),
  checkoutSkuIds: [],
  checkoutAddressId: null,
  bannerIndex: 0,
  bannerTimer: null
};

/* ============ 小工具 ============ */
function h(html) {
  const div = document.createElement('div');
  div.innerHTML = html.trim();
  return div.firstElementChild;
}

function money(v) {
  return Number(v || 0).toFixed(2);
}

let toastTimer = null;
function toast(msg) {
  const old = document.querySelector('.toast');
  if (old) old.remove();
  const el = h(`<div class="toast">${msg}</div>`);
  document.body.appendChild(el);
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.remove(), 1800);
}

function productCard(p) {
  return `<div class="product" onclick="App.go('/goods/${p.id}')">
      <img src="${p.picture}" alt="${p.name}">
      <div class="info">
        <div class="name">${p.name}</div>
        <div class="price"><small>￥</small>${money(p.price)}</div>
        <div class="meta"><span>已售 ${p.orderNum || 0}</span><span>${p.desc || ''}</span></div>
      </div>
    </div>`;
}

function productGrid(list) {
  if (!list || !list.length) return '<div class="empty">暂无商品数据，请先导入 sql/java10143.sql</div>';
  return list.map(productCard).join('');
}

/* ============ 路由 ============ */
const App = {
  go(path) {
    location.hash = '#' + path;
  },

  async render() {
    const hash = location.hash.replace(/^#/, '') || '/home';
    const [path] = hash.split('?');
    const parts = path.split('/').filter(Boolean);
    const app = document.getElementById('app');
    document.getElementById('apiHint').textContent = location.origin;
    App.refreshUserArea();
    App.refreshCartBadge();

    try {
      const page = parts[0] || 'home';
      if (page === 'home') await App.viewHome(app);
      else if (page === 'category') await App.viewCategory(app, parts[1]);
      else if (page === 'goods') await App.viewGoods(app, parts[1]);
      else if (page === 'cart') await App.viewCart(app);
      else if (page === 'checkout') await App.viewCheckout(app, (hash.split('skuIds=')[1] || '').split(','));
      else if (page === 'pay') await App.viewPay(app, parts[1]);
      else if (page === 'my') await App.viewMy(app);
      else if (page === 'order') await App.viewOrder(app, parts[1]);
      else await App.viewHome(app);
      window.scrollTo(0, 0);
    } catch (e) {
      app.innerHTML = `<div class="card"><h3>出错了</h3><p style="color:#e1251b">${e.message}</p>
        <p style="color:#999;font-size:13px">请检查：1) 后端是否启动 2) 数据库是否导入 3) 是否需要先登录</p></div>`;
    }
  },

  /* ---------------- 首页 ---------------- */
  async viewHome(app) {
    const [banners, news, hot] = await Promise.all([
      API.banner(1), API.newGoods(8), API.hot(8)
    ]);
    app.innerHTML = `
      <div class="banner">
        ${banners.map((b, i) => `<img class="banner-img" data-i="${i}" src="${b.imgUrl}"
            style="display:${i === 0 ? 'block' : 'none'}" onclick="App.bannerGo('${(b.hrefUrl || '').replace(/'/g, '')}')">`).join('')}
        <div class="dots">${banners.map((b, i) =>
      `<i class="${i === 0 ? 'on' : ''}" onclick="App.switchBanner(${i})"></i>`).join('')}</div>
      </div>

      <div class="play-bar">
        <div class="play-card sk" onclick="location.href='/seckill.html'">
          <div class="pc-ico">⚡</div>
          <div>
            <div class="pc-t1">限时秒杀</div>
            <div class="pc-t2" id="skBrief">整点开抢 · 售完即止</div>
          </div>
          <div class="pc-go">去抢 ›</div>
        </div>
        <div class="play-card cp" onclick="location.href='/coupon.html'">
          <div class="pc-ico">🎫</div>
          <div>
            <div class="pc-t1">领券中心</div>
            <div class="pc-t2" id="cpBrief">先领券 · 最高立减 800</div>
          </div>
          <div class="pc-go">去领 ›</div>
        </div>
      </div>

      <div class="card">
        <h3 class="section-title">新人专享 · 新鲜好物</h3>
        <div class="grid">${productGrid(news)}</div>
      </div>
      <div class="card">
        <h3 class="section-title">热销榜单</h3>
        <div class="grid">${productGrid(hot)}</div>
      </div>
      <div class="card" id="recCard" style="display:none">
        <h3 class="section-title">猜你喜欢 <span style="font-weight:400;color:#999;font-size:13px" id="recReason"></span></h3>
        <div class="grid" id="recGrid"></div>
      </div>`;
    App.startBanner(banners.length);
    App.loadHomeExtras();
  },

  /** 首页附加数据：秒杀简报 + 领券简报 + AI 个性化推荐（失败不影响主流程） */
  async loadHomeExtras() {
    try {
      const sk = await API.seckillList();
      const run = (sk.running || [])[0];
      const up = (sk.upcoming || [])[0];
      const el = document.getElementById('skBrief');
      if (el) {
        if (run) el.innerHTML = `${run.name.slice(0, 10)}… <b style="color:#fff">剩 ${run.left} 件</b>`;
        else if (up) el.textContent = '下一场即将开始，先逛逛';
      }
    } catch (e) { /* 秒杀服务异常不影响首页 */ }

    try {
      const cp = await API.couponCenter();
      const el = document.getElementById('cpBrief');
      if (el) el.textContent = `${cp.availableCount || 0} 张券可领 · 先领再下单`;
    } catch (e) { /* 忽略 */ }

    try {
      const rec = await API.recommend(8);
      const list = Array.isArray(rec) ? rec : (rec.list || rec.goods || []);
      if (list.length) {
        document.getElementById('recCard').style.display = '';
        document.getElementById('recGrid').innerHTML = productGrid(list);
        // 推荐理由挂在第一个商品上，抽出来做小标题
        const r = document.getElementById('recReason');
        const reason = list[0] && list[0].reason;
        if (r && reason) r.textContent = `（${reason}）`;
      }
    } catch (e) { /* 未登录或服务异常都不显示推荐区 */ }
  },

  startBanner(count) {
    clearInterval(state.bannerTimer);
    if (count <= 1) return;
    state.bannerTimer = setInterval(() => {
      state.bannerIndex = (state.bannerIndex + 1) % count;
      App.switchBanner(state.bannerIndex);
    }, 3500);
  },

  switchBanner(i) {
    state.bannerIndex = i;
    document.querySelectorAll('.banner-img').forEach((img, idx) => {
      img.style.display = idx === i ? 'block' : 'none';
    });
    document.querySelectorAll('.banner .dots i').forEach((d, idx) => {
      d.className = idx === i ? 'on' : '';
    });
  },

  /** 轮播图点击跳转：优先取 hrefUrl 里的 #/xxx 路由；没有路由就原样打开链接 */
  bannerGo(hrefUrl) {
    if (!hrefUrl) return;
    const m = String(hrefUrl).match(/#(\/[^?#]+)/);
    if (m) return App.go(m[1]);          // 站内路由，如 #/goods/1001
    location.href = hrefUrl;             // 外链兜底
  },

  async search() {
    const kw = document.getElementById('searchInput').value.trim();
    if (!kw) return App.go('/home');
    const app = document.getElementById('app');
    const all = await API.newGoods(100);
    const list = all.filter(p => (p.name + p.desc).includes(kw));
    app.innerHTML = `<div class="card">
        <h3 class="section-title">“${kw}” 的搜索结果（${list.length} 件）</h3>
        <div class="grid">${productGrid(list)}</div>
      </div>`;
  },

  /* ---------------- 分类 ---------------- */
  async viewCategory(app, id) {
    const heads = await API.categoryHead();
    state.categories = heads;
    const activeId = id || heads[0]?.id;
    state.activeCategoryId = activeId;
    const detail = await API.category(activeId);

    const side = heads.map(c =>
      `<div class="${String(c.id) === String(activeId) ? 'on' : ''}"
            onclick="App.go('/category/${c.id}')">${c.name}</div>`).join('');

    const groups = (detail.children || []).map(child => `
      <div class="card">
        <div class="cat-group-title">${child.name}</div>
        <div class="grid">${productGrid(child.goods)}</div>
      </div>`).join('');

    app.innerHTML = `<div class="cat-wrap">
        <div class="cat-side">${side}</div>
        <div class="cat-main">${groups || '<div class="empty">该分类下暂无商品</div>'}</div>
      </div>`;
  },

  /* ---------------- 商品详情 ---------------- */
  async viewGoods(app, id) {
    if (!id) return App.go('/home');
    // 从短视频页「视频同款」跳进来时带 from=video，详情页顶部显示来源条
    const fromVideo = /[?&]from=video/.test(location.href);
    const g = await API.goods(id);
    state.goods = g;
    // 默认选中第一个规格值
    state.specChoose = {};
    (g.specs || []).forEach(s => {
      if (s.values && s.values.length) state.specChoose[s.name] = s.values[0].name;
    });
    state.chosenSku = App.matchSku();
    app.innerHTML = App.goodsHtml(g, fromVideo);

    // 若后端没返回 specs，就直接用第一个 sku
    if ((!g.specs || !g.specs.length) && g.skus && g.skus.length) {
      state.chosenSku = g.skus[0];
    }
    App.refreshSkuArea();
    App.setMainPic(g.mainPictures[0]);
    App.loadGoodsComments(g.id);
  },

  goodsHtml(g, fromVideo) {
    const thumbs = (g.mainPictures || []).map((p, i) =>
      `<img class="${i === 0 ? 'on' : ''}" src="${p}" onclick="App.setMainPic('${p}', this)">`).join('');
    const specs = (g.specs || []).map(s => `
      <div class="spec-row">
        <b>${s.name}</b>
        ${(s.values || []).map(v => `<span class="spec-item ${state.specChoose[s.name] === v.name ? 'on' : ''}"
             onclick="App.chooseSpec('${s.name}','${v.name}', this)">${v.name}</span>`).join('')}
      </div>`).join('');
    const props = ((g.details && g.details.properties) || [])
      .map(p => `<div class="spec-row"><b>${p.name}</b><span>${p.value}</span></div>`).join('');

    return `<div class="card">
      ${fromVideo ? `<div class="from-video show" onclick="location.href='/video.html'">
        <div class="fv-icon">▶</div>
        <div class="fv-text">
          <div class="fv-t1">来自短视频 · 视频同款</div>
          <div class="fv-t2">你从短视频里点进来的这件宝贝</div>
        </div>
        <div class="fv-go">继续刷</div>
      </div>` : ''}
      <div class="detail">
        <div class="detail-gallery">
          <img class="main" id="mainPic" src="${g.mainPictures[0] || ''}" alt="${g.name}">
          <div class="thumbs">${thumbs}</div>
        </div>
        <div class="detail-info">
          <h1>${g.name}</h1>
          <p style="color:#999;font-size:13px">${g.desc || ''}</p>
          <div class="price-box">
            <span class="now">￥<span id="skuPrice">${money(g.price)}</span></span>
            <span class="old">￥${money(g.oldPrice)}</span>
            <div style="color:#999;font-size:12px;margin-top:6px">
              库存 <span id="skuStock">${g.inventory || 0}</span> 件 · 已售 ${g.salesCount || 0} 件 · 评价 ${g.commentCount || 0} 条
            </div>
            <div class="ph-entry" id="phEntry" onclick="App.togglePrice()">
              <span class="ph-ico">📈</span>
              <span id="phBrief">近 90 天价格走势</span>
              <span class="ph-arrow" id="phArrow">展开 ▾</span>
            </div>
          </div>
          <div class="ph-panel" id="phPanel">
            <div class="ph-stat" id="phStat"></div>
            <div class="ph-canvas-wrap"><canvas id="phChart" height="150"></canvas></div>
            <div class="ph-advice" id="phAdvice"></div>
            <div class="ph-tabs">
              <span class="ph-t on" data-d="30" onclick="App.loadPrice(30)">近30天</span>
              <span class="ph-t" data-d="60" onclick="App.loadPrice(60)">近60天</span>
              <span class="ph-t" data-d="90" onclick="App.loadPrice(90)">近90天</span>
            </div>
          </div>
          <div id="specArea">${specs}</div>
          <div class="spec-row">
            <b>数量</b>
            <div class="qty">
              <button onclick="App.changeQty(-1)">-</button>
              <span id="qtyVal">1</span>
              <button onclick="App.changeQty(1)">+</button>
            </div>
          </div>
          <div class="detail-actions">
            <button class="btn-cart" onclick="App.addCart()">加入购物车</button>
            <button class="btn-buy" onclick="App.buyNow()">立即购买</button>
          </div>
          <div style="margin-top:18px;color:#666;font-size:13px">
            <b style="color:#333">商品参数</b>${props}
          </div>
        </div>
      </div>
      <h3 class="section-title" style="margin-top:24px">同类推荐</h3>
      <div class="grid">${productGrid(g.similarProducts)}</div>
      <h3 class="section-title" style="margin-top:24px">商品评论 <span id="cmtTotal" style="font-weight:400;color:#999;font-size:13px"></span></h3>
      <div id="goodsComments"><div class="empty">评论加载中…</div></div>
      <div class="cmt-form">
        <input id="goodsCmtInput" placeholder="说说你的使用感受…（需登录）" maxlength="200"
               onkeydown="if(event.key==='Enter')App.postGoodsComment()">
        <button class="btn-primary" onclick="App.postGoodsComment()">发表评论</button>
      </div>
    </div>`;
  },

  /* ---------------- 价格走势（近 N 天最低价曲线） ---------------- */
  priceDays: 30,
  priceData: null,

  togglePrice() {
    const p = document.getElementById('phPanel');
    if (!p) return;
    const open = p.classList.toggle('show');
    document.getElementById('phArrow').textContent = open ? '收起 ▴' : '展开 ▾';
    if (open && !this.priceData) this.loadPrice(this.priceDays);
    if (open && this.priceData) this.drawPrice();
  },

  async loadPrice(days) {
    const g = state.goods;
    if (!g) return;
    this.priceDays = days || 30;
    document.querySelectorAll('.ph-t').forEach(el => el.classList.toggle('on', +el.dataset.d === this.priceDays));
    const stat = document.getElementById('phStat');
    if (stat) stat.innerHTML = '<span style="color:#999">加载中…</span>';
    try {
      this.priceData = await API.priceHistory(g.id, this.priceDays);
    } catch (e) {
      if (stat) stat.innerHTML = `<span style="color:#e1251b">${e.message}</span>`;
      return;
    }
    this.renderPrice();
  },

  renderPrice() {
    const d = this.priceData;
    if (!d) return;
    const stat = document.getElementById('phStat');
    if (stat) {
      stat.innerHTML = `
        <div class="phs"><em>当前价</em><b style="color:#e1251b">￥${d.current}</b></div>
        <div class="phs"><em>历史最低</em><b>￥${d.min}</b><i>${d.minDate || ''}</i></div>
        <div class="phs"><em>历史最高</em><b>￥${d.max}</b><i>${d.maxDate || ''}</i></div>
        <div class="phs"><em>平均价</em><b>￥${d.avg}</b></div>`;
    }
    const adv = document.getElementById('phAdvice');
    if (adv) {
      const color = d.adviceLevel === 'low' ? '#0a8a4a' : (d.adviceLevel === 'high' ? '#e1251b' : '#8a6d1f');
      const bg = d.adviceLevel === 'low' ? '#eafaf1' : (d.adviceLevel === 'high' ? '#fff1ef' : '#fdf7e5');
      adv.innerHTML = `<span style="color:${color};background:${bg}">${d.adviceLevel === 'low' ? '💡' : (d.adviceLevel === 'high' ? '⚠️' : 'ℹ️')} ${d.advice}</span>
        <em>当前价在近 ${d.days} 天的第 ${d.percentile} 百分位${d.saveVsMax !== '0.00' ? '，比最高价省 ￥' + d.saveVsMax : ''}</em>`;
    }
    const brief = document.getElementById('phBrief');
    if (brief) brief.textContent = `近 ${d.days} 天最低 ￥${d.min}`;
    this.drawPrice();
  },

  /** 手绘折线（不引第三方库，纯 canvas，省一个 CDN 依赖） */
  drawPrice() {
    const d = this.priceData;
    const cv = document.getElementById('phChart');
    if (!d || !cv || !d.labels || !d.labels.length) return;
    const dpr = window.devicePixelRatio || 1;
    const W = cv.parentElement.clientWidth || 600;
    const H = 150;
    cv.width = W * dpr; cv.height = H * dpr;
    cv.style.width = W + 'px'; cv.style.height = H + 'px';
    const ctx = cv.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, W, H);

    const padL = 46, padR = 14, padT = 14, padB = 24;
    const w = W - padL - padR, h = H - padT - padB;
    const prices = d.prices.map(Number);
    let min = Math.min(...prices), max = Math.max(...prices);
    if (max === min) { max = min + 1; }
    const pad = (max - min) * 0.15;
    min -= pad; max += pad;

    const X = i => padL + (prices.length === 1 ? w / 2 : (i / (prices.length - 1)) * w);
    const Y = v => padT + h - ((v - min) / (max - min)) * h;

    // 网格 + Y 轴刻度
    ctx.strokeStyle = '#eef0f3'; ctx.lineWidth = 1;
    ctx.fillStyle = '#9aa0a8'; ctx.font = '10px -apple-system,sans-serif'; ctx.textAlign = 'right';
    for (let i = 0; i <= 4; i++) {
      const v = min + ((max - min) * i) / 4;
      const y = Y(v);
      ctx.beginPath(); ctx.moveTo(padL, y); ctx.lineTo(W - padR, y); ctx.stroke();
      ctx.fillText('￥' + Math.round(v), padL - 6, y + 3);
    }

    // 面积
    const grad = ctx.createLinearGradient(0, padT, 0, padT + h);
    grad.addColorStop(0, 'rgba(225,37,27,.22)');
    grad.addColorStop(1, 'rgba(225,37,27,0)');
    ctx.beginPath();
    prices.forEach((v, i) => { i === 0 ? ctx.moveTo(X(i), Y(v)) : ctx.lineTo(X(i), Y(v)); });
    ctx.lineTo(X(prices.length - 1), padT + h);
    ctx.lineTo(X(0), padT + h);
    ctx.closePath(); ctx.fillStyle = grad; ctx.fill();

    // 折线
    ctx.beginPath();
    prices.forEach((v, i) => { i === 0 ? ctx.moveTo(X(i), Y(v)) : ctx.lineTo(X(i), Y(v)); });
    ctx.strokeStyle = '#e1251b'; ctx.lineWidth = 2; ctx.lineJoin = 'round'; ctx.stroke();

    // 最低点标记
    const iMin = prices.indexOf(Math.min(...prices));
    ctx.beginPath();
    ctx.arc(X(iMin), Y(prices[iMin]), 4.5, 0, Math.PI * 2);
    ctx.fillStyle = '#fff'; ctx.fill();
    ctx.strokeStyle = '#0a8a4a'; ctx.lineWidth = 2.5; ctx.stroke();
    ctx.fillStyle = '#0a8a4a'; ctx.font = 'bold 10px -apple-system,sans-serif'; ctx.textAlign = 'center';
    ctx.fillText('最低', X(iMin), Y(prices[iMin]) - 9);

    // 最后一点 = 当前价
    const lastI = prices.length - 1;
    ctx.beginPath();
    ctx.arc(X(lastI), Y(prices[lastI]), 4, 0, Math.PI * 2);
    ctx.fillStyle = '#e1251b'; ctx.fill();

    // X 轴首尾日期
    ctx.fillStyle = '#9aa0a8'; ctx.font = '10px -apple-system,sans-serif';
    ctx.textAlign = 'left'; ctx.fillText((d.labels[0] || '').slice(5), padL, H - 7);
    ctx.textAlign = 'right'; ctx.fillText((d.labels[lastI] || '').slice(5), W - padR, H - 7);
  },

  /* ---------------- 商品评论（看 / 发 / 删） ---------------- */
  async loadGoodsComments(goodsId) {
    const box = document.getElementById('goodsComments');
    if (!box) return;
    try {
      const list = await API.commentList('goods', goodsId);
      const user = API.user();
      const total = document.getElementById('cmtTotal');
      if (total) total.textContent = `（${list.length} 条）`;
      box.innerHTML = list.length ? list.map(c => `
        <div class="cmt">
          <img src="${c.avatar || ''}">
          <div class="cb">
            <div class="cn">${c.nickname || '京东用户'} ${c.score ? `<span class="star">${'★'.repeat(Math.round(Number(c.score)))}</span>` : ''}</div>
            <div class="cc">${c.content || ''}</div>
            <div class="cf">
              <span>${c.createTime || ''}</span>
              <b onclick="App.likeComment(${c.id}, this)">赞 ${c.likeCount || 0}</b>
              ${(user && (String(c.userId) === String(user.id) || user.admin))
                ? `<b class="del" onclick="App.delGoodsComment(${c.id},'${goodsId}')">删除</b>` : ''}
            </div>
          </div>
        </div>`).join('')
        : '<div class="empty">还没有评论，来做第一个吧~</div>';
    } catch (e) {
      box.innerHTML = `<div class="empty" style="color:#e1251b">${e.message}</div>`;
    }
  },

  async postGoodsComment() {
    if (!App.requireLogin()) return;
    const goodsId = state.goods ? state.goods.id : null;
    if (!goodsId) return;
    const input = document.getElementById('goodsCmtInput');
    const text = input.value.trim();
    if (!text) return toast('评论内容不能为空');
    try {
      await API.commentAdd('goods', goodsId, text, 5);
      input.value = '';
      toast('评论成功');
      App.loadGoodsComments(goodsId);
    } catch (e) { toast(e.message); }
  },

  async delGoodsComment(id, goodsId) {
    if (!confirm('确定删除这条评论吗？')) return;
    try {
      await API.commentDelete(id);
      toast('已删除');
      App.loadGoodsComments(goodsId);
    } catch (e) { toast(e.message); }
  },

  async likeComment(id, el) {
    try {
      const res = await API.commentLike(id);
      el.textContent = '赞 ' + res.likeCount;
    } catch (e) { toast(e.message); }
  },

  setMainPic(url, el) {
    const main = document.getElementById('mainPic');
    if (main) main.src = url;
    if (el) {
      document.querySelectorAll('.thumbs img').forEach(i => i.classList.remove('on'));
      el.classList.add('on');
    }
  },

  chooseSpec(name, value, el) {
    state.specChoose[name] = value;
    state.chosenSku = App.matchSku();
    if (el) {
      const row = el.parentElement;
      row.querySelectorAll('.spec-item').forEach(i => i.classList.remove('on'));
      el.classList.add('on');
    }
    App.refreshSkuArea();
  },

  matchSku() {
    const g = state.goods;
    if (!g || !g.skus) return null;
    return g.skus.find(sku => {
      const specs = sku.specs || [];
      return Object.keys(state.specChoose).every(k =>
        specs.some(s => s.name === k && s.valueName === state.specChoose[k]));
    }) || null;
  },

  refreshSkuArea() {
    const sku = state.chosenSku;
    const priceEl = document.getElementById('skuPrice');
    const stockEl = document.getElementById('skuStock');
    if (sku && priceEl) {
      priceEl.textContent = money(sku.price);
      if (stockEl) stockEl.textContent = sku.inventory;
    }
  },

  currentQty: 1,
  changeQty(d) {
    const el = document.getElementById('qtyVal');
    let n = parseInt(el.textContent) + d;
    if (n < 1) n = 1;
    if (n > 99) n = 99;
    el.textContent = n;
    App.currentQty = n;
  },

  requireLogin() {
    if (!API.token()) {
      App.openLogin();
      return false;
    }
    return true;
  },

  async addCart() {
    if (!App.requireLogin()) return;
    const sku = state.chosenSku;
    if (!sku) return toast('请选择商品规格');
    await API.cartAdd(sku.id, App.currentQty);
    toast('已加入购物车');
    App.refreshCartBadge();
  },

  async buyNow() {
    if (!App.requireLogin()) return;
    const sku = state.chosenSku;
    if (!sku) return toast('请选择商品规格');
    await API.cartAdd(sku.id, App.currentQty);
    const cart = await API.cartList();
    const ids = cart.filter(c => String(c.skuId) === String(sku.id)).map(c => c.skuId);
    App.go('/checkout?skuIds=' + ids.join(','));
  },

  /* ---------------- 购物车 ---------------- */
  async viewCart(app) {
    if (!API.token()) {
      app.innerHTML = '<div class="card empty">请先登录后查看购物车<br><br><button class="btn-primary" onclick="App.openLogin()">立即登录</button></div>';
      return;
    }
    const list = await API.cartList();
    App.refreshCartBadge(list);
    if (!list.length) {
      app.innerHTML = '<div class="card empty">购物车还是空的，去逛逛吧~<br><br><button class="btn-primary" onclick="App.go(\'/home\')">去首页</button></div>';
      return;
    }

    // 已存在的 skuId 保留勾选状态，新来的默认选中
    if (!state.cartSelection.size) list.forEach(c => { if (c.selected) state.cartSelection.add(String(c.skuId)); });

    app.innerHTML = `<div class="card">
      <h3 class="section-title">我的购物车（${list.length} 种商品）</h3>
      <div id="cartList">
        ${list.map(c => App.cartItemHtml(c)).join('')}
      </div>
      <div class="cart-summary">
        <div class="total">已选 <b id="cartCount">0</b> 件，合计：<b>￥<span id="cartTotal">0.00</span></b></div>
        <div>
          <button class="btn-plain" onclick="App.removeSelected()">删除选中</button>
          <button class="btn-primary" style="margin-left:10px" onclick="App.toCheckout()">去结算</button>
        </div>
      </div>
    </div>`;
    App.calcCart(list);
  },

  cartItemHtml(c) {
    const checked = state.cartSelection.has(String(c.skuId));
    return `<div class="cart-item" data-sku="${c.skuId}">
      <input type="checkbox" ${checked ? 'checked' : ''} onchange="App.toggleCart('${c.skuId}')">
      <img src="${c.picture}" onclick="App.go('/goods/${c.id}')">
      <div class="cname">
        <div>${c.name}</div>
        <p>${c.attrsText || ''}</p>
        <div style="color:#e1251b;font-weight:700">￥${money(c.nowPrice)}</div>
      </div>
      <div class="qty">
        <button onclick="App.cartQty('${c.skuId}',-1)">-</button>
        <span id="qty-${c.skuId}">${c.count}</span>
        <button onclick="App.cartQty('${c.skuId}',1)">+</button>
      </div>
    </div>`;
  },

  async toggleCart(skuId) {
    skuId = String(skuId);
    const on = !state.cartSelection.has(skuId);
    if (on) state.cartSelection.add(skuId); else state.cartSelection.delete(skuId);
    await API.cartUpdate(skuId, { selected: on });
    const list = await API.cartList();
    App.calcCart(list);
  },

  async cartQty(skuId, d) {
    const el = document.getElementById('qty-' + skuId);
    let n = parseInt(el.textContent) + d;
    if (n < 1) return;
    el.textContent = n;
    await API.cartUpdate(skuId, { count: n });
    const list = await API.cartList();
    App.calcCart(list);
  },

  calcCart(list) {
    let count = 0, total = 0;
    list.forEach(c => {
      if (state.cartSelection.has(String(c.skuId))) {
        count += c.count;
        total += Number(c.nowPrice) * c.count;
      }
    });
    const cEl = document.getElementById('cartCount');
    const tEl = document.getElementById('cartTotal');
    if (cEl) cEl.textContent = count;
    if (tEl) tEl.textContent = money(total);
  },

  async removeSelected() {
    if (!state.cartSelection.size) return toast('请先勾选商品');
    await API.cartDelete([...state.cartSelection]);
    state.cartSelection.clear();
    toast('已删除');
    App.render();
  },

  async toCheckout() {
    if (!state.cartSelection.size) return toast('请先勾选商品');
    App.go('/checkout?skuIds=' + [...state.cartSelection].join(','));
  },

  /* ---------------- 结算 ---------------- */
  async viewCheckout(app, skuIds) {
    if (!API.token()) return App.go('/cart');
    const ids = skuIds.filter(Boolean);
    App.checkoutSkuIds = ids;
    App.refreshCartBadge();
    const pre = await API.orderPre(ids);
    const addresses = pre.userAddresses || [];
    if (!App.checkoutAddressId && addresses.length) {
      const def = addresses.find(a => a.isDefault === 0) || addresses[0];
      App.checkoutAddressId = def.id;
    }

    app.innerHTML = `<div class="card">
      <h3 class="section-title">确认收货地址</h3>
      ${addresses.length ? addresses.map(a => `
        <div class="address-item ${String(a.id) === String(App.checkoutAddressId) ? 'on' : ''}"
             onclick="App.pickAddress('${a.id}', this)">
          <div class="line1">${a.receiver} <span style="color:#666;font-weight:400">${a.contact}</span>
            ${a.isDefault === 0 ? '<span class="tag">默认</span>' : ''}</div>
          <div class="line2">${a.fullLocation || ''} ${a.address || ''}</div>
        </div>`).join('')
      : '<div class="empty">还没有收货地址，先添加一个吧</div>'}
      <div id="addAddrBox" style="display:none;margin-top:14px">
        <input id="ar" placeholder="收货人" class="dialog-input" style="width:100%;height:38px;margin-bottom:8px">
        <input id="ac" placeholder="手机号" style="width:100%;height:38px;margin-bottom:8px">
        <input id="af" placeholder="省市区，如：北京市 北京市 海淀区" style="width:100%;height:38px;margin-bottom:8px">
        <input id="aa" placeholder="详细地址" style="width:100%;height:38px;margin-bottom:8px">
        <button class="btn-primary" onclick="App.saveAddress()">保存地址</button>
      </div>
      <button class="btn-plain" style="margin-top:12px" onclick="App.toggleAddrForm()">+ 新增收货地址</button>
    </div>

    <div class="card">
      <h3 class="section-title">商品清单</h3>
      ${pre.goods.map(g => `<div class="cart-item">
        <img src="${g.picture}">
        <div class="cname"><div>${g.name}</div><p>${g.attrsText || ''}</p>
          <div style="color:#e1251b">￥${money(g.price)} × ${g.count}</div></div>
        <div style="font-weight:700">￥${money(g.totalPrice)}</div>
      </div>`).join('')}
      <div class="cart-summary">
        <div class="total">共 ${pre.summary.goodsCount} 件，运费 ￥${money(pre.summary.postFee)}，
          应付：<b id="payTotal">￥${money(pre.summary.totalPayPrice)}</b></div>
        <button class="btn-primary" onclick="App.submitOrder()">提交订单</button>
      </div>
    </div>

    <div class="card">
      <h3 class="section-title">优惠券 <span style="font-weight:400;color:#999;font-size:13px">已自动选择最优券，可手动更换</span></h3>
      <div id="cpPick">${App.couponPickHtml(pre)}</div>
    </div>`;

    // 记住结算上下文，切换券时不用再请求 /order/pre
    App.checkoutCtx = {
      amount: Number(pre.summary.totalPrice),
      spuIds: (pre.goods || []).map(g => g.spuId || '').filter(Boolean),
      coupons: pre.coupons || { options: [], best: null, discount: '0.00' },
      selectedUcId: (pre.coupons && pre.coupons.best) ? pre.coupons.best.ucId : null,
      basePay: Number(pre.summary.totalPayPrice),
      total: Number(pre.summary.totalPrice)
    };
  },

  /** 结算页券列表 HTML */
  couponPickHtml(pre) {
    const info = pre.coupons || {};
    const opts = info.options || [];
    const best = info.best;
    if (!opts.length) {
      return `<div class="empty" style="padding:14px 0">暂无可用券，
        <a href="/coupon.html" style="color:#e1251b">去领券中心 ›</a></div>`;
    }
    const chosen = best ? best.ucId : null;
    return opts.map(o => `
      <div class="cp-opt ${o.ucId === chosen ? 'on' : ''}" onclick="App.pickCoupon(${o.ucId}, ${o.discount}, this)">
        <span class="cpo-amt">${o.type === 2 ? o.discount + '折' : '¥' + (+o.amount)}</span>
        <div class="cpo-txt">
          <div class="cpo-name">${o.name}</div>
          <div class="cpo-sub">${(+o.threshold) > 0 ? '满 ' + (+o.threshold) + ' 元可用' : '无门槛'} · 可减 ￥${o.discount}</div>
        </div>
        ${o.ucId === chosen ? '<span class="cpo-best">已选</span>' : ''}
      </div>`).join('') +
      `<div class="cp-opt ${chosen === null ? 'on' : ''}" onclick="App.pickCoupon(null, 0, this)">
        <span class="cpo-amt" style="background:#9aa0a8">不使用</span>
        <div class="cpo-txt"><div class="cpo-name">不使用优惠券</div>
          <div class="cpo-sub">保留券下次使用</div></div>
        ${chosen === null ? '<span class="cpo-best">已选</span>' : ''}
      </div>`;
  },

  pickCoupon(ucId, discount, el) {
    const ctx = App.checkoutCtx;
    if (!ctx) return;
    ctx.selectedUcId = ucId;
    const pay = Math.max(0, ctx.total - Number(discount));
    const pt = document.getElementById('payTotal');
    if (pt) pt.textContent = '￥' + money(pay);
    // 重绘选中态（el 由内联 onclick 传入）
    document.querySelectorAll('.cp-opt').forEach(e => e.classList.remove('on'));
    document.querySelectorAll('.cpo-best').forEach(e => e.remove());
    const box = el || (window.event && window.event.currentTarget);
    if (box) {
      box.classList.add('on');
      const tag = document.createElement('span');
      tag.className = 'cpo-best';
      tag.textContent = '已选';
      box.appendChild(tag);
    }
  },

  toggleAddrForm() {
    const box = document.getElementById('addAddrBox');
    box.style.display = box.style.display === 'none' ? 'block' : 'none';
  },

  pickAddress(id, el) {
    App.checkoutAddressId = id;
    document.querySelectorAll('.address-item').forEach(i => i.classList.remove('on'));
    el.classList.add('on');
  },

  async saveAddress() {
    const receiver = document.getElementById('ar').value.trim();
    const contact = document.getElementById('ac').value.trim();
    const fullLocation = document.getElementById('af').value.trim();
    const address = document.getElementById('aa').value.trim();
    if (!receiver || !contact || !address) return toast('请填写收货人、手机号和详细地址');
    await API.addressAdd({
      receiver, contact, fullLocation, address,
      provinceCode: '110000', cityCode: '110100', countyCode: '110101',
      postalCode: '100000', addressTags: '家', isDefault: 1
    });
    toast('地址已保存');
    App.render();
  },

  async submitOrder() {
    if (!App.checkoutAddressId) return toast('请先选择收货地址');
    const ids = (App.checkoutSkuIds && App.checkoutSkuIds.length) ? App.checkoutSkuIds : [...state.cartSelection];
    if (!ids.length) return toast('没有可结算的商品');
    // 数量以结算接口返回的为准
    const pre = await API.orderPre(ids);
    const body = {
      addressId: String(App.checkoutAddressId),
      buyerMessage: '',
      deliveryTimeType: 1,
      payChannel: 1,
      payType: 1,
      goods: pre.goods.map(g => ({ skuId: String(g.skuId), count: g.count }))
    };
    // 带上用户在结算页选中的券；没选就交给后端自动挑最优
    if (App.checkoutCtx && App.checkoutCtx.selectedUcId) {
      body.couponId = String(App.checkoutCtx.selectedUcId);
    } else if (App.checkoutCtx) {
      body.autoCoupon = '0';
    }
    if (!body.goods.length) return toast('没有可结算的商品');
    const res = await API.orderSubmit(body);
    const tip = (res.couponDiscount && Number(res.couponDiscount) > 0)
      ? `下单成功，券已抵扣 ￥${res.couponDiscount}` : '下单成功';
    toast(tip);
    state.cartSelection.clear();
    App.refreshCartBadge();
    App.go('/pay/' + res.orderId);
  },

  /* ---------------- 支付 ---------------- */
  async viewPay(app, orderId) {
    const pay = await API.pay(orderId);
    app.innerHTML = `<div class="card" style="text-align:center;padding:40px 16px">
      <h3 class="section-title" style="border:none;text-align:center">收银台</h3>
      <p style="color:#666">订单号：<b>${pay.orderId}</b></p>
      <p style="color:#e1251b;font-size:26px;font-weight:700;margin:16px 0">支付成功</p>
      <p style="color:#999;font-size:13px;word-break:break-all">模拟支付宝回调地址：<br>${pay.redirect}</p>
      <div style="margin-top:24px;display:flex;gap:12px;justify-content:center">
        <a class="btn-plain" style="line-height:42px;text-decoration:none" href="${pay.redirect}" target="_blank">打开支付结果页</a>
        <button class="btn-primary" onclick="App.go('/order/${pay.orderId}')">查看订单详情</button>
      </div>
    </div>`;
  },

  /* ---------------- 我的 / 订单 ---------------- */
  async viewMy(app) {
    const user = API.user();
    if (!user) {
      app.innerHTML = `<div class="card empty">
        还没有登录<br><br><button class="btn-primary" onclick="App.openLogin()">立即登录</button>
        <p style="font-size:13px;margin-top:16px">演示账号：hm / 123456</p></div>`;
      return;
    }
    const orders = await API.orderList();
    const stateText = { 1: '待付款', 2: '待发货', 3: '待收货', 4: '待评价', 5: '已完成', 6: '已取消' };

    app.innerHTML = `
      <div class="card" style="display:flex;align-items:center;gap:14px">
        <img src="${user.avatar}" style="width:56px;height:56px;border-radius:50%">
        <div>
          <div style="font-size:17px;font-weight:700">${user.nickname}</div>
          <div style="color:#999;font-size:13px">${user.mobile || ''} · ${user.profession || ''}</div>
        </div>
      </div>
      <div class="card">
        <h3 class="section-title">我的订单（${orders.length}）</h3>
        ${orders.length ? orders.map(o => `<div class="order-card">
            <div class="order-head">
              <span>订单号：${o.id}</span>
              <span class="state ${o.orderState >= 5 ? 'done' : ''}">${stateText[o.orderState] || o.orderState}</span>
            </div>
            <div class="order-body">
              <img src="${(o.skus && o.skus[0] && o.skus[0].image) || ''}">
              <div style="flex:1">
                <div>${(o.skus && o.skus[0] && o.skus[0].name) || ''}
                  ${o.skus && o.skus.length > 1 ? ` 等 ${o.skus.length} 件商品` : ''}</div>
                <div style="color:#999;font-size:12px;margin-top:6px">${o.createTime || ''}</div>
              </div>
              <div style="text-align:right">
                <div style="color:#e1251b;font-weight:700">￥${money(o.payMoney)}</div>
                <button class="btn-plain" style="height:30px;margin-top:8px;font-size:12px"
                  onclick="App.go('/order/${o.id}')">查看详情</button>
              </div>
            </div>
          </div>`).join('')
      : '<div class="empty">还没有订单，去下一单试试</div>'}
      </div>`;
  },

  async viewOrder(app, id) {
    const o = await API.orderDetail(id);
    const stateText = { 1: '待付款', 2: '待发货', 3: '待收货', 4: '待评价', 5: '已完成', 6: '已取消' };
    app.innerHTML = `<div class="card">
      <h3 class="section-title">订单详情</h3>
      <div style="color:#666;line-height:2">
        <div>订单号：<b>${o.id}</b></div>
        <div>订单状态：<b style="color:#e1251b">${stateText[o.orderState] || o.orderState}</b></div>
        <div>下单时间：${o.createTime || ''}</div>
        <div>收货人：${o.receiverContact || ''} ${o.receiverMobile || ''}</div>
        <div>收货地址：${o.receiverAddress || ''}</div>
      </div>
      <h3 class="section-title" style="margin-top:20px">商品</h3>
      ${(o.skus || []).map(s => `<div class="cart-item">
        <img src="${s.image}">
        <div class="cname"><div>${s.name}</div><p>${s.attrsText || ''}</p>
          <div>￥${money(s.curPrice)} × ${s.quantity}</div></div>
        <div style="font-weight:700">￥${money(s.realPay)}</div>
      </div>`).join('')}
      <div class="cart-summary">
        <div class="total">商品总额 ￥${money(o.totalMoney)} ＋ 运费 ￥${money(o.postFee)}
          ＝ 实付 <b>￥${money(o.payMoney)}</b></div>
        <button class="btn-plain" onclick="App.go('/my')">返回我的订单</button>
      </div>
    </div>`;
  },

  /* ---------------- 登录 ---------------- */
  openLogin() {
    document.getElementById('loginMask').classList.add('show');
  },

  closeLogin() {
    document.getElementById('loginMask').classList.remove('show');
  },

  async doLogin() {
    const account = document.getElementById('loginAccount').value.trim();
    const password = document.getElementById('loginPassword').value;
    try {
      const user = await API.login(account, password);
      API.setLogin(user);
      App.closeLogin();
      toast('欢迎回来，' + user.nickname);
      App.refreshUserArea();
      App.render();
    } catch (e) {
      toast(e.message);
    }
  },

  refreshUserArea() {
    const user = API.user();
    const box = document.getElementById('userArea');
    if (!box) return;
    box.innerHTML = user
      ? `<img src="${user.avatar}"><span>${user.nickname}</span>
         <button class="btn-link" onclick="App.logout()">退出</button>`
      : `<button class="btn-link" onclick="App.openLogin()">登录 / 注册</button>`;
  },

  logout() {
    API.logout();
    state.cartSelection.clear();
    toast('已退出登录');
    App.go('/home');
  },

  async refreshCartBadge(list) {
    const badge = document.getElementById('cartBadge');
    if (!badge) return;
    if (!API.token()) {
      badge.textContent = '';
      return;
    }
    try {
      const data = list || await API.cartList();
      const n = data.reduce((sum, c) => sum + c.count, 0);
      badge.textContent = n > 0 ? n : '';
    } catch (e) {
      badge.textContent = '';
    }
  }
};

window.App = App;
window.addEventListener('hashchange', App.render);
window.addEventListener('DOMContentLoaded', () => {
  const input = document.getElementById('searchInput');
  if (input) input.addEventListener('keydown', e => { if (e.key === 'Enter') App.search(); });
  App.render();
});
