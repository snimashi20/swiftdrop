import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { WaitingApprovalPage } from "./WaitingApprovalPage";
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

describe("WaitingApprovalPage", () => {
  const mockRefreshProfile = vi.fn();
  const mockLogout = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: "2",
        name: "Partner Courier",
        email: "courier@example.com",
        role: "COURIER",
        status: "PENDING_APPROVAL",
      },
      isLoading: false,
      login: vi.fn(),
      signup: vi.fn(),
      selectRole: vi.fn(),
      refreshProfile: mockRefreshProfile,
      logout: mockLogout,
      hasRole: vi.fn(),
    });
  });

  it("renders the exact 'Waiting for Admin's approval...' text", () => {
    render(
      <BrowserRouter>
        <WaitingApprovalPage />
      </BrowserRouter>
    );

    expect(
      screen.getByRole("heading", { name: /Waiting for Admin's approval\.\.\./i })
    ).toBeInTheDocument();
    expect(screen.getByText(/Pending Approval/i)).toBeInTheDocument();
    expect(screen.getByText("courier@example.com")).toBeInTheDocument();
  });

  it("checks approval status and redirects to dashboard once approved", async () => {
    mockRefreshProfile.mockResolvedValueOnce({
      id: "2",
      name: "Partner Courier",
      email: "courier@example.com",
      role: "COURIER",
      status: "ACTIVE",
    });

    render(
      <BrowserRouter>
        <WaitingApprovalPage />
      </BrowserRouter>
    );

    const checkBtn = screen.getByRole("button", { name: /Check Approval Status/i });
    await userEvent.click(checkBtn);

    expect(mockRefreshProfile).toHaveBeenCalled();
    expect(
      await screen.findByText(/Great news! Your account has been approved/i)
    ).toBeInTheDocument();
  });

  it("allows user to sign out", async () => {
    render(
      <BrowserRouter>
        <WaitingApprovalPage />
      </BrowserRouter>
    );

    const logoutBtn = screen.getByRole("button", { name: /Sign Out/i });
    await userEvent.click(logoutBtn);

    expect(mockLogout).toHaveBeenCalled();
    expect(mockNavigate).toHaveBeenCalledWith("/login", { replace: true });
  });
});

