// VAPID Public Key — nach 'ant generate-vapid-keys' hier eintragen
const VAPID_PUBLIC_KEY = 'BM_6rWcY-Ol77wUT9nvz1YQKL18v_rfTIaKzFLOrdYGyqrfVKtOK_5GH-j4F6SBs7sXz35NYwHgNTFQBrcaaDVw';

// i18n translations
const i18n = {
    de: {
        title: 'Push-Benachrichtigungen',
        subtitle: 'Tischtennis-Turnier',
        playerLabel: 'Spieler:',
        btnActivate: 'Push-Benachrichtigungen aktivieren',
        btnRegistered: 'Registriert ✓',
        btnUnsubscribe: 'Abmelden',
        messagesTitle: 'Nachrichten',
        clearText: 'Nachrichten löschen',
        unsupported: 'Push-Benachrichtigungen werden von diesem Browser nicht unterstützt.',
        noPlayer: 'Keine Spielernummer angegeben. URL-Format: ?player=2001',
        permDenied: 'Push-Berechtigung wurde verweigert.',
        registered: 'Push-Benachrichtigungen aktiviert!',
        unsubscribed: 'Abmeldung erfolgreich.',
        error: 'Fehler bei der Registrierung: '
    },
    en: {
        title: 'Push Notifications',
        subtitle: 'Table Tennis Tournament',
        playerLabel: 'Player:',
        btnActivate: 'Enable Push Notifications',
        btnRegistered: 'Registered ✓',
        btnUnsubscribe: 'Unsubscribe',
        messagesTitle: 'Messages',
        clearText: 'Clear Messages',
        unsupported: 'Push notifications are not supported in this browser.',
        noPlayer: 'No player number specified. URL format: ?player=2001',
        permDenied: 'Push permission was denied.',
        registered: 'Push notifications enabled!',
        unsubscribed: 'Successfully unsubscribed.',
        error: 'Registration error: '
    },
    es: {
        title: 'Notificaciones Push',
        subtitle: 'Torneo de Tenis de Mesa',
        playerLabel: 'Jugador:',
        btnActivate: 'Activar Notificaciones Push',
        btnRegistered: 'Registrado ✓',
        btnUnsubscribe: 'Cancelar suscripción',
        messagesTitle: 'Mensajes',
        clearText: 'Borrar Mensajes',
        unsupported: 'Las notificaciones push no son compatibles con este navegador.',
        noPlayer: 'No se especificó número de jugador. Formato URL: ?player=2001',
        permDenied: 'El permiso de push fue denegado.',
        registered: '¡Notificaciones push activadas!',
        unsubscribed: 'Suscripción cancelada.',
        error: 'Error de registro: '
    },
    fr: {
        title: 'Notifications Push',
        subtitle: 'Tournoi de Tennis de Table',
        playerLabel: 'Joueur:',
        btnActivate: 'Activer les Notifications Push',
        btnRegistered: 'Enregistré ✓',
        btnUnsubscribe: 'Se désabonner',
        messagesTitle: 'Messages',
        clearText: 'Effacer les Messages',
        unsupported: 'Les notifications push ne sont pas prises en charge par ce navigateur.',
        noPlayer: 'Aucun numéro de joueur spécifié. Format URL: ?player=2001',
        permDenied: 'L\'autorisation push a été refusée.',
        registered: 'Notifications push activées!',
        unsubscribed: 'Désabonnement réussi.',
        error: 'Erreur d\'enregistrement: '
    },
    ja: {
        title: 'プッシュ通知',
        subtitle: '卓球トーナメント',
        playerLabel: '選手:',
        btnActivate: 'プッシュ通知を有効にする',
        btnRegistered: '登録済み ✓',
        btnUnsubscribe: '登録解除',
        messagesTitle: 'メッセージ',
        clearText: 'メッセージを削除',
        unsupported: 'このブラウザではプッシュ通知がサポートされていません。',
        noPlayer: '選手番号が指定されていません。URL形式: ?player=2001',
        permDenied: 'プッシュ通知の許可が拒否されました。',
        registered: 'プッシュ通知が有効になりました！',
        unsubscribed: '登録が解除されました。',
        error: '登録エラー: '
    }
};

