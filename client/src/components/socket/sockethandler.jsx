import { useEffect, useRef, useState } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

export default function useSocket(url) {
    const stompClient = useRef(null);
    const [isConnected, setIsConnected] = useState(false);
    const subscriptions = useRef(new Set());
    const token = localStorage.getItem('accessToken');
    useEffect(() => {
        const client = new Client({
            webSocketFactory: () => new SockJS(url),
            connectHeaders: { 'Authorization': `Bearer ${token}` },
            debug: (str) => console.log('STOMP Debug:', str),
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: () => {
                console.log('Connected to STOMP server');
                setIsConnected(true);
            },
            onDisconnect: () => {
                console.log('Disconnected from STOMP server');
                setIsConnected(false);
            },
            onStompError: (frame) => {
                console.error('STOMP error:', frame);
            }
        });

        stompClient.current = client;
        client.activate();

        return () => {
            subscriptions.current.forEach((sub) => sub.unsubscribe());
            subscriptions.current.clear();
            client.deactivate();
        };
    }, [url]);

    const send = (destination, body) => {
        const token = localStorage.getItem('accessToken');
        if (stompClient.current && stompClient.current.connected) {
            stompClient.current.publish({
                destination,
                body: JSON.stringify(body),
            });
        } else {
            console.error('STOMP client not connected');
        }
    };

    const subscribe = (destination, callback) => {
        if (!stompClient.current || !stompClient.current.connected) {
            console.error('STOMP client not connected');
            return () => { };
        }

        const subscription = stompClient.current.subscribe(destination, (message) => {
            const data = JSON.parse(message.body);
            callback(data);
        });

        subscriptions.current.add(subscription);

        console.log(`Subscribed to ${destination} (ID: ${subscription.id})`);


        return () => {
            subscription.unsubscribe();
            subscriptions.current.delete(subscription);
            console.log(`Unsubscribed from ${destination} (ID: ${subscription.id})`);
        };
    };

    return {
        stompClient: stompClient.current,
        isConnected,
        send,
        subscribe
    };
}