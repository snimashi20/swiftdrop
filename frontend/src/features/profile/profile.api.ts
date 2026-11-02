import { api } from "../../lib/api";

export interface UserProfile {
  id: number;
  email: string;
  fullName: string;
  phoneNumber?: string;
  provider: string;
  role: string;
  enabled?: boolean;
}

export interface UpdateProfileRequest {
  fullName: string;
  phoneNumber?: string;
}

export interface Address {
  id: number;
  addressLabel?: string;
  label?: string;
  recipientName?: string;
  phoneNumber?: string;
  streetAddress: string;
  city: string;
  district: string;
  postalCode?: string;
  isDefault?: boolean;
}

export interface CreateAddressRequest {
  addressLabel: string;
  streetAddress: string;
  city: string;
  district: string;
  postalCode?: string;
  isDefault?: boolean;
}

export const fetchUserProfile = async (): Promise<UserProfile> => {
  const response = await api.get<UserProfile>("/users/me");
  return response.data;
};

export const updateUserProfile = async (data: UpdateProfileRequest): Promise<UserProfile> => {
  const response = await api.put<UserProfile>("/users/me", data);
  return response.data;
};

export const fetchUserAddresses = async (): Promise<Address[]> => {
  const response = await api.get<Address[]>("/users/addresses");
  return response.data;
};

export const createUserAddress = async (data: CreateAddressRequest): Promise<Address> => {
  const response = await api.post<Address>("/users/addresses", data);
  return response.data;
};

export const deleteUserAddress = async (addressId: number): Promise<void> => {
  await api.delete(`/users/addresses/${addressId}`);
};
