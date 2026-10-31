import React, { useEffect, useState } from 'react';
import { fetchUserProfile, updateUserProfile, UserProfile } from '../api';

export const UserProfilePage: React.FC = () => {
    const [profile, setProfile] = useState<UserProfile | null>(null);
    const [fullName, setFullName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState('');

    useEffect(() => {
        fetchUserProfile()
            .then((data) => {
                setProfile(data);
                setFullName(data.fullName || '');
                setPhoneNumber(data.phoneNumber || '');
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setLoading(false);
            });
    }, []);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setMessage('');
        try {
            const updated = await updateUserProfile({ fullName, phoneNumber });
            setProfile(updated);
            setMessage('Profile updated successfully!');
        } catch (err) {
            setMessage('Failed to update profile.');
        }
    };

    if (loading) return <div className="p-6">Loading profile...</div>;

    return (
        <div className="max-w-xl mx-auto p-6 bg-white rounded-lg shadow-md mt-10">
            <h1 className="text-2xl font-bold mb-6">User Profile (M2)</h1>
            {message && <div className="mb-4 p-3 bg-blue-100 text-blue-700 rounded">{message}</div>}

            <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                    <label className="block text-sm font-medium text-gray-700">Email (Read Only)</label>
                    <input
                        type="email"
                        value={profile?.email || ''}
                        disabled
                        className="w-full mt-1 p-2 bg-gray-100 border rounded cursor-not-allowed"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Auth Provider</label>
                    <input
                        type="text"
                        value={profile?.provider || 'LOCAL'}
                        disabled
                        className="w-full mt-1 p-2 bg-gray-100 border rounded cursor-not-allowed"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Full Name</label>
                    <input
                        type="text"
                        value={fullName}
                        onChange={(e) => setFullName(e.target.value)}
                        required
                        className="w-full mt-1 p-2 border rounded"
                    />
                </div>

                <div>
                    <label className="block text-sm font-medium text-gray-700">Phone Number</label>
                    <input
                        type="text"
                        value={phoneNumber}
                        onChange={(e) => setPhoneNumber(e.target.value)}
                        className="w-full mt-1 p-2 border rounded"
                        placeholder="+947XXXXXXXX"
                    />
                </div>

                <button
                    type="submit"
                    className="w-full bg-blue-600 text-white py-2 rounded font-semibold hover:bg-blue-700 transition"
                >
                    Update Profile
                </button>
            </form>
        </div>
    );
};