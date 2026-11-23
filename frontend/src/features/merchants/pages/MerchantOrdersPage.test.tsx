import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { fetchDashboardOrders } from "../api";
import type { MerchantOrder, Page } from "../types";
import { MerchantOrdersPage } from "./MerchantOrdersPage";

vi.mock("../api", () => ({
  fetchDashboardOrders: vi.fn(),
}));

const orders: MerchantOrder[] = [
  { trackingNumber: "SD-TEST-1001", status: "IN_TRANSIT", fee: 1250, createdAt: "2026-11-08T10:00:00Z", externalOrderId: "SHOP-44" },
  { trackingNumber: "SD-TEST-1002", status: "DELIVERED", fee: 875.5, createdAt: "2026-11-07T10:00:00Z" },
];
const orderPage = (content: MerchantOrder[]): Page<MerchantOrder> => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, last: true });
describe("MerchantOrdersPage", () => {
  beforeEach(() => {
    vi.mocked(fetchDashboardOrders).mockResolvedValue(orderPage(orders));
  });

  it("shows a loading state while the dashboard requests are pending", async () => {
    let resolveOrders!: (page: Page<MerchantOrder>) => void;
    vi.mocked(fetchDashboardOrders).mockReturnValue(new Promise((resolve) => { resolveOrders = resolve; }));
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);

    expect(await screen.findByText(/loading orders/i)).toBeInTheDocument();
    resolveOrders(orderPage([]));
    await screen.findByText("No matching orders yet");
  });

  it("shows the empty state when no orders match", async () => {
    vi.mocked(fetchDashboardOrders).mockResolvedValue(orderPage([]));
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);

    expect(await screen.findByText("No matching orders yet")).toBeInTheDocument();
    expect(screen.getByText("0 orders")).toBeInTheDocument();
  });

  it("renders order details without duplicating overview summaries", async () => {
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);

    expect(await screen.findByRole("link", { name: "SD-TEST-1001" })).toHaveAttribute("href", "/track/SD-TEST-1001");
    expect(screen.getAllByText("IN TRANSIT").length).toBeGreaterThan(1);
    expect(screen.getByText(/Store order SHOP-44/)).toBeInTheDocument();
    expect(screen.getByText("LKR 875.50")).toBeInTheDocument();
    expect(screen.queryByText("all your orders")).not.toBeInTheDocument();
  });

  it("filters by status and requests the selected page data", async () => {
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);
    await screen.findByRole("link", { name: "SD-TEST-1001" });
    await userEvent.setup().click(screen.getByRole("button", { name: "DELIVERED" }));

    await waitFor(() => expect(fetchDashboardOrders).toHaveBeenLastCalledWith("DELIVERED", 0, undefined, "DESC"));
  });

  it("searches by order ID and sorts from oldest to newest", async () => {
    const user = userEvent.setup();
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);
    await screen.findByRole("link", { name: "SD-TEST-1001" });

    await user.type(screen.getByRole("searchbox", { name: "Search by order ID" }), "SHOP-44");
    await waitFor(() => expect(fetchDashboardOrders).toHaveBeenLastCalledWith(undefined, 0, "SHOP-44", "DESC"));

    await user.selectOptions(screen.getByRole("combobox", { name: "Sort orders by date" }), "ASC");
    await waitFor(() => expect(fetchDashboardOrders).toHaveBeenLastCalledWith(undefined, 0, "SHOP-44", "ASC"));
  });

  it("allows filtering cancelled merchant orders", async () => {
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);
    await screen.findByRole("link", { name: "SD-TEST-1001" });
    await userEvent.setup().click(screen.getByRole("button", { name: "CANCELLED" }));

    await waitFor(() => expect(fetchDashboardOrders).toHaveBeenLastCalledWith("CANCELLED", 0, undefined, "DESC"));
  });

  it("shows errors from the dashboard API", async () => {
    vi.mocked(fetchDashboardOrders).mockRejectedValue(new Error("Orders service unavailable"));
    render(<MemoryRouter><MerchantOrdersPage /></MemoryRouter>);

    expect(await screen.findByRole("alert")).toHaveTextContent("Orders service unavailable");
  });
});
