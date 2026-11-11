import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { notificationsApi } from "../dispatch/api";
import type { MyNotification, MyNotificationPage } from "../dispatch/types";
import { MyNotificationsPage } from "./MyNotificationsPage";

vi.mock("../dispatch/api", () => ({
  notificationsApi: {
    myFeed: vi.fn(),
    myUnreadCount: vi.fn(),
    markAllRead: vi.fn(),
    markRead: vi.fn(),
  },
}));

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
  return { ...actual, useNavigate: () => mockNavigate };
});

const makeItem = (overrides: Partial<MyNotification> = {}): MyNotification => ({
  id: 1,
  parcelId: 100,
  trackingNumber: "SD-0001",
  oldStatus: "PICKED_UP",
  newStatus: "IN_TRANSIT",
  message: "Your parcel is now in transit",
  read: false,
  readAt: null,
  createdAt: new Date().toISOString(),
  ...overrides,
});

const makePage = (content: MyNotification[], extra: Partial<MyNotificationPage> = {}): MyNotificationPage => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  last: true,
  ...extra,
});

describe("MyNotificationsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockNavigate.mockReset();
    vi.mocked(notificationsApi.myUnreadCount).mockResolvedValue({ unreadCount: 1 });
    vi.mocked(notificationsApi.myFeed).mockResolvedValue(makePage([makeItem()]));
    vi.mocked(notificationsApi.markAllRead).mockResolvedValue({ updated: 1 });
    vi.mocked(notificationsApi.markRead).mockResolvedValue({ updated: 1 });
  });

  const renderPage = () =>
    render(<MemoryRouter><MyNotificationsPage /></MemoryRouter>);

  it("renders the notification feed", async () => {
    renderPage();
    expect(await screen.findByText("Your parcel is now in transit")).toBeInTheDocument();
    expect(screen.getByText("SD-0001")).toBeInTheDocument();
  });

  it("shows empty state when there are no notifications", async () => {
    vi.mocked(notificationsApi.myFeed).mockResolvedValue(makePage([]));
    vi.mocked(notificationsApi.myUnreadCount).mockResolvedValue({ unreadCount: 0 });
    renderPage();
    expect(await screen.findByText(/nothing yet/i)).toBeInTheDocument();
  });

  it("toggles unread-only filter and resets page", async () => {
    renderPage();
    await screen.findByText("Your parcel is now in transit");

    await userEvent.setup().click(screen.getByRole("button", { name: /show unread only/i }));

    await waitFor(() =>
      expect(notificationsApi.myFeed).toHaveBeenLastCalledWith(
        expect.objectContaining({ page: 0, unreadOnly: true }),
      ),
    );
  });

  it("marks all as read and dispatches sync event", async () => {
    const dispatch = vi.spyOn(window, "dispatchEvent");
    renderPage();
    await screen.findByText("Your parcel is now in transit");

    await userEvent.setup().click(screen.getByRole("button", { name: /mark all as read/i }));

    expect(notificationsApi.markAllRead).toHaveBeenCalledOnce();
    await waitFor(() =>
      expect(dispatch).toHaveBeenCalledWith(expect.objectContaining({ type: "notifications:read" })),
    );
    dispatch.mockRestore();
  });

  it("clicks a notification, marks it read, and navigates to the parcel", async () => {
    renderPage();
    await userEvent.setup().click(await screen.findByText("Your parcel is now in transit"));

    expect(notificationsApi.markRead).toHaveBeenCalledWith(1);
    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith("/parcels/SD-0001"));
  });

  it("shows the mark-all-read button based on global unread count", async () => {
    vi.mocked(notificationsApi.myFeed).mockResolvedValue(
      makePage([makeItem({ read: true })]),
    );
    vi.mocked(notificationsApi.myUnreadCount).mockResolvedValue({ unreadCount: 3 });
    renderPage();

    expect(await screen.findByRole("button", { name: /mark all as read/i })).toBeInTheDocument();
  });

  it("hides mark-all-read when global unread is zero", async () => {
    vi.mocked(notificationsApi.myFeed).mockResolvedValue(
      makePage([makeItem({ read: true })]),
    );
    vi.mocked(notificationsApi.myUnreadCount).mockResolvedValue({ unreadCount: 0 });
    renderPage();

    await screen.findByText("Your parcel is now in transit");
    expect(screen.queryByRole("button", { name: /mark all as read/i })).not.toBeInTheDocument();
  });

  it("shows pagination when there are multiple pages", async () => {
    vi.mocked(notificationsApi.myFeed).mockResolvedValue(
      makePage([makeItem()], { totalPages: 3, totalElements: 50 }),
    );
    renderPage();

    expect(await screen.findByText(/page 1 of 3/i)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /next/i })).toBeEnabled();
    expect(screen.getByRole("button", { name: /previous/i })).toBeDisabled();
  });

  it("shows an error when the feed request fails", async () => {
    vi.mocked(notificationsApi.myFeed).mockRejectedValue(new Error("Service unavailable"));
    vi.mocked(notificationsApi.myUnreadCount).mockRejectedValue(new Error("Service unavailable"));
    renderPage();

    expect(await screen.findByText("Service unavailable")).toBeInTheDocument();
  });
});
