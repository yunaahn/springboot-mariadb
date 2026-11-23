const API_URL = '/api/memos';
const form = document.querySelector('#memo-form');
const input = document.querySelector('#content');
const list = document.querySelector('#memo-list');
const message = document.querySelector('#message');

async function request(options) {
    const response = await fetch(API_URL, options);
    if (!response.ok) throw new Error(`요청 실패 (${response.status})`);
    return response.json();
}

async function loadMemos() {
    const memos = await request();
    list.replaceChildren(...memos.map(({ content }) => {
        const item = document.createElement('li');
        item.textContent = content;
        return item;
    }));
}

async function run(action) {
    message.textContent = '';
    try {
        await action();
    } catch (error) {
        message.textContent = error.message;
    }
}

form.addEventListener('submit', (event) => {
    event.preventDefault();
    run(async () => {
        await request({
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ content: input.value }),
        });
        form.reset();
        await loadMemos();
    });
});

run(loadMemos);
