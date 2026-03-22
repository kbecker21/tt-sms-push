// Service Worker for TTM Push Notifications
// Native Web Push API — no Firebase

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
        Promise.all([
            self.registration.showNotification(data.title, options),
            // Forward to open clients
            self.clients.matchAll({ type: 'window', includeUncontrolled: true })
                .then(function(clients) {
                    clients.forEach(function(client) {
                        client.postMessage({
                            type: 'push-message',
                            title: data.title,
                            message: data.body,
                            playerId: data.playerId
                        });
                    });
                })
        ])
    );
});

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
