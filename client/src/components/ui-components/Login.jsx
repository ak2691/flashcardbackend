import { useState } from 'react';
import { login } from '../service/AuthService';

export default function Login({ onSwitchToRegister }) {
    const [formData, setFormData] = useState({
        username: '',
        password: '',
    });

    const [errors, setErrors] = useState({});
    const [serverError, setServerError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const validateForm = () => {
        const newErrors = {};

        if (!formData.username.trim()) {
            newErrors.username = 'Username is required';
        }

        if (!formData.password.trim()) {
            newErrors.password = 'Password is required';
        }

        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prev) => ({
            ...prev,
            [name]: value,
        }));
        if (errors[name]) {
            setErrors((prev) => ({
                ...prev,
                [name]: '',
            }));
        }
        setServerError('');
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!validateForm()) {
            return;
        }

        setIsLoading(true);
        setServerError('');

        try {
            const response = await login(formData);
            localStorage.setItem('accessToken', response.accessToken);
            setFormData({ username: '', password: '' });
        } catch (error) {
            setServerError(error.message || 'Login failed');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: '#222831' }}>
            <div className="w-full max-w-md p-8" style={{ backgroundColor: '#393E46' }}>
                <h2 className="text-3xl font-bold mb-8 text-center" style={{ color: '#DFD0B8' }}>
                    Login
                </h2>

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label htmlFor="username" className="block text-sm font-medium mb-2" style={{ color: '#DFD0B8' }}>
                            Username
                        </label>
                        <input
                            type="text"
                            id="username"
                            name="username"
                            value={formData.username}
                            onChange={handleChange}
                            className="w-full px-4 py-3 outline-none"
                            style={{
                                backgroundColor: '#222831',
                                color: '#DFD0B8',
                                border: errors.username ? '2px solid #ff4444' : '2px solid #948979'
                            }}
                        />
                        {errors.username && (
                            <p className="mt-1 text-sm" style={{ color: '#ff4444' }}>
                                {errors.username}
                            </p>
                        )}
                    </div>

                    <div>
                        <label htmlFor="password" className="block text-sm font-medium mb-2" style={{ color: '#DFD0B8' }}>
                            Password
                        </label>
                        <input
                            type="password"
                            id="password"
                            name="password"
                            value={formData.password}
                            onChange={handleChange}
                            className="w-full px-4 py-3 outline-none"
                            style={{
                                backgroundColor: '#222831',
                                color: '#DFD0B8',
                                border: errors.password ? '2px solid #ff4444' : '2px solid #948979'
                            }}
                        />
                        {errors.password && (
                            <p className="mt-1 text-sm" style={{ color: '#ff4444' }}>
                                {errors.password}
                            </p>
                        )}
                    </div>

                    {serverError && (
                        <div className="p-3" style={{ backgroundColor: '#222831', border: '2px solid #ff4444' }}>
                            <p className="text-sm" style={{ color: '#ff4444' }}>
                                {serverError}
                            </p>
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full py-3 font-semibold transition-colors"
                        style={{
                            backgroundColor: '#948979',
                            color: '#222831',
                            opacity: isLoading ? 0.6 : 1
                        }}
                    >
                        {isLoading ? 'Logging in...' : 'Login'}
                    </button>
                </form>

                <div className="mt-6 text-center">
                    <p style={{ color: '#948979' }}>
                        Don't have an account?{' '}
                        <button
                            onClick={onSwitchToRegister}
                            className="font-semibold hover:underline"
                            style={{ color: '#DFD0B8' }}
                        >
                            Register
                        </button>
                    </p>
                </div>
            </div>
        </div>
    );
};