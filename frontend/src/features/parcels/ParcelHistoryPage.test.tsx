import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "../auth/AuthContext";
import { ParcelHistoryPage } from "./ParcelHistoryPage";
import { cancelParcel, downloadReceipt, listParcels } from "./parcels.api";
import type { ParcelDetail, ParcelPage } from "./parcels.api";

vi.mock("../auth/AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("./parcels.api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./parcels.api")>()),
  listParcels: vi.fn(),
  cancelParcel: vi.fn(),
  downloadReceipt: vi.fn(),
}));

const parcel = (over: Partial<ParcelDetail>): ParcelDetail =>
  ({
    id: 1,
    trackingNumber: "SDAAAAAAAAAA",
    status: "CREATED",
    pickup: { line1: "1 Road", city: "Colombo", district: "Colombo" },
    dropoff: { line1: "2 Road", city: "Galle", district: "Galle" },
    receiverName: "Saman Palitha",
    receiverPhone: "0771234567",
    size: "M",
    weightKg: 1,
    fee: 850,
    createdAt: "2026-11-19T04:00:00Z",
    updatedAt: "2026-11-19T04:00:00Z",
    ...over,
  }) as ParcelDetail;

const pageOf = (...content: ParcelDetail[]): ParcelPage =>
  ({ content, number: 0, totalPages: 1, totalElements: content.length, size: 10 }) as ParcelPage;

const renderPage = (role = "CUSTOMER") => {
  vi.mocked(useAuth).mockReturnValue({ user: { name: "Isuri", role } } as unknown as ReturnType<typeof useAuth>);
  return render(
    <MemoryRouter>
      <ParcelHistoryPage />
    </MemoryRouter>,
  );
};

