import { act, render, screen, waitFor, within } from "@testing-library/react";
import { StrictMode } from "react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { approveMerchant, fetchMerchants, rejectMerchant } from "../api";
import type { Merchant, Page } from "../types";
import { MerchantApprovalsPage } from "./MerchantApprovalsPage";

vi.mock("../api", () => ({
  fetchMerchants: vi.fn(),
  approveMerchant: vi.fn(),
  rejectMerchant: vi.fn(),
}));

const pendingMerchant: Merchant = {
  id: 12,
  businessName: "Northstar Goods",
  registrationNumber: "BR-123456",
  contactName: "Alex Morgan",
  contactEmail: "orders@example.test",
  contactPhone: "0771234567",
  status: "PENDING",
  createdAt: "2026-11-08T10:00:00Z",
};

const approvedMerchant: Merchant = { ...pendingMerchant, id: 13, businessName: "Approved Shop", status: "APPROVED" };
const merchantPage = (content: Merchant[]): Page<Merchant> => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, last: true });

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

describe("MerchantApprovalsPage", () => {
  beforeEach(() => {
    vi.mocked(fetchMerchants).mockResolvedValue(merchantPage([pendingMerchant]));
    vi.mocked(approveMerchant).mockResolvedValue({ ...pendingMerchant, status: "APPROVED" });
    vi.mocked(rejectMerchant).mockResolvedValue({ ...pendingMerchant, status: "REJECTED", rejectionReason: "Incomplete details" });
  });

  it("renders pending merchant applications and their actions", async () => {
    render(<MerchantApprovalsPage />);

    expect(await screen.findByRole("heading", { name: "Northstar Goods" })).toBeInTheDocument();
    expect(screen.getAllByText("PENDING").length).toBeGreaterThan(1);
    expect(screen.getByText(/orders@example\.test/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Approve$/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Reject$/i })).toBeInTheDocument();
    expect(fetchMerchants).toHaveBeenCalledWith("PENDING", 0);
  });

  it("reloads the list when a status filter is selected", async () => {
    vi.mocked(fetchMerchants).mockImplementation(async (status) => merchantPage(status === "APPROVED" ? [approvedMerchant] : [pendingMerchant]));
    render(<MerchantApprovalsPage />);
    await screen.findByRole("heading", { name: "Northstar Goods" });
    await userEvent.setup().click(screen.getByRole("button", { name: "APPROVED" }));

    expect(await screen.findByRole("heading", { name: "Approved Shop" })).toBeInTheDocument();
    expect(fetchMerchants).toHaveBeenLastCalledWith("APPROVED", 0);
  });

  it("approves a merchant and displays the successful status notice", async () => {
    vi.mocked(fetchMerchants)
      .mockResolvedValueOnce(merchantPage([pendingMerchant]))
      .mockResolvedValueOnce(merchantPage([pendingMerchant]))
      .mockResolvedValueOnce(merchantPage([{ ...pendingMerchant, status: "APPROVED" }]));
    render(<MerchantApprovalsPage />);
    await screen.findByRole("heading", { name: "Northstar Goods" });
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "All merchants" }));
    await waitFor(() => expect(fetchMerchants).toHaveBeenLastCalledWith(undefined, 0));
    await screen.findByRole("button", { name: /^Approve$/i });
    await user.click(screen.getByRole("button", { name: /^Approve$/i }));

    expect(await screen.findByRole("status")).toHaveTextContent("Northstar Goods approved.");
    expect(approveMerchant).toHaveBeenCalledWith(12);
    expect(within(await screen.findByRole("article")).getByText("APPROVED")).toBeInTheDocument();
  });

  it("requests a rejection reason and sends it to the API", async () => {
    vi.mocked(fetchMerchants)
      .mockResolvedValueOnce(merchantPage([pendingMerchant]))
      .mockResolvedValueOnce(merchantPage([pendingMerchant]))
      .mockResolvedValueOnce(merchantPage([{ ...pendingMerchant, status: "REJECTED", rejectionReason: "Incomplete details" }]));
    vi.spyOn(window, "prompt").mockReturnValue("Incomplete details");
    render(<MerchantApprovalsPage />);
    await screen.findByRole("heading", { name: "Northstar Goods" });
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "All merchants" }));
    await waitFor(() => expect(fetchMerchants).toHaveBeenLastCalledWith(undefined, 0));
    await screen.findByRole("button", { name: /^Reject$/i });
    await user.click(screen.getByRole("button", { name: /^Reject$/i }));

    expect(rejectMerchant).toHaveBeenCalledWith(12, "Incomplete details");
    expect(await screen.findByRole("status")).toHaveTextContent("Northstar Goods rejected.");
    expect(within(await screen.findByRole("article")).getByText("REJECTED")).toBeInTheDocument();
  });

  it("removes a reviewed merchant from the pending filter", async () => {
    vi.mocked(fetchMerchants)
      .mockResolvedValueOnce(merchantPage([pendingMerchant]))
      .mockResolvedValueOnce(merchantPage([]));
    render(<MerchantApprovalsPage />);
    await screen.findByRole("heading", { name: "Northstar Goods" });
    await userEvent.setup().click(screen.getByRole("button", { name: /^Approve$/i }));

    expect(await screen.findByRole("status")).toHaveTextContent("Northstar Goods approved.");
    expect(await screen.findByText("No merchants found for this filter.")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Northstar Goods" })).not.toBeInTheDocument();
  });

  it("shows loading and empty states", async () => {
    let resolvePage!: (page: Page<Merchant>) => void;
    vi.mocked(fetchMerchants).mockReturnValue(new Promise((resolve) => { resolvePage = resolve; }));
    render(<MerchantApprovalsPage />);
    expect(screen.getByText(/loading merchant applications/i)).toBeInTheDocument();
    resolvePage(merchantPage([]));
    await screen.findByText("No merchants found for this filter.");
  });

  it("shows request errors", async () => {
    vi.mocked(fetchMerchants).mockRejectedValue(new Error("Merchant list unavailable"));
    render(<MerchantApprovalsPage />);
    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("Merchant list unavailable"));
  });

  it("refreshes the current ALL filter when approval finishes after the filter changes", async () => {
    const approval = deferred<Merchant>();
    let allRequests = 0;
    vi.mocked(approveMerchant).mockReturnValue(approval.promise);
    vi.mocked(fetchMerchants).mockImplementation(async (status) => {
      if (status !== undefined) return merchantPage([pendingMerchant]);
      allRequests += 1;
      return merchantPage(allRequests === 1 ? [pendingMerchant] : [{ ...pendingMerchant, status: "APPROVED" }]);
    });
    render(<MerchantApprovalsPage />);
    const user = userEvent.setup();
    await screen.findByRole("heading", { name: "Northstar Goods" });
    await user.click(screen.getByRole("button", { name: /^Approve$/i }));
    expect(approveMerchant).toHaveBeenCalledOnce();

    await user.click(screen.getByRole("button", { name: "All merchants" }));
    await waitFor(() => expect(fetchMerchants).toHaveBeenLastCalledWith(undefined, 0));
    await screen.findByRole("heading", { name: "Northstar Goods" });

    await act(async () => approval.resolve({ ...pendingMerchant, status: "APPROVED" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Northstar Goods approved.");
    expect(fetchMerchants).toHaveBeenLastCalledWith(undefined, 0);
    expect(within(await screen.findByRole("article")).getByText("APPROVED")).toBeInTheDocument();
  });

  it("refreshes the current REJECTED filter when rejection finishes after the filter changes", async () => {
    const rejection = deferred<Merchant>();
    let rejectedRequests = 0;
    vi.spyOn(window, "prompt").mockReturnValue("Incomplete details");
    vi.mocked(rejectMerchant).mockReturnValue(rejection.promise);
    vi.mocked(fetchMerchants).mockImplementation(async (status) => {
      if (status !== "REJECTED") return merchantPage([pendingMerchant]);
      rejectedRequests += 1;
      return merchantPage(rejectedRequests === 1 ? [] : [{ ...pendingMerchant, status: "REJECTED", rejectionReason: "Incomplete details" }]);
    });
    render(<MerchantApprovalsPage />);
    const user = userEvent.setup();
    await screen.findByRole("heading", { name: "Northstar Goods" });
    await user.click(screen.getByRole("button", { name: /^Reject$/i }));
    expect(rejectMerchant).toHaveBeenCalledOnce();

    await user.click(screen.getByRole("button", { name: "REJECTED" }));
    expect(await screen.findByText("No merchants found for this filter.")).toBeInTheDocument();
    await act(async () => rejection.resolve({ ...pendingMerchant, status: "REJECTED", rejectionReason: "Incomplete details" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Northstar Goods rejected.");
    expect(fetchMerchants).toHaveBeenLastCalledWith("REJECTED", 0);
    expect(within(await screen.findByRole("article")).getByText("REJECTED")).toBeInTheDocument();
  });

  it("ignores an older list response that completes after a newer filter response", async () => {
    const olderRequest = deferred<Page<Merchant>>();
    const newerRequest = deferred<Page<Merchant>>();
    vi.mocked(fetchMerchants)
      .mockReturnValueOnce(olderRequest.promise)
      .mockReturnValueOnce(newerRequest.promise);
    render(<MerchantApprovalsPage />);
    const user = userEvent.setup();
    expect(screen.getByText(/loading merchant applications/i)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "All merchants" }));
    await waitFor(() => expect(fetchMerchants).toHaveBeenCalledTimes(2));
    await act(async () => newerRequest.resolve(merchantPage([approvedMerchant])));
    expect(await screen.findByRole("heading", { name: "Approved Shop" })).toBeInTheDocument();

    await act(async () => olderRequest.resolve(merchantPage([pendingMerchant])));

    expect(screen.getByRole("heading", { name: "Approved Shop" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Northstar Goods" })).not.toBeInTheDocument();
    expect(fetchMerchants).toHaveBeenLastCalledWith(undefined, 0);
  });

  it("does not send duplicate approval requests while one is pending", async () => {
    const approval = deferred<Merchant>();
    vi.mocked(approveMerchant).mockReturnValue(approval.promise);
    render(<MerchantApprovalsPage />);
    const user = userEvent.setup();
    await screen.findByRole("heading", { name: "Northstar Goods" });
    const approveButton = screen.getByRole("button", { name: /^Approve$/i });
    await user.dblClick(approveButton);

    expect(approveMerchant).toHaveBeenCalledOnce();
    expect(screen.getByRole("button", { name: /^Approve$/i })).toBeDisabled();
    await act(async () => approval.resolve({ ...pendingMerchant, status: "APPROVED" }));
  });

  it("keeps a merchant pending when the approval mutation fails", async () => {
    vi.mocked(approveMerchant).mockRejectedValue(new Error("Approval failed"));
    render(<MerchantApprovalsPage />);
    await screen.findByRole("heading", { name: "Northstar Goods" });
    await userEvent.setup().click(screen.getByRole("button", { name: /^Approve$/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Approval failed");
    expect(within(screen.getByRole("article")).getByText("PENDING")).toBeInTheDocument();
    expect(fetchMerchants).toHaveBeenCalledOnce();
    expect(approveMerchant).toHaveBeenCalledOnce();
  });

  it("ignores a merchant list response that resolves after unmount", async () => {
    const listRequest = deferred<Page<Merchant>>();
    vi.mocked(fetchMerchants).mockReturnValue(listRequest.promise);
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = render(<MerchantApprovalsPage />);
    expect(screen.getByText(/loading merchant applications/i)).toBeInTheDocument();

    unmount();
    await act(async () => listRequest.resolve(merchantPage([pendingMerchant])));

    expect(fetchMerchants).toHaveBeenCalledOnce();
    expect(consoleError).not.toHaveBeenCalled();
  });

  it("does not refresh or update state when approval resolves after unmount", async () => {
    const approval = deferred<Merchant>();
    vi.mocked(approveMerchant).mockReturnValue(approval.promise);
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = render(<MerchantApprovalsPage />);
    await screen.findByRole("heading", { name: "Northstar Goods" });
    await userEvent.setup().click(screen.getByRole("button", { name: /^Approve$/i }));

    unmount();
    await act(async () => approval.resolve({ ...pendingMerchant, status: "APPROVED" }));

    expect(approveMerchant).toHaveBeenCalledOnce();
    expect(fetchMerchants).toHaveBeenCalledOnce();
    expect(consoleError).not.toHaveBeenCalled();
  });

  it("does not refresh or update state when rejection resolves after unmount", async () => {
    const rejection = deferred<Merchant>();
    vi.spyOn(window, "prompt").mockReturnValue("Incomplete details");
    vi.mocked(rejectMerchant).mockReturnValue(rejection.promise);
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = render(<MerchantApprovalsPage />);
    await screen.findByRole("heading", { name: "Northstar Goods" });
    await userEvent.setup().click(screen.getByRole("button", { name: /^Reject$/i }));

    unmount();
    await act(async () => rejection.resolve({ ...pendingMerchant, status: "REJECTED", rejectionReason: "Incomplete details" }));

    expect(rejectMerchant).toHaveBeenCalledWith(12, "Incomplete details");
    expect(fetchMerchants).toHaveBeenCalledOnce();
    expect(consoleError).not.toHaveBeenCalled();
  });

  it("loads the list after Strict Mode replays effect setup and cleanup", async () => {
    render(<StrictMode><MerchantApprovalsPage /></StrictMode>);

    expect(await screen.findByRole("heading", { name: "Northstar Goods" })).toBeInTheDocument();
    expect(fetchMerchants).toHaveBeenCalled();
  });
});
