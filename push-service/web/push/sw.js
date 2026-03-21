// Service Worker for TTM Push Notifications
// Native Web Push API — no Firebase

self.addEventListener('push', function(event) {
    let data = { title: 'TTM', body: 'Neue Nachricht' };

    if (event.data) {
        try {
            const json = event.data.json();
            data.title = json.title || 'TTM';
            data.body = json.body || json.message || event.data.text();
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
        data: { url: self.registration.scope }
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
                            message: data.body
                        });
                    });
                })
        ])
    );
});

self.addEventListener('notificationclick', function(event) {
    event.notification.close();

    event.waitUntil(
        self.clients.matchAll({ type: 'window' }).then(function(clients) {
            // Focus existing window if available
            for (var i = 0; i < clients.length; i++) {
                if (clients[i].url.includes('/push/') && 'focus' in clients[i]) {
                    return clients[i].focus();
                }
            }
            // Otherwise open new window
            if (self.clients.openWindow) {
                return self.clients.openWindow(event.notification.data.url || '/push/');
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
