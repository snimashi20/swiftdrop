import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { useAuth } from "../auth/AuthContext";
import { fetchUserProfile } from "../profile/profile.api";
import { BookParcelPage } from "./BookParcelPage";
import { BookingDraftProvider } from "./booking/BookingDraft";

vi.mock("../auth/AuthContext", () => ({ useAuth: vi.fn() }));
vi.mock("../profile/profile.api", () => ({ fetchUserProfile: vi.fn() }));

const renderAt = (path: string) => {
  vi.mocked(fetchUserProfile).mockResolvedValue({ phoneNumber: "0771234567" } as Awaited<ReturnType<typeof fetchUserProfile>>);
  vi.mocked(useAuth).mockReturnValue({ user: { name: "Isuri Hansani" } } as unknown as ReturnType<typeof useAuth>);
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/parcels/start" element={<p>Send or receive page</p>} />
        <Route
          path="/parcels/new"
          element={
            <BookingDraftProvider>
              <BookParcelPage />
            </BookingDraftProvider>
          }
        />
      </Routes>
    </MemoryRouter>,
  );
};

describe("BookParcelPage", () => {
  it("sends the customer to the send/receive page when no mode was chosen", () => {
    renderAt("/parcels/new");
    expect(screen.getByText("Send or receive page")).toBeInTheDocument();
  });

  it("shows the pick up and drop rows in send mode", () => {
    renderAt("/parcels/new?mode=send");
    expect(screen.getByText("Where to pick up and drop your parcel?")).toBeInTheDocument();
    expect(screen.getAllByText("Set location")).toHaveLength(2);
    expect(screen.getByText(/You, Isuri Hansani/)).toBeInTheDocument();
  });

  it("will not go to the next step until both locations are set", async () => {
    renderAt("/parcels/new?mode=receive");
    screen.getByRole("button", { name: "Next" }).click();
    expect(await screen.findByText("Please set both the pick up and drop locations.")).toBeInTheDocument();
  });
});
