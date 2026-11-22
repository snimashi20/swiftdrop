import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { DashboardPage } from "./DashboardPage";
import { useAuth } from "../auth/AuthContext";
import { fetchApiKeys, fetchDashboardOrderSummary, fetchDashboardOrderTrend } from "../merchants/api";
import { listParcels } from "../parcels/parcels.api";
import type { ParcelDetail, ParcelPage } from "../parcels/parcels.api";

vi.mock("../auth/AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("../merchants/api", () => ({
  fetchApiKeys: vi.fn(),
  fetchDashboardOrderSummary: vi.fn(),
  fetchDashboardOrderTrend: vi.fn(),
}));
vi.mock("../parcels/parcels.api", () => ({
  listParcels: vi.fn(),
  STATUS_LABELS: {
    CREATED: "Booked",
    PICKED_UP: "Picked up",
    IN_TRANSIT: "In transit",
    OUT_FOR_DELIVERY: "Out for delivery",
    DELIVERED: "Delivered",
    FAILED: "Failed",
    CANCELLED: "Cancelled",
  },
}));

const mockParcel = (over: Partial<ParcelDetail> = {}): ParcelDetail => ({
  id: 1,
  trackingNumber: "SDABC1234567",
  status: "CREATED",
  pickup: { line1: "123 Galle Rd", city: "Colombo", district: "Colombo" },
  dropoff: { line1: "456 Kandy Rd", city: "Peradeniya", district: "Kandy" },
  receiverName: "Nimal Perera",
  receiverPhone: "0771234567",
  size: "M",
  weightKg: 2,
  fee: 750,
  deliveryInstructions: null,
  createdAt: "2026-11-20T10:00:00Z",
  updatedAt: "2026-11-20T10:00:00Z",
  ...over,
});

const mockPage = (items: ParcelDetail[], totalElements = items.length): ParcelPage => ({
  content: items,
  number: 0,
  totalPages: Math.ceil(totalElements / 10) || 1,
  totalElements,
});

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

describe("CustomerOverviewPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useAuth).mockReturnValue({
      user: { id: 10, name: "Kasun Silva", email: "kasun@example.com", role: "CUSTOMER", status: "ACTIVE" },
      isLoading: false,
      login: vi.fn(),
      signup: vi.fn(),
      selectRole: vi.fn(),
      refreshProfile: vi.fn(),
      logout: vi.fn(),
      hasRole: vi.fn(),
    });
  });

  it("renders live customer metrics and recent shipments from real data", async () => {
    const parcels = [
      mockParcel({ id: 1, trackingNumber: "SD1111111111", status: "CREATED", fee: 500 }),
      mockParcel({ id: 2, trackingNumber: "SD2222222222", status: "IN_TRANSIT", fee: 700 }),
      mockParcel({ id: 3, trackingNumber: "SD3333333333", status: "DELIVERED", fee: 1200 }),
    ];
    vi.mocked(listParcels).mockResolvedValue(mockPage(parcels));

    render(<MemoryRouter><DashboardPage /></MemoryRouter>);

    expect(await screen.findByText("Hello, Kasun.")).toBeInTheDocument();
    expect(screen.getByText("Total parcels")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();

    // 2 active deliveries (CREATED + IN_TRANSIT)
    expect(screen.getByText("Active now")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("In transit (latest 10)")).toBeInTheDocument();

    // 1 delivered
    expect(screen.getByText("Recently delivered")).toBeInTheDocument();
    expect(screen.getByText("Completed in last 10")).toBeInTheDocument();
    expect(screen.getByText("1")).toBeInTheDocument();

    // Total spent: 500 + 700 + 1200 = 2400
    expect(screen.getByText("Spent on recent shipments")).toBeInTheDocument();
    expect(screen.getByText("Across latest 10 parcels")).toBeInTheDocument();
    expect(screen.getByText("LKR 2,400")).toBeInTheDocument();

    // Recent shipments list
    expect(screen.getByText("SD1111111111")).toBeInTheDocument();
    expect(screen.getByText("SD2222222222")).toBeInTheDocument();
    expect(screen.getByText("SD3333333333")).toBeInTheDocument();
    expect(screen.getByText("Send a parcel")).toBeInTheDocument();
  });

  it("handles pagination scope mismatch accurately when totalElements exceeds page size", async () => {
    // 10 items in content, but 30 total in database
    const tenParcels: ParcelDetail[] = [
      mockParcel({ id: 1, status: "CREATED", fee: 100 }),
      mockParcel({ id: 2, status: "PICKED_UP", fee: 100 }),
      mockParcel({ id: 3, status: "IN_TRANSIT", fee: 100 }),
      mockParcel({ id: 4, status: "OUT_FOR_DELIVERY", fee: 100 }),
      mockParcel({ id: 5, status: "DELIVERED", fee: 200 }),
      mockParcel({ id: 6, status: "DELIVERED", fee: 200 }),
      mockParcel({ id: 7, status: "DELIVERED", fee: 200 }),
      mockParcel({ id: 8, status: "FAILED", fee: 100 }),
      mockParcel({ id: 9, status: "CANCELLED", fee: 0 }),
      mockParcel({ id: 10, status: "FAILED", fee: 100 }),
    ];
    vi.mocked(listParcels).mockResolvedValue(mockPage(tenParcels, 30));

    render(<MemoryRouter><DashboardPage /></MemoryRouter>);

    expect(await screen.findByText("Hello, Kasun.")).toBeInTheDocument();
    // All-time total elements displayed in "Total parcels"
    expect(screen.getByText("Total parcels")).toBeInTheDocument();
    expect(screen.getByText("30")).toBeInTheDocument();
    expect(screen.getByText("30 shipments recorded")).toBeInTheDocument();

    // Derived from the 10 fetched shipments
    // Active now: 4 (CREATED, PICKED_UP, IN_TRANSIT, OUT_FOR_DELIVERY)
    expect(screen.getByText("Active now")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
    expect(screen.getByText("In transit (latest 10)")).toBeInTheDocument();

    // Recently delivered: 3 DELIVERED
    expect(screen.getByText("Recently delivered")).toBeInTheDocument();
    expect(screen.getByText("3")).toBeInTheDocument();
    expect(screen.getByText("Completed in last 10")).toBeInTheDocument();

    // Spent on recent shipments: sum of 10 items = 1200
    expect(screen.getByText("Spent on recent shipments")).toBeInTheDocument();
    expect(screen.getByText("Across latest 10 parcels")).toBeInTheDocument();
    expect(screen.getByText("LKR 1,200")).toBeInTheDocument();
  });

  it("renders empty state when customer has no parcels", async () => {
    vi.mocked(listParcels).mockResolvedValue(mockPage([]));

    render(<MemoryRouter><DashboardPage /></MemoryRouter>);

    expect(await screen.findByText("Hello, Kasun.")).toBeInTheDocument();
    expect(screen.getByText("No parcels booked yet")).toBeInTheDocument();
    expect(screen.getByText("Book your first delivery")).toBeInTheDocument();
  });

  it("handles fetch error gracefully", async () => {
    vi.mocked(listParcels).mockRejectedValue(new Error("Network disconnect"));

    render(<MemoryRouter><DashboardPage /></MemoryRouter>);

    expect(await screen.findByRole("alert")).toHaveTextContent("Network disconnect");
  });
});