// Detect language
const userLang = navigator.language.substring(0, 2);
const t = i18n[userLang] || i18n.en;

// Apply translations
document.getElementById('title').textContent = t.title;
document.getElementById('subtitle').textContent = t.subtitle;
document.getElementById('player-label').textContent = t.playerLabel;
document.getElementById('btn-text').textContent = t.btnActivate;
document.getElementById('messages-title').textContent = t.messagesTitle;
document.getElementById('clear-text').textContent = t.clearText;
document.getElementById('unsupported-text').textContent = t.unsupported;
document.getElementById('unsubscribe-text').textContent = t.btnUnsubscribe;

// Get player ID from URL
const params = new URLSearchParams(window.location.search);
const playerId = params.get('player');

const statusEl = document.getElementById('status');
const btn = document.getElementById('subscribe-btn');

if (playerId) {
    document.getElementById('player-info').style.display = 'block';
    document.getElementById('player-id').textContent = playerId;
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
    loadMessages();
}

// Listen for messages from service worker (foreground)
if ('serviceWorker' in navigator) {
    navigator.serviceWorker.addEventListener('message', function(event) {
        if (event.data && event.data.type === 'push-message') {
            const messages = JSON.parse(localStorage.getItem('push_messages_' + playerId) || '[]');
            messages.unshift({
                text: event.data.message,
                time: new Date().toLocaleTimeString()
            });
            // Keep last 50 messages
            if (messages.length > 50) messages.length = 50;
            localStorage.setItem('push_messages_' + playerId, JSON.stringify(messages));
            loadMessages();
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
        }
        // Clear local message history for this player
        localStorage.removeItem('push_messages_' + playerId);
        loadMessages();

        // Reset UI to allow re-registration
        btn.textContent = t.btnActivate;
        btn.classList.remove('registered');
        btn.disabled = false;
        statusEl.textContent = t.unsubscribed;
        statusEl.className = 'status success';
        document.getElementById('unsubscribe-btn').style.display = 'none';
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

    if (!playerId) {
        statusEl.textContent = t.noPlayer;
        statusEl.className = 'status error';
        return;
    }

    loadMessages();

    try {
        const registration = await navigator.serviceWorker.register('/push/sw.js', { scope: '/push/' });
        await navigator.serviceWorker.ready;

        // Check existing browser subscription
        const existing = await registration.pushManager.getSubscription();
        if (existing) {
            // Browser has a subscription — verify with server for THIS player
            const serverKnows = await checkServerStatus(existing.endpoint);
            if (serverKnows) {
                showRegistered();
            } else {
                // Server doesn't know about this player — re-register silently
                await registerWithServer(existing);
                showRegistered();
            }
            return;
        }

        // No browser subscription — show subscribe button
        btn.disabled = false;
        btn.addEventListener('click', async function() {
            btn.disabled = true;

            const permission = await Notification.requestPermission();
            if (permission !== 'granted') {
                statusEl.textContent = t.permDenied;
                statusEl.className = 'status error';
                btn.disabled = false;
                return;
            }

            try {
                const subscription = await registration.pushManager.subscribe({
                    userVisibleOnly: true,
                    applicationServerKey: urlBase64ToUint8Array(VAPID_PUBLIC_KEY)
                });

                await registerWithServer(subscription);
                showRegistered();
            } catch (err) {
                statusEl.textContent = t.error + err.message;
                statusEl.className = 'status error';
                btn.disabled = false;
            }
        });
    } catch (err) {
        statusEl.textContent = t.error + err.message;
        statusEl.className = 'status error';
    }
}

init();
