import { act, render, screen, waitFor, within } from "@testing-library/react";
import { StrictMode } from "react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { approveCourierApplication, fetchCourierApplications, rejectCourierApplication } from "../admin.api";
import type { CourierApplication, Page } from "../types";
import { CourierApprovalsPage } from "./CourierApprovalsPage";

vi.mock("../admin.api", () => ({
  fetchCourierApplications: vi.fn(),
  approveCourierApplication: vi.fn(),
  rejectCourierApplication: vi.fn(),
}));

const pendingCourier: CourierApplication = {
  id: 12,
  userId: 9,
  fullName: "Nuwan Perera",
  contactEmail: "rider@example.test",
  contactPhone: "0771234567",
  licenseNumber: "B-998877",
  vehicleType: "MOTORBIKE",
  status: "PENDING",
  createdAt: "2026-11-08T10:00:00Z",
};

const approvedCourier: CourierApplication = { ...pendingCourier, id: 13, fullName: "Approved Rider", status: "APPROVED" };
const courierPage = (content: CourierApplication[]): Page<CourierApplication> => ({ content, page: 0, size: 20, totalElements: content.length, totalPages: 1, last: true });

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

describe("CourierApprovalsPage", () => {
  beforeEach(() => {
    vi.mocked(fetchCourierApplications).mockResolvedValue(courierPage([pendingCourier]));
    vi.mocked(approveCourierApplication).mockResolvedValue({ ...pendingCourier, status: "APPROVED" });
    vi.mocked(rejectCourierApplication).mockResolvedValue({ ...pendingCourier, status: "REJECTED", rejectionReason: "No valid licence" });
  });

  it("renders pending courier applications and their actions", async () => {
    render(<CourierApprovalsPage />);

    expect(await screen.findByRole("heading", { name: "Nuwan Perera" })).toBeInTheDocument();
    expect(screen.getAllByText("PENDING").length).toBeGreaterThan(1);
    expect(screen.getByText(/rider@example\.test/)).toBeInTheDocument();
    expect(screen.getByText(/Motorbike/)).toBeInTheDocument();
    expect(screen.getByText(/B-998877/)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Approve$/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /^Reject$/i })).toBeInTheDocument();
    expect(fetchCourierApplications).toHaveBeenCalledWith("PENDING", 0);
  });

  it("reloads the list when a status filter is selected", async () => {
    vi.mocked(fetchCourierApplications).mockImplementation(async (status) => courierPage(status === "APPROVED" ? [approvedCourier] : [pendingCourier]));
    render(<CourierApprovalsPage />);
    await screen.findByRole("heading", { name: "Nuwan Perera" });
    await userEvent.setup().click(screen.getByRole("button", { name: "APPROVED" }));

    expect(await screen.findByRole("heading", { name: "Approved Rider" })).toBeInTheDocument();
    expect(fetchCourierApplications).toHaveBeenLastCalledWith("APPROVED", 0);
  });

  it("approves a courier and displays the successful status notice", async () => {
    vi.mocked(fetchCourierApplications)
      .mockResolvedValueOnce(courierPage([pendingCourier]))
      .mockResolvedValueOnce(courierPage([pendingCourier]))
      .mockResolvedValueOnce(courierPage([{ ...pendingCourier, status: "APPROVED" }]));
    render(<CourierApprovalsPage />);
    await screen.findByRole("heading", { name: "Nuwan Perera" });
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "All couriers" }));
    await waitFor(() => expect(fetchCourierApplications).toHaveBeenLastCalledWith(undefined, 0));
    await screen.findByRole("button", { name: /^Approve$/i });
    await user.click(screen.getByRole("button", { name: /^Approve$/i }));

    expect(await screen.findByRole("status")).toHaveTextContent("Nuwan Perera approved.");
    expect(approveCourierApplication).toHaveBeenCalledWith(12);
    expect(within(await screen.findByRole("article")).getByText("APPROVED")).toBeInTheDocument();
  });

  it("requests a rejection reason and sends it to the API", async () => {
    vi.mocked(fetchCourierApplications)
      .mockResolvedValueOnce(courierPage([pendingCourier]))
      .mockResolvedValueOnce(courierPage([pendingCourier]))
      .mockResolvedValueOnce(courierPage([{ ...pendingCourier, status: "REJECTED", rejectionReason: "No valid licence" }]));
    vi.spyOn(window, "prompt").mockReturnValue("No valid licence");
    render(<CourierApprovalsPage />);
    await screen.findByRole("heading", { name: "Nuwan Perera" });
    const user = userEvent.setup();
    await user.click(screen.getByRole("button", { name: "All couriers" }));
    await waitFor(() => expect(fetchCourierApplications).toHaveBeenLastCalledWith(undefined, 0));
    await screen.findByRole("button", { name: /^Reject$/i });
    await user.click(screen.getByRole("button", { name: /^Reject$/i }));

    expect(rejectCourierApplication).toHaveBeenCalledWith(12, "No valid licence");
    expect(await screen.findByRole("status")).toHaveTextContent("Nuwan Perera rejected.");
    expect(within(await screen.findByRole("article")).getByText("REJECTED")).toBeInTheDocument();
  });

  it("does not call the API when the rejection prompt is dismissed", async () => {
    vi.spyOn(window, "prompt").mockReturnValue(null);
    render(<CourierApprovalsPage />);
    await screen.findByRole("heading", { name: "Nuwan Perera" });
    await userEvent.setup().click(screen.getByRole("button", { name: /^Reject$/i }));

    expect(rejectCourierApplication).not.toHaveBeenCalled();
    expect(within(screen.getByRole("article")).getByText("PENDING")).toBeInTheDocument();
  });

  it("removes a reviewed courier from the pending filter", async () => {
    vi.mocked(fetchCourierApplications)
      .mockResolvedValueOnce(courierPage([pendingCourier]))
      .mockResolvedValueOnce(courierPage([]));
    render(<CourierApprovalsPage />);
    await screen.findByRole("heading", { name: "Nuwan Perera" });
    await userEvent.setup().click(screen.getByRole("button", { name: /^Approve$/i }));

    expect(await screen.findByRole("status")).toHaveTextContent("Nuwan Perera approved.");
    expect(await screen.findByText("No couriers found for this filter.")).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Nuwan Perera" })).not.toBeInTheDocument();
  });

  it("shows loading and empty states", async () => {
    let resolvePage!: (page: Page<CourierApplication>) => void;
    vi.mocked(fetchCourierApplications).mockReturnValue(new Promise((resolve) => { resolvePage = resolve; }));
    render(<CourierApprovalsPage />);
    expect(screen.getByText(/loading courier applications/i)).toBeInTheDocument();
    resolvePage(courierPage([]));
    await screen.findByText("No couriers found for this filter.");
  });

  it("shows request errors", async () => {
    vi.mocked(fetchCourierApplications).mockRejectedValue(new Error("Courier list unavailable"));
    render(<CourierApprovalsPage />);
    await waitFor(() => expect(screen.getByRole("alert")).toHaveTextContent("Courier list unavailable"));
  });

  it("refreshes the current ALL filter when approval finishes after the filter changes", async () => {
    const approval = deferred<CourierApplication>();
    let allRequests = 0;
    vi.mocked(approveCourierApplication).mockReturnValue(approval.promise);
    vi.mocked(fetchCourierApplications).mockImplementation(async (status) => {
      if (status !== undefined) return courierPage([pendingCourier]);
      allRequests += 1;
      return courierPage(allRequests === 1 ? [pendingCourier] : [{ ...pendingCourier, status: "APPROVED" }]);
    });
    render(<CourierApprovalsPage />);
    const user = userEvent.setup();
    await screen.findByRole("heading", { name: "Nuwan Perera" });
    await user.click(screen.getByRole("button", { name: /^Approve$/i }));
    expect(approveCourierApplication).toHaveBeenCalledOnce();

    await user.click(screen.getByRole("button", { name: "All couriers" }));
    await waitFor(() => expect(fetchCourierApplications).toHaveBeenLastCalledWith(undefined, 0));
    await screen.findByRole("heading", { name: "Nuwan Perera" });

    await act(async () => approval.resolve({ ...pendingCourier, status: "APPROVED" }));

    expect(await screen.findByRole("status")).toHaveTextContent("Nuwan Perera approved.");
    expect(fetchCourierApplications).toHaveBeenLastCalledWith(undefined, 0);
    expect(within(await screen.findByRole("article")).getByText("APPROVED")).toBeInTheDocument();
  });

  it("ignores an older list response that completes after a newer filter response", async () => {
    const olderRequest = deferred<Page<CourierApplication>>();
    const newerRequest = deferred<Page<CourierApplication>>();
    vi.mocked(fetchCourierApplications)
      .mockReturnValueOnce(olderRequest.promise)
      .mockReturnValueOnce(newerRequest.promise);
    render(<CourierApprovalsPage />);
    const user = userEvent.setup();
    expect(screen.getByText(/loading courier applications/i)).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "All couriers" }));
    await waitFor(() => expect(fetchCourierApplications).toHaveBeenCalledTimes(2));
    await act(async () => newerRequest.resolve(courierPage([approvedCourier])));
    expect(await screen.findByRole("heading", { name: "Approved Rider" })).toBeInTheDocument();

    await act(async () => olderRequest.resolve(courierPage([pendingCourier])));

    expect(screen.getByRole("heading", { name: "Approved Rider" })).toBeInTheDocument();
    expect(screen.queryByRole("heading", { name: "Nuwan Perera" })).not.toBeInTheDocument();
    expect(fetchCourierApplications).toHaveBeenLastCalledWith(undefined, 0);
  });

  it("does not send duplicate approval requests while one is pending", async () => {
    const approval = deferred<CourierApplication>();
    vi.mocked(approveCourierApplication).mockReturnValue(approval.promise);
    render(<CourierApprovalsPage />);
    const user = userEvent.setup();
    await screen.findByRole("heading", { name: "Nuwan Perera" });
    const approveButton = screen.getByRole("button", { name: /^Approve$/i });
    await user.dblClick(approveButton);

    expect(approveCourierApplication).toHaveBeenCalledOnce();
    expect(screen.getByRole("button", { name: /^Approve$/i })).toBeDisabled();
    await act(async () => approval.resolve({ ...pendingCourier, status: "APPROVED" }));
  });

  it("keeps a courier pending when the approval mutation fails", async () => {
    vi.mocked(approveCourierApplication).mockRejectedValue(new Error("Approval failed"));
    render(<CourierApprovalsPage />);
    await screen.findByRole("heading", { name: "Nuwan Perera" });
    await userEvent.setup().click(screen.getByRole("button", { name: /^Approve$/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent("Approval failed");
    expect(within(screen.getByRole("article")).getByText("PENDING")).toBeInTheDocument();
    expect(fetchCourierApplications).toHaveBeenCalledOnce();
    expect(approveCourierApplication).toHaveBeenCalledOnce();
  });

  it("ignores a courier list response that resolves after unmount", async () => {
    const listRequest = deferred<Page<CourierApplication>>();
    vi.mocked(fetchCourierApplications).mockReturnValue(listRequest.promise);
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = render(<CourierApprovalsPage />);
    expect(screen.getByText(/loading courier applications/i)).toBeInTheDocument();

    unmount();
    await act(async () => listRequest.resolve(courierPage([pendingCourier])));

    expect(fetchCourierApplications).toHaveBeenCalledOnce();
    expect(consoleError).not.toHaveBeenCalled();
  });

  it("does not refresh or update state when approval resolves after unmount", async () => {
    const approval = deferred<CourierApplication>();
    vi.mocked(approveCourierApplication).mockReturnValue(approval.promise);
    const consoleError = vi.spyOn(console, "error").mockImplementation(() => {});
    const { unmount } = render(<CourierApprovalsPage />);
    await screen.findByRole("heading", { name: "Nuwan Perera" });
    await userEvent.setup().click(screen.getByRole("button", { name: /^Approve$/i }));

    unmount();
    await act(async () => approval.resolve({ ...pendingCourier, status: "APPROVED" }));

    expect(approveCourierApplication).toHaveBeenCalledOnce();
    expect(fetchCourierApplications).toHaveBeenCalledOnce();
    expect(consoleError).not.toHaveBeenCalled();
  });

  it("loads the list after Strict Mode replays effect setup and cleanup", async () => {
    render(<StrictMode><CourierApprovalsPage /></StrictMode>);

    expect(await screen.findByRole("heading", { name: "Nuwan Perera" })).toBeInTheDocument();
    expect(fetchCourierApplications).toHaveBeenCalled();
  });
});
