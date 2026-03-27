// Service Worker for TTM Push Notifications
// Native Web Push API — no Firebase

// --- IndexedDB helpers for offline message storage ---

function openMessagesDB() {
    return new Promise(function(resolve, reject) {
        var request = indexedDB.open('ttm-push-messages', 1);
        request.onupgradeneeded = function(event) {
            var db = event.target.result;
            if (!db.objectStoreNames.contains('messages')) {
                var store = db.createObjectStore('messages', { keyPath: 'id', autoIncrement: true });
                store.createIndex('playerId', 'playerId', { unique: false });
            }
        };
        request.onsuccess = function(event) { resolve(event.target.result); };
        request.onerror = function(event) { reject(event.target.error); };
    });
}

function storeMessage(playerId, message) {
    return openMessagesDB().then(function(db) {
        return new Promise(function(resolve, reject) {
            var tx = db.transaction('messages', 'readwrite');
            tx.objectStore('messages').add({
                playerId: playerId,
                text: message,
                time: new Date().toISOString()
            });
            tx.oncomplete = function() { resolve(); };
            tx.onerror = function(event) { reject(event.target.error); };
        });
    });
}

function getAndDeleteMessages(playerId) {
    return openMessagesDB().then(function(db) {
        return new Promise(function(resolve, reject) {
            var tx = db.transaction('messages', 'readwrite');
            var store = tx.objectStore('messages');
            var index = store.index('playerId');
            var results = [];
            var request = index.openCursor(IDBKeyRange.only(playerId));
            request.onsuccess = function(event) {
                var cursor = event.target.result;
                if (cursor) {
                    results.push({ text: cursor.value.text, time: cursor.value.time });
                    cursor.delete();
                    cursor.continue();
                }
            };
            tx.oncomplete = function() { resolve(results); };
            tx.onerror = function(event) { reject(event.target.error); };
        });
    });
}

function clearMessagesForPlayer(playerId) {
    return openMessagesDB().then(function(db) {
        return new Promise(function(resolve, reject) {
            var tx = db.transaction('messages', 'readwrite');
            var store = tx.objectStore('messages');
            var index = store.index('playerId');
            var request = index.openCursor(IDBKeyRange.only(playerId));
            request.onsuccess = function(event) {
                var cursor = event.target.result;
                if (cursor) {
                    cursor.delete();
                    cursor.continue();
                }
            };
            tx.oncomplete = function() { resolve(); };
            tx.onerror = function(event) { reject(event.target.error); };
        });
    });
}

// --- Push event handler ---

self.addEventListener('push', function(event) {
    let data = { title: 'TTM', body: 'Neue Nachricht', playerId: '' };

    if (event.data) {
        try {
            const json = event.data.json();
            data.playerId = json.playerId || '';
            data.body = json.body || json.message || event.data.text();
            data.title = data.playerId ? ('TTM - ' + data.playerId) : (json.title || 'TTM');
        } catch (e) {
            data.body = event.data.text();
        }
    }

    const options = {
        body: data.body,
        icon: '/push/icon-192.png',
        badge: '/push/icon-192.png',
        vibrate: [200, 100, 200],
        tag: 'ttm-push-' + Date.now(),
        data: {
            url: data.playerId
                ? (self.registration.scope + '?player=' + data.playerId)
                : self.registration.scope,
            playerId: data.playerId
        }
    };

    event.waitUntil(
        self.clients.matchAll({ type: 'window', includeUncontrolled: true })
            .then(function(clients) {
                var promises = [self.registration.showNotification(data.title, options)];

                if (clients.length > 0) {
                    // Tabs are open — forward via postMessage (tabs handle localStorage storage)
                    clients.forEach(function(client) {
                        client.postMessage({
                            type: 'push-message',
                            title: data.title,
                            message: data.body,
                            playerId: data.playerId
                        });
                    });
                } else {
                    // No tabs open — store in IndexedDB for later retrieval
                    if (data.playerId) {
                        promises.push(storeMessage(data.playerId, data.body));
                    }
                }

                return Promise.all(promises);
            })
    );
});

// --- Message handler: tabs can request missed messages ---

self.addEventListener('message', function(event) {
    if (!event.data) return;

    if (event.data.type === 'get-messages' && event.data.playerId) {
        event.waitUntil(
            getAndDeleteMessages(event.data.playerId).then(function(messages) {
                event.source.postMessage({
                    type: 'missed-messages',
                    playerId: event.data.playerId,
                    messages: messages
                });
            })
        );
    }

    if (event.data.type === 'clear-messages' && event.data.playerId) {
        event.waitUntil(clearMessagesForPlayer(event.data.playerId));
    }
});

// --- Notification click handler ---

self.addEventListener('notificationclick', function(event) {
    event.notification.close();

    var targetUrl = event.notification.data.url || '/push/';
    var targetPlayerId = event.notification.data.playerId || '';

    event.waitUntil(
        self.clients.matchAll({ type: 'window' }).then(function(clients) {
            // Prefer window with matching player
            if (targetPlayerId) {
                for (var i = 0; i < clients.length; i++) {
                    if (clients[i].url.includes('player=' + targetPlayerId) && 'focus' in clients[i]) {
                        return clients[i].focus();
                    }
                }
            }
            // Fallback: any window with /push/
            for (var i = 0; i < clients.length; i++) {
                if (clients[i].url.includes('/push/') && 'focus' in clients[i]) {
                    return clients[i].focus();
                }
            }
            // Otherwise open new window
            if (self.clients.openWindow) {
                return self.clients.openWindow(targetUrl);
            }
        })
    );
});

self.addEventListener('install', function(event) {
    self.skipWaiting();
});

self.addEventListener('activate', function(event) {
    event.waitUntil(self.clients.claim());
});
