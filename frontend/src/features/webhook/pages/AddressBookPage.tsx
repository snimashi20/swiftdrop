import React, { useEffect, useState } from 'react';
import { fetchUserAddresses, createUserAddress, deleteUserAddress, Address, CreateAddressRequest } from '../api';

export const AddressBookPage: React.FC = () => {
    const [addresses, setAddresses] = useState<Address[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    // Form input states
    const [label, setLabel] = useState('Home');
    const [recipientName, setRecipientName] = useState('');
    const [phoneNumber, setPhoneNumber] = useState('');
    const [streetAddress, setStreetAddress] = useState('');
    const [city, setCity] = useState('');
    const [district, setDistrict] = useState('Colombo');

    const loadAddresses = () => {
        setLoading(true);
        fetchUserAddresses()
            .then((data) => {
                setAddresses(data);
                setLoading(false);
            })
            .catch((err) => {
                console.error(err);
                setError('Failed to load addresses.');
                setLoading(false);
            });
    };

    useEffect(() => {
        loadAddresses();
    }, []);

    const handleAddAddress = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        try {
            const newAddr = {
                addressLabel: label, // Matched with backend AddressDto.addressLabel
                streetAddress,
                city,
                district,
                isDefault: false,
            };
            await createUserAddress(newAddr as any);
            // Reset form
            setStreetAddress('');
            setCity('');
            loadAddresses(); // Refresh list
        } catch (err) {
            console.error(err);
            setError('Failed to save address.');
        }
    };

    const handleDelete = async (id: number) => {
        if (!window.confirm('Are you sure you want to delete this address?')) return;
        try {
            await deleteUserAddress(id);
            loadAddresses(); // Refresh list
        } catch (err) {
            setError('Failed to delete address.');
        }
    };

    return (
        <div className="max-w-3xl mx-auto p-6 bg-white rounded-lg shadow-md mt-10">
            <h1 className="text-2xl font-bold mb-6 text-gray-800">Saved Address Book (M2)</h1>

            {error && <div className="mb-4 p-3 bg-red-100 text-red-700 rounded">{error}</div>}

            {/* Address List */}
            <div className="mb-8">
                <h2 className="text-lg font-semibold mb-3 text-gray-700">Your Saved Addresses</h2>
                {loading ? (
                    <p className="text-gray-500">Loading addresses...</p>
                ) : addresses.length === 0 ? (
                    <p className="text-gray-500 italic">No saved addresses found.</p>
                ) : (
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        {addresses.map((addr) => (
                            <div key={addr.id} className="p-4 border rounded-lg bg-gray-50 relative flex flex-col justify-between">
                                <div>
                                    <span className="inline-block px-2 py-1 text-xs font-semibold text-blue-800 bg-blue-100 rounded mb-2">
                                        {addr.addressLabel || addr.label}
                                    </span>
                                    <p className="text-sm text-gray-600">{addr.streetAddress}, {addr.city}</p>
                                    <p className="text-sm text-gray-600">{addr.district}</p>
                                </div>
                                <button
                                    onClick={() => handleDelete(addr.id)}
                                    className="mt-3 text-sm text-red-600 hover:underline self-end font-medium"
                                >
                                    Delete
                                </button>
                            </div>
                        ))}
                    </div>
                )}
            </div>

            <hr className="my-6" />

            {/* Add New Address Form */}
            <div>
                <h2 className="text-lg font-semibold mb-4 text-gray-700">Add New Address</h2>
                <form onSubmit={handleAddAddress} className="space-y-4">
                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Label</label>
                            <input
                                type="text"
                                value={label}
                                onChange={(e) => setLabel(e.target.value)}
                                placeholder="e.g. Home, Office"
                                required
                                className="w-full mt-1 p-2 border rounded"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Recipient Name</label>
                            <input
                                type="text"
                                value={recipientName}
                                onChange={(e) => setRecipientName(e.target.value)}
                                required
                                className="w-full mt-1 p-2 border rounded"
                            />
                        </div>
                    </div>

                    <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-gray-700">Phone Number</label>
                            <input
                                type="text"
                                value={phoneNumber}
                                onChange={(e) => setPhoneNumber(e.target.value)}
                                placeholder="+947XXXXXXXX"
                                required
                                className="w-full mt-1 p-2 border rounded"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-gray-700">District</label>
                            <input
                                type="text"
                                value={district}
                                onChange={(e) => setDistrict(e.target.value)}
                                required
                                className="w-full mt-1 p-2 border rounded"
                            />
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700">Street Address</label>
                        <input
                            type="text"
                            value={streetAddress}
                            onChange={(e) => setStreetAddress(e.target.value)}
                            required
                            className="w-full mt-1 p-2 border rounded"
                        />
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-gray-700">City</label>
                        <input
                            type="text"
                            value={city}
                            onChange={(e) => setCity(e.target.value)}
                            required
                            className="w-full mt-1 p-2 border rounded"
                        />
                    </div>

                    <button
                        type="submit"
                        className="w-full bg-green-600 text-white py-2 rounded font-semibold hover:bg-green-700 transition"
                    >
                        Save Address
                    </button>
                </form>
            </div>
        </div>
    );
};