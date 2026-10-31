import React, { useEffect } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';

export const OAuthCallbackPage: React.FC = () => {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();

    useEffect(() => {
        const token = searchParams.get('token');
        const error = searchParams.get('error');

        if (token) {
            // JWT token එක safe ලෙස localStorage හි තබමු
            localStorage.setItem('swiftdrop_token', token);

            // Successfully authenticated පසු Dashboard / Home වෙත යොමු කරමු
            navigate('/dashboard', { replace: true });
        } else if (error) {
            // Disabled හෝ invalid account නම් Error එකක් සමඟ Login එකට යවමු
            console.error('Authentication Error:', error);
            navigate('/login?error=' + error, { replace: true });
        } else {
            navigate('/login', { replace: true });
        }
    }, [searchParams, navigate]);

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50">
            <div className="p-6 bg-white rounded-lg shadow-md text-center">
                <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin mx-auto mb-4"></div>
                <h2 className="text-lg font-semibold text-gray-700">Completing Sign-In...</h2>
                <p className="text-sm text-gray-500 mt-1">Please wait while we log you into SwiftDrop.</p>
            </div>
        </div>
    );
};