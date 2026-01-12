
import { useState, useEffect } from 'react';
import { SubmitTurn } from "../service/SubmitTurn";
import fetchGameState from '../service/FetchGameState';
import { useParams } from 'react-router-dom';
import useSocket from '../socket/sockethandler';
import TurnInput from './TurnInput';
export default function GamePage() {
    const [message, setMessage] = useState('');
    const { socket, isConnected } = useSocket('http://localhost:3000');
    const MAX_CHARS = 250;
    const { gameId } = useParams();
    const [myMessageCount, setMyMessageCount] = useState(0);
    const [opponentMessageCount, setOpponentMessageCount] = useState(0);
    const [gameComplete, setGameComplete] = useState(false);
    const [results, setResults] = useState(null);
    useEffect(() => {

        fetchGameState(gameId).then(data => {
            setMyMessageCount(data.myMessageCount);
            setOpponentMessageCount(data.opponentMessageCount);
        });
        socket.on('turnSubmitted', (data) => {
            // data could be: { userId, messageCount }
            if (data.userId === myUserId) {
                setMyMessageCount(data.messageCount);
            } else {
                setOpponentMessageCount(data.messageCount);
            }
        });
        socket.on('gameComplete', (data) => {
            setGameComplete(true);
            //setResults(data.finalState);
        });
        return () => {
            socket.off('turnSubmitted');
            socket.off('gameComplete');
        };

    }, [gameId]);
    const handleSubmitTurn = async (message) => {
        try {
            await submitTurn({ gameId, myUserId, message });
        } catch (error) {
            console.error('Error submitting turn:', error);
        }


    };
    const handleSubmit = () => {
        if (message.trim()) {
            handleSubmitTurn(message);
            setMessage('');
        }
    };

    return (
        <div className="w-full max-w-2xl mx-auto p-4">
            {/* Score Display */}
            <div className="flex gap-4 mb-4">
                <div className="bg-[#393E46] rounded-lg px-4 py-2 flex-1">
                    <p className="text-[#DFD0B8] font-medium">
                        Your prompts: <span className="text-[#948979]">{myMessageCount}/5</span>
                    </p>
                </div>
                <div className="bg-[#393E46] rounded-lg px-4 py-2 flex-1">
                    <p className="text-[#DFD0B8] font-medium">
                        Opponent: <span className="text-[#948979]">{opponentMessageCount}/5</span>
                    </p>
                </div>
            </div>

            {/* Turn Input Component */}
            <TurnInput onSubmit={handleSubmitTurn} disabled={myMessageCount >= 5} />
        </div>
    );
}