import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { OAuthCallbackPage } from './features/webhook/pages/OAuthCallbackPage';
import { WebhookSettingsPage } from './features/webhook/pages/WebhookSettingsPage'; // ✅ Wrapped in curly braces
import { UserProfilePage } from './features/webhook/pages/UserProfilePage';
import { AddressBookPage } from './features/webhook/pages/AddressBookPage';

export function App() {
    return (
        <div className="min-h-screen bg-gray-50">
            <Routes>
                <Route path="/" element={<Navigate to="/profile" replace />} />
                <Route path="/oauth/callback" element={<OAuthCallbackPage />} />
                <Route path="/profile" element={<UserProfilePage />} />
                <Route path="/profile/addresses" element={<AddressBookPage />} />
                <Route path="/webhooks" element={<WebhookSettingsPage />} />
                <Route path="*" element={<Navigate to="/profile" replace />} />
            </Routes>
        </div>
    );
}