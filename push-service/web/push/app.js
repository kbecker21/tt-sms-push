// VAPID Public Key — nach 'ant generate-vapid-keys' hier eintragen
const VAPID_PUBLIC_KEY = 'BMc9QDpeA0mXWd46pcWDl4FK7nfqhasWhP9zvjcFQZ5wRWeAKLyYwlS62RxUvo_VbQZSQoWhui4UAlPkpIrhteA';

// i18n — loaded from external file
let t = {};

// Get player ID from URL
const params = new URLSearchParams(window.location.search);
const playerId = params.get('player');

const statusEl = document.getElementById('status');
const btn = document.getElementById('subscribe-btn');

// Track the current browser subscription for re-use after unsubscribe
let currentSubscription = null;

// Load i18n translations from external file
async function loadI18n() {
    try {
        const response = await fetch('/push/i18n.json');
        const i18n = await response.json();
        const userLang = navigator.language.substring(0, 2);
        t = i18n[userLang] || i18n.en;
    } catch (e) {
        // Fallback: German defaults if i18n.json fails to load
        t = {
            title: 'Push-Benachrichtigungen', subtitle: 'Tischtennis-Turnier',
            playerLabel: 'Spieler:', btnActivate: 'Push-Benachrichtigungen aktivieren',
            btnRegistered: 'Registriert ✓', btnUnsubscribe: 'Abmelden',
            messagesTitle: 'Nachrichten', clearText: 'Nachrichten löschen',
            unsupported: 'Push-Benachrichtigungen werden von diesem Browser nicht unterstützt.',
            noPlayer: 'Keine Spielernummer angegeben. URL-Format: ?player=2001',
            permDenied: 'Push-Berechtigung wurde verweigert.',
            registered: 'Push-Benachrichtigungen aktiviert!',
            unsubscribed: 'Abmeldung erfolgreich.', error: 'Fehler bei der Registrierung: ',
            playerInputLabel: 'Spielernummer eingeben:', playerInputPlaceholder: 'z.B. 2001',
            playerSubmitBtn: 'Weiter'
        };
    }
}

// Apply translations to all UI elements
function applyTranslations() {
    document.getElementById('title').textContent = t.title;
    document.getElementById('subtitle').textContent = t.subtitle;
    document.getElementById('player-label').textContent = t.playerLabel;
    document.getElementById('btn-text').textContent = t.btnActivate;
    document.getElementById('messages-title').textContent = t.messagesTitle;
    document.getElementById('clear-text').textContent = t.clearText;
    document.getElementById('unsupported-text').textContent = t.unsupported;
    document.getElementById('unsubscribe-text').textContent = t.btnUnsubscribe;

    var playerInputLabel = document.getElementById('player-input-label');
    if (playerInputLabel) playerInputLabel.textContent = t.playerInputLabel;
    var playerInput = document.getElementById('player-input');
    if (playerInput) playerInput.placeholder = t.playerInputPlaceholder;
    var playerSubmitText = document.getElementById('player-submit-text');
    if (playerSubmitText) playerSubmitText.textContent = t.playerSubmitBtn;

    if (playerId) {
        document.getElementById('player-info').style.display = 'block';
        document.getElementById('player-id').textContent = playerId;
    }
}

// Load and display stored messages
function loadMessages() {
    const messages = JSON.parse(localStorage.getItem('push_messages_' + playerId) || '[]');
    const list = document.getElementById('messages-list');
    const section = document.getElementById('messages-section');

    if (messages.length > 0) {
        section.style.display = 'block';
        list.innerHTML = '';
        messages.forEach(function(msg) {
            const li = document.createElement('li');
            li.innerHTML = '<span class="msg-time">' + msg.time + '</span> ' + msg.text;
            list.appendChild(li);
        });
    } else {
        section.style.display = 'none';
    }
}

function clearMessages() {
    localStorage.removeItem('push_messages_' + playerId);
    // Also clear messages in SW IndexedDB
    if (navigator.serviceWorker && navigator.serviceWorker.controller) {
        navigator.serviceWorker.controller.postMessage({
            type: 'clear-messages',
            playerId: playerId
        });
    }
    loadMessages();
}

// Listen for messages from service worker (foreground + missed messages)
if ('serviceWorker' in navigator) {
    navigator.serviceWorker.addEventListener('message', function(event) {
        if (!event.data) return;

        if (event.data.type === 'push-message') {
            var msgPlayerId = event.data.playerId || '';
            // Only process messages for THIS tab's player — ignore other players
            if (msgPlayerId !== playerId) return;

            var messageId = event.data.messageId || '';
            var messages = JSON.parse(localStorage.getItem('push_messages_' + playerId) || '[]');

            // Deduplicate by messageId (prevents duplicates from retries or multiple tabs)
            if (messageId && messages.some(function(m) { return m.messageId === messageId; })) {
                return;
            }

            messages.unshift({
                text: event.data.message,
                time: new Date().toLocaleTimeString(),
                messageId: messageId,
                playerId: playerId
            });
            // Keep last 50 messages
            if (messages.length > 50) messages.length = 50;
            localStorage.setItem('push_messages_' + playerId, JSON.stringify(messages));
            loadMessages();
        }

        if (event.data.type === 'missed-messages' && event.data.playerId === playerId) {
            // Merge missed messages from IndexedDB into localStorage (messageId dedup)
            if (event.data.messages && event.data.messages.length > 0) {
                var messages = JSON.parse(localStorage.getItem('push_messages_' + playerId) || '[]');
                var existingIds = {};
                messages.forEach(function(m) {
                    if (m.messageId) existingIds[m.messageId] = true;
                });
                event.data.messages.forEach(function(msg) {
                    // Skip if messageId already known
                    if (msg.messageId && existingIds[msg.messageId]) return;
                    var time = new Date(msg.time).toLocaleTimeString();
                    messages.unshift({ text: msg.text, time: time, messageId: msg.messageId || '', playerId: playerId });
                    if (msg.messageId) existingIds[msg.messageId] = true;
                });
                if (messages.length > 50) messages.length = 50;
                localStorage.setItem('push_messages_' + playerId, JSON.stringify(messages));
                loadMessages();
            }
        }
    });
}

