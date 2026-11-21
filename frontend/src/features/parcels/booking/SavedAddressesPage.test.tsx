import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { BookingDraftProvider } from "./BookingDraft";
import { SavedAddressesPage } from "./SavedAddressesPage";
import { fetchUserAddresses } from "../../profile/profile.api";

vi.mock("../../profile/profile.api", () => ({ fetchUserAddresses: vi.fn() }));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return { ...actual, useNavigate: () => mockNavigate };
});

const renderPage = () =>
  render(
    <MemoryRouter>
      <BookingDraftProvider>
        <SavedAddressesPage />
      </BookingDraftProvider>
    </MemoryRouter>,
  );

describe("SavedAddressesPage", () => {
  beforeEach(() => vi.clearAllMocks());

  it("fills the address and returns to the booking page when one is picked", async () => {
    vi.mocked(fetchUserAddresses).mockResolvedValue([
      { id: 1, addressLabel: "Home", streetAddress: "12 Galle Road", city: "Colombo", district: "Colombo" },
    ]);
    renderPage();
    await userEvent.click(await screen.findByText("Home"));
    expect(mockNavigate).toHaveBeenCalledWith("/parcels/new");
  });

  it("does not allow an address outside the delivery districts", async () => {
    vi.mocked(fetchUserAddresses).mockResolvedValue([
      { id: 2, addressLabel: "Far away", streetAddress: "1 Main St", city: "Nowhere", district: "Atlantis" },
    ]);
    renderPage();
    const button = (await screen.findByText("Far away")).closest("button");
    expect(button).toBeDisabled();
    expect(screen.getByText(/do not deliver to Atlantis/i)).toBeInTheDocument();
    await userEvent.click(button as HTMLElement);
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it("shows a message when there are no saved addresses", async () => {
    vi.mocked(fetchUserAddresses).mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(screen.getByText(/no saved addresses yet/i)).toBeInTheDocument());
  });
});
