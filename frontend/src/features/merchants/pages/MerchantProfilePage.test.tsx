import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { fetchMyMerchant, updateMyMerchantProfile } from "../api";
import type { Merchant } from "../types";
import { MerchantProfilePage } from "./MerchantProfilePage";

vi.mock("../api", () => ({
  fetchMyMerchant: vi.fn(),
  updateMyMerchantProfile: vi.fn(),
}));

const existingProfile: Merchant = {
  id: 12,
  businessName: "Ceylon Craft Corner (Demo)",
  businessType: "SOLE_PROPRIETORSHIP",
  registrationNumber: "DEMO-BR-M9-2026-001",
  businessAddress: "24 Galle Road, Colombo 03, Sri Lanka",
  businessDescription: "Demonstration business selling local crafts and gifts.",
  verificationDeclarationAccepted: true,
  contactName: "Alex Morgan",
  contactEmail: "merchant@example.test",
  contactPhone: "0771234567",
  status: "APPROVED",
  createdAt: "2026-11-08T10:00:00Z",
};

describe("MerchantProfilePage", () => {
  beforeEach(() => {
    vi.mocked(fetchMyMerchant).mockResolvedValue(existingProfile);
    vi.mocked(updateMyMerchantProfile).mockResolvedValue({ ...existingProfile, status: "PENDING" });
  });

  it("loads the merchant business and contact details", async () => {
    render(<MerchantProfilePage />);

    expect(await screen.findByDisplayValue("Ceylon Craft Corner (Demo)")).toBeInTheDocument();
    expect(screen.getByDisplayValue("DEMO-BR-M9-2026-001")).toBeInTheDocument();
    expect(screen.getByDisplayValue("merchant@example.test")).toHaveAttribute("readonly");
    expect(screen.getByText("APPROVED")).toBeInTheDocument();
  });

  it("submits the current business details and shows that admin review is required", async () => {
    const user = userEvent.setup();
    render(<MerchantProfilePage />);
    await screen.findByDisplayValue("Ceylon Craft Corner (Demo)");

    await user.click(screen.getByRole("button", { name: /save and resubmit for review/i }));

    expect(await screen.findByRole("status")).toHaveTextContent("pending admin review");
    expect(updateMyMerchantProfile).toHaveBeenCalledWith({
      businessName: "Ceylon Craft Corner (Demo)",
      businessType: "SOLE_PROPRIETORSHIP",
      registrationNumber: "DEMO-BR-M9-2026-001",
      businessAddress: "24 Galle Road, Colombo 03, Sri Lanka",
      businessDescription: "Demonstration business selling local crafts and gifts.",
      contactName: "Alex Morgan",
      contactPhone: "0771234567",
      verificationDeclarationAccepted: true,
    });
  });
});
