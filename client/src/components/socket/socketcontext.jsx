import { createContext, useContext } from 'react';
import useSocket from './sockethandler';
const SocketContext = createContext(null);

export function SocketProvider({ children }) {
    const { stompClient, isConnected, send, subscribe } = useSocket('http://localhost:8080/ws');

    return (
        <SocketContext.Provider value={{ stompClient, isConnected, send, subscribe }}>
            {children}
        </SocketContext.Provider>
    );
}
export const useSocketContext = () => useContext(SocketContext);