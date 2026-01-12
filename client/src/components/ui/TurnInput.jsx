import { useState } from 'react';
export default function TurnInput({ onSubmit, disabled }) {
    const [message, setMessage] = useState('');
    const MAX_CHARS = 250;

    const handleSubmit = () => {
        if (message.trim() && !disabled) {
            onSubmit(message);
            setMessage('');
        }
    };

    return (
        <div className="bg-[#393E46] rounded-lg p-4 shadow-lg">
            <textarea
                value={message}
                onChange={(e) => {
                    if (e.target.value.length <= MAX_CHARS) {
                        setMessage(e.target.value);
                    }
                }}
                placeholder={disabled ? "Turn limit reached" : "Enter your message..."}
                disabled={disabled}
                className="w-full bg-[#222831] text-[#DFD0B8] placeholder-[#948979] rounded p-3 resize-none focus:outline-none focus:ring-2 focus:ring-[#948979] disabled:opacity-50 disabled:cursor-not-allowed"
                rows={4}
            />
            <div className="flex justify-between items-center mt-3">
                <span className="text-[#948979] text-sm">
                    {message.length}/{MAX_CHARS}
                </span>
                <button
                    onClick={handleSubmit}
                    disabled={!message.trim() || disabled}
                    className="bg-[#948979] text-[#222831] px-6 py-2 rounded font-medium hover:bg-[#DFD0B8] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                >
                    Submit
                </button>
            </div>
        </div>
    );
}