import useSocket from "../socket/sockethandler";
import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
export default function Matchmaking() {
    const { socket, isConnected } = useSocket('http://localhost:3000');
    const [userId, setUserId] = useState('');
    const navigate = useNavigate();
    useEffect(() => {
        if (!socket) return;

        socket.on('gameFound', (data) => {
            navigate(`/game/${data.gameId}?userId=${userId}`);
        });

        return () => {
            socket.off('gameFound');
        };
    }, [socket]);

    const handleJoinQueue = () => {
        if (userId.trim()) {
            socket.emit('joinQueue', { userId: userId });
        }
    };

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-[#222831] gap-6">
            <p className="text-[#DFD0B8] text-lg font-medium">
                Status: {isConnected ? 'Connected' : 'Disconnected'}
            </p>

            <input
                type="text"
                value={userId}
                onChange={(e) => setUserId(e.target.value)}
                placeholder="Enter User ID"
                className="px-6 py-3 text-lg bg-[#393E46] text-[#DFD0B8] border-2 border-[#948979] rounded-lg focus:outline-none focus:border-[#DFD0B8]"
            />

            <button
                onClick={handleJoinQueue}
                className="px-12 py-4 text-lg font-medium text-[#DFD0B8] bg-[#393E46] border-2 border-[#948979] rounded-lg hover:bg-[#948979] hover:text-[#222831] transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"
                disabled={!userId.trim()}
            >
                Join Queue
            </button>
        </div>
    );
}