import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useEffect } from "react";
import type { ReactNode } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { useAuth } from "../auth/AuthContext";
import { fetchUserProfile } from "../profile/profile.api";
import { BookParcelPage } from "./BookParcelPage";
import { BookingDraftProvider, useBookingDraft } from "./booking/BookingDraft";
import { createParcel, downloadReceipt, getQuote } from "./parcels.api";

vi.mock("../auth/AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("../profile/profile.api", () => ({ fetchUserProfile: vi.fn() }));
vi.mock("./parcels.api", async (importOriginal) => ({
  ...(await importOriginal<typeof import("./parcels.api")>()),
  getQuote: vi.fn(),
  createParcel: vi.fn(),
  downloadReceipt: vi.fn(),
}));

// Puts both locations into the booking draft so the test can go straight to the details step.
function Seed({ children }: { children: ReactNode }) {
  const draft = useBookingDraft();
  useEffect(() => {
    draft.setChosen("pickup", { address: { line1: "12 Galle Road", city: "Colombo", district: "Colombo" }, position: null });
    draft.setChosen("drop", { address: { line1: "5 Temple Street", city: "Kandy", district: "Kandy" }, position: null });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  return <>{children}</>;
}

const renderDetails = async (mode: "send" | "receive" = "send") => {
  vi.mocked(fetchUserProfile).mockResolvedValue({ phoneNumber: "0771234567" } as Awaited<ReturnType<typeof fetchUserProfile>>);
  vi.mocked(useAuth).mockReturnValue({ user: { name: "Isuri Hansani" } } as unknown as ReturnType<typeof useAuth>);
  render(
    <MemoryRouter initialEntries={[`/parcels/new?mode=${mode}`]}>
      <Routes>
        <Route
          path="/parcels/new"
          element={
            <BookingDraftProvider>
              <Seed>
                <BookParcelPage />
              </Seed>
            </BookingDraftProvider>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
  await userEvent.click(screen.getByRole("button", { name: "Next" }));
  await screen.findByLabelText("Receiver name");
};

describe("BookParcelPage details step", () => {
  beforeEach(() => vi.clearAllMocks());

  it("has an item type drop-down with all the types", async () => {
    await renderDetails();
    const select = screen.getByLabelText("Item type");
    const labels = Array.from(select.querySelectorAll("option")).map((o) => o.textContent);
    expect(labels).toEqual([
      "Select item type",
      "Documents",
      "Food",
      "Groceries",
      "Business Supplies / Samples",
      "Clothes",
      "Household Goods",
      "Electronics",
      "Sports Goods",
      "Other",
    ]);
  });

  it("shows an error under every wrong field and does not ask for a price", async () => {
    await renderDetails();
    await userEvent.type(screen.getByLabelText("Receiver name"), "hhhh123");
    await userEvent.type(screen.getByLabelText("Receiver phone"), "12345");
    await userEvent.clear(screen.getByLabelText("Weight (kg)"));
    await userEvent.type(screen.getByLabelText("Weight (kg)"), "-2");
    await userEvent.click(screen.getByRole("button", { name: "Next" }));

    expect(screen.getByText("The name can only have letters, spaces, . ' and -.")).toBeInTheDocument();
    expect(screen.getByText(/Enter a valid Sri Lankan number/)).toBeInTheDocument();
    expect(screen.getByText("Use a number with up to 2 decimals, like 1.5.")).toBeInTheDocument();
    expect(screen.getByText("Select the item type.")).toBeInTheDocument();
    expect(getQuote).not.toHaveBeenCalled();
  });

  it("offers the customer and the other person as the payer, with the customer first", async () => {
    await renderDetails("send");
    expect(screen.getByRole("radio", { name: /Isuri Hansani \(You\)/ })).toBeChecked();
    await userEvent.type(screen.getByLabelText("Receiver name"), "Nimal Perera");
    expect(screen.getByRole("radio", { name: /Nimal Perera/ })).not.toBeChecked();
  });

  it("books the parcel with the item type and payer, then offers the receipt download", async () => {
    vi.mocked(getQuote).mockResolvedValue({ fee: 750, pickupZone: "Colombo", dropoffZone: "Kandy", interZone: true });
    vi.mocked(createParcel).mockResolvedValue({
      id: 42,
      trackingNumber: "SDABCDEFGHIJ",
      status: "CREATED",
      fee: 750,
      createdAt: "2026-11-17T00:00:00Z",
    });
    await renderDetails("send");

    await userEvent.type(screen.getByLabelText("Receiver name"), "Nimal Perera");
    await userEvent.type(screen.getByLabelText("Receiver phone"), "077 123 4567");
    await userEvent.selectOptions(screen.getByLabelText("Item type"), "FOOD");
    await userEvent.click(screen.getByRole("radio", { name: /Nimal Perera/ }));
    await userEvent.click(screen.getByRole("button", { name: "Next" }));

    expect(await screen.findByText("Review your booking")).toBeInTheDocument();
    expect(screen.getByText("Delivery paid by")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: "Confirm booking" }));

    await waitFor(() =>
      expect(createParcel).toHaveBeenCalledWith(
        expect.objectContaining({
          receiverName: "Nimal Perera",
          receiverPhone: "0771234567",
          itemType: "FOOD",
          deliveryPaidBy: "RECEIVER",
        }),
      ),
    );

    expect(await screen.findByText("Parcel booked")).toBeInTheDocument();
    await userEvent.click(screen.getByRole("button", { name: /Download receipt/ }));
    expect(downloadReceipt).toHaveBeenCalledWith(42, "SDABCDEFGHIJ");
  });
});
