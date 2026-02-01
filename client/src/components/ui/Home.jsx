import { checkAuth } from "../service/AuthService";
import { useState } from 'react';
export default function Home() {
    const [message, setMessage] = useState('');
    const handleCheck = async () => {
        try {
            const res = await checkAuth();
            setMessage(res);
        } catch (error) {
            setMessage(error.message);
        }

    }

    return (
        <>
            <div className="flex items-center justify-center min-h-screen bg-[#222831]">
                <a href="/matchmaking" className="px-12 py-4 text-lg font-medium text-[#DFD0B8] bg-[#393E46] border-2 border-[#948979] rounded-lg hover:bg-[#948979] hover:text-[#222831] transition-all duration-200">
                    matchmaking
                </a>
                <button
                    onClick={handleCheck}
                    className="hover:bg-gray-100 px-4 py-2 rounded transition-colors"
                >
                    Check authentication
                </button>
                {message && (
                    <p className="text-[#DFD0B8] ml-4">
                        {message}
                    </p>
                )}
            </div >
        </>
    );
}