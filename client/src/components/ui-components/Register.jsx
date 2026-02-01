import { useState } from 'react';
import { register } from '../service/AuthService';

export default function Register({ onSwitchToLogin }) {
    const [formData, setFormData] = useState({
        email: '',
        username: '',
        password: '',
    });

    const [errors, setErrors] = useState({});
    const [serverError, setServerError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    const validateForm = () => {
        const newErrors = {};

        if (!formData.email.trim()) {
            newErrors.email = 'Email is required';
        } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
            newErrors.email = 'Email must be valid';
        }

        if (!formData.username.trim()) {
            newErrors.username = 'Username is required';
        } else if (formData.username.length < 5) {
            newErrors.username = 'Username must be at least 5 characters long';
        }

        if (!formData.password.trim()) {
            newErrors.password = 'Password is required';
        } else if (formData.password.length < 5) {
            newErrors.password = 'Password must be at least 5 characters long';
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
            const response = await register(formData);
            localStorage.setItem('accessToken', response.accessToken);
            setFormData({ email: '', username: '', password: '' });
        } catch (error) {
            setServerError(error.message || 'Registration failed');
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen flex items-center justify-center" style={{ backgroundColor: '#222831' }}>
            <div className="w-full max-w-md p-8" style={{ backgroundColor: '#393E46' }}>
                <h2 className="text-3xl font-bold mb-8 text-center" style={{ color: '#DFD0B8' }}>
                    Register
                </h2>

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div>
                        <label htmlFor="email" className="block text-sm font-medium mb-2" style={{ color: '#DFD0B8' }}>
                            Email
                        </label>
                        <input
                            type="email"
                            id="email"
                            name="email"
                            value={formData.email}
                            onChange={handleChange}
                            className="w-full px-4 py-3 outline-none"
                            style={{
                                backgroundColor: '#222831',
                                color: '#DFD0B8',
                                border: errors.email ? '2px solid #ff4444' : '2px solid #948979'
                            }}
                        />
                        {errors.email && (
                            <p className="mt-1 text-sm" style={{ color: '#ff4444' }}>
                                {errors.email}
                            </p>
                        )}
                    </div>

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
                        {isLoading ? 'Registering...' : 'Register'}
                    </button>
                </form>

                <div className="mt-6 text-center">
                    <p style={{ color: '#948979' }}>
                        Already have an account?{' '}
                        <button
                            onClick={onSwitchToLogin}
                            className="font-semibold hover:underline"
                            style={{ color: '#DFD0B8' }}
                        >
                            Login
                        </button>
                    </p>
                </div>
            </div>
        </div>
    );
};
