import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { BookModePage } from "./BookModePage";

const mockNavigate = vi.fn();
vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return { ...actual, useNavigate: () => mockNavigate };
});

describe("BookModePage", () => {
  beforeEach(() => vi.clearAllMocks());

  it("asks whether the customer wants to send or receive", () => {
    render(<MemoryRouter><BookModePage /></MemoryRouter>);
    expect(screen.getByText("I want to")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /send/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /receive/i })).toBeInTheDocument();
  });

  it("goes to the addresses page in send mode", async () => {
    render(<MemoryRouter><BookModePage /></MemoryRouter>);
    await userEvent.click(screen.getByRole("button", { name: /send/i }));
    expect(mockNavigate).toHaveBeenCalledWith("/parcels/new?mode=send");
  });

  it("goes to the addresses page in receive mode", async () => {
    render(<MemoryRouter><BookModePage /></MemoryRouter>);
    await userEvent.click(screen.getByRole("button", { name: /receive/i }));
    expect(mockNavigate).toHaveBeenCalledWith("/parcels/new?mode=receive");
  });
});
