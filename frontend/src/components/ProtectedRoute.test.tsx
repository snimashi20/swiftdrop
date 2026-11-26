import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "../features/auth/AuthContext";
import { ProtectedRoute } from "./ProtectedRoute";

vi.mock("../features/auth/AuthContext", () => ({
  useAuth: vi.fn(),
}));

describe("ProtectedRoute", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("redirects a rejected merchant away from other protected routes to /merchant/profile", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: 10,
        email: "merchant@example.test",
        name: "Shop Owner",
        role: "MERCHANT",
        status: "REJECTED",
      },
      isLoading: false,
      login: vi.fn(),
      signup: vi.fn(),
      selectRole: vi.fn(),
      refreshProfile: vi.fn(),
      logout: vi.fn(),
      hasRole: vi.fn((...roles) => roles.includes("MERCHANT")),
    });

    render(
      <MemoryRouter initialEntries={["/merchant/orders"]}>
        <Routes>
          <Route element={<ProtectedRoute allowedRoles={["MERCHANT"]} />}>
            <Route path="/merchant/orders" element={<div>Orders</div>} />
            <Route path="/merchant/profile" element={<div>Merchant Profile</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Merchant Profile")).toBeInTheDocument();
    expect(screen.queryByText("Orders")).not.toBeInTheDocument();
  });

  it("allows a rejected merchant to render /merchant/profile", () => {
    vi.mocked(useAuth).mockReturnValue({
      user: {
        id: 10,
        email: "merchant@example.test",
        name: "Shop Owner",
        role: "MERCHANT",
        status: "REJECTED",
      },
      isLoading: false,
      login: vi.fn(),
      signup: vi.fn(),
      selectRole: vi.fn(),
      refreshProfile: vi.fn(),
      logout: vi.fn(),
      hasRole: vi.fn((...roles) => roles.includes("MERCHANT")),
    });

    render(
      <MemoryRouter initialEntries={["/merchant/profile"]}>
        <Routes>
          <Route element={<ProtectedRoute allowedRoles={["MERCHANT"]} />}>
            <Route path="/merchant/profile" element={<div>Merchant Profile</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    expect(screen.getByText("Merchant Profile")).toBeInTheDocument();
  });
});
