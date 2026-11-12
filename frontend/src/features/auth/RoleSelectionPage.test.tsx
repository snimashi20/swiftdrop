import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { RoleSelectionPage } from "./RoleSelectionPage";
import { useAuth } from "./AuthContext";

vi.mock("./AuthContext", () => ({
  useAuth: vi.fn(),
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe("RoleSelectionPage", () => {
  const mockSelectRole = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: "1",
        name: "Google User",
        email: "google@example.com",
        role: "CUSTOMER",
        status: "PENDING_ROLE",
      },
      isLoading: false,
      login: vi.fn(),
      signup: vi.fn(),
      selectRole: mockSelectRole,
      refreshProfile: vi.fn(),
      logout: vi.fn(),
      hasRole: vi.fn(),
    });
  });

  it("renders only Customer, Merchant, and Courier without Admin or Dispatcher", () => {
    render(
      <BrowserRouter>
        <RoleSelectionPage />
      </BrowserRouter>
    );

    expect(screen.getByRole("heading", { name: /Choose your account type/i })).toBeInTheDocument();

    // Verify allowed self-registration roles exist
    expect(screen.getByText(/^Customer$/i)).toBeInTheDocument();
    expect(screen.getByText(/^Merchant$/i)).toBeInTheDocument();
    expect(screen.getByText(/^Courier$/i)).toBeInTheDocument();

    // Verify Admin and Dispatcher are strictly NOT present
    expect(screen.queryByText(/^Admin$/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/^Dispatcher$/i)).not.toBeInTheDocument();
  });

  it("navigates to /dashboard when Customer role is selected", async () => {
    mockSelectRole.mockResolvedValueOnce({
      id: "1",
      name: "Google User",
      email: "google@example.com",
      role: "CUSTOMER",
      status: "ACTIVE",
    });

    render(
      <BrowserRouter>
        <RoleSelectionPage />
      </BrowserRouter>
    );

    const submitBtn = screen.getByRole("button", { name: /Complete Registration & Continue/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockSelectRole).toHaveBeenCalledWith("CUSTOMER");
      expect(mockNavigate).toHaveBeenCalledWith("/dashboard", { replace: true });
    });
  });

  it("navigates to /waiting-approval when Merchant role is selected", async () => {
    mockSelectRole.mockResolvedValueOnce({
      id: "1",
      name: "Google User",
      email: "google@example.com",
      role: "MERCHANT",
      status: "PENDING_APPROVAL",
    });

    render(
      <BrowserRouter>
        <RoleSelectionPage />
      </BrowserRouter>
    );

    // Select Merchant card
    const merchantCard = screen.getByText(/^Merchant$/i);
    await userEvent.click(merchantCard);

    const submitBtn = screen.getByRole("button", { name: /Complete Registration & Continue/i });
    await userEvent.click(submitBtn);

    await waitFor(() => {
      expect(mockSelectRole).toHaveBeenCalledWith("MERCHANT");
      expect(mockNavigate).toHaveBeenCalledWith("/waiting-approval", { replace: true });
    });
  });
});

