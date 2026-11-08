import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { createApiKey, fetchApiKeys, fetchMyMerchant, revokeApiKey } from "../api";
import type { ApiKey, Merchant } from "../types";
import { MerchantApiKeysPage } from "./MerchantApiKeysPage";

vi.mock("../api", () => ({
  createApiKey: vi.fn(),
  fetchApiKeys: vi.fn(),
  fetchMyMerchant: vi.fn(),
  revokeApiKey: vi.fn(),
}));

const approvedMerchant: Merchant = {
  id: 12,
  businessName: "Northstar Goods",
  registrationNumber: "BR-123456",
  contactName: "Alex Morgan",
  contactEmail: "orders@example.test",
  contactPhone: "0771234567",
  status: "APPROVED",
  createdAt: "2026-11-08T10:00:00Z",
};

const activeKey: ApiKey = {
  id: 91,
  label: "Production store",
  keyPrefix: "sd_live_ab12",
  active: true,
  createdAt: "2026-11-08T10:00:00Z",
  lastUsedAt: null,
};

describe("MerchantApiKeysPage", () => {
  beforeEach(() => {
    vi.mocked(fetchMyMerchant).mockResolvedValue(approvedMerchant);
    vi.mocked(fetchApiKeys).mockResolvedValue([activeKey]);
    vi.mocked(createApiKey).mockResolvedValue({ ...activeKey, id: 92, apiKey: "sd_test_fake_key_do_not_use" });
    vi.mocked(revokeApiKey).mockResolvedValue({ ...activeKey, active: false, revokedAt: "2026-11-08T11:00:00Z" });
  });

  it("lists keys without exposing raw key material", async () => {
    render(<MerchantApiKeysPage />);

    expect(await screen.findByText("Production store")).toBeInTheDocument();
    expect(screen.getByText(/sd_live_ab12/)).toBeInTheDocument();
    expect(screen.queryByText(/sd_test_fake_key_do_not_use/)).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /revoke/i })).toBeInTheDocument();
  });

  it("shows the raw generated key once and hides it when dismissed", async () => {
    render(<MerchantApiKeysPage />);
    await screen.findByText("Production store");
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Key label"), "Staging store");
    await user.click(screen.getByRole("button", { name: /generate key/i }));

    expect(await screen.findByText("sd_test_fake_key_do_not_use")).toBeInTheDocument();
    expect(createApiKey).toHaveBeenCalledWith("Staging store");
    await user.click(screen.getByRole("button", { name: "I saved the key" }));
    expect(screen.queryByText("sd_test_fake_key_do_not_use")).not.toBeInTheDocument();
  });

  it("revokes a key after confirmation and displays the revoked state", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(fetchApiKeys)
      .mockResolvedValueOnce([activeKey])
      .mockResolvedValueOnce([{ ...activeKey, active: false, revokedAt: "2026-11-08T11:00:00Z" }]);
    render(<MerchantApiKeysPage />);
    await screen.findByText("Production store");
    await userEvent.setup().click(screen.getByRole("button", { name: /revoke/i }));

    expect(revokeApiKey).toHaveBeenCalledWith(91);
    expect(await screen.findByText("Revoked")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /revoke/i })).not.toBeInTheDocument();
  });

  it("hides generation controls for a pending merchant", async () => {
    vi.mocked(fetchMyMerchant).mockResolvedValue({ ...approvedMerchant, status: "PENDING" });
    render(<MerchantApiKeysPage />);

    expect(await screen.findByText("Your merchant account is pending.")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /generate key/i })).not.toBeInTheDocument();
  });

  it("shows API errors from key creation", async () => {
    vi.mocked(createApiKey).mockRejectedValue(new Error("The API key limit has been reached"));
    render(<MerchantApiKeysPage />);
    await screen.findByText("Production store");
    const user = userEvent.setup();
    await user.type(screen.getByLabelText("Key label"), "Extra key");
    await user.click(screen.getByRole("button", { name: /generate key/i }));

    expect(await screen.findByRole("alert")).toHaveTextContent("The API key limit has been reached");
  });
});
