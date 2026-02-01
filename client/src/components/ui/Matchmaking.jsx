import useSocket from "../socket/sockethandler";
import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSocketContext } from "../socket/socketcontext";
export default function Matchmaking() {
    const { stompClient, isConnected, send, subscribe } = useSocketContext();

    const navigate = useNavigate();
    useEffect(() => {
        if (!isConnected) return;


        const unsubscribeQueueJoined = subscribe(`/user/queue/queueJoined`, (data) => {
            console.log('Successfully joined queue: You are in position', data.position);
            // Show toast notification, update UI, etc.
        });

        // Subscribe to game found notifications
        const unsubscribeGameFound = subscribe(`/user/queue/gameFound`, (data) => {
            console.log("GAME FOUND");
            navigate(`/game/${data.id}`);
        });

        // Cleanup both subscriptions
        return () => {
            unsubscribeQueueJoined();
            unsubscribeGameFound();
        };
    }, [isConnected, subscribe, navigate]);

    const handleJoinQueue = () => {
        const token = localStorage.getItem('accessToken');

        send('/app/joinQueue', {});

    };


    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-[#222831] gap-6">
            <p className="text-[#DFD0B8] text-lg font-medium">
                Status: {isConnected ? 'Connected' : 'Disconnected'}
            </p>

            <button
                onClick={handleJoinQueue}
                className="px-12 py-4 text-lg font-medium text-[#DFD0B8] bg-[#393E46] border-2 border-[#948979] rounded-lg hover:bg-[#948979] hover:text-[#222831] transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed"

            >
                Join Queue
            </button>
        </div>
    );
}