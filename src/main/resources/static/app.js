const API = '/api/v1';
const view = document.getElementById('view');
const nav = document.querySelector('nav');
const toastBox = document.getElementById('toast');

const DAYS = [['MON', '월'], ['TUE', '화'], ['WED', '수'], ['THU', '목'], ['FRI', '금'], ['SAT', '토'], ['SUN', '일']];
const MEALS = { BEFORE: '식전', AFTER: '식후', ANY: '식사 무관' };
const STATUS = { ACTIVE: '복용 중', PAUSED: '일시중지', ENDED: '복용 종료' };
const SPECIES = { CAT: ['🐱', '고양이'], DOG: ['🐶', '강아지'], RABBIT: ['🐰', '토끼'] };
const STAGES = ['알', '유아', '성장', '완전체'];
const REWARDS = { INTAKE: '복용 체크', DAY_COMPLETE: '하루 완료', STREAK: '연속 복용 달성', AD: '광고', CANCEL: '체크 취소' };

// 공통 --------------------------------------------------------------

/** 요소를 만든다. 문자열은 항상 텍스트로 넣으므로 사용자 입력이 HTML로 해석되지 않는다. */
function h(tag, props = {}, ...children) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(props)) {
    if (value === undefined || value === null || value === false) continue;
    if (key.startsWith('on')) el.addEventListener(key.slice(2), value);
    else if (key === 'class') el.className = value;
    else el.setAttribute(key, value === true ? '' : value);
  }
  el.append(...children.flat().filter(c => c !== null && c !== undefined && c !== false));
  return el;
}

