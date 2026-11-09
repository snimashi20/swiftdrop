import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { fetchDashboardOrderSummary, fetchDashboardOrders } from "../api";
import type { MerchantOrder, Page } from "../types";
import { MerchantOrdersPage } from "./MerchantOrdersPage";

vi.mock("../api", () => ({
  fetchDashboardOrderSummary: vi.fn(),
  fetchDashboardOrders: vi.fn(),
}));

const orders: MerchantOrder[] = [
  { trackingNumber: "SD-TEST-1001", status: "IN_TRANSIT", fee: 1250, createdAt: "2026-11-08T10:00:00Z", externalOrderId: "SHOP-44" },
  { trackingNumber: "SD-TEST-1002", status: "DELIVERED", fee: 875.5, createdAt: "2026-11-07T10:00:00Z" },
];
const orderPage = (content: MerchantOrder[]): Page<MerchantOrder> => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, last: true });
const summary = { CREATED: 1, PICKED_UP: 2, IN_TRANSIT: 4, OUT_FOR_DELIVERY: 3, DELIVERED: 7, FAILED: 1 };

describe("MerchantOrdersPage", () => {
  beforeEach(() => {
    vi.mocked(fetchDashboardOrders).mockResolvedValue(orderPage(orders));
    vi.mocked(fetchDashboardOrderSummary).mockResolvedValue(summary);
  });

  it("shows a loading state while the dashboard requests are pending", async () => {
    let resolveOrders!: (page: Page<MerchantOrder>) => void;
    let resolveSummary!: (counts: Record<string, number>) => void;
    vi.mocked(fetchDashboardOrders).mockReturnValue(new Promise((resolve) => { resolveOrders = resolve; }));
    vi.mocked(fetchDashboardOrderSummary).mockReturnValue(new Promise((resolve) => { resolveSummary = resolve; }));
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);

    expect(await screen.findByText(/loading orders/i)).toBeInTheDocument();
    resolveOrders(orderPage([]));
    resolveSummary(summary);
    await screen.findByText("No matching orders yet");
  });

  it("shows the empty state when no orders match", async () => {
    vi.mocked(fetchDashboardOrders).mockResolvedValue(orderPage([]));
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);

    expect(await screen.findByText("No matching orders yet")).toBeInTheDocument();
    expect(screen.getByText("0 orders")).toBeInTheDocument();
  });

  it("renders order details and status summary counts", async () => {
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);

    expect(await screen.findByRole("link", { name: "SD-TEST-1001" })).toHaveAttribute("href", "/track/SD-TEST-1001");
    expect(screen.getAllByText("IN TRANSIT").length).toBeGreaterThan(1);
    expect(screen.getByText(/Store order SHOP-44/)).toBeInTheDocument();
    expect(screen.getByText("LKR 875.50")).toBeInTheDocument();
    const deliveredSummary = screen.getByText("DELIVERED", { selector: "p" }).parentElement;
    expect(within(deliveredSummary!).getByText("7")).toBeInTheDocument();
    expect(fetchDashboardOrderSummary).toHaveBeenCalledOnce();
  });

  it("filters by status and requests the selected page data", async () => {
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);
    await screen.findByRole("link", { name: "SD-TEST-1001" });
    await userEvent.setup().click(screen.getByRole("button", { name: "DELIVERED" }));

    await waitFor(() => expect(fetchDashboardOrders).toHaveBeenLastCalledWith("DELIVERED", 0));
  });

  it("shows errors from the dashboard API", async () => {
    vi.mocked(fetchDashboardOrders).mockRejectedValue(new Error("Orders service unavailable"));
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);

    expect(await screen.findByRole("alert")).toHaveTextContent("Orders service unavailable");
  });
});