// Convert VAPID key from base64url to Uint8Array
function urlBase64ToUint8Array(base64String) {
    const padding = '='.repeat((4 - base64String.length % 4) % 4);
    const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = atob(base64);
    const outputArray = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; i++) {
        outputArray[i] = rawData.charCodeAt(i);
    }
    return outputArray;
}

// Show UI for registered state
function showRegistered() {
    btn.textContent = t.btnRegistered;
    btn.classList.add('registered');
    btn.disabled = true;
    btn.onclick = null;
    statusEl.textContent = t.registered;
    statusEl.className = 'status success';
    document.getElementById('unsubscribe-btn').style.display = '';
}

// Register subscription with server
async function registerWithServer(subscription) {
    const response = await fetch('/api/push/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            playerId: playerId,
            subscription: subscription.toJSON()
        })
    });
    if (!response.ok) {
        throw new Error('Server returned ' + response.status);
    }
}

// Check server-side registration status
async function checkServerStatus(endpoint) {
    const url = '/api/push/status?playerId=' + encodeURIComponent(playerId) +
                '&endpoint=' + encodeURIComponent(endpoint);
    const response = await fetch(url);
    if (!response.ok) {
        return false;
    }
    const data = await response.json();
    return data.registered === true;
}

// Attach click handler for subscribe button (works with existing or new subscription)
function attachSubscribeHandler(existingSubscription) {
    btn.textContent = t.btnActivate;
    btn.classList.remove('registered');
    btn.disabled = false;
    document.getElementById('unsubscribe-btn').style.display = 'none';

    btn.onclick = async function() {
        btn.disabled = true;

        try {
            if (existingSubscription) {
                // Re-use existing browser subscription
                await registerWithServer(existingSubscription);
                showRegistered();
            } else {
                // Create new browser subscription
                const permission = await Notification.requestPermission();
                if (permission !== 'granted') {
                    statusEl.textContent = t.permDenied;
                    statusEl.className = 'status error';
                    btn.disabled = false;
                    return;
                }

                const registration = await navigator.serviceWorker.ready;
                const subscription = await registration.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY)
                });

                currentSubscription = subscription;
                await registerWithServer(subscription);
                showRegistered();
            }
        } catch (err) {
            statusEl.textContent = t.error + err.message;
            statusEl.className = 'status error';
            btn.disabled = false;
        }
    };
}

// Unsubscribe player from push notifications
async function unsubscribe() {
    try {
        const registration = await navigator.serviceWorker.ready;
        const subscription = await registration.pushManager.getSubscription();
        if (subscription) {
            await fetch('/api/push/unregister', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    playerId: playerId,
                    subscription: { endpoint: subscription.endpoint }
                })
            });
            // Keep browser subscription alive for other players — do NOT call subscription.unsubscribe()
            currentSubscription = subscription;
        }
        // Clear local message history for this player only
        localStorage.removeItem('push_messages_' + playerId);
        loadMessages();

        // Reset UI and attach handler for re-registration
        statusEl.textContent = t.unsubscribed;
        statusEl.className = 'status success';
        attachSubscribeHandler(currentSubscription);
    } catch (err) {
        statusEl.textContent = t.error + err.message;
        statusEl.className = 'status error';
    }
}

async function init() {
    if (!('serviceWorker' in navigator) || !('PushManager' in window)) {
        document.getElementById('unsupported').style.display = 'block';
        return;
    }

    // FEATURE 1: No player parameter — show input field
    if (!playerId) {
        var inputSection = document.getElementById('player-input-section');
        if (inputSection) {
            inputSection.style.display = 'block';
            var submitBtn = document.getElementById('player-submit-btn');
            var inputField = document.getElementById('player-input');

            submitBtn.onclick = function() {
                var val = inputField.value.trim();
                if (val) {
                    window.location.href = window.location.pathname + '?player=' + encodeURIComponent(val);
                }
            };
            // Allow Enter key to submit
            inputField.onkeydown = function(e) {
                if (e.key === 'Enter') {
                    submitBtn.onclick();
                }
            };
        } else {
            statusEl.textContent = t.noPlayer;
            statusEl.className = 'status error';
        }
        return;
    }

    loadMessages();

    try {
        const registration = await navigator.serviceWorker.register('/push/sw.js', { scope: '/push/' });
        await navigator.serviceWorker.ready;

        // Request missed messages from SW (stored in IndexedDB while tab was closed)
        if (navigator.serviceWorker.controller) {
            navigator.serviceWorker.controller.postMessage({
                type: 'get-messages',
                playerId: playerId
            });
        }

        // Check existing browser subscription
        const existing = await registration.pushManager.getSubscription();
        if (existing) {
            currentSubscription = existing;
            // Browser has a subscription — verify with server for THIS player
            const serverKnows = await checkServerStatus(existing.endpoint);
            if (serverKnows) {
                // Player is in DB — show as registered immediately
                showRegistered();
            } else {
                // Player NOT in DB — show activate button, require explicit click
                attachSubscribeHandler(existing);
            }
            return;
        }

        // No browser subscription — show subscribe button
        attachSubscribeHandler(null);
    } catch (err) {
        statusEl.textContent = t.error + err.message;
        statusEl.className = 'status error';
    }
}

// Bootstrap: load translations, then apply, then initialize
loadI18n().then(function() {
    applyTranslations();
    init();
});