async function request(method, path, body, whole) {
  let res;
  try {
    res = await fetch(API + path, {
      method,
      headers: body ? { 'Content-Type': 'application/json' } : {},
      body: body ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new Error('서버에 연결하지 못했어요. 앱이 켜져 있는지 확인해 주세요.');
  }
  if (res.status === 204) return null;
  const json = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(json.error?.message ?? '요청을 처리하지 못했어요.');
  return whole ? json : json.data;
}

let toastTimer;
function toast(message) {
  toastBox.textContent = message;
  toastBox.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { toastBox.hidden = true; }, 3500);
}

/** 처리하는 동안 버튼을 잠가 두 번 눌리지 않게 한다. */
function button(label, onclick, props = {}) {
  const el = h('button', { type: 'button', ...props }, label);
  el.addEventListener('click', async () => {
    el.disabled = true;
    try {
      await onclick();
    } catch (e) {
      toast(e.message);
    } finally {
      el.disabled = false;
    }
  });
  return el;
}

const num = value => Number(value).toLocaleString('ko-KR', { maximumFractionDigits: 2 });

function shiftDate(iso, days) {
  const d = new Date(iso + 'T00:00:00Z');
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
}

function dateLabel(iso) {
  return new Intl.DateTimeFormat('ko-KR', { month: 'long', day: 'numeric', weekday: 'short', timeZone: 'UTC' })
    .format(new Date(iso + 'T00:00:00Z'));
}

function pet(character, extra = '') {
  const icon = character.stage === 0 ? '🥚' : SPECIES[character.speciesCode]?.[0] ?? '🐾';
  return h('div', { class: `pet s${character.stage} ${extra}`, 'aria-hidden': 'true' }, icon);
}

function bar(value, max, cls = '') {
  const el = h('i');
  el.style.width = (max > 0 ? Math.min(100, value / max * 100) : 100) + '%';
  return h('div', { class: 'bar ' + cls, role: 'progressbar', 'aria-valuenow': value, 'aria-valuemax': max }, el);
}

// 홈 ----------------------------------------------------------------

let homeDate = null; // null이면 서버 기준 오늘
let cheer = false;

async function homeScreen() {
  const [active, today] = await Promise.all([request('GET', '/characters/active'), request('GET', '/intakes')]);
  if (!active.character) return characterPicker(active);
  const day = homeDate && homeDate !== today.date ? await request('GET', '/intakes?date=' + homeDate) : today;
  const offset = [-1, 0, 1].find(n => shiftDate(today.date, n) === day.date);
  if (offset === undefined) {
    homeDate = null;
    return homeScreen();
  }
  const c = active.character;
  const jump = cheer ? 'jump' : '';
  cheer = false;

  const go = n => { homeDate = shiftDate(day.date, n); return render(); };
  return [
    h('div', { class: 'row between' },
      h('div', {}, h('p', { class: 'muted' }, ['어제', '오늘', '내일'][offset + 1]), h('h1', {}, dateLabel(day.date))),
      h('div', { class: 'row' },
        offset > -1 && button('‹', () => go(-1), { 'aria-label': '이전 날짜' }),
        offset < 1 && button('›', () => go(1), { 'aria-label': '다음 날짜' }))),
    h('section', { class: 'hero', 'aria-label': '캐릭터' },
      h('div', { class: 'row' },
        pet(c, jump),
        h('div', { class: 'grow' },
          h('div', { class: 'row between' }, h('h3', {}, c.name),
            h('span', { class: 'streak' }, `🔥 연속 ${active.streak.current}일`)),
          bar(c.exp, c.nextLevelExp),
          h('p', { class: 'muted' }, `레벨 ${c.level} · ${c.exp} / ${c.nextLevelExp}`)))),
    h('div', { class: 'row between' }, h('h2', {}, '복용 체크'),
      h('p', { class: 'muted' }, `${day.progress.taken} / ${day.progress.scheduled}`)),
    day.slots.length
      ? [bar(day.progress.taken, day.progress.scheduled, 'light'), ...day.slots.map(s => slotCard(s, offset))]
      : h('div', { class: 'card center stack' },
        h('p', {}, '이날은 챙길 영양제가 없어요.'),
        h('a', { href: '#/add' }, '영양제 등록하기')),
  ];
}

function slotCard(slot, offset) {
  const pending = slot.items.filter(i => i.status === 'PENDING');
  return h('section', { class: 'card slot' },
    h('div', { class: 'row between' }, h('time', {}, slot.time),
      offset < 1 && pending.length > 1
        && button('모두 먹었어요', () => take(pending.map(i => i.intakeId)), { class: 'primary small' })),
    slot.items.map(i => intakeItem(i, offset)));
}

function intakeItem(item, offset) {
  const taken = item.status === 'TAKEN' || item.status === 'LATE';
  const pending = item.status === 'PENDING';
  const label = {
    PENDING: offset > 0 ? '예정' : '',
    TAKEN: '제시간에 먹었어요',
    LATE: '먹었어요 (시간 벗어남)',
    SKIPPED: '건너뛰었어요',
    MISSED: '놓쳤어요',
  }[item.status];
  const detail = [`${num(item.dose)}개`, MEALS[item.mealTiming], label].filter(Boolean).join(' · ');
  const cancel = () => cancelIntake(item);
  return h('div', { class: 'item ' + (pending ? '' : 'done') },
    button(taken ? '✓' : item.status === 'SKIPPED' ? '–' : '', taken ? cancel : () => take([item.intakeId]), {
      class: 'check ' + (taken ? 'on' : pending && offset < 1 ? '' : 'off'),
      'aria-label': `${item.name} ${taken ? '체크 취소' : '먹었어요'}`,
      'aria-pressed': taken ? 'true' : 'false',
      disabled: offset > 0 || (taken ? offset !== 0 : !pending),
    }),
    h('div', { class: 'grow' }, h('p', { class: 'name' }, h('strong', {}, item.name)), h('p', { class: 'muted' }, detail)),
    pending && offset < 1 && button('건너뛰기', async () => {
      await request('POST', `/intakes/${item.intakeId}/skip`);
      await render();
    }, { class: 'plain small' }),
    offset === 0 && item.status === 'SKIPPED' && button('되돌리기', cancel, { class: 'plain small' }));
}

async function take(intakeIds) {
  const r = await request('POST', '/intakes/take', { intakeIds });
  const exp = r.results.reduce((sum, x) => sum + x.exp, 0) + r.bonuses.reduce((sum, x) => sum + x.exp, 0);
  const lines = [exp > 0 ? `경험치 +${exp}` : '체크했어요'];
  if (r.bonuses.some(b => b.type === 'STREAK')) lines.push(`🔥 ${r.streak.current}일 연속 달성!`);
  else if (r.dayCompleted) lines.push('오늘 복용 완료!');
  if (r.character?.completed) lines.push('완전체가 됐어요!');
  else if (r.character?.stageChanged) lines.push(`${STAGES[r.character.stage]} 단계로 자랐어요!`);
  else if (r.character?.leveledUp) lines.push(`레벨 ${r.character.level}!`);
  cheer = exp > 0;
  await render();
  toast(lines.join(' · '));
}

async function cancelIntake(item) {
  const r = await request('POST', `/intakes/${item.intakeId}/cancel`);
  await render();
  toast(r.reclaimedExp > 0 ? `체크를 취소했어요 · 경험치 -${r.reclaimedExp}` : '되돌렸어요');
}

// 캐릭터 ------------------------------------------------------------

function characterPicker(active) {
  let species = 'CAT';
  const picks = Object.entries(SPECIES).map(([code, [icon, name]]) =>
    h('button', {
      type: 'button', class: 'pick', 'aria-pressed': String(code === species),
      onclick: e => {
        species = code;
        picks.forEach(p => p.setAttribute('aria-pressed', String(p === e.currentTarget)));
      },
    }, icon, h('span', {}, name)));
  const name = h('input', { maxlength: 30, required: true, placeholder: '예: 콩이', autocomplete: 'off' });
  return [
    h('h1', {}, '함께 자랄 친구를 골라 주세요'),
    h('p', { class: 'muted' }, '영양제를 챙겨 먹을 때마다 자라요. 알에서 시작해요.'),
    active.streak.best > 0 && h('p', { class: 'muted' }, `연속 ${active.streak.current}일은 그대로 이어져요.`),
    h('div', { class: 'card stack', style: 'margin-top:16px' },
      h('div', { class: 'chips' }, picks),
      h('label', {}, '이름', name),
      button('시작하기', async () => {
        if (!name.value.trim()) {
          name.focus();
          throw new Error('이름을 지어 주세요.');
        }
        await request('POST', '/characters', { speciesCode: species, name: name.value });
        await render();
      }, { class: 'primary wide' })),
  ];
}

async function characterScreen() {
  const active = await request('GET', '/characters/active');
  if (!active.character) return characterPicker(active);
  const c = active.character;
  const list = h('div', { class: 'card' });
  const more = h('div', { class: 'center' });
  const load = async cursor => {
    const r = await request('GET', '/rewards?size=20' + (cursor ? '&cursor=' + encodeURIComponent(cursor) : ''), null, true);
    list.append(...r.data.map(x => h('div', { class: 'item between' },
      h('div', {}, h('strong', {}, REWARDS[x.type] ?? x.type),
        h('p', { class: 'muted' }, x.createdAt.slice(0, 16).replace('T', ' '))),
      h('strong', {}, (x.exp > 0 ? '+' : '') + x.exp))));
    if (!list.children.length) list.append(h('p', { class: 'muted' }, '아직 받은 보상이 없어요.'));
    more.replaceChildren(r.page.hasNext ? button('더 보기', () => load(r.page.nextCursor)) : '');
  };
  await load();
  return [
    h('h1', {}, '캐릭터'),
    h('section', { class: 'hero' },
      h('div', { class: 'row' }, pet(c),
        h('div', { class: 'grow' },
          h('h3', {}, c.name),
          h('p', { class: 'muted' }, `${STAGES[c.stage]} 단계 · 레벨 ${c.level}`),
          bar(c.exp, c.nextLevelExp),
          h('p', { class: 'muted' }, `다음 레벨까지 ${Math.max(0, c.nextLevelExp - c.exp)}`)))),
    h('div', { class: 'card row between', style: 'margin-top:12px' },
      h('p', {}, `🔥 연속 ${active.streak.current}일`), h('p', { class: 'muted' }, `최고 연속 ${active.streak.best}일`)),
    h('h2', {}, '보상 내역'), list, more,
  ];
}

// 영양제 ------------------------------------------------------------

async function supplementsScreen() {
  const list = await request('GET', '/supplements');
  return [
    h('div', { class: 'row between' }, h('h1', {}, '내 영양제'),
      button('등록', () => { location.hash = '#/add'; }, { class: 'primary' })),
    h('div', { class: 'stack', style: 'margin-top:16px' },
      list.length ? list.map(supplementCard)
        : h('div', { class: 'card center' }, h('p', {}, '아직 등록한 영양제가 없어요.'))),
  ];
}

function supplementCard(s) {
  const setStatus = status => async () => {
    await request('PATCH', `/supplements/${s.id}/status`, { status });
    await render();
  };
  const days = s.daysOfWeek.length === 7 ? '매일' : DAYS.filter(d => s.daysOfWeek.includes(d[0])).map(d => d[1]).join('·');
  return h('section', { class: 'card' },
    h('div', { class: 'row between' }, h('h3', {}, s.alias ?? s.name), h('span', { class: 'muted' }, STATUS[s.status])),
    s.alias && h('p', { class: 'muted' }, s.name),
    h('p', {}, `${days} ${s.times.join(', ')}`),
    h('p', { class: 'muted' }, `1회 ${num(s.dosePerIntake)}${s.servingUnit} · ${MEALS[s.mealTiming]}`
      + (s.remainingQty === null ? '' : ` · 남은 수량 ${num(s.remainingQty)}${s.servingUnit}`)),
    h('div', { class: 'chips', style: 'margin-top:10px' },
      button('수정', () => { location.hash = '#/edit/' + s.id; }, { class: 'small' }),
      s.status === 'ACTIVE' ? button('일시중지', setStatus('PAUSED'), { class: 'small' })
        : button('다시 복용', setStatus('ACTIVE'), { class: 'small' }),
      s.status !== 'ENDED' && button('복용 종료', setStatus('ENDED'), { class: 'small' }),
      button('삭제', async () => {
        if (!confirm(`'${s.alias ?? s.name}'을(를) 삭제할까요?\n복용 기록도 함께 지워지고 되돌릴 수 없어요.`)) return;
        await request('DELETE', '/supplements/' + s.id);
        await render();
      }, { class: 'small danger' })));
}

/** id가 있으면 수정, 없으면 등록. */
async function formScreen(id) {
  const [s, nutrients] = await Promise.all([
    id ? request('GET', '/supplements/' + id) : null,
    id ? [] : request('GET', '/nutrients'),
  ]);
  const days = new Set(s ? s.daysOfWeek : DAYS.map(d => d[0]));
  const field = (props, value) => {
    const el = h('input', { autocomplete: 'off', ...props });
    el.value = value ?? '';
    return el;
  };
  const decimal = { type: 'number', step: '0.01', inputmode: 'decimal' };
  const product = id ? null : productFields(nutrients);
  const alias = field({ maxlength: 50, placeholder: '예: 아침 비타민' }, s?.alias);
  const dose = field({ ...decimal, min: '0.01', max: '9999.99', required: true }, s?.dosePerIntake ?? 1);
  const total = field({ ...decimal, min: '0.01', max: '999999.99', placeholder: '선택' }, s?.totalQty);
  const remaining = field({ ...decimal, min: '0', max: '999999.99', placeholder: '선택' }, s?.remainingQty);
  const meal = h('select', {}, Object.entries(MEALS).map(([v, t]) => h('option', { value: v }, t)));
  meal.value = s?.mealTiming ?? 'ANY';

  const timeList = h('div', { class: 'stack' });
  const addTime = value => {
    const row = h('div', { class: 'row' },
      field({ type: 'time', required: true, 'aria-label': '복용 시각' }, value),
      button('✕', () => {
        if (timeList.children.length === 1) throw new Error('복용 시각은 하나 이상 필요해요.');
        row.remove();
      }, { 'aria-label': '시각 삭제' }));
    timeList.append(row);
  };
  (s?.times.length ? s.times : ['08:30']).forEach(addTime);

  const error = h('p', { class: 'error', role: 'alert', hidden: true });
  const submit = h('button', { class: 'primary wide' }, id ? '저장' : '등록하기');
  const form = h('form', {
    class: 'card stack', style: 'margin-top:16px',
    onsubmit: async e => {
      e.preventDefault();
      error.hidden = true;
      submit.disabled = true;
      try {
        const times = [...new Set([...timeList.querySelectorAll('input')].map(i => i.value.slice(0, 5)))].sort();
        if (!days.size) throw new Error('복용 요일을 하나 이상 골라 주세요.');
        const body = { dosePerIntake: Number(dose.value), mealTiming: meal.value, alias: alias.value };
        if (total.value) body.totalQty = Number(total.value);
        if (id) {
          // 일정은 바뀐 경우에만 보낸다. 남은 수량도 고치지 않았으면 그대로 둔다.
          if (times.join() !== s.times.join()) body.times = times;
          if (days.size !== s.daysOfWeek.length || s.daysOfWeek.some(d => !days.has(d))) body.daysOfWeek = [...days];
          if (remaining.value && Number(remaining.value) !== s.remainingQty) body.remainingQty = Number(remaining.value);
          await request('PATCH', '/supplements/' + id, body);
          toast('저장했어요');
        } else {
          const r = await request('POST', '/supplements', { ...body, ...product.value(), times, daysOfWeek: [...days] });
          const names = nutrients.filter(n => r.duplicateNutrients.includes(n.code)).map(n => n.name);
          toast(names.length ? `등록했어요 · 이미 드시는 영양제와 겹치는 성분: ${names.join(', ')}` : '등록했어요');
        }
        location.hash = '#/supplements';
      } catch (err) {
        error.textContent = err.message;
        error.hidden = false;
      } finally {
        submit.disabled = false;
      }
    },
  },
  product ? product.element : h('h3', {}, s.name),
  h('label', {}, '별칭 (선택)', alias),
  h('label', {}, '1회 복용량', dose),
  h('label', {}, '식사', meal),
  h('fieldset', {}, h('legend', {}, '복용 요일'), h('div', { class: 'chips' }, DAYS.map(([code, name]) =>
    h('button', {
      type: 'button', 'aria-pressed': String(days.has(code)),
      onclick: e => {
        if (!days.delete(code)) days.add(code);
        e.currentTarget.setAttribute('aria-pressed', String(days.has(code)));
      },
    }, name)))),
  h('fieldset', {}, h('legend', {}, '복용 시각'), timeList,
    button('+ 시각 추가', () => {
      if (timeList.children.length >= 24) throw new Error('시각은 24개까지 넣을 수 있어요.');
      addTime('20:00');
    }, { class: 'plain' })),
  h('label', {}, '총 수량 (선택)', total),
  id && h('label', {}, '남은 수량 (선택)', remaining),
  error, submit);
  return [
    h('a', { href: '#/supplements', class: 'muted' }, '‹ 내 영양제'),
    h('h1', {}, id ? '영양제 수정' : '영양제 등록'), form,
  ];
}

/** 제품 검색 결과에서 고르거나 이름·성분을 직접 입력한다. */
function productFields(nutrients) {
  let picked = null;
  const name = h('input', { required: true, maxlength: 200, placeholder: '예: 비타민D 2000IU', autocomplete: 'off' });
  const maker = h('input', { maxlength: 100, autocomplete: 'off' });
  const found = h('div', { class: 'stack' });
  const rows = h('div', { class: 'stack' });
  const manual = h('div', { class: 'stack' },
    h('label', {}, '제조사 (선택)', maker),
    h('fieldset', {}, h('legend', {}, '성분 (선택, 1회 섭취량당)'), rows,
      button('+ 성분 추가', () => {
        const unit = h('span', {}, nutrients[0]?.unit ?? '');
        const select = h('select', {
          'aria-label': '성분',
          onchange: () => { unit.textContent = nutrients.find(n => n.code === select.value).unit; },
        }, nutrients.map(n => h('option', { value: n.code }, n.name)));
        const row = h('div', { class: 'row' }, select,
          h('input', { type: 'number', step: '0.0001', min: '0.0001', max: '99999999', required: true, 'aria-label': '함량', inputmode: 'decimal' }),
          unit, button('✕', () => row.remove(), { 'aria-label': '성분 삭제' }));
        rows.append(row);
      }, { class: 'plain' })));

  const reset = () => {
    picked = null;
    name.readOnly = false;
    manual.hidden = false;
    // 숨긴 성분 입력이 폼 검증에 걸리지 않도록 다시 켠다.
    rows.querySelectorAll('input').forEach(i => { i.disabled = false; });
    found.replaceChildren();
  };
  const choose = p => {
    picked = p;
    name.value = p.name;
    name.readOnly = true;
    manual.hidden = true;
    rows.querySelectorAll('input').forEach(i => { i.disabled = true; });
    found.replaceChildren(h('div', { class: 'row between' }, h('p', { class: 'muted' }, '등록된 제품을 골랐어요.'),
      button('직접 입력', reset, { class: 'small' })));
  };
  let timer;
  let seq = 0;
  name.addEventListener('input', () => {
    clearTimeout(timer);
    const mine = ++seq;
    const q = name.value.trim();
    found.replaceChildren();
    if (!q) return;
    timer = setTimeout(async () => {
      try {
        const r = await request('GET', '/products?size=5&q=' + encodeURIComponent(q.slice(0, 100)), null, true);
        // 그사이 입력이 바뀌었거나 이미 골랐으면 늦게 온 결과는 버린다.
        if (mine !== seq || picked) return;
        found.replaceChildren(...r.data.map(p =>
          h('button', { type: 'button', onclick: () => choose(p) }, p.manufacturer ? `${p.name} (${p.manufacturer})` : p.name)));
      } catch {
        // 검색이 안 돼도 직접 입력은 할 수 있다.
      }
    }, 300);
  });

  return {
    element: h('div', { class: 'stack' }, h('label', {}, '제품명', name), found, manual),
    value() {
      if (picked) return { productId: picked.id };
      return {
        newProduct: {
          name: name.value,
          manufacturer: maker.value.trim() || null,
          nutrients: [...rows.children].map(r => ({
            code: r.querySelector('select').value,
            amount: Number(r.querySelector('input').value),
          })),
        },
      };
    },
  };
}

// 화면 전환 ----------------------------------------------------------

let renderSeq = 0;
async function render() {
  const mine = ++renderSeq;
  const [, page = '', id] = location.hash.split('/');
  const screens = {
    '': homeScreen,
    supplements: supplementsScreen,
    add: () => formScreen(),
    edit: () => formScreen(id),
    character: characterScreen,
  };
  const tab = { add: 'supplements', edit: 'supplements' }[page] ?? page;
  nav.querySelectorAll('a').forEach(a => {
    if (a.dataset.tab === tab) a.setAttribute('aria-current', 'page');
    else a.removeAttribute('aria-current');
  });
  let content;
  try {
    content = await (screens[page] ?? homeScreen)();
  } catch (e) {
    content = [h('div', { class: 'card center stack' }, h('p', {}, e.message), button('다시 시도', render))];
  }
  if (mine !== renderSeq) return; // 그사이 다른 화면으로 이동했다.
  view.replaceChildren(...content.flat(Infinity).filter(Boolean));
}

window.addEventListener('hashchange', () => {
  homeDate = null;
  window.scrollTo(0, 0);
  render();
});
// 앱으로 돌아왔을 때 홈을 최신으로 맞춘다. 입력 중인 폼은 건드리지 않는다.
document.addEventListener('visibilitychange', () => {
  if (!document.hidden && ['', '#/'].includes(location.hash) && !view.querySelector('input')) render();
});
render();
