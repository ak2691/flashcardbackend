import { useState, useEffect } from 'react';


import Login from '../ui-components/Login';
import Register from '../ui-components/Register';
export default function LoginPage() {
    const [showLogin, setShowLogin] = useState(true);
    return (

        <>
            {showLogin ? (
                <Login onSwitchToRegister={() => setShowLogin(false)} />
            ) : (
                <Register onSwitchToLogin={() => setShowLogin(true)} />
            )}
        </>
    )

}