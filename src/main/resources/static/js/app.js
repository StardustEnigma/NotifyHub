/**
 * NotifyHub — Client-side application logic.
 *
 * Handles WebSocket connection via SockJS + STOMP, REST API calls
 * for CRUD operations, and dynamic UI rendering with real-time updates.
 */
(function () {
    'use strict';

    // ───── Configuration ─────
    const API_BASE = '/api/notifications';
    const WS_ENDPOINT = '/ws';

    // ───── DOM References ─────
    const currentUserSelect = document.getElementById('currentUser');
    const connectionStatus = document.getElementById('connectionStatus');
    const statusText = connectionStatus.querySelector('.status-text');

    const notificationForm = document.getElementById('notificationForm');
    const recipientSelect = document.getElementById('recipientId');
    const titleInput = document.getElementById('notifTitle');
    const messageInput = document.getElementById('notifMessage');
    const btnSend = document.getElementById('btnSend');

    const notificationList = document.getElementById('notificationList');
    const emptyState = document.getElementById('emptyState');
    const unreadBadge = document.getElementById('unreadBadge');
    const btnRefresh = document.getElementById('btnRefresh');
    const btnMarkAllRead = document.getElementById('btnMarkAllRead');
    const filterButtons = document.querySelectorAll('.btn-filter');
    const toastContainer = document.getElementById('toastContainer');

    // ───── State ─────
    let stompClient = null;
    let currentSubscription = null;
    let currentFilter = 'all';
    let notifications = [];

    // ───── WebSocket ─────

    function connectWebSocket() {
        const socket = new SockJS(WS_ENDPOINT);
        stompClient = Stomp.over(socket);

        // Suppress STOMP debug logs in production-like usage
        stompClient.debug = null;

        stompClient.connect({}, onConnected, onConnectionError);
    }

    function onConnected() {
        setConnectionStatus('connected', 'Connected');
        subscribeToUser(currentUserSelect.value);
    }

    function onConnectionError(error) {
        setConnectionStatus('disconnected', 'Disconnected');
        console.error('WebSocket error:', error);

        // Auto-reconnect after 5 seconds
        setTimeout(connectWebSocket, 5000);
    }

    function subscribeToUser(userId) {
        // Unsubscribe from previous user's topic if any
        if (currentSubscription) {
            currentSubscription.unsubscribe();
        }

        const topic = '/topic/notifications/' + userId;
        currentSubscription = stompClient.subscribe(topic, function (message) {
            const notification = JSON.parse(message.body);
            handleIncomingNotification(notification);
        });
    }

    function handleIncomingNotification(notification) {
        // Add to the top of the local list
        notifications.unshift(notification);
        renderNotifications();
        updateUnreadCount();
        showToast(notification);
    }

    function setConnectionStatus(status, text) {
        connectionStatus.className = 'connection-status ' + status;
        statusText.textContent = text;
    }

    // ───── REST API ─────

    async function fetchNotifications(userId) {
        try {
            const endpoint = currentFilter === 'unread'
                ? API_BASE + '/' + userId + '/unread'
                : API_BASE + '/' + userId;

            const response = await fetch(endpoint, {
                headers: { 'Accept': 'application/json' }
            });
            const data = await response.json();

            if (data.success) {
                notifications = data.data || [];
                renderNotifications();
                updateUnreadCount();
            }
        } catch (error) {
            console.error('Failed to fetch notifications:', error);
        }
    }

    async function sendNotification(payload) {
        try {
            const response = await fetch(API_BASE, {
                method: 'POST',
                headers: {
                    'Accept': 'application/json',
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
            });

            const data = await response.json();

            if (data.success) {
                notificationForm.reset();
                // Re-check the default "Info" radio
                document.getElementById('typeInfo').checked = true;
                showToast({
                    title: 'Sent!',
                    message: 'Notification delivered to ' + payload.recipientId,
                    type: 'SUCCESS'
                });
            } else {
                showToast({
                    title: 'Error',
                    message: data.message || 'Failed to send',
                    type: 'ERROR'
                });
            }
        } catch (error) {
            console.error('Failed to send notification:', error);
            showToast({ title: 'Error', message: 'Network error', type: 'ERROR' });
        }
    }

    async function markAsRead(notificationId) {
        try {
            const response = await fetch(API_BASE + '/' + notificationId + '/read', {
                method: 'PATCH',
                headers: { 'Accept': 'application/json' }
            });
            const data = await response.json();

            if (data.success) {
                const notif = notifications.find(n => n.id === notificationId);
                if (notif) notif.read = true;
                renderNotifications();
                updateUnreadCount();
            }
        } catch (error) {
            console.error('Failed to mark as read:', error);
        }
    }

    async function markAllAsRead() {
        const userId = currentUserSelect.value;
        try {
            const response = await fetch(API_BASE + '/' + userId + '/read-all', {
                method: 'PATCH',
                headers: { 'Accept': 'application/json' }
            });
            const data = await response.json();

            if (data.success) {
                notifications.forEach(n => n.read = true);
                renderNotifications();
                updateUnreadCount();
            }
        } catch (error) {
            console.error('Failed to mark all as read:', error);
        }
    }

    // ───── UI Rendering ─────

    function renderNotifications() {
        let filtered = notifications;
        if (currentFilter === 'unread') {
            filtered = notifications.filter(n => !n.read);
        }

        if (filtered.length === 0) {
            notificationList.innerHTML = '';
            notificationList.appendChild(createEmptyState());
            return;
        }

        notificationList.innerHTML = filtered.map(createNotificationCard).join('');

        // Attach click handlers for mark-as-read buttons
        notificationList.querySelectorAll('.btn-mark-read').forEach(btn => {
            btn.addEventListener('click', function (e) {
                e.stopPropagation();
                markAsRead(this.dataset.id);
            });
        });
    }

    function createNotificationCard(notification) {
        const unreadClass = notification.read ? '' : ' unread';
        const timeAgo = notification.timeAgo || formatTimeAgo(notification.createdAt);
        const readButton = notification.read
            ? ''
            : '<button class="btn-mark-read" data-id="' + notification.id + '">Mark read</button>';

        return '<div class="notification-card' + unreadClass + '">' +
            '<div class="notif-top">' +
                '<span class="notif-type-badge ' + notification.type + '">' + notification.type + '</span>' +
                '<span class="notif-time">' + timeAgo + '</span>' +
            '</div>' +
            '<div class="notif-title">' + escapeHtml(notification.title) + '</div>' +
            '<div class="notif-message">' + escapeHtml(notification.message) + '</div>' +
            '<div class="notif-footer">' +
                '<span class="notif-sender">from ' + escapeHtml(notification.senderId) + '</span>' +
                readButton +
            '</div>' +
        '</div>';
    }

    function createEmptyState() {
        const div = document.createElement('div');
        div.className = 'empty-state';
        div.innerHTML =
            '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="empty-icon">' +
                '<path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"/>' +
                '<path d="M13.73 21a2 2 0 0 1-3.46 0"/>' +
            '</svg>' +
            '<p>No notifications yet</p>' +
            '<span>Send one from the panel on the left!</span>';
        return div;
    }

    function updateUnreadCount() {
        const count = notifications.filter(n => !n.read).length;
        if (count > 0) {
            unreadBadge.textContent = count > 99 ? '99+' : count;
            unreadBadge.style.display = 'inline-block';
        } else {
            unreadBadge.style.display = 'none';
        }
    }

    // ───── Toast Notifications ─────

    function showToast(notification) {
        const toast = document.createElement('div');
        toast.className = 'toast';
        toast.innerHTML =
            '<div class="toast-accent ' + (notification.type || 'INFO') + '"></div>' +
            '<div class="toast-body">' +
                '<div class="toast-title">' + escapeHtml(notification.title) + '</div>' +
                '<div class="toast-message">' + escapeHtml(notification.message) + '</div>' +
            '</div>';

        toastContainer.appendChild(toast);

        // Auto-dismiss after 4 seconds
        setTimeout(function () {
            toast.classList.add('toast-out');
            toast.addEventListener('animationend', function () {
                toast.remove();
            });
        }, 4000);
    }

    // ───── Utilities ─────

    function formatTimeAgo(dateStr) {
        if (!dateStr) return '';
        const date = new Date(dateStr);
        const seconds = Math.floor((Date.now() - date.getTime()) / 1000);

        if (seconds < 60) return 'just now';
        const minutes = Math.floor(seconds / 60);
        if (minutes < 60) return minutes + (minutes === 1 ? ' minute ago' : ' minutes ago');
        const hours = Math.floor(minutes / 60);
        if (hours < 24) return hours + (hours === 1 ? ' hour ago' : ' hours ago');
        const days = Math.floor(hours / 24);
        return days + (days === 1 ? ' day ago' : ' days ago');
    }

    function escapeHtml(text) {
        if (!text) return '';
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    // ───── Event Handlers ─────

    notificationForm.addEventListener('submit', function (e) {
        e.preventDefault();

        const selectedType = document.querySelector('input[name="notifType"]:checked');
        const payload = {
            senderId: currentUserSelect.value,
            recipientId: recipientSelect.value,
            title: titleInput.value.trim(),
            message: messageInput.value.trim(),
            type: selectedType ? selectedType.value : 'INFO'
        };

        if (!payload.recipientId || !payload.title || !payload.message) return;

        sendNotification(payload);
    });

    currentUserSelect.addEventListener('change', function () {
        const userId = this.value;
        // Re-subscribe to the new user's WebSocket topic
        if (stompClient && stompClient.connected) {
            subscribeToUser(userId);
        }
        fetchNotifications(userId);
    });

    btnRefresh.addEventListener('click', function () {
        fetchNotifications(currentUserSelect.value);
    });

    btnMarkAllRead.addEventListener('click', markAllAsRead);

    filterButtons.forEach(function (btn) {
        btn.addEventListener('click', function () {
            filterButtons.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            currentFilter = this.dataset.filter;
            renderNotifications();
        });
    });

    // ───── Initialize ─────

    connectWebSocket();
    fetchNotifications(currentUserSelect.value);

})();