describe("ParcelHistoryPage", () => {
  beforeEach(() => vi.clearAllMocks());

  it("shows each parcel with its route, receiver, fee and customer-friendly status", async () => {
    vi.mocked(listParcels).mockResolvedValue(
      pageOf(parcel({ itemType: "FOOD", deliveryPaidBy: "RECEIVER" }), parcel({ id: 2, trackingNumber: "SDBBBBBBBBBB", status: "OUT_FOR_DELIVERY" })),
    );
    renderPage();

    expect(await screen.findByText("SDAAAAAAAAAA")).toBeInTheDocument();
    expect(screen.getAllByText(/Colombo\s*Galle/)).toHaveLength(2);
    expect(screen.getAllByText("For Saman Palitha")).toHaveLength(2);
    expect(screen.getAllByText("Rs. 850.00")).toHaveLength(2);
    expect(screen.getByText("Food")).toBeInTheDocument();
    expect(screen.getByText("Paid by receiver")).toBeInTheDocument();
    // "Booked" is the customer's word for CREATED; it is on the filter chip and on the parcel.
    expect(screen.getAllByText("Booked").length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText("Out for delivery").length).toBeGreaterThanOrEqual(2);
  });

  it("shows the town with its district when they differ", async () => {
    vi.mocked(listParcels).mockResolvedValue(
      pageOf(
        parcel({
          pickup: { line1: "1 Road", city: "Moratuwa", district: "Colombo" },
          dropoff: { line1: "2 Road", city: "Kelaniya", district: "Gampaha" },
        }),
      ),
    );
    renderPage();

    expect(await screen.findByText(/Moratuwa, Colombo/)).toBeInTheDocument();
    expect(screen.getByText(/Kelaniya, Gampaha/)).toBeInTheDocument();
  });

  it("filters by the chosen status and starts again from the first page", async () => {
    vi.mocked(listParcels).mockResolvedValue(pageOf(parcel({})));
    renderPage();
    await screen.findByText("SDAAAAAAAAAA");

    await userEvent.click(screen.getByRole("button", { name: "Delivered" }));

    await waitFor(() => expect(listParcels).toHaveBeenLastCalledWith(0, "DELIVERED"));
    expect(screen.getByRole("button", { name: "Delivered" })).toHaveAttribute("aria-pressed", "true");
  });

  it("offers every real status in journey order", async () => {
    vi.mocked(listParcels).mockResolvedValue(pageOf(parcel({})));
    renderPage();
    await screen.findByText("SDAAAAAAAAAA");

    const group = screen.getByRole("group", { name: "Filter by status" });
    const names = within(group).getAllByRole("button").map((b) => b.textContent);
    expect(names).toEqual(["All", "Booked", "Picked up", "In transit", "Out for delivery", "Delivered", "Failed", "Cancelled"]);
  });

  it("asks for a reason before cancelling, and only a booked parcel can be cancelled", async () => {
    vi.mocked(listParcels).mockResolvedValue(
      pageOf(parcel({}), parcel({ id: 2, trackingNumber: "SDBBBBBBBBBB", status: "IN_TRANSIT" })),
    );
    vi.mocked(cancelParcel).mockResolvedValue(parcel({ status: "CANCELLED" }));
    renderPage();
    await screen.findByText("SDAAAAAAAAAA");

    expect(screen.getAllByRole("button", { name: "Cancel" })).toHaveLength(1);
    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(screen.getByText("Why are you cancelling this parcel?")).toBeInTheDocument();

    // cannot confirm until a reason is picked
    const confirm = screen.getByRole("button", { name: "Confirm cancellation" });
    expect(confirm).toBeDisabled();
    expect(cancelParcel).not.toHaveBeenCalled();

    await userEvent.click(screen.getByLabelText("Wrong address or receiver details"));
    expect(confirm).toBeEnabled();
    await userEvent.click(confirm);

    await waitFor(() => expect(cancelParcel).toHaveBeenCalledWith(1, "WRONG_DETAILS", ""));
    await waitFor(() => expect(listParcels).toHaveBeenCalledTimes(2));
  });

  it("needs a written reason when the customer picks Other", async () => {
    vi.mocked(listParcels).mockResolvedValue(pageOf(parcel({})));
    vi.mocked(cancelParcel).mockResolvedValue(parcel({ status: "CANCELLED" }));
    renderPage();
    await screen.findByText("SDAAAAAAAAAA");

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    await userEvent.click(screen.getByLabelText("Other"));
    const confirm = screen.getByRole("button", { name: "Confirm cancellation" });
    expect(confirm).toBeDisabled();

    await userEvent.type(screen.getByLabelText("Tell us more"), "Receiver is travelling");
    expect(confirm).toBeEnabled();
    await userEvent.click(confirm);

    await waitFor(() => expect(cancelParcel).toHaveBeenCalledWith(1, "OTHER", "Receiver is travelling"));
  });

  it("can change its mind about cancelling", async () => {
    vi.mocked(listParcels).mockResolvedValue(pageOf(parcel({})));
    renderPage();
    await screen.findByText("SDAAAAAAAAAA");

    await userEvent.click(screen.getByRole("button", { name: "Cancel" }));
    await userEvent.click(screen.getByRole("button", { name: "Keep parcel" }));

    expect(cancelParcel).not.toHaveBeenCalled();
    expect(screen.getByRole("button", { name: "Cancel" })).toBeInTheDocument();
  });

  it("shows why a parcel was cancelled", async () => {
    vi.mocked(listParcels).mockResolvedValue(
      pageOf(parcel({ status: "CANCELLED", cancelReason: "OTHER", cancelComment: "Receiver is travelling" })),
    );
    renderPage();

    expect(await screen.findByText("Reason: Other - Receiver is travelling")).toBeInTheDocument();
  });

  it("downloads the receipt, but not for a cancelled parcel", async () => {
    vi.mocked(listParcels).mockResolvedValue(
      pageOf(parcel({}), parcel({ id: 2, trackingNumber: "SDBBBBBBBBBB", status: "CANCELLED" })),
    );
    renderPage();
    await screen.findByText("SDAAAAAAAAAA");

    const buttons = screen.getAllByRole("button", { name: /Download receipt/ });
    expect(buttons[1]).toBeDisabled();
    // Only the active parcel can be tracked
    expect(screen.getAllByRole("link", { name: "Track" })).toHaveLength(1);
    await userEvent.click(buttons[0]);
    expect(downloadReceipt).toHaveBeenCalledWith(1, "SDAAAAAAAAAA");
  });

  it("shows a helpful empty state, with a way to clear the filter", async () => {
    vi.mocked(listParcels).mockResolvedValue(pageOf());
    renderPage();
    expect(await screen.findByText("No parcels yet")).toBeInTheDocument();

    await userEvent.click(screen.getByRole("button", { name: "Failed" }));
    expect(await screen.findByText("No failed parcels")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Clear filter" }));
    await waitFor(() => expect(listParcels).toHaveBeenLastCalledWith(0, undefined));
  });

  it("tells other roles that history is for customers", () => {
    renderPage("MERCHANT");
    expect(screen.getByText(/available to customer accounts/)).toBeInTheDocument();
    expect(listParcels).not.toHaveBeenCalled();
  });
});
