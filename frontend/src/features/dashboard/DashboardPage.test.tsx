import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardPage } from "./DashboardPage";
import { useAuth } from "../auth/AuthContext";
import { fetchApiKeys, fetchDashboardOrderSummary, fetchDashboardOrderTrend } from "../merchants/api";

vi.mock("../auth/AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("../merchants/api", () => ({
  fetchApiKeys: vi.fn(),
  fetchDashboardOrderSummary: vi.fn(),
  fetchDashboardOrderTrend: vi.fn(),
}));

describe("MerchantOverview", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.stubGlobal("ResizeObserver", class {
      observe() {}
      unobserve() {}
      disconnect() {}
    });
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 7, name: "Demo Merchant", email: "merchant@example.com", role: "MERCHANT", status: "ACTIVE" },
      isLoading: false,
      login: vi.fn(),
      signup: vi.fn(),
      selectRole: vi.fn(),
      refreshProfile: vi.fn(),
      logout: vi.fn(),
      hasRole: vi.fn(),
    });
    vi.mocked(fetchDashboardOrderSummary).mockResolvedValue({ CREATED: 1, IN_TRANSIT: 2, DELIVERED: 4 });
    vi.mocked(fetchDashboardOrderTrend).mockResolvedValue([
      { date: "2026-11-10", count: 1 },
      { date: "2026-11-11", count: 0 },
      { date: "2026-11-12", count: 2 },
      { date: "2026-11-13", count: 0 },
      { date: "2026-11-14", count: 0 },
      { date: "2026-11-15", count: 0 },
      { date: "2026-11-16", count: 0 },
    ]);
    vi.mocked(fetchApiKeys).mockResolvedValue([
      { id: 1, label: "Store key", keyPrefix: "sd_1234", active: true, createdAt: "2026-11-01T00:00:00Z", lastUsedAt: null },
    ]);
  });

  it("renders the seven-day order chart and integration summary", async () => {
    render(<MemoryRouter><DashboardPage /></MemoryRouter>);

    expect(await screen.findByText("Business overview")).toBeInTheDocument();
    await waitFor(() => expect(screen.getByLabelText("Seven day order activity chart")).toBeInTheDocument());
    expect(screen.getByText("Total orders")).toBeInTheDocument();
    expect(screen.getByText("7")).toBeInTheDocument();
    expect(screen.getByText("Active API keys")).toBeInTheDocument();
    expect(screen.getByText("Key available")).toBeInTheDocument();
  });

  it("keeps summary and API-key data when the trend request fails", async () => {
    vi.mocked(fetchDashboardOrderTrend).mockRejectedValue(new Error("trend unavailable"));

    render(<MemoryRouter><DashboardPage /></MemoryRouter>);

    expect(await screen.findByText("Business overview")).toBeInTheDocument();
    expect(await screen.findByRole("alert")).toHaveTextContent("trend unavailable");
    expect(screen.getByText("Total orders")).toBeInTheDocument();
    expect(screen.getByText("Active API keys")).toBeInTheDocument();
  });
});
